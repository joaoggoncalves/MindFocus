package com.vonbraunlabs.mindfocus.di

import com.vonbraunlabs.mindfocus.data.local.dao.FocusSessionDao
import io.mockk.mockk
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.check.checkModules

/**
 * Koin resolves at runtime, so a missing binding would otherwise surface as a crash on first use.
 * This is what replaces Hilt's compile-time graph check — keep it in CI.
 *
 * `checkModules` is deprecated in favour of `verify()`, but `verify()` only walks constructors it
 * can see: our definitions are lambdas returning interfaces, so it passes vacuously. `checkModules`
 * actually instantiates the graph, which is the check worth having.
 *
 * `databaseModule` is swapped for a fake DAO — opening Room needs a real `Context`, which would
 * drag Robolectric into an otherwise plain JVM test.
 */
class KoinModuleVerificationTest {

    private val fakeDatabaseModule = module {
        single<FocusSessionDao> { mockk(relaxed = true) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `every declaration in the app graph resolves`() {
        koinApplication {
            modules(coroutinesModule, networkModule, dataModule, fakeDatabaseModule)
        }.checkModules()
    }
}
