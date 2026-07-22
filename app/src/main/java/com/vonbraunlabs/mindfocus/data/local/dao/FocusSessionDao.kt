package com.vonbraunlabs.mindfocus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vonbraunlabs.mindfocus.data.local.entity.DistractionEventEntity
import com.vonbraunlabs.mindfocus.data.local.entity.FocusSessionEntity
import com.vonbraunlabs.mindfocus.data.local.entity.SessionWithDistractions
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

    @Transaction
    suspend fun upsertSessionsWithDistractions(sessions: List<SessionWithDistractions>) {
        upsertSessions(sessions.map { it.session })
        upsertDistractions(sessions.flatMap { it.distractions })
    }
}
