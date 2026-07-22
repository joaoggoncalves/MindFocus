package com.vonbraunlabs.mindfocus.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "started_at") val startedAtEpochMillis: Long,
    @ColumnInfo(name = "ended_at") val endedAtEpochMillis: Long?,
    /** False until the session has been accepted by the API; drives retryable sync. */
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
)

@Entity(
    tableName = "distraction_events",
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("session_id")],
)
data class DistractionEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "occurred_at") val occurredAtEpochMillis: Long,
    @ColumnInfo(name = "intensity") val intensity: Float,
)

data class SessionWithDistractions(
    @Embedded val session: FocusSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "session_id")
    val distractions: List<DistractionEventEntity>,
)
