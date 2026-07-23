package com.kis.mindfocus.di

import com.kis.mindfocus.feature.session.FocusSessionViewModel
import com.kis.mindfocus.feature.sessiondetail.SessionDetailViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureModule = module {
    viewModel {
        FocusSessionViewModel(
            repository = get(),
            monitor = get(),
            clock = get(),
            applicationScope = get<CoroutineScope>(ApplicationScope),
        )
    }

    // sessionId comes from the navigation route, so it is a runtime parameter, not a binding.
    viewModel { (sessionId: String) ->
        SessionDetailViewModel(sessionId = sessionId, repository = get())
    }
}
