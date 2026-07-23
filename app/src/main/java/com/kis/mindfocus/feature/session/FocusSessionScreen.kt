package com.kis.mindfocus.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kis.mindfocus.R
import com.kis.mindfocus.feature.session.components.DistractionStatsRow
import com.kis.mindfocus.feature.session.components.SessionControlCard
import com.kis.mindfocus.feature.session.components.SessionErrorBanner
import com.kis.mindfocus.feature.session.components.SessionHistoryEmptyState
import com.kis.mindfocus.feature.session.components.SessionHistoryItem
import com.kis.mindfocus.ui.theme.LocalSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun FocusSessionScreen(
    onNavigateToSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusSessionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Resolved up front: stringResource needs composable scope, the effect collector does not have it.
    val messages = SessionMessageUi.entries.associateWith { stringResource(it.messageRes) }
    val errorMessages = SessionErrorUi.entries.associateWith { stringResource(it.messageRes) }

    // Drives sensor monitoring: the mic and accelerometer are only worth running while this screen
    // is actually in front of the user in order to preserve battery.
    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenVisible()
        onPauseOrDispose { viewModel.onScreenHidden() }
    }

    LaunchedEffect(lifecycle, messages, errorMessages) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                val text = when (effect) {
                    is FocusSessionEffect.ShowMessage -> messages.getValue(effect.message)
                    is FocusSessionEffect.ShowError -> errorMessages.getValue(effect.error)
                }
                snackbarHostState.showSnackbar(text)
            }
        }
    }

    FocusSessionContent(
        uiState = uiState,
        actions = viewModel,
        snackbarHostState = snackbarHostState,
        onSessionClick = onNavigateToSession,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FocusSessionContent(
    uiState: FocusSessionUiState,
    actions: FocusSessionActions,
    snackbarHostState: SnackbarHostState,
    /**
     * Navigation is not a ViewModel decision, so it arrives as a plain lambda rather than joining
     * [FocusSessionActions] — the ViewModel has nothing to contribute to it.
     */
    onSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val permissions = rememberFocusPermissionsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.session_title)) },
                actions = {
                    IconButton(onClick = actions::onRefresh, enabled = !uiState.isRefreshing) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                ) {
                    uiState.error?.let { error ->
                        SessionErrorBanner(error = error, onDismiss = actions::onErrorDismissed)
                    }

                    SessionControlCard(
                        isActive = uiState.isSessionActive,
                        elapsed = uiState.elapsed,
                        onStart = {
                            if (permissions.hasMicrophone) {
                                actions.onStartSession()
                            } else {
                                permissions.request()
                            }
                        },
                        onStop = actions::onStopSession,
                    )

                    if (!permissions.hasMicrophone) {
                        MicrophoneRationale(onGrant = permissions::request)
                    }

                    SectionHeader(text = stringResource(R.string.distractions_title))
                    DistractionStatsRow(
                        noiseCount = uiState.noiseCount,
                        movementCount = uiState.movementCount,
                    )

                    SectionHeader(text = stringResource(R.string.history_title))
                }
            }

            when {
                uiState.isLoading -> item {
                    CircularProgressIndicator(modifier = Modifier.padding(spacing.large))
                }

                uiState.history.isEmpty() -> item {
                    SessionHistoryEmptyState(
                        modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH_DP.dp),
                    )
                }

                else -> items(uiState.history, key = { it.id }) { summary ->
                    Column(modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH_DP.dp)) {
                        SessionHistoryItem(
                            summary = summary,
                            onClick = onSessionClick,
                            onRetrySync = actions::onRetrySync,
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        // Lets screen-reader users jump between sections instead of reading everything in order.
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun MicrophoneRationale(onGrant: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        Text(
            text = stringResource(R.string.permission_microphone_rationale),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onGrant) {
            Text(stringResource(R.string.permission_grant))
        }
    }
}

private val SessionMessageUi.messageRes: Int
    get() = when (this) {
        SessionMessageUi.SessionSaved -> R.string.message_session_saved
        SessionMessageUi.SyncSucceeded -> R.string.message_sync_succeeded
        SessionMessageUi.SyncFailed -> R.string.message_sync_failed
    }

private const val CONTENT_MAX_WIDTH_DP = 640
