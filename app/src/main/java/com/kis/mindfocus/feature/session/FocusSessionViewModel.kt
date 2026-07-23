package com.kis.mindfocus.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kis.mindfocus.core.model.FocusSession
import com.kis.mindfocus.domain.detection.DistractionMonitor
import com.kis.mindfocus.domain.repository.FocusSessionRepository
import com.kis.mindfocus.feature.session.model.toSummaryUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FocusSessionViewModel(
    private val repository: FocusSessionRepository,
    private val monitor: DistractionMonitor,
    private val clock: Clock,
    /**
     * Sync outlives the screen: a user who stops a session and immediately backs out should still
     * get their data pushed. `viewModelScope` would cancel that mid-flight.
     */
    private val applicationScope: CoroutineScope,
) : ViewModel(), FocusSessionActions {

    private val transientState = MutableStateFlow(TransientState())

    private val screenVisible = MutableStateFlow(false)

    private val activeSession: Flow<FocusSession?> = repository.observeActiveSession()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    /**
     * Ticks only while a session is running — an idle screen does no work at all, which is the
     * cheapest version of this that still shows a live timer.
     */
    private val elapsed: Flow<Duration> = activeSession.flatMapLatest { session ->
        if (session == null) {
            flowOf(Duration.ZERO)
        } else {
            flow {
                while (true) {
                    emit(session.durationAt(clock.instant()))
                    delay(1.seconds)
                }
            }
        }
    }

    val uiState: StateFlow<FocusSessionUiState> = combine(
        activeSession,
        elapsed,
        repository.observeSessions(),
        transientState,
    ) { active, elapsedDuration, sessions, transient ->
        FocusSessionUiState(
            activeSession = active,
            history = sessions.filterNot { it.isActive }.map { it.toSummaryUi() },
            elapsed = elapsedDuration,
            isLoading = false,
            isRefreshing = transient.isRefreshing,
            error = transient.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FocusSessionUiState(),
    )

    private val _effects = Channel<FocusSessionEffect>(Channel.BUFFERED)
    val effects: Flow<FocusSessionEffect> = _effects.receiveAsFlow()

    init {
        refreshSessions(userInitiated = false)
        monitorWhileVisibleAndActive()
    }

    /**
     * Sensors run only when a session is active *and* the screen is on top. Two reasons: the OS
     * revokes microphone access to backgrounded apps anyway, so recording would silently return
     * nothing, and leaving the accelerometer registered would drain the battery for no data.
     *
     * The `map` to a plain id matters — `activeSession` re-emits a new object on every recorded
     * distraction, and without `distinctUntilChanged` each one would tear down and restart the
     * microphone.
     */
    private fun monitorWhileVisibleAndActive() {
        viewModelScope.launch {
            combine(activeSession, screenVisible) { session, visible ->
                session?.id?.takeIf { visible }
            }
                .distinctUntilChanged()
                .collectLatest { sessionId -> sessionId?.let { monitor.monitor(it) } }
        }
    }

    fun onScreenVisible() {
        screenVisible.value = true
    }

    fun onScreenHidden() {
        screenVisible.value = false
    }

    override fun onStartSession() {
        viewModelScope.launch {
            repository.startSession().onFailure(::publishError)
        }
    }

    override fun onStopSession() {
        val sessionId = uiState.value.activeSession?.id ?: return
        viewModelScope.launch {
            repository.endSession(sessionId)
                .onSuccess {
                    _effects.trySend(
                        FocusSessionEffect.ShowMessage(SessionMessageUi.SessionSaved),
                    )
                    syncInBackground(sessionId, announceSuccess = false)
                }
                .onFailure(::publishError)
        }
    }

    override fun onRefresh() = refreshSessions(userInitiated = true)

    /**
     * A refresh the user did not ask for stays silent when it fails. The startup refresh is
     * opportunistic, it's already saved offline, so announcing "no connection"
     * before the user has touched anything is unnecessary.
     */
    private fun refreshSessions(userInitiated: Boolean) {
        viewModelScope.launch {
            transientState.update { it.copy(isRefreshing = true) }
            repository.refreshSessions().onFailure { throwable ->
                if (userInitiated) publishError(throwable)
            }
            transientState.update { it.copy(isRefreshing = false) }
        }
    }

    override fun onRetrySync(sessionId: String) {
        syncInBackground(sessionId, announceSuccess = true)
    }

    override fun onErrorDismissed() {
        transientState.update { it.copy(error = null) }
    }

    private fun syncInBackground(sessionId: String, announceSuccess: Boolean) {
        applicationScope.launch {
            val message = repository.syncSession(sessionId).fold(
                onSuccess = { if (announceSuccess) SessionMessageUi.SyncSucceeded else null },
                // A failed push is not a lost session — it stays unsynced and stays retryable, so
                // this is a passing message rather than the error banner.
                onFailure = { SessionMessageUi.SyncFailed },
            )
            message?.let { _effects.trySend(FocusSessionEffect.ShowMessage(it)) }
        }
    }

    /**
     * Routes by consequence, not by origin: a failure that risks the user's data holds the screen
     * until dismissed, one that merely failed to reach the server passes by in a snackbar.
     */
    private fun publishError(throwable: Throwable) {
        val error = throwable.toSessionErrorUi()
        if (error.isTransient) {
            _effects.trySend(FocusSessionEffect.ShowError(error))
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
