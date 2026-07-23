package com.kis.mindfocus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kis.mindfocus.data.local.entity.DistractionEventEntity
import com.kis.mindfocus.data.local.entity.FocusSessionEntity
import com.kis.mindfocus.data.local.entity.SessionWithDistractions
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Transaction
    @Query("SELECT * FROM focus_sessions ORDER BY started_at DESC")
    fun observeSessions(): Flow<List<SessionWithDistractions>>

    @Transaction
    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    fun observeSession(id: String): Flow<SessionWithDistractions?>

    @Transaction
    @Query("SELECT * FROM focus_sessions WHERE ended_at IS NULL ORDER BY started_at DESC LIMIT 1")
    fun observeActiveSession(): Flow<SessionWithDistractions?>

    @Transaction
    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getSession(id: String): SessionWithDistractions?

    @Query("SELECT * FROM focus_sessions WHERE ended_at IS NULL ORDER BY started_at DESC LIMIT 1")
    suspend fun getActiveSession(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE is_synced = 0")
    suspend fun getUnsyncedSessions(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDistraction(event: DistractionEventEntity)

    @Query("UPDATE focus_sessions SET ended_at = :endedAtEpochMillis WHERE id = :id")
    suspend fun markEnded(id: String, endedAtEpochMillis: Long)

    @Query("UPDATE focus_sessions SET is_synced = :isSynced WHERE id = :id")
    suspend fun markSynced(id: String, isSynced: Boolean)

    @Upsert
    suspend fun upsertSessions(sessions: List<FocusSessionEntity>)

    @Upsert
    suspend fun upsertDistractions(events: List<DistractionEventEntity>)

    /**
     * Enforces the one-active-session invariant, returning the id of whichever session is active
     * afterwards. The check and the insert share a transaction, so two concurrent starts cannot
     * both see "none active" — the second gets back the id the first inserted.
     */
    @Transaction
    suspend fun insertSessionIfNoneActive(session: FocusSessionEntity): String {
        getActiveSession()?.let { return it.id }
        insertSession(session)
        return session.id
    }

    @Transaction
    suspend fun upsertSessionsWithDistractions(sessions: List<SessionWithDistractions>) {
        upsertSessions(sessions.map { it.session })
        upsertDistractions(sessions.flatMap { it.distractions })
    }
}
