package com.kis.mindfocus.di

import com.kis.mindfocus.data.remote.api.FakeFocusSessionApi
import com.kis.mindfocus.data.remote.api.FocusSessionApi
import org.koin.dsl.module

/**
 * Debug half of the API binding. The release source set declares the same `apiModule` backed by
 * Retrofit, so the fake cannot reach a shipping build — it is not merely unused there, it is not
 * compiled into it.
 */
val apiModule = module {
    single<FocusSessionApi> { FakeFocusSessionApi(clock = get()) }
}
