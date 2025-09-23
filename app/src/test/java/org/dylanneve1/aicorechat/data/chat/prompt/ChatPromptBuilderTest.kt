package org.dylanneve1.aicorechat.data.chat.prompt

import kotlinx.coroutines.test.runTest
import org.dylanneve1.aicorechat.data.BioInformation
import org.dylanneve1.aicorechat.data.MemoryEntry
import org.dylanneve1.aicorechat.data.chat.model.ChatMessage
import org.dylanneve1.aicorechat.data.chat.model.ChatUiState
import org.dylanneve1.aicorechat.data.context.PersonalContextProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPromptBuilderTest {

    @Test
    fun buildInitialPrompt_includesConfiguredSections() = runTest {
        val personalContext = "[PERSONAL_CONTEXT]\nPersonalized context\n[/PERSONAL_CONTEXT]\n\n"
        val builder = ChatPromptBuilder(FakePersonalContextProvider(personalContext))
        val priorUser = ChatMessage(id = 1L, text = "Hello", isFromUser = true)
        val priorAssistant = ChatMessage(id = 2L, text = "Hi there", isFromUser = false)
        val describedMessage =
            ChatMessage(id = 3L, text = "See image", isFromUser = true, imageDescription = "A cloudy skyline")
        val userMessage = ChatMessage(id = 4L, text = "Plan a weekend getaway", isFromUser = true)
        val state = ChatUiState(
            messages = listOf(priorUser, priorAssistant, describedMessage, userMessage),
            personalContextEnabled = true,
            customInstructions = "Keep answers short.",
            customInstructionsEnabled = true,
            memoryContextEnabled = true,
            memoryEntries = listOf(MemoryEntry(content = "Loves planning weekend getaways and outdoor adventures")),
            bioInformation = BioInformation(name = "Skye"),
            bioContextEnabled = true,
            multimodalEnabled = true,
            userName = "Skye",
        )

        val prompt = builder.buildInitialPrompt(
            state = state,
            userPrompt = userMessage.text,
            userMessage = userMessage,
            allowSearch = true,
            offlineNotice = false,
        )

        assertContains(prompt, "[CUSTOM_INSTRUCTIONS]")
        assertContains(prompt, personalContext)
        assertContains(prompt, "[MEMORY_CONTEXT]")
        assertContains(prompt, "[BIO_CONTEXT]")
        assertContains(prompt, "[IMAGE_DESCRIPTION]\nA cloudy skyline\n[/IMAGE_DESCRIPTION]")
        assertContains(prompt, "[USER]\nPlan a weekend getaway\n[/USER]")
        assertContains(prompt, "[ASSISTANT]\n")
        assertContains(prompt, "Tool call: To request a web search")
        assertNotContains(prompt, "[CONVERSATION_STATE]")
    }

    @Test
    fun buildInitialPrompt_addsEmptyHistoryNoticeWhenNoPriorMessages() = runTest {
        val builder = ChatPromptBuilder(FakePersonalContextProvider("IGNORED"))
        val userMessage = ChatMessage(id = 10L, text = "Start chatting", isFromUser = true)
        val state = ChatUiState(
            messages = listOf(userMessage),
            personalContextEnabled = false,
            customInstructionsEnabled = false,
            memoryContextEnabled = false,
            multimodalEnabled = false,
        )

        val prompt = builder.buildInitialPrompt(
            state = state,
            userPrompt = userMessage.text,
            userMessage = userMessage,
            allowSearch = false,
            offlineNotice = true,
        )

        assertContains(prompt, "[CONVERSATION_STATE]")
        assertContains(prompt, "Note: Device is offline")
        assertNotContains(prompt, "[PERSONAL_CONTEXT]")
        assertNotContains(prompt, "[CUSTOM_INSTRUCTIONS]")
    }

    @Test
    fun buildSearchFollowUpPrompt_includesWebResultsAndMemory() {
        val builder = ChatPromptBuilder(FakePersonalContextProvider("IGNORED"))
        val userMessage = ChatMessage(id = 35L, text = "What dishes should I cook tonight", isFromUser = true)
        val assistantMessage = ChatMessage(id = 40L, text = "Try pasta", isFromUser = false)
        val state = ChatUiState(
            messages = emptyList(),
            customInstructions = "Be playful.",
            customInstructionsEnabled = true,
            memoryContextEnabled = true,
            memoryEntries = listOf(MemoryEntry(content = "Enjoys spicy cooking experiments")),
            bioInformation = BioInformation(name = "Quinn"),
            bioContextEnabled = true,
        )
        val recent = listOf(assistantMessage, userMessage)
        val webResults = "Result 1\nResult 2"

        val prompt = builder.buildSearchFollowUpPrompt(
            state = state,
            userMessage = userMessage,
            recentMessages = recent,
            webResults = webResults,
        )

        assertContains(prompt, "[WEB_RESULTS]\n$webResults\n[/WEB_RESULTS]")
        assertContains(prompt, "[MEMORY_CONTEXT]")
        assertContains(prompt, "[BIO_CONTEXT]")
        assertContains(prompt, "[CUSTOM_INSTRUCTIONS]")
        assertContains(prompt, "[ASSISTANT]\n")
        assertContains(prompt, "Try pasta")
        assertContains(prompt, "What dishes should I cook tonight")
    }

    private class FakePersonalContextProvider(
        private val context: String,
    ) : PersonalContextProvider {
        override suspend fun build(userName: String): String = context
    }
}

private fun assertContains(haystack: String, needle: String) {
    assertTrue("Expected to find \"$needle\" in:\n$haystack", haystack.contains(needle))
}

private fun assertNotContains(haystack: String, needle: String) {
    assertFalse("Did not expect to find \"$needle\" in:\n$haystack", haystack.contains(needle))
}
