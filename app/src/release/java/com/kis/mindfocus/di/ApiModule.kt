package com.kis.mindfocus.di

import com.kis.mindfocus.data.remote.api.FocusSessionApi
import org.koin.dsl.module
import retrofit2.Retrofit

/** Release half of the API binding; the debug source set swaps in an in-memory fake. */
val apiModule = module {
    single<FocusSessionApi> { get<Retrofit>().create(FocusSessionApi::class.java) }
}
