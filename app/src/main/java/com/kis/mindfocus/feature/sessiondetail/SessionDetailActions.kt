package com.kis.mindfocus.feature.sessiondetail

import androidx.compose.runtime.Stable

@Stable
interface SessionDetailActions {
    fun onRefresh() = Unit
    fun onErrorDismissed() = Unit
}
