package com.kis.mindfocus.feature.session.model

import androidx.compose.runtime.Immutable
import com.kis.mindfocus.core.model.FocusSession
import java.time.Duration
import java.time.Instant

/**
 * A completed session as the history list needs it. Instants and durations stay unformatted —
 * date and number formatting is locale-dependent, so it happens in the composable, not here.
 */
@Immutable
data class SessionSummaryUi(
    val id: String,
    val startedAt: Instant,
    val duration: Duration,
    val noiseCount: Int,
    val movementCount: Int,
    val isSynced: Boolean,
) {
    val distractionCount: Int get() = noiseCount + movementCount
}

fun FocusSession.toSummaryUi(): SessionSummaryUi = SessionSummaryUi(
    id = id,
    startedAt = startedAt,
    duration = durationAt(endedAt ?: startedAt),
    noiseCount = noiseCount,
    movementCount = movementCount,
    isSynced = isSynced,
)
