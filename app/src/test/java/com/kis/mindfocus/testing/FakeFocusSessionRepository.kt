package com.kis.mindfocus.testing

import com.kis.mindfocus.core.error.DataError
import com.kis.mindfocus.core.model.DistractionEvent
import com.kis.mindfocus.core.model.FocusSession
import com.kis.mindfocus.domain.repository.FocusSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock

class FakeFocusSessionRepository(private val clock: Clock) : FocusSessionRepository {

    private val sessions = MutableStateFlow<List<FocusSession>>(emptyList())
    private var nextId = 0

    /** When set, every suspending call fails with this instead of succeeding. */
    var failure: DataError? = null

    override fun observeSessions(): Flow<List<FocusSession>> = sessions

    override fun observeSession(id: String): Flow<FocusSession?> =
        sessions.map { list -> list.firstOrNull { it.id == id } }

    override fun observeActiveSession(): Flow<FocusSession?> =
        sessions.map { list -> list.firstOrNull { it.isActive } }

    /** Idempotent, mirroring the real repository's one-active-session invariant. */
    override suspend fun startSession(): Result<FocusSession> = orFail {
        sessions.value.firstOrNull { it.isActive } ?: FocusSession(
            id = "session-${nextId++}",
            startedAt = clock.instant(),
            endedAt = null,
        ).also { sessions.value += it }
    }

    override suspend fun endSession(sessionId: String): Result<FocusSession> = orFail {
        val ended = requireSession(sessionId).copy(endedAt = clock.instant())
        sessions.value = sessions.value.map { if (it.id == sessionId) ended else it }
        ended
    }

    override suspend fun recordDistraction(
        sessionId: String,
        event: DistractionEvent,
    ): Result<Unit> = orFail {
        val updated = requireSession(sessionId).let { it.copy(distractions = it.distractions + event) }
        sessions.value = sessions.value.map { if (it.id == sessionId) updated else it }
    }

    override suspend fun refreshSessions(): Result<Unit> = orFail { }

    override suspend fun refreshSession(id: String): Result<Unit> = orFail {
        requireSession(id)
    }

    override suspend fun syncSession(sessionId: String): Result<Unit> = orFail {
        val synced = requireSession(sessionId).copy(isSynced = true)
        sessions.value = sessions.value.map { if (it.id == sessionId) synced else it }
    }

    private fun requireSession(id: String): FocusSession =
        sessions.value.firstOrNull { it.id == id } ?: throw DataError.NotFound(id)

    private inline fun <T> orFail(block: () -> T): Result<T> =
        failure?.let { Result.failure(it) } ?: runCatching(block)
}
