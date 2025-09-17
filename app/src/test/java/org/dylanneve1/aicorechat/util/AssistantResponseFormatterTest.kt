package org.dylanneve1.aicorechat.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantResponseFormatterTest {

    @Test
    fun ensureAssistantEndToken_appendsMissingToken() {
        val result = AssistantResponseFormatter.ensureAssistantEndToken("Hello world")

        assertEquals("Hello world\n\n[/ASSISTANT]", result)
    }

    @Test
    fun ensureAssistantEndToken_canonicalizesClosingCase() {
        val result = AssistantResponseFormatter.ensureAssistantEndToken("Reply body\n[/assistant]")

        assertEquals("Reply body\n[/ASSISTANT]", result)
    }

    @Test
    fun ensureAssistantEndToken_handlesDanglingAssistantOpen() {
        val result = AssistantResponseFormatter.ensureAssistantEndToken("Partial chunk [ASSISTANT]")

        assertTrue(result.endsWith("[/ASSISTANT]"))
    }

    @Test
    fun finalizeAssistantDisplayText_stripsToolTagsAndWhitespace() {
        val raw = "Here is the answer\n[WEB_RESULTS]\nunused\n[/WEB_RESULTS]\n\n\n"

        val result = AssistantResponseFormatter.finalizeAssistantDisplayText(raw)

        assertEquals("Here is the answer", result)
    }

    @Test
    fun finalizeAssistantDisplayText_usesFallbackWhenRawBlank() {
        val fallback = "Streamed chunk so far [/assistant]"

        val result = AssistantResponseFormatter.finalizeAssistantDisplayText("", fallback)

        assertEquals("Streamed chunk so far", result)
    }

    @Test
    fun sanitizeAssistantText_collapsesExcessiveNewlines() {
        val raw = "Line one\n\n\n\nLine two"

        val result = AssistantResponseFormatter.sanitizeAssistantText(raw)

        assertEquals("Line one\n\nLine two", result)
    }

    @Test
    fun ensureAssistantEndToken_respectsOtherStopTokens() {
        val result = AssistantResponseFormatter.ensureAssistantEndToken("Some text [USER]")

        assertTrue(result.endsWith("[USER]"))
        assertFalse(result.endsWith("[/ASSISTANT]"))
    }

    @Test
    fun finalizeAssistantDisplayText_trimsTrailingAssistantToken() {
        val result = AssistantResponseFormatter.finalizeAssistantDisplayText("Answer body\n[/ASSISTANT]\n", "")

        assertEquals("Answer body", result)
    }

    @Test
    fun sanitizeAssistantText_removesSearchBlocks() {
        val raw = "Intro text\n[SEARCH]query[/SEARCH]\nTrailing"

        val result = AssistantResponseFormatter.sanitizeAssistantText(raw)

        assertEquals("Intro text\n\nTrailing", result)
    }
}
