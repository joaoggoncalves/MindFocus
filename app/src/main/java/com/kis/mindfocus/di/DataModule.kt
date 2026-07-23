package com.kis.mindfocus.di

import com.kis.mindfocus.core.util.IdGenerator
import com.kis.mindfocus.core.util.UuidIdGenerator
import com.kis.mindfocus.data.repository.DefaultFocusSessionRepository
import com.kis.mindfocus.domain.repository.FocusSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.dsl.module

val dataModule = module {

    single<IdGenerator> { UuidIdGenerator() }

    single<FocusSessionRepository> {
        DefaultFocusSessionRepository(
            dao = get(),
            api = get(),
            clock = get(),
            idGenerator = get(),
            ioDispatcher = get<CoroutineDispatcher>(IoDispatcher),
        )
    }
}
