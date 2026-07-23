package com.kis.mindfocus.feature.session

import androidx.compose.runtime.Stable

/**
 * The content composable depends on this rather than on the ViewModel, which keeps it pure and
 * previewable — previews pass `object : FocusSessionActions {}`.
 */
@Stable
interface FocusSessionActions {
    fun onStartSession() = Unit
    fun onStopSession() = Unit
    fun onRefresh() = Unit
    fun onRetrySync(sessionId: String) = Unit
    fun onErrorDismissed() = Unit
}
