package com.kis.mindfocus.di

import com.kis.mindfocus.domain.detection.DistractionDetector
import com.kis.mindfocus.domain.detection.DistractionMonitor
import com.kis.mindfocus.domain.notification.DistractionNotifier
import com.kis.mindfocus.domain.sensor.MotionSource
import com.kis.mindfocus.domain.sensor.NoiseSource
import com.kis.mindfocus.notification.AndroidDistractionNotifier
import com.kis.mindfocus.sensor.AndroidMotionSource
import com.kis.mindfocus.sensor.AndroidNoiseSource
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val detectionModule = module {

    single<NoiseSource> {
        AndroidNoiseSource(
            context = androidContext(),
            ioDispatcher = get<CoroutineDispatcher>(IoDispatcher),
        )
    }

    single<MotionSource> { AndroidMotionSource(androidContext()) }

    single<DistractionNotifier> { AndroidDistractionNotifier(androidContext()) }

    factory { DistractionDetector() }

    factory {
        DistractionMonitor(
            noiseSource = get(),
            motionSource = get(),
            detector = get(),
            repository = get(),
            notifier = get(),
            idGenerator = get(),
            clock = get(),
        )
    }
}
