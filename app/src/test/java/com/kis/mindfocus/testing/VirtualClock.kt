package com.kis.mindfocus.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A clock driven by the test scheduler's virtual time, so `advanceTimeBy` moves the wall clock too.
 * Without this, a test that advances coroutine time would still read the same instant and every
 * elapsed duration would come out as zero.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VirtualClock(
    private val scheduler: TestCoroutineScheduler,
    private val origin: Instant = Instant.parse("2026-01-01T00:00:00Z"),
) : Clock() {
    override fun instant(): Instant = origin.plusMillis(scheduler.currentTime)
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
}
