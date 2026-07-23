package com.kis.mindfocus.domain.detection

import com.kis.mindfocus.core.model.DistractionEvent
import com.kis.mindfocus.core.model.DistractionType
import com.kis.mindfocus.core.util.IdGenerator
import com.kis.mindfocus.core.util.Throttle
import com.kis.mindfocus.domain.notification.DistractionNotifier
import com.kis.mindfocus.domain.repository.FocusSessionRepository
import com.kis.mindfocus.domain.sensor.MotionSource
import com.kis.mindfocus.domain.sensor.NoiseSource
import com.kis.mindfocus.domain.sensor.SensorReading
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.time.Clock
import java.time.Duration

/**
 * Joins both sensors to the detector, the store and the notifier.
 *
 * [monitor] runs until its coroutine is cancelled; cancelling it releases the microphone and
 * unregisters the accelerometer, because both sources are cold.
 */
class DistractionMonitor(
    private val noiseSource: NoiseSource,
    private val motionSource: MotionSource,
    private val detector: DistractionDetector,
    private val repository: FocusSessionRepository,
    private val notifier: DistractionNotifier,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    /**
     * Coarser than the detector's refractory period: every distraction is worth recording, but not
     * every one is worth interrupting the user for.
     */
    private val notificationThrottle: Throttle<DistractionType> =
        Throttle(DEFAULT_NOTIFICATION_INTERVAL),
) {
    suspend fun monitor(sessionId: String) {
        detector.reset()
        notificationThrottle.reset()

        merge(
            noiseSource.readings.map { SensorReading(DistractionType.NOISE, it) },
            motionSource.readings.map { SensorReading(DistractionType.MOVEMENT, it) },
        ).collect { reading -> handle(sessionId, reading) }
    }

    private suspend fun handle(sessionId: String, reading: SensorReading) {
        val now = clock.instant()
        val signal = detector.evaluate(reading, now) ?: return

        repository.recordDistraction(
            sessionId = sessionId,
            event = DistractionEvent(
                id = idGenerator.newId(),
                type = signal.type,
                occurredAt = now,
                intensity = signal.intensity,
            ),
        )

        if (notificationThrottle.tryAcquire(signal.type, now)) {
            notifier.notifyDistraction(signal)
        }
    }

    companion object {
        val DEFAULT_NOTIFICATION_INTERVAL: Duration = Duration.ofSeconds(30)
    }
}
