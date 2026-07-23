package com.kis.mindfocus.data.remote.mapper

import com.kis.mindfocus.core.model.DistractionEvent
import com.kis.mindfocus.core.model.DistractionType
import com.kis.mindfocus.core.model.FocusSession
import com.kis.mindfocus.data.remote.dto.DistractionEventDto
import com.kis.mindfocus.data.remote.dto.FocusSessionDto
import java.time.Instant

fun FocusSessionDto.toDomain(): FocusSession = FocusSession(
    id = id,
    startedAt = Instant.parse(startedAt),
    endedAt = endedAt?.let(Instant::parse),
    // A distraction type the client doesn't know about is dropped rather than failing the whole
    // session: a newer server should not brick an older client.
    distractions = distractions.mapNotNull(DistractionEventDto::toDomainOrNull),
    isSynced = true,
)

fun DistractionEventDto.toDomainOrNull(): DistractionEvent? {
    val parsedType = DistractionType.entries.firstOrNull { it.name.equals(type, ignoreCase = true) }
        ?: return null
    return DistractionEvent(
        id = id,
        type = parsedType,
        occurredAt = Instant.parse(occurredAt),
        intensity = intensity,
    )
}

fun FocusSession.toDto(): FocusSessionDto = FocusSessionDto(
    id = id,
    startedAt = startedAt.toString(),
    endedAt = endedAt?.toString(),
    distractions = distractions.map(DistractionEvent::toDto),
)

fun DistractionEvent.toDto(): DistractionEventDto = DistractionEventDto(
    id = id,
    type = type.name,
    occurredAt = occurredAt.toString(),
    intensity = intensity,
)
