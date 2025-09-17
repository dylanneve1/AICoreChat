package org.dylanneve1.aicorechat.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ChatViewModelStopTokenTest {

    @Test
    fun findPartialStopSuffixLength_enforcesMinimumBuffer() {
        val result = findPartialStopSuffixLength("Answer [/A", listOf("[/ASSISTANT]"))

        assertEquals(3, result)
    }

    @Test
    fun findPartialStopSuffixLength_returnsActualOverlapWhenLonger() {
        val result = findPartialStopSuffixLength("Something [/ASSI", listOf("[/ASSISTANT]"))

        assertEquals(6, result)
    }

    @Test
    fun findPartialStopSuffixLength_handlesNoOverlap() {
        val result = findPartialStopSuffixLength("Completed response", listOf("[/ASSISTANT]"))

        assertEquals(0, result)
    }
}
