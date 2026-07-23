package com.kis.mindfocus.feature.session

import androidx.compose.runtime.Immutable
import com.kis.mindfocus.core.error.DataError
import com.kis.mindfocus.core.model.FocusSession
import com.kis.mindfocus.feature.session.model.SessionSummaryUi
import java.time.Duration

@Immutable
data class FocusSessionUiState(
    // Persisted snapshot — everything below comes from the repository.
    val activeSession: FocusSession? = null,
    val history: List<SessionSummaryUi> = emptyList(),
    // Transient UI-only.
    val elapsed: Duration = Duration.ZERO,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: SessionErrorUi? = null,
) {
    // Derived — getters so no `copy()` can produce a state that contradicts itself.
    val isSessionActive: Boolean get() = activeSession != null
    val noiseCount: Int get() = activeSession?.noiseCount ?: 0
    val movementCount: Int get() = activeSession?.movementCount ?: 0
    val distractionCount: Int get() = activeSession?.distractionCount ?: 0
}

/**
 * The error as the UI understands it. The ViewModel does not build user-facing strings — that
 * would make it hold a `Context` and would put localisation outside the resource system.
 */
enum class SessionErrorUi {
    NoConnection,
    ServerUnavailable,
    StorageFailure,
    SessionMissing,
    Unexpected;

    /**
     * Whether the failure passes by rather than persisting.
     *
     * Reaching the API is optional in a local-first app: the device already holds every session, so
     * a network or server failure costs the user nothing and does not deserve a standing banner.
     * Storage failures do — those put data at risk.
     */
    val isTransient: Boolean
        get() = this == NoConnection || this == ServerUnavailable
}

fun Throwable.toSessionErrorUi(): SessionErrorUi = when (this) {
    is DataError.Network -> SessionErrorUi.NoConnection
    is DataError.Server -> SessionErrorUi.ServerUnavailable
    is DataError.Local -> SessionErrorUi.StorageFailure
    is DataError.NotFound -> SessionErrorUi.SessionMissing
    else -> SessionErrorUi.Unexpected
}
