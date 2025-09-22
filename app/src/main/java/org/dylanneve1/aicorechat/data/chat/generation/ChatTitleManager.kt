package org.dylanneve1.aicorechat.data.chat.generation

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dylanneve1.aicorechat.data.chat.model.ChatMessage
import org.dylanneve1.aicorechat.data.chat.model.ChatSessionMeta
import org.dylanneve1.aicorechat.data.chat.model.ChatUiState
import org.dylanneve1.aicorechat.data.chat.session.ChatSessionManager

class ChatTitleManager(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<ChatUiState>,
    private val sessionManager: ChatSessionManager,
    private val modelManager: ChatModelManager,
) {

    fun generateCurrentChatTitle() {
        if (state.value.isGenerating) {
            state.update { it.copy(modelError = "Cannot rename while generating.") }
            return
        }

        val sessionId = state.value.currentSessionId
        if (sessionId == null || state.value.messages.none { it.isFromUser }) {
            state.update { it.copy(modelError = "Chat is empty.") }
            return
        }

        val model = modelManager.getModel()
        if (model == null) {
            state.update { it.copy(modelError = "Model not ready.") }
            return
        }

        val history = state.value.messages
        val prompt = buildTitlePrompt(history)

        scope.launch {
            var result = ""
            model.generateContentStream(prompt)
                .onStart {
                    state.update { it.copy(isTitleGenerating = true, modelError = null) }
                }
                .onCompletion {
                    val cleaned = cleanTitle(result)
                    if (cleaned.isNotBlank()) {
                        sessionManager.renameSession(sessionId, cleaned)
                        state.update { current ->
                            val updatedSessions = current.sessions.map { meta ->
                                if (meta.id == sessionId) meta.copy(name = cleaned) else meta
                            }
                            current.copy(
                                isTitleGenerating = false,
                                currentSessionName = cleaned,
                                sessions = updatedSessions,
                            )
                        }
                    } else {
                        state.update { it.copy(isTitleGenerating = false) }
                    }
                }
                .catch { throwable ->
                    Log.e(TAG, "Error generating title", throwable)
                    state.update { it.copy(isTitleGenerating = false, modelError = throwable.message) }
                }
                .collect { chunk ->
                    result += chunk.text
                }
        }
    }

    fun generateAllChatTitles() {
        if (state.value.isGenerating) {
            state.update { it.copy(modelError = "Please wait for current generation to finish.") }
            return
        }
        val model = modelManager.getModel()
        if (model == null) {
            state.update { it.copy(modelError = "Model not ready.") }
            return
        }

        scope.launch {
            try {
                state.update { it.copy(isBulkTitleGenerating = true, modelError = null) }
                val currentId = state.value.currentSessionId
                val purgeMeta = sessionManager.purgeEmptySessions(currentId)
                state.update { it.copy(sessions = purgeMeta) }

                val sessions = sessionManager.loadSessions()
                sessions.forEach { session ->
                    if (session.id != currentId && session.messages.none { it.isFromUser }) {
                        return@forEach
                    }
                    if (session.messages.none { it.isFromUser }) return@forEach
                    if (session.name != DEFAULT_SESSION_NAME) return@forEach

                    val prompt = buildTitlePrompt(session.messages)
                    var result = ""

                    model.generateContentStream(prompt)
                        .onStart { result = "" }
                        .catch { throwable ->
                            Log.e(TAG, "Title generation error for session ${session.id}", throwable)
                        }
                        .collect { chunk -> result += chunk.text }

                    val cleaned = cleanTitle(result)
                    if (cleaned.isNotBlank()) {
                        sessionManager.renameSession(session.id, cleaned)
                    }
                }

                val refreshed = sessionManager.loadSessions()
                val updatedSessions = refreshed.map { ChatSessionMeta(it.id, it.name) }
                val currentName = refreshed.find { it.id == state.value.currentSessionId }?.name
                    ?: state.value.currentSessionName
                state.update {
                    it.copy(
                        isBulkTitleGenerating = false,
                        sessions = updatedSessions,
                        currentSessionName = currentName,
                    )
                }
            } catch (throwable: Exception) {
                state.update { it.copy(isBulkTitleGenerating = false, modelError = throwable.message) }
            }
        }
    }

    private fun buildTitlePrompt(messages: List<ChatMessage>): String {
        val builder = StringBuilder()
        builder.append("You are to summarize the following chat into a very short, descriptive title.\n")
        builder.append("Rules: 3-4 words max, no quotes, no punctuation, Title Case, be specific.\n\n")
        messages.forEach { message ->
            if (message.isFromUser) {
                builder.append("User: ${message.text}\n")
            } else {
                builder.append("Assistant: ${message.text}\n")
            }
        }
        builder.append("\nReturn only the title.")
        return builder.toString()
    }

    private fun cleanTitle(raw: String): String = raw
        .replace("\n", " ")
        .replace("\"", "")
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(4)
        .joinToString(" ")
        .replace(Regex("[.,!?:;]+$"), "")

    companion object {
        private const val TAG = "ChatTitleManager"
        private const val DEFAULT_SESSION_NAME = "New Chat"
    }
}
