package com.vonbraunlabs.mindfocus.di

import com.vonbraunlabs.mindfocus.core.util.IdGenerator
import com.vonbraunlabs.mindfocus.core.util.UuidIdGenerator
import com.vonbraunlabs.mindfocus.data.repository.DefaultFocusSessionRepository
import com.vonbraunlabs.mindfocus.domain.repository.FocusSessionRepository
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
