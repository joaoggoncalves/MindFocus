package com.kis.mindfocus.feature.sessiondetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kis.mindfocus.R
import com.kis.mindfocus.core.model.DistractionEvent
import com.kis.mindfocus.core.model.DistractionType
import com.kis.mindfocus.feature.session.SessionErrorUi
import com.kis.mindfocus.feature.session.components.SessionErrorBanner
import com.kis.mindfocus.feature.session.messageRes
import com.kis.mindfocus.feature.session.toClockLabel
import com.kis.mindfocus.ui.theme.LocalSpacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SessionDetailScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = koinViewModel { parametersOf(sessionId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val errorMessages = SessionErrorUi.entries.associateWith { stringResource(it.messageRes) }

    LaunchedEffect(lifecycle, errorMessages) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is SessionDetailEffect.ShowError ->
                        snackbarHostState.showSnackbar(errorMessages.getValue(effect.error))
                }
            }
        }
    }

    SessionDetailContent(
        uiState = uiState,
        actions = viewModel,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionDetailContent(
    uiState: SessionDetailUiState,
    actions: SessionDetailActions,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                },
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

                    when {
                        uiState.isLoading -> CircularProgressIndicator()
                        uiState.isMissing -> Text(
                            text = stringResource(R.string.detail_missing),
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        else -> {
                            SessionSummaryCard(uiState)
                            SectionHeader(stringResource(R.string.detail_events_title))
                        }
                    }
                }
            }

            if (uiState.session != null && uiState.distractions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.detail_no_events),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH_DP.dp),
                    )
                }
            }

            items(uiState.distractions, key = { it.id }) { event ->
                Column(modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH_DP.dp)) {
                    DistractionEventRow(event)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(uiState: SessionDetailUiState, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    val session = uiState.session ?: return

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            LabelledValue(
                label = stringResource(R.string.detail_started_at),
                value = rememberDateTimeLabel(session.startedAt),
            )
            LabelledValue(
                label = stringResource(R.string.detail_duration),
                value = uiState.duration.toClockLabel(),
            )
            LabelledValue(
                label = stringResource(R.string.distraction_noise),
                value = session.noiseCount.toString(),
            )
            LabelledValue(
                label = stringResource(R.string.distraction_movement),
                value = session.movementCount.toString(),
            )
        }
    }
}

@Composable
private fun LabelledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DistractionEventRow(event: DistractionEvent, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    val isNoise = event.type == DistractionType.NOISE

    ListItem(
        modifier = modifier.semantics(mergeDescendants = true) {},
        leadingContent = {
            // Icon differs by shape as well as position, so type is not carried by colour alone.
            Icon(
                imageVector = if (isNoise) {
                    Icons.Outlined.GraphicEq
                } else {
                    Icons.AutoMirrored.Outlined.DirectionsRun
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(spacing.large),
            )
        },
        headlineContent = {
            Text(
                stringResource(
                    if (isNoise) R.string.distraction_noise else R.string.distraction_movement,
                ),
            )
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.detail_intensity, event.intensity),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = { Text(rememberTimeLabel(event.occurredAt)) },
    )
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun rememberDateTimeLabel(instant: Instant): String {
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
    }
    return remember(instant) { formatter.format(instant) }
}

@Composable
private fun rememberTimeLabel(instant: Instant): String {
    val formatter = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    return remember(instant) { formatter.format(instant) }
}

private const val CONTENT_MAX_WIDTH_DP = 640
