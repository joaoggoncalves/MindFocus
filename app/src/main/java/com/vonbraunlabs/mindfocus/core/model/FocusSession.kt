package com.vonbraunlabs.mindfocus.core.model

import java.time.Duration
import java.time.Instant

data class FocusSession(
    val id: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val distractions: List<DistractionEvent> = emptyList(),
) {
    val isActive: Boolean get() = endedAt == null

    val distractionCount: Int get() = distractions.size

    val noiseCount: Int get() = countOf(DistractionType.NOISE)

    val movementCount: Int get() = countOf(DistractionType.MOVEMENT)

    /** A running session has no end, so the caller supplies the reference point. */
    fun durationAt(now: Instant): Duration = Duration.between(startedAt, endedAt ?: now)

    fun countOf(type: DistractionType): Int = distractions.count { it.type == type }
}
