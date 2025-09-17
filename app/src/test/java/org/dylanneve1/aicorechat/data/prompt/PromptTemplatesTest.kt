package org.dylanneve1.aicorechat.data.prompt

import org.dylanneve1.aicorechat.data.BioInformation
import org.dylanneve1.aicorechat.data.MemoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTemplatesTest {

    @Test
    fun systemPreamble_includesSearchInstructionsWhenEnabled() {
        val preamble = PromptTemplates.systemPreamble(allowSearch = true, offlineNotice = false)

        assertTrue(preamble.contains("Tool call:"))
        assertFalse(preamble.contains("Device is offline"))
    }

    @Test
    fun systemPreamble_includesOfflineNoticeWhenSearchUnavailable() {
        val preamble = PromptTemplates.systemPreamble(allowSearch = false, offlineNotice = true)

        assertTrue(preamble.contains("Device is offline"))
        assertFalse(preamble.contains("Tool call"))
    }

    @Test
    fun customInstructionsBlock_wrapsContent() {
        val block = PromptTemplates.customInstructionsBlock("Be concise.")

        assertTrue(block.startsWith("[CUSTOM_INSTRUCTIONS]"))
        assertTrue(block.trim().endsWith("[/CUSTOM_INSTRUCTIONS]"))
        assertTrue(block.contains("Be concise."))
    }

    @Test
    fun customInstructionsBlock_returnsEmptyForBlankInput() {
        val block = PromptTemplates.customInstructionsBlock("   ")

        assertEquals("", block)
    }

    @Test
    fun memoryContextBlock_includesBioAndMemories() {
        val bio = BioInformation(
            id = "bio",
            name = "Alex",
            age = 32,
            occupation = "Engineer",
            location = "Seattle",
            interests = listOf("hiking", "coffee"),
        )
        val memories = listOf(
            MemoryEntry(id = "m1", content = "Met at the hiking meetup."),
        )

        val block = PromptTemplates.memoryContextBlock(memories, bio)

        assertTrue(block.contains("[BIO_CONTEXT]"))
        assertTrue(block.contains("Name: Alex"))
        assertTrue(block.contains("[MEMORY_CONTEXT]"))
        assertTrue(block.contains("Met at the hiking meetup."))
    }

    @Test
    fun buildMemoryContextFromQuery_filtersDisabledAndLimitsToFive() {
        val memories = listOf(
            MemoryEntry(id = "m1", content = "Planning a hiking trip to the Alps.", isEnabled = true),
            MemoryEntry(id = "m2", content = "Weekend hiking checklist.", isEnabled = true),
            MemoryEntry(id = "m3", content = "Favorite hiking snacks list.", isEnabled = true),
            MemoryEntry(id = "m4", content = "Looking for new hiking boots.", isEnabled = true),
            MemoryEntry(id = "m5", content = "Hiking with friends next month.", isEnabled = true),
            MemoryEntry(id = "m6", content = "Hiking routes near Seattle.", isEnabled = true),
            MemoryEntry(id = "m7", content = "Hiking reminder but disabled.", isEnabled = false),
        )

        val result = PromptTemplates.buildMemoryContextFromQuery("Any hiking recommendations?", memories)

        assertEquals(5, result.size)
        assertEquals(listOf("m1", "m2", "m3", "m4", "m5"), result.map { it.id })
        assertFalse(result.any { it.id == "m7" })
    }

    @Test
    fun memoryContextBlock_returnsEmptyWhenNoData() {
        val block = PromptTemplates.memoryContextBlock(emptyList(), null)

        assertEquals("", block)
    }

    @Test
    fun postSearchPreamble_includesResultInstructions() {
        val preamble = PromptTemplates.postSearchPreamble()

        assertTrue(preamble.contains("answer the user's last question"))
        assertTrue(preamble.contains("Do NOT mention"))
    }

    @Test
    fun systemPreamble_withoutSearchOptionsOmitsToolHint() {
        val preamble = PromptTemplates.systemPreamble(allowSearch = false, offlineNotice = false)

        assertFalse(preamble.contains("Tool call"))
        assertFalse(preamble.contains("Device is offline"))
    }
}
