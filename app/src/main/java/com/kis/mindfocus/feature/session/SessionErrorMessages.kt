package com.kis.mindfocus.feature.session

import androidx.annotation.StringRes
import com.kis.mindfocus.R

/** Shared by the persistent banner and the transient snackbar, so wording cannot drift apart. */
@get:StringRes
internal val SessionErrorUi.messageRes: Int
    get() = when (this) {
        SessionErrorUi.NoConnection -> R.string.error_no_connection
        SessionErrorUi.ServerUnavailable -> R.string.error_server_unavailable
        SessionErrorUi.StorageFailure -> R.string.error_storage
        SessionErrorUi.SessionMissing -> R.string.error_session_missing
        SessionErrorUi.Unexpected -> R.string.error_unexpected
    }
