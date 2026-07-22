package com.vonbraunlabs.mindfocus.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import java.time.Clock

val coroutinesModule = module {
    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }
    single<CoroutineDispatcher>(DefaultDispatcher) { Dispatchers.Default }

    single<CoroutineScope>(ApplicationScope) {
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(DefaultDispatcher))
    }

    single<Clock> { Clock.systemUTC() }
}
