package com.kis.mindfocus.feature.session

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class DurationFormatTest {

    @Test
    fun `under an hour the hours group is omitted`() {
        assertEquals("00:00", Duration.ZERO.toClockLabel())
        assertEquals("00:09", Duration.ofSeconds(9).toClockLabel())
        assertEquals("25:03", Duration.ofMinutes(25).plusSeconds(3).toClockLabel())
        assertEquals("59:59", Duration.ofSeconds(3599).toClockLabel())
    }

    @Test
    fun `an hour and over shows the hours group`() {
        assertEquals("1:00:00", Duration.ofHours(1).toClockLabel())
        assertEquals("2:05:07", Duration.ofHours(2).plusMinutes(5).plusSeconds(7).toClockLabel())
    }

    @Test
    fun `a negative duration reads as zero rather than a negative clock`() {
        assertEquals("00:00", Duration.ofSeconds(-30).toClockLabel())
    }
}
