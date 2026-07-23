package com.kis.mindfocus.testing

import com.kis.mindfocus.domain.detection.DistractionSignal
import com.kis.mindfocus.domain.notification.DistractionNotifier
import com.kis.mindfocus.domain.sensor.MotionSource
import com.kis.mindfocus.domain.sensor.NoiseSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

class FakeNoiseSource(override val readings: Flow<Float> = emptyFlow()) : NoiseSource {
    constructor(vararg values: Float) : this(flowOf(*values.toTypedArray()))
}

class FakeMotionSource(override val readings: Flow<Float> = emptyFlow()) : MotionSource {
    constructor(vararg values: Float) : this(flowOf(*values.toTypedArray()))
}

class RecordingNotifier : DistractionNotifier {
    val signals = mutableListOf<DistractionSignal>()
    override fun notifyDistraction(signal: DistractionSignal) {
        signals += signal
    }
}
