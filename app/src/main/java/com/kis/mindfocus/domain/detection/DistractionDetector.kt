package com.kis.mindfocus.domain.detection

import com.kis.mindfocus.core.model.DistractionType
import com.kis.mindfocus.core.util.Throttle
import com.kis.mindfocus.domain.sensor.SensorReading
import java.time.Duration
import java.time.Instant

/**
 * Threshold detection with a refractory period.
 *
 * Deliberately free of Android and coroutines: it takes a reading and a timestamp and returns a
 * verdict, which is what lets the detection rules be tested exhaustively with plain JUnit.
 */
class DistractionDetector(
    private val thresholds: DistractionThresholds = DistractionThresholds(),
    refractoryPeriod: Duration = DEFAULT_REFRACTORY_PERIOD,
) {
    /**
     * Without this, a single slammed door produces one distraction per sample for as long as it
     * takes the room to go quiet.
     */
    private val refractory = Throttle<DistractionType>(refractoryPeriod)

    fun evaluate(reading: SensorReading, now: Instant): DistractionSignal? {
        val threshold = thresholds.of(reading.type)
        if (reading.value < threshold) return null
        if (!refractory.tryAcquire(reading.type, now)) return null
        return DistractionSignal(type = reading.type, intensity = reading.value / threshold)
    }

    fun reset() = refractory.reset()

    companion object {
        val DEFAULT_REFRACTORY_PERIOD: Duration = Duration.ofSeconds(5)
    }
}
