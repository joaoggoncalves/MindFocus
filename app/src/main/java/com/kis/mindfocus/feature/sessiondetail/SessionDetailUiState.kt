package com.kis.mindfocus.feature.sessiondetail

import androidx.compose.runtime.Immutable
import com.kis.mindfocus.core.model.DistractionEvent
import com.kis.mindfocus.core.model.FocusSession
import com.kis.mindfocus.feature.session.SessionErrorUi
import java.time.Duration

@Immutable
data class SessionDetailUiState(
    val session: FocusSession? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: SessionErrorUi? = null,
) {
    /** Newest first: the most recent interruption is the one the user is looking for. */
    val distractions: List<DistractionEvent>
        get() = session?.distractions?.sortedByDescending { it.occurredAt }.orEmpty()

    val duration: Duration
        get() = session?.let { it.durationAt(it.endedAt ?: it.startedAt) } ?: Duration.ZERO

    /** The session existed long enough to load but came back empty — deleted, or a bad deep link. */
    val isMissing: Boolean get() = !isLoading && session == null
}
