package com.kis.mindfocus.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ThrottleTest {

    private val start: Instant = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `the first acquire for a key always succeeds`() {
        val throttle = Throttle<String>(Duration.ofSeconds(10))

        assertTrue(throttle.tryAcquire("a", start))
    }

    @Test
    fun `a second acquire inside the interval is refused`() {
        val throttle = Throttle<String>(Duration.ofSeconds(10))
        throttle.tryAcquire("a", start)

        assertFalse(throttle.tryAcquire("a", start.plusSeconds(9)))
        assertTrue(throttle.tryAcquire("a", start.plusSeconds(10)))
    }

    @Test
    fun `the window does not slide on a refused acquire`() {
        val throttle = Throttle<String>(Duration.ofSeconds(10))
        throttle.tryAcquire("a", start)
        throttle.tryAcquire("a", start.plusSeconds(9))

        // Still measured from the granted acquire at t=0, not the refused one at t=9.
        assertTrue(throttle.tryAcquire("a", start.plusSeconds(10)))
    }

    @Test
    fun `keys are throttled independently`() {
        val throttle = Throttle<String>(Duration.ofSeconds(10))
        throttle.tryAcquire("a", start)

        assertTrue(throttle.tryAcquire("b", start))
    }
}
