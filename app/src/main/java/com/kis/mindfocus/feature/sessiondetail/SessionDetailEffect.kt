package com.kis.mindfocus.feature.sessiondetail

import com.kis.mindfocus.feature.session.SessionErrorUi

sealed interface SessionDetailEffect {
    data class ShowError(val error: SessionErrorUi) : SessionDetailEffect
}
