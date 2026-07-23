package com.kis.mindfocus.data.repository

import com.kis.mindfocus.core.error.DataError
import com.kis.mindfocus.core.model.DistractionEvent
import com.kis.mindfocus.core.model.FocusSession
import com.kis.mindfocus.core.util.IdGenerator
import com.kis.mindfocus.data.local.dao.FocusSessionDao
import com.kis.mindfocus.data.local.mapper.toDomain
import com.kis.mindfocus.data.local.mapper.toEntity
import com.kis.mindfocus.data.local.mapper.toEntityWithDistractions
import com.kis.mindfocus.data.remote.api.FocusSessionApi
import com.kis.mindfocus.data.remote.mapper.toDomain
import com.kis.mindfocus.data.remote.mapper.toDto
import com.kis.mindfocus.data.runCatchingData
import com.kis.mindfocus.domain.repository.FocusSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock

class DefaultFocusSessionRepository(
    private val dao: FocusSessionDao,
    private val api: FocusSessionApi,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val ioDispatcher: CoroutineDispatcher,
) : FocusSessionRepository {

    override fun observeSessions(): Flow<List<FocusSession>> =
        dao.observeSessions().map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observeSession(id: String): Flow<FocusSession?> =
        dao.observeSession(id).map { it?.toDomain() }.flowOn(ioDispatcher)

    override fun observeActiveSession(): Flow<FocusSession?> =
        dao.observeActiveSession().map { it?.toDomain() }.flowOn(ioDispatcher)

    override suspend fun startSession(): Result<FocusSession> = withContext(ioDispatcher) {
        runCatchingData {
            val session = FocusSession(
                id = idGenerator.newId(),
                startedAt = clock.instant(),
                endedAt = null,
            )
            dao.insertSession(session.toEntity())
            session
        }
    }

    override suspend fun endSession(sessionId: String): Result<FocusSession> =
        withContext(ioDispatcher) {
            runCatchingData {
                dao.markEnded(sessionId, clock.instant().toEpochMilli())
                dao.getSession(sessionId)?.toDomain() ?: throw DataError.NotFound(sessionId)
            }
        }

    override suspend fun recordDistraction(
        sessionId: String,
        event: DistractionEvent,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatchingData { dao.insertDistraction(event.toEntity(sessionId)) }
    }

    override suspend fun refreshSessions(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingData {
            val remote = api.getSessions().map { it.toDomain().toEntityWithDistractions(isSynced = true) }
            dao.upsertSessionsWithDistractions(remote)
        }
    }

    override suspend fun refreshSession(id: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatchingData {
                val remote = api.getSession(id).toDomain().toEntityWithDistractions(isSynced = true)
                dao.upsertSessionsWithDistractions(listOf(remote))
            }
        }

    override suspend fun syncSession(sessionId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatchingData {
                val session = dao.getSession(sessionId)?.toDomain()
                    ?: throw DataError.NotFound(sessionId)
                api.createSession(session.toDto())
                dao.markSynced(sessionId, isSynced = true)
            }
        }
}
