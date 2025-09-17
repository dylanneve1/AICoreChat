package org.dylanneve1.aicorechat.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilTest {

    @Test
    fun timeAgoShort_returnsJustNowForRecentTimestamps() {
        val now = System.currentTimeMillis()

        val result = timeAgoShort(now)

        assertEquals("just now", result)
    }

    @Test
    fun timeAgoShort_formatsSeconds() {
        val past = System.currentTimeMillis() - 15_000

        val result = timeAgoShort(past)

        assertEquals("15s ago", result)
    }

    @Test
    fun timeAgoShort_formatsMinutes() {
        val past = System.currentTimeMillis() - 5 * 60_000

        val result = timeAgoShort(past)

        assertEquals("5m ago", result)
    }

    @Test
    fun timeAgoShort_formatsHours() {
        val past = System.currentTimeMillis() - 3 * 60 * 60_000

        val result = timeAgoShort(past)

        assertEquals("3h ago", result)
    }

    @Test
    fun timeAgoShort_formatsDays() {
        val past = System.currentTimeMillis() - 2 * 24 * 60 * 60_000

        val result = timeAgoShort(past)

        assertEquals("2d ago", result)
    }
}
