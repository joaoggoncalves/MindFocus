package com.kis.mindfocus.domain.detection

import com.kis.mindfocus.core.model.DistractionType
import com.kis.mindfocus.core.util.IdGenerator
import com.kis.mindfocus.core.util.Throttle
import com.kis.mindfocus.testing.FakeFocusSessionRepository
import com.kis.mindfocus.testing.FakeMotionSource
import com.kis.mindfocus.testing.FakeNoiseSource
import com.kis.mindfocus.testing.RecordingNotifier
import com.kis.mindfocus.testing.VirtualClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class DistractionMonitorTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = VirtualClock(dispatcher.scheduler)
    private val repository = FakeFocusSessionRepository(clock)
    private val notifier = RecordingNotifier()
    private var nextId = 0

    private fun monitor(
        noise: FakeNoiseSource = FakeNoiseSource(),
        motion: FakeMotionSource = FakeMotionSource(),
        notificationInterval: Duration = Duration.ofSeconds(30),
    ) = DistractionMonitor(
        noiseSource = noise,
        motionSource = motion,
        // Refractory disabled: these tests are about the monitor's wiring, and the detector's own
        // suppression rules are covered in DistractionDetectorTest.
        detector = DistractionDetector(
            thresholds = DistractionThresholds(noiseAmplitude = 0.2f, movementAcceleration = 2f),
            refractoryPeriod = Duration.ZERO,
        ),
        repository = repository,
        notifier = notifier,
        idGenerator = IdGenerator { "event-${nextId++}" },
        clock = clock,
        notificationThrottle = Throttle(notificationInterval),
    )

    private suspend fun startSession(): String =
        repository.startSession().getOrThrow().id

    @Test
    fun `readings past the threshold are recorded against the session`() = runTest(dispatcher) {
        val sessionId = startSession()

        monitor(noise = FakeNoiseSource(0.05f, 0.5f, 0.01f)).monitor(sessionId)

        val recorded = repository.observeSession(sessionId).first()!!.distractions
        assertEquals(1, recorded.size)
        assertEquals(DistractionType.NOISE, recorded.single().type)
    }

    @Test
    fun `both sensors feed the same session`() = runTest(dispatcher) {
        val sessionId = startSession()

        monitor(
            noise = FakeNoiseSource(0.5f),
            motion = FakeMotionSource(9f),
        ).monitor(sessionId)

        val session = repository.observeSession(sessionId).first()!!
        assertEquals(1, session.noiseCount)
        assertEquals(1, session.movementCount)
    }

    @Test
    fun `every distraction is recorded but notifications are throttled`() = runTest(dispatcher) {
        val sessionId = startSession()

        // The virtual clock does not advance during a burst, so all four land in one window.
        monitor(noise = FakeNoiseSource(0.5f, 0.6f, 0.7f, 0.8f)).monitor(sessionId)

        assertEquals(4, repository.observeSession(sessionId).first()!!.distractionCount)
        assertEquals(1, notifier.signals.size)
    }

    @Test
    fun `noise and movement notify independently`() = runTest(dispatcher) {
        val sessionId = startSession()

        monitor(
            noise = FakeNoiseSource(0.5f, 0.6f),
            motion = FakeMotionSource(9f, 10f),
        ).monitor(sessionId)

        assertEquals(
            listOf(DistractionType.NOISE, DistractionType.MOVEMENT),
            notifier.signals.map { it.type }.sortedBy { it.ordinal },
        )
    }

    @Test
    fun `readings below the threshold are ignored entirely`() = runTest(dispatcher) {
        val sessionId = startSession()

        monitor(
            noise = FakeNoiseSource(0.01f, 0.05f),
            motion = FakeMotionSource(0.2f, 1.9f),
        ).monitor(sessionId)

        assertEquals(0, repository.observeSession(sessionId).first()!!.distractionCount)
        assertEquals(0, notifier.signals.size)
    }
}
