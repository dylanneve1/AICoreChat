package org.dylanneve1.aicorechat.data.chat.generation

import android.app.Application
import android.os.SystemClock
import android.util.Log
import com.google.ai.edge.aicore.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dylanneve1.aicorechat.data.chat.model.ChatMessage
import org.dylanneve1.aicorechat.data.chat.model.ChatUiState
import org.dylanneve1.aicorechat.data.chat.prompt.ChatPromptBuilder
import org.dylanneve1.aicorechat.data.chat.session.ChatSessionManager
import org.dylanneve1.aicorechat.data.search.WebSearchService
import org.dylanneve1.aicorechat.util.AssistantResponseFormatter
import org.dylanneve1.aicorechat.util.trimTrailingPartialStopToken

class ChatGenerationManager(
    private val application: Application,
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<ChatUiState>,
    private val sessionManager: ChatSessionManager,
    private val promptBuilder: ChatPromptBuilder,
    private val modelManager: ChatModelManager,
    private val webSearchService: WebSearchService,
) {

    private var generationJob: Job? = null

    fun stopGeneration() {
        generationJob?.cancel()
    }

    fun sendMessage(promptText: String) {
        val model = modelManager.getModel()
        if (model == null) {
            state.update { it.copy(modelError = "Model is not initialized yet.") }
            return
        }

        val currentPendingUri = state.value.pendingImageUri
        val currentPendingDesc = state.value.pendingImageDescription
        val userMessage = ChatMessage(
            text = promptText,
            isFromUser = true,
            imageUri = currentPendingUri,
            imageDescription = currentPendingDesc,
        )

        state.update {
            it.copy(
                messages = it.messages + userMessage,
                pendingImageUri = null,
                pendingImageDescription = null,
            )
        }

        state.value.currentSessionId?.let { sessionId ->
            sessionManager.appendMessage(sessionId, userMessage)
        }

        launchAssistantResponse(model, userMessage, promptText)
    }

    fun regenerateFromUserMessage(userMessage: ChatMessage) {
        val model = modelManager.getModel()
        if (model == null) {
            state.update { it.copy(modelError = "Model is not initialized yet.") }
            return
        }

        launchAssistantResponse(model, userMessage, userMessage.text)
    }

    private fun launchAssistantResponse(model: GenerativeModel, userMessage: ChatMessage, userPrompt: String) {
        generationJob?.cancel()

        generationJob = scope.launch {
            try {
                val allowSearch = state.value.webSearchEnabled && webSearchService.isOnline()
                val offlineNotice = state.value.webSearchEnabled && !allowSearch
                val prompt = promptBuilder.buildInitialPrompt(
                    state = state.value,
                    userPrompt = userPrompt,
                    userMessage = userMessage,
                    allowSearch = allowSearch,
                    offlineNotice = offlineNotice,
                )

                debugLog("Sending prompt:\n$prompt")

                var fullResponse = ""
                val stopTokens = listOf("[/ASSISTANT]", "[ASSISTANT]", "[/USER]", "[USER]")
                var searchTriggered = false
                var searchStartDetected = false
                var searchStartIndex = -1
                val streamStartMs = SystemClock.elapsedRealtime()

                model.generateContentStream(prompt)
                    .onStart {
                        state.update {
                            it.copy(
                                isGenerating = true,
                                modelError = null,
                                messages = it.messages + ChatMessage(text = "", isFromUser = false, isStreaming = true),
                            )
                        }
                    }
                    .onCompletion {
                        if (searchStartDetected && !searchTriggered) {
                            state.update { it.copy(isSearchInProgress = false, currentSearchQuery = null) }
                        }
                        if (!searchTriggered) {
                            state.update { currentState ->
                                val updated = currentState.messages.toMutableList()
                                if (updated.isNotEmpty() && updated.last().isStreaming) {
                                    val finalText = AssistantResponseFormatter.finalizeAssistantDisplayText(
                                        fullResponse,
                                        updated.last().text,
                                    )
                                    updated[updated.lastIndex] = updated.last().copy(
                                        text = finalText,
                                        isStreaming = false,
                                    )
                                }
                                currentState.copy(isGenerating = false, messages = updated)
                            }
                            persistMessages()
                        }
                    }
                    .catch { throwable ->
                        Log.e(TAG, "Error generating content", throwable)
                        state.update { currentState ->
                            val updated = currentState.messages.toMutableList()
                            if (updated.isNotEmpty() && updated.last().isStreaming) {
                                val errorText = "Error: ${throwable.message}"
                                val finalText = AssistantResponseFormatter.finalizeAssistantDisplayText(
                                    errorText,
                                    updated.last().text,
                                )
                                updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                            }
                            currentState.copy(isGenerating = false, messages = updated)
                        }
                        persistMessages()
                    }
                    .collect { chunk ->
                        fullResponse += chunk.text

                        if (allowSearch) {
                            val searchToken = "[SEARCH]"
                            val firstNonWhitespaceIndex = fullResponse.indexOfFirst { !it.isWhitespace() }
                                .let { if (it < 0) Int.MAX_VALUE else it }

                            if (!searchStartDetected && firstNonWhitespaceIndex != Int.MAX_VALUE) {
                                val after = fullResponse.substring(firstNonWhitespaceIndex)
                                if (after.isNotEmpty() && after.length < searchToken.length && searchToken.startsWith(
                                        after,
                                    )
                                ) {
                                    searchStartDetected = true
                                    searchStartIndex = firstNonWhitespaceIndex
                                    state.update { current ->
                                        val updated = current.messages.toMutableList()
                                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                                            updated[updated.lastIndex] = updated.last().copy(text = "")
                                        }
                                        current.copy(
                                            isSearchInProgress = true,
                                            currentSearchQuery = "",
                                            messages = updated,
                                        )
                                    }
                                    return@collect
                                }
                            }

                            val explicitIndex = fullResponse.indexOf(searchToken)
                            if (!searchStartDetected && explicitIndex >= 0 && explicitIndex == firstNonWhitespaceIndex) {
                                searchStartDetected = true
                                searchStartIndex = explicitIndex
                                val partialQuery = if (fullResponse.length >= explicitIndex + searchToken.length) {
                                    fullResponse.substring(explicitIndex + searchToken.length)
                                        .substringBefore("[/SEARCH]")
                                        .trim()
                                } else {
                                    ""
                                }
                                state.update { current ->
                                    val updated = current.messages.toMutableList()
                                    if (updated.isNotEmpty() && updated.last().isStreaming) {
                                        updated[updated.lastIndex] = updated.last().copy(text = "")
                                    }
                                    current.copy(
                                        isSearchInProgress = true,
                                        currentSearchQuery = partialQuery,
                                        messages = updated,
                                    )
                                }
                            }

                            if (searchStartDetected) {
                                val tokenEndBase = searchStartIndex + searchToken.length
                                val partialQuery = if (fullResponse.length > tokenEndBase) {
                                    fullResponse.substring(tokenEndBase).substringBefore("[/SEARCH]").trim()
                                } else {
                                    ""
                                }
                                state.update { it.copy(currentSearchQuery = partialQuery) }
                                val endIndex = if (fullResponse.length > tokenEndBase) {
                                    fullResponse.indexOf("[/SEARCH]", tokenEndBase)
                                } else {
                                    -1
                                }
                                if (endIndex != -1 && !searchTriggered) {
                                    searchTriggered = true
                                    val query = fullResponse.substring(tokenEndBase, endIndex).trim()
                                    scope.launch {
                                        continueWithSearchResults(userMessage, query)
                                    }
                                    stopGeneration()
                                    return@collect
                                }
                                return@collect
                            }
                        }

                        val elapsed = SystemClock.elapsedRealtime() - streamStartMs
                        if (!searchStartDetected && elapsed < 300 && fullResponse.length < 24) {
                            return@collect
                        }

                        val earliestIndex = stopTokens
                            .map { token ->
                                fullResponse.indexOf(token).let { idx -> if (idx >= 0) idx else Int.MAX_VALUE }
                            }
                            .minOrNull() ?: Int.MAX_VALUE

                        delay(35)
                        state.update { currentState ->
                            val updated = currentState.messages.toMutableList()
                            if (updated.isNotEmpty() && updated.last().isStreaming) {
                                val displayTextRaw = if (earliestIndex != Int.MAX_VALUE) {
                                    fullResponse.substring(0, earliestIndex)
                                } else {
                                    trimTrailingPartialStopToken(fullResponse, stopTokens)
                                }
                                val displayText = AssistantResponseFormatter.sanitizeAssistantText(displayTextRaw)
                                val previousText = updated.last().text
                                val stableText = if (displayText.length < previousText.length) previousText else displayText
                                updated[updated.lastIndex] = updated.last().copy(text = stableText)
                            }
                            currentState.copy(messages = updated)
                        }

                        if (earliestIndex != Int.MAX_VALUE) {
                            stopGeneration()
                        }
                    }
            } catch (throwable: Exception) {
                if (throwable is kotlinx.coroutines.CancellationException) {
                    debugLog("Generation cancelled")
                } else {
                    Log.e(TAG, "Exception during generation", throwable)
                    state.update {
                        it.copy(
                            isGenerating = false,
                            messages = it.messages + ChatMessage(
                                text = "Error: ${throwable.message}",
                                isFromUser = false,
                            ),
                        )
                    }
                    persistMessages()
                }
            }
        }
    }

    private suspend fun continueWithSearchResults(userMessage: ChatMessage, query: String) {
        try {
            val model = modelManager.getModel() ?: return
            val results = webSearchService.search(query)
            val stateSnapshot = state.value
            val recentMessages = stateSnapshot.messages.filter { !it.isStreaming } + userMessage
            val prompt = promptBuilder.buildSearchFollowUpPrompt(
                state = stateSnapshot,
                userMessage = userMessage,
                recentMessages = recentMessages,
                webResults = results,
            )

            debugLog("Follow-up with web results:\n$prompt")

            var fullResponse = ""
            val stopTokens = listOf("[/ASSISTANT]", "[ASSISTANT]", "[/USER]", "[USER]")
            val streamStartMs = SystemClock.elapsedRealtime()

            model.generateContentStream(prompt)
                .onStart {
                    state.update {
                        it.copy(
                            isSearchInProgress = false,
                            currentSearchQuery = null,
                            isGenerating = true,
                        )
                    }
                }
                .onCompletion {
                    state.update { currentState ->
                        val updated = currentState.messages.toMutableList()
                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                            val finalText = AssistantResponseFormatter.finalizeAssistantDisplayText(
                                fullResponse,
                                updated.last().text,
                            )
                            updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                        }
                        currentState.copy(isGenerating = false, messages = updated)
                    }
                    persistMessages()
                }
                .catch { throwable ->
                    Log.e(TAG, "Error generating after search", throwable)
                    state.update { currentState ->
                        val updated = currentState.messages.toMutableList()
                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                            val errorText = "Error: ${throwable.message}"
                            val finalText = AssistantResponseFormatter.finalizeAssistantDisplayText(
                                errorText,
                                updated.last().text,
                            )
                            updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                        }
                        currentState.copy(isGenerating = false, messages = updated)
                    }
                }
                .collect { chunk ->
                    fullResponse += chunk.text
                    val elapsed = SystemClock.elapsedRealtime() - streamStartMs
                    if (elapsed < 300 && fullResponse.length < 24) {
                        return@collect
                    }
                    val earliestIndex = stopTokens
                        .map { token ->
                            fullResponse.indexOf(
                                token,
                            ).let { idx -> if (idx >= 0) idx else Int.MAX_VALUE }
                        }
                        .minOrNull() ?: Int.MAX_VALUE
                    delay(35)
                    state.update { currentState ->
                        val updated = currentState.messages.toMutableList()
                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                            val displayTextRaw = if (earliestIndex != Int.MAX_VALUE) {
                                fullResponse.substring(0, earliestIndex)
                            } else {
                                trimTrailingPartialStopToken(fullResponse, stopTokens)
                            }
                            val displayText = AssistantResponseFormatter.sanitizeAssistantText(displayTextRaw)
                            val previousText = updated.last().text
                            val stableText = if (displayText.length < previousText.length) previousText else displayText
                            updated[updated.lastIndex] = updated.last().copy(text = stableText)
                        }
                        currentState.copy(messages = updated)
                    }
                    if (earliestIndex != Int.MAX_VALUE) {
                        stopGeneration()
                    }
                }
        } catch (throwable: Exception) {
            state.update {
                it.copy(isSearchInProgress = false, currentSearchQuery = null, modelError = throwable.message)
            }
        }
    }

    private fun persistMessages() {
        val sessionId = state.value.currentSessionId ?: return
        sessionManager.replaceMessages(sessionId, state.value.messages)
    }

    private fun debugLog(message: String) {
        val isDebuggable =
            (application.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable || Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message)
        }
    }

    companion object {
        private const val TAG = "ChatGenerationManager"
    }
}
