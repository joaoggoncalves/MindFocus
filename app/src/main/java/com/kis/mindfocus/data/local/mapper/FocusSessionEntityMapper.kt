package com.kis.mindfocus.data.local.mapper

import com.kis.mindfocus.core.model.DistractionEvent
import com.kis.mindfocus.core.model.DistractionType
import com.kis.mindfocus.core.model.FocusSession
import com.kis.mindfocus.data.local.entity.DistractionEventEntity
import com.kis.mindfocus.data.local.entity.FocusSessionEntity
import com.kis.mindfocus.data.local.entity.SessionWithDistractions
import java.time.Instant

fun SessionWithDistractions.toDomain(): FocusSession = FocusSession(
    id = session.id,
    startedAt = Instant.ofEpochMilli(session.startedAtEpochMillis),
    endedAt = session.endedAtEpochMillis?.let(Instant::ofEpochMilli),
    distractions = distractions
        .mapNotNull(DistractionEventEntity::toDomainOrNull)
        .sortedBy { it.occurredAt },
    isSynced = session.isSynced,
)

fun DistractionEventEntity.toDomainOrNull(): DistractionEvent? {
    val parsedType = DistractionType.entries.firstOrNull { it.name == type } ?: return null
    return DistractionEvent(
        id = id,
        type = parsedType,
        occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis),
        intensity = intensity,
    )
}

fun FocusSession.toEntity(isSynced: Boolean = this.isSynced): FocusSessionEntity = FocusSessionEntity(
    id = id,
    startedAtEpochMillis = startedAt.toEpochMilli(),
    endedAtEpochMillis = endedAt?.toEpochMilli(),
    isSynced = isSynced,
)

fun FocusSession.toEntityWithDistractions(isSynced: Boolean = this.isSynced) = SessionWithDistractions(
    session = toEntity(isSynced),
    distractions = distractions.map { it.toEntity(sessionId = id) },
)

fun DistractionEvent.toEntity(sessionId: String): DistractionEventEntity = DistractionEventEntity(
    id = id,
    sessionId = sessionId,
    type = type.name,
    occurredAtEpochMillis = occurredAt.toEpochMilli(),
    intensity = intensity,
)
