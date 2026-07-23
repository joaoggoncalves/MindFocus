package com.kis.mindfocus.feature.sessiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kis.mindfocus.domain.repository.FocusSessionRepository
import com.kis.mindfocus.feature.session.SessionErrorUi
import com.kis.mindfocus.feature.session.toSessionErrorUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reads the session from the local store and uses `GET /session/{id}` only to pull the server's
 * copy into it. The screen therefore works offline, and a failed refresh changes nothing on it.
 */
class SessionDetailViewModel(
    private val sessionId: String,
    private val repository: FocusSessionRepository,
) : ViewModel(), SessionDetailActions {

    private val transientState = MutableStateFlow(TransientState())

    val uiState: StateFlow<SessionDetailUiState> = combine(
        repository.observeSession(sessionId),
        transientState,
    ) { session, transient ->
        SessionDetailUiState(
            session = session,
            isLoading = false,
            isRefreshing = transient.isRefreshing,
            error = transient.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SessionDetailUiState(),
    )

    private val _effects = Channel<SessionDetailEffect>(Channel.BUFFERED)
    val effects: Flow<SessionDetailEffect> = _effects.receiveAsFlow()

    init {
        refresh(userInitiated = false)
    }

    override fun onRefresh() = refresh(userInitiated = true)

    override fun onErrorDismissed() {
        transientState.update { it.copy(error = null) }
    }

    private fun refresh(userInitiated: Boolean) {
        viewModelScope.launch {
            transientState.update { it.copy(isRefreshing = true) }
            repository.refreshSession(sessionId).onFailure { throwable ->
                if (userInitiated) publishError(throwable)
            }
            transientState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun publishError(throwable: Throwable) {
        val error = throwable.toSessionErrorUi()
        if (error.isTransient) {
            _effects.trySend(SessionDetailEffect.ShowError(error))
        } else {
            transientState.update { it.copy(error = error) }
        }
    }

    private data class TransientState(
        val isRefreshing: Boolean = false,
        val error: SessionErrorUi? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
