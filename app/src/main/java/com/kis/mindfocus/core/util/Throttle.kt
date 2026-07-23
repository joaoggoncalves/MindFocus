package com.kis.mindfocus.core.util

import java.time.Duration
import java.time.Instant

/**
 * Rate-limits per key: [tryAcquire] returns true at most once per [minInterval].
 *
 * Used for two different jobs — the detector's refractory period (one loud bang is one
 * distraction, not forty) and notification spam control — so the mechanism is widened rather than
 * written twice.
 *
 * Not thread-safe: both callers drive it from a single collecting coroutine.
 */
class Throttle<K : Any>(private val minInterval: Duration) {

    private val lastAcquiredAt = mutableMapOf<K, Instant>()

    fun tryAcquire(key: K, now: Instant): Boolean {
        val last = lastAcquiredAt[key]
        if (last != null && Duration.between(last, now) < minInterval) return false
        lastAcquiredAt[key] = now
        return true
    }

    fun reset() = lastAcquiredAt.clear()
}
