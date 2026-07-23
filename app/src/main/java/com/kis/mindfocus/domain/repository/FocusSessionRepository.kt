package com.kis.mindfocus.domain.repository

import com.kis.mindfocus.core.model.DistractionEvent
import com.kis.mindfocus.core.model.FocusSession
import kotlinx.coroutines.flow.Flow

/**
 * Local-first: every write lands in the local store immediately and returns, then the session is
 * pushed to the API. A failed push is not a failed session — [syncSession] can be retried.
 */
interface FocusSessionRepository {

    fun observeSessions(): Flow<List<FocusSession>>

    fun observeSession(id: String): Flow<FocusSession?>

    fun observeActiveSession(): Flow<FocusSession?>

    suspend fun startSession(): Result<FocusSession>

    suspend fun endSession(sessionId: String): Result<FocusSession>

    suspend fun recordDistraction(sessionId: String, event: DistractionEvent): Result<Unit>

    /** Fetches from the API and reconciles into the local store. */
    suspend fun refreshSessions(): Result<Unit>

    /**
     * Single-session counterpart to [refreshSessions]. Reads still come from [observeSession] —
     * the local store stays the source of truth — this only pulls the server's copy into it.
     */
    suspend fun refreshSession(id: String): Result<Unit>

    /** Pushes a locally-stored session to the API and marks it synced on success. */
    suspend fun syncSession(sessionId: String): Result<Unit>
}
