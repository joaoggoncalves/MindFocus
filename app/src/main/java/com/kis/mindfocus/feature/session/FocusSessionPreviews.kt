package com.kis.mindfocus.feature.session

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.kis.mindfocus.core.model.FocusSession
import com.kis.mindfocus.feature.session.model.SessionSummaryUi
import com.kis.mindfocus.ui.theme.MindFocusTheme
import java.time.Duration
import java.time.Instant

/** The content composable takes `FocusSessionActions`, so previews need no ViewModel or Koin. */
private val NoOpActions = object : FocusSessionActions {}

@Preview(name = "Idle", showBackground = true)
@Preview(
    name = "Idle · dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(name = "Idle · large font", showBackground = true, fontScale = 1.8f)
@Composable
private fun FocusSessionIdlePreview() = PreviewScaffold(
    FocusSessionUiState(
        isLoading = false,
        history = listOf(
            SessionSummaryUi(
                id = "1",
                startedAt = Instant.parse("2026-07-21T09:15:00Z"),
                duration = Duration.ofMinutes(25),
                noiseCount = 3,
                movementCount = 1,
                isSynced = true,
            ),
            SessionSummaryUi(
                id = "2",
                startedAt = Instant.parse("2026-07-21T14:00:00Z"),
                duration = Duration.ofMinutes(50),
                noiseCount = 0,
                movementCount = 6,
                isSynced = false,
            ),
        ),
    ),
)

@Preview(name = "Active session", showBackground = true)
@Composable
private fun FocusSessionActivePreview() = PreviewScaffold(
    FocusSessionUiState(
        isLoading = false,
        activeSession = FocusSession(
            id = "active",
            startedAt = Instant.parse("2026-07-22T10:00:00Z"),
            endedAt = null,
        ),
        elapsed = Duration.ofMinutes(12).plusSeconds(34),
    ),
)

@Preview(name = "Error", showBackground = true)
@Composable
private fun FocusSessionErrorPreview() = PreviewScaffold(
    FocusSessionUiState(isLoading = false, error = SessionErrorUi.NoConnection),
)

@Composable
private fun PreviewScaffold(uiState: FocusSessionUiState) {
    MindFocusTheme {
        FocusSessionContent(
            uiState = uiState,
            actions = NoOpActions,
            snackbarHostState = remember { SnackbarHostState() },
            onSessionClick = {},
        )
    }
}
