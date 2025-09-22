package org.dylanneve1.aicorechat.data.chat.prompt

import org.dylanneve1.aicorechat.data.chat.model.ChatMessage
import org.dylanneve1.aicorechat.data.chat.model.ChatUiState
import org.dylanneve1.aicorechat.data.context.PersonalContextBuilder
import org.dylanneve1.aicorechat.data.prompt.PromptTemplates

class ChatPromptBuilder(
    private val personalContextBuilder: PersonalContextBuilder,
) {

    suspend fun buildInitialPrompt(
        state: ChatUiState,
        userPrompt: String,
        userMessage: ChatMessage,
        allowSearch: Boolean,
        offlineNotice: Boolean,
    ): String {
        val builder = StringBuilder()
        builder.append(PromptTemplates.systemPreamble(allowSearch = allowSearch, offlineNotice = offlineNotice))

        if (state.customInstructionsEnabled && state.customInstructions.isNotBlank()) {
            builder.append(PromptTemplates.customInstructionsBlock(state.customInstructions))
        }

        builder.append(PromptTemplates.fewShotGeneral())
        if (allowSearch) {
            builder.append(PromptTemplates.fewShotSearch())
        }

        if (state.personalContextEnabled) {
            builder.append(personalContextBuilder.build(state.userName))
        }

        if (state.memoryContextEnabled) {
            val relevantMemories = PromptTemplates.buildMemoryContextFromQuery(
                query = userPrompt,
                allMemories = state.memoryEntries,
            )
            val bioInfo = if (state.bioContextEnabled) state.bioInformation else null
            builder.append(PromptTemplates.memoryContextBlock(relevantMemories, bioInfo))
        }

        if (state.multimodalEnabled) {
            state.messages.forEach { message ->
                if (!message.imageDescription.isNullOrBlank()) {
                    builder.append("[IMAGE_DESCRIPTION]\n${message.imageDescription}\n[/IMAGE_DESCRIPTION]\n\n")
                }
            }
        }

        val history = state.messages.takeLast(10)
        val priorMessages = history.filter { it.id != userMessage.id }
        if (priorMessages.isEmpty()) {
            builder.append(PromptTemplates.emptyHistoryNotice())
        } else {
            priorMessages.forEach { message ->
                if (message.isFromUser) {
                    builder.append("[USER]\n${message.text}\n[/USER]\n")
                } else {
                    builder.append("[ASSISTANT]\n${message.text}\n[/ASSISTANT]\n")
                }
            }
        }

        builder.append("[USER]\n$userPrompt\n[/USER]\n")
        builder.append("[ASSISTANT]\n")
        return builder.toString()
    }

    fun buildSearchFollowUpPrompt(
        state: ChatUiState,
        userMessage: ChatMessage,
        recentMessages: List<ChatMessage>,
        webResults: String,
    ): String {
        val builder = StringBuilder()
        builder.append(PromptTemplates.postSearchPreamble())

        if (state.customInstructionsEnabled && state.customInstructions.isNotBlank()) {
            builder.append(PromptTemplates.customInstructionsBlock(state.customInstructions))
        }

        builder.append("[WEB_RESULTS]\n$webResults\n[/WEB_RESULTS]\n\n")

        if (state.memoryContextEnabled) {
            val relevantMemories = PromptTemplates.buildMemoryContextFromQuery(
                query = userMessage.text,
                allMemories = state.memoryEntries,
            )
            val bioInfo = if (state.bioContextEnabled) state.bioInformation else null
            builder.append(PromptTemplates.memoryContextBlock(relevantMemories, bioInfo))
        }

        recentMessages.takeLast(10).forEach { message ->
            if (message.isFromUser) {
                builder.append("[USER]\n${message.text}\n[/USER]\n")
            } else {
                builder.append("[ASSISTANT]\n${message.text}\n[/ASSISTANT]\n")
            }
        }

        builder.append("[ASSISTANT]\n")
        return builder.toString()
    }
}
