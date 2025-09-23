package org.dylanneve1.aicorechat.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StopTokenUtilsTest {

    @Test
    fun trimTrailingPartialStopToken_dropsPartialSuffix() {
        val result = trimTrailingPartialStopToken(
            text = "Answer [/ASSI",
            tokens = listOf("[/ASSISTANT]"),
        )

        assertEquals("Answer", result)
    }

    @Test
    fun trimTrailingPartialStopToken_respectsMinimumBuffer() {
        val result = trimTrailingPartialStopToken(
            text = "Reply [/A",
            tokens = listOf("[/ASSISTANT]"),
        )

        assertEquals("Reply", result)
    }

    @Test
    fun trimTrailingPartialStopToken_returnsOriginalWhenNoOverlap() {
        val input = "Completed response"

        val result = trimTrailingPartialStopToken(
            text = input,
            tokens = listOf("[/ASSISTANT]"),
        )

        assertEquals(input, result)
    }
}
