package com.kis.mindfocus.domain.detection

import com.kis.mindfocus.core.model.DistractionType
import com.kis.mindfocus.domain.sensor.SensorReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** No Android, no coroutines, no test dispatcher — the point of keeping detection in the domain. */
class DistractionDetectorTest {

    private val thresholds = DistractionThresholds(noiseAmplitude = 0.2f, movementAcceleration = 2f)
    private val start: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun detector(refractory: Duration = Duration.ofSeconds(5)) =
        DistractionDetector(thresholds, refractory)

    @Test
    fun `a reading below the threshold is not a distraction`() {
        val signal = detector().evaluate(SensorReading(DistractionType.NOISE, 0.19f), start)

        assertNull(signal)
    }

    @Test
    fun `a reading exactly at the threshold counts`() {
        val signal = detector().evaluate(SensorReading(DistractionType.NOISE, 0.2f), start)

        assertNotNull(signal)
        assertEquals(1f, signal!!.intensity, 0.001f)
    }

    @Test
    fun `intensity is measured relative to the threshold`() {
        val signal = detector().evaluate(SensorReading(DistractionType.MOVEMENT, 5f), start)

        assertEquals(2.5f, signal!!.intensity, 0.001f)
    }

    @Test
    fun `repeat readings inside the refractory period are suppressed`() {
        val detector = detector(refractory = Duration.ofSeconds(5))
        val loud = SensorReading(DistractionType.NOISE, 0.9f)

        assertNotNull(detector.evaluate(loud, start))
        assertNull(detector.evaluate(loud, start.plusSeconds(1)))
        assertNull(detector.evaluate(loud, start.plusSeconds(4)))
        assertNotNull(detector.evaluate(loud, start.plusSeconds(5)))
    }

    @Test
    fun `the refractory period is tracked per type`() {
        val detector = detector()

        assertNotNull(detector.evaluate(SensorReading(DistractionType.NOISE, 0.9f), start))
        // Movement is unaffected by a recent noise event.
        assertNotNull(detector.evaluate(SensorReading(DistractionType.MOVEMENT, 9f), start))
    }

    @Test
    fun `reset clears the refractory state between sessions`() {
        val detector = detector()
        val loud = SensorReading(DistractionType.NOISE, 0.9f)
        detector.evaluate(loud, start)

        detector.reset()

        assertNotNull(detector.evaluate(loud, start.plusMillis(10)))
    }
}
