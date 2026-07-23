package com.kis.mindfocus.di

import com.kis.mindfocus.data.local.dao.FocusSessionDao
import com.kis.mindfocus.domain.notification.DistractionNotifier
import com.kis.mindfocus.domain.sensor.MotionSource
import com.kis.mindfocus.domain.sensor.NoiseSource
import com.kis.mindfocus.testing.FakeMotionSource
import com.kis.mindfocus.testing.FakeNoiseSource
import com.kis.mindfocus.testing.RecordingNotifier
import io.mockk.mockk
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.check.checkModules

/**
 * Koin resolves at runtime, so a missing binding would otherwise surface as a crash on first use.
 * This test is meant to avoid that.
 *
 * `checkModules` is deprecated in favour of `verify()`, but `verify()` only walks constructors it
 * can see: our definitions are lambdas returning interfaces, so it passes vacuously. `checkModules`
 * actually instantiates the graph.
 *
 * The bindings are all fake this is just meant to test the dependency graph shape.
 */
class KoinModuleVerificationTest {

    private val fakePlatformModule = module {
        single<FocusSessionDao> { mockk(relaxed = true) }
        single<NoiseSource> { FakeNoiseSource() }
        single<MotionSource> { FakeMotionSource() }
        single<DistractionNotifier> { RecordingNotifier() }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `every declaration in the app graph resolves`() {
        koinApplication {
            modules(
                coroutinesModule,
                networkModule,
                dataModule,
                detectionModule,
                featureModule,
                fakePlatformModule,
            )
        }.checkModules()
    }
}
