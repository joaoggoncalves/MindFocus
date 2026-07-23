package com.kis.mindfocus.feature.session

sealed interface FocusSessionEffect {
    data class ShowMessage(val message: SessionMessageUi) : FocusSessionEffect

    /** A failure worth mentioning but not worth blocking on — see [SessionErrorUi.isTransient]. */
    data class ShowError(val error: SessionErrorUi) : FocusSessionEffect
}

enum class SessionMessageUi {
    SessionSaved,
    SyncSucceeded,
    SyncFailed,
}
