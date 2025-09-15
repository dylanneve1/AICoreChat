package org.dylanneve1.aicorechat.data

import android.app.Application
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dylanneve1.aicorechat.data.search.WebSearchService
import org.dylanneve1.aicorechat.data.context.PersonalContextBuilder
import org.dylanneve1.aicorechat.data.image.ImageDescriptionService
import java.io.InputStream
import android.graphics.ImageDecoder
import android.provider.MediaStore
import org.dylanneve1.aicorechat.data.prompt.PromptTemplates
import org.dylanneve1.aicorechat.data.MemoryRepository
import org.dylanneve1.aicorechat.data.MemoryEntry
import org.dylanneve1.aicorechat.data.CustomInstruction
import org.dylanneve1.aicorechat.data.BioInformation
import org.dylanneve1.aicorechat.data.MemoryCategory
import org.dylanneve1.aicorechat.data.MemoryImportance
import org.dylanneve1.aicorechat.data.mediapipe.LlmManager
import org.dylanneve1.aicorechat.data.model.ModelBackend
import org.dylanneve1.aicorechat.data.model.gemma1B_mediapipe
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * ChatViewModel orchestrates sessions, model streaming, tools, and persistence.
 * Heavily IO-bound work is delegated to small services.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val llmManager: LlmManager
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generativeModel: GenerativeModel? = null
    private var generationJob: Job? = null

    private val sharedPreferences =
        application.getSharedPreferences("AICoreChatPrefs", Context.MODE_PRIVATE)

    private val repository = ChatRepository(application.applicationContext)
    private val memoryRepository = MemoryRepository(application.applicationContext)

    // Services
    private val webSearchService = WebSearchService(application)
    private val personalContextBuilder = PersonalContextBuilder(application)
    private val imageDescriptionService = ImageDescriptionService(application)

    companion object {
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_TOP_K = "top_k"
        const val KEY_USER_NAME = "user_name"
        const val KEY_PERSONAL_CONTEXT = "personal_context"
        const val KEY_WEB_SEARCH = "web_search"
        const val KEY_MULTIMODAL = "multimodal"
        const val KEY_MEMORY_CONTEXT = "memory_context"
        const val KEY_CUSTOM_INSTRUCTIONS = "custom_instructions_enabled"
        const val KEY_CUSTOM_INSTRUCTIONS_TEXT = "custom_instructions_text"
        const val KEY_BIO_CONTEXT = "bio_context"
    }

    private fun sanitizeAssistantText(text: String): String {
        return text
            .replace("[WEB_RESULTS]", "")
            .replace("[/WEB_RESULTS]", "")
            .replace("[SEARCH]", "")
            .replace("[/SEARCH]", "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun stripAssistantEndTokens(text: String): String {
        val token = "[/ASSISTANT]"
        var result = text.trimEnd()
        while (result.endsWith(token)) {
            result = result.dropLast(token.length).trimEnd()
        }
        return result
    }

    /**
     * Ensures the assistant response ends with the proper [/ASSISTANT] token to prevent infinite loops.
     * If the response doesn't end with [/ASSISTANT], appends it automatically.
     */
    private fun ensureAssistantEndToken(text: String): String {
        val trimmed = text.trim()

        // Check if response already ends with [/ASSISTANT]
        if (trimmed.endsWith("[/ASSISTANT]")) {
            return trimmed
        }

        // If it ends with any stop token, return as-is
        val stopTokens = listOf("[/ASSISTANT]", "[ASSISTANT]", "[/USER]", "[USER]")
        for (token in stopTokens) {
            if (trimmed.endsWith(token)) {
                return trimmed
            }
        }

        // If response is empty or only whitespace, return a minimal valid response
        if (trimmed.isEmpty()) {
            return "[/ASSISTANT]"
        }

        // Check if response contains [/ASSISTANT] anywhere and ends with another token
        if (trimmed.contains("[/ASSISTANT]")) {
            val lastAssistantIndex = trimmed.lastIndexOf("[/ASSISTANT]")
            val afterLastToken = trimmed.substring(lastAssistantIndex + "[/ASSISTANT]".length).trim()
            if (afterLastToken.isEmpty()) {
                return trimmed
            }
            // If there's content after the last [/ASSISTANT], it might be malformed
            // Return everything up to and including the last [/ASSISTANT]
            return trimmed.substring(0, lastAssistantIndex + "[/ASSISTANT]".length)
        }

        // If no [/ASSISTANT] token found, append it to prevent infinite loops
        Log.w("ChatViewModel", "Response missing [/ASSISTANT] token, appending automatically: ${trimmed.take(50)}...")
        return "$trimmed\n\n[/ASSISTANT]"
    }

    private fun finalizeAssistantDisplayText(rawText: String, fallback: String = ""): String {
        if (rawText.isBlank() && fallback.isBlank()) return ""
        val source = if (rawText.isBlank()) fallback else rawText
        val ensured = ensureAssistantEndToken(source)
        val withoutToken = stripAssistantEndTokens(ensured)
        return sanitizeAssistantText(withoutToken)
    }

    init {
        loadSettings()
        loadMemoryData()
        initOrStartNewSession()
        
        viewModelScope.launch {
            settingsRepository.appSettingsFlow.collect { (backend) ->
                _uiState.update { it.copy(selectedBackend = backend) }
                reinitializeModel()
            }
        }
    }

    private fun reinitializeModel() {
        viewModelScope.launch {
            val backend = _uiState.value.selectedBackend
            _uiState.update { it.copy(isModelSwitching = true, modelError = "Initializing ${backend.displayName}…") }

            // Close any existing models
            generativeModel?.close()
            generativeModel = null
            llmManager.close()

            when (backend) {
                ModelBackend.AICORE_GEMINI_NANO -> {
                    try {
                        val config = generationConfig {
                            context = getApplication<Application>().applicationContext
                            temperature = _uiState.value.temperature
                            topK = _uiState.value.topK
                        }
                        generativeModel = GenerativeModel(generationConfig = config)
                        generativeModel?.prepareInferenceEngine()
                        _uiState.update { it.copy(modelError = null) }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error initializing AICore model", e)
                        _uiState.update { it.copy(modelError = "Model initialization failed: ${e.message}") }
                    }
                }
                ModelBackend.MEDIAPIPE_GEMMA_1B -> {
                    val modelFile = File(gemma1B_mediapipe.getPath(getApplication()))
                    if (modelFile.exists()) {
                        _uiState.update { it.copy(gemmaDownloadStatus = ModelDownloadStatus.DOWNLOADED) }
                        val error = llmManager.initialize(
                            gemma1B_mediapipe,
                            _uiState.value.temperature,
                            _uiState.value.topK
                        )
                        if (error.isEmpty()) {
                            _uiState.update { it.copy(modelError = null) }
                        } else {
                            _uiState.update { it.copy(modelError = "Gemma init failed: $error") }
                        }
                    } else {
                        _uiState.update { it.copy(gemmaDownloadStatus = ModelDownloadStatus.NOT_DOWNLOADED, modelError = "Gemma 1B model not downloaded.") }
                    }
                }
            }
            _uiState.update { it.copy(isModelSwitching = false) }
        }
    }

    fun updateSelectedBackend(backend: ModelBackend) {
        viewModelScope.launch {
            settingsRepository.updateSelectedBackend(backend)
        }
    }

    fun downloadGemmaModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(gemmaDownloadStatus = ModelDownloadStatus.DOWNLOADING, gemmaDownloadProgress = 0f) }
            try {
                val model = gemma1B_mediapipe
                val url = URL(model.url)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength
                val input = connection.inputStream
                val outputFile = File(model.getPath(getApplication()))
                val output = FileOutputStream(outputFile)

                val data = ByteArray(4096)
                var total = 0L
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toFloat() / 100
                        _uiState.update { it.copy(gemmaDownloadProgress = progress) }
                    }
                    output.write(data, 0, count)
                }
                output.flush()
                output.close()
                input.close()
                _uiState.update { it.copy(gemmaDownloadStatus = ModelDownloadStatus.DOWNLOADED, gemmaDownloadProgress = 1f) }
                reinitializeModel() // Initialize after download
            } catch (e: Exception) {
                Log.e(TAG, "Gemma download failed", e)
                _uiState.update { it.copy(gemmaDownloadStatus = ModelDownloadStatus.FAILED, modelError = "Download failed: ${e.message}") }
            }
        }
    }

    fun sendMessage(prompt: String) {
        if (_uiState.value.selectedBackend == ModelBackend.AICORE_GEMINI_NANO) {
            sendMessageAICore(prompt)
        } else {
            sendMessageMediaPipe(prompt)
        }
    }
    
    private fun sendMessageMediaPipe(prompt: String) {
        generationJob?.cancel()

        if (!llmManager.isInitialized) {
            _uiState.update { it.copy(modelError = "Gemma model is not initialized yet.") }
            return
        }

        val userMessage = ChatMessage(text = prompt, isFromUser = true)
        _uiState.update { it.copy(messages = it.messages + userMessage) }
        _uiState.value.currentSessionId?.let { repository.appendMessage(it, userMessage) }

        generationJob = viewModelScope.launch {
            var fullResponse = ""
            val assistantMessageId = System.nanoTime()
            val assistantMessage = ChatMessage(id = assistantMessageId, text = "", isFromUser = false, isStreaming = true)
            _uiState.update { it.copy(isGenerating = true, messages = it.messages + assistantMessage) }
            
            val promptBuilder = StringBuilder()
            promptBuilder.append("<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n")
            
            llmManager.generateResponse(promptBuilder.toString()) { partialResult, done ->
                fullResponse += partialResult
                _uiState.update { currentState ->
                    val updatedMessages = currentState.messages.map {
                        if (it.id == assistantMessageId) {
                            it.copy(text = fullResponse)
                        } else it
                    }
                    currentState.copy(messages = updatedMessages)
                }
                
                if (done) {
                    _uiState.update { currentState ->
                        val finalMessages = currentState.messages.map {
                            if (it.id == assistantMessageId) {
                                it.copy(isStreaming = false)
                            } else it
                        }
                        currentState.copy(isGenerating = false, messages = finalMessages)
                    }
                    _uiState.value.currentSessionId?.let { sid -> repository.replaceMessages(sid, _uiState.value.messages) }
                }
            }
        }
    }

    private fun sendMessageAICore(prompt: String) {
        generationJob?.cancel()

        if (generativeModel == null) {
            _uiState.update { it.copy(modelError = "Model is not initialized yet.") }
            return
        }

        val currentPendingUri = _uiState.value.pendingImageUri
        val currentPendingDesc = _uiState.value.pendingImageDescription
        val userMessage = ChatMessage(text = prompt, isFromUser = true, imageUri = currentPendingUri, imageDescription = currentPendingDesc)
        _uiState.update { it.copy(messages = it.messages + userMessage, pendingImageUri = null, pendingImageDescription = null) }
        _uiState.value.currentSessionId?.let { repository.appendMessage(it, userMessage) }

        generationJob = viewModelScope.launch {
            try {
                val allowSearchThisTurn = _uiState.value.webSearchEnabled && webSearchService.isOnline()
                val offlineNotice = _uiState.value.webSearchEnabled && !allowSearchThisTurn
                val promptBuilder = StringBuilder()
                promptBuilder.append(PromptTemplates.systemPreamble(allowSearch = allowSearchThisTurn, offlineNotice = offlineNotice))

                // Add custom instructions if enabled
                if (_uiState.value.customInstructionsEnabled && _uiState.value.customInstructions.isNotBlank()) {
                    promptBuilder.append(PromptTemplates.customInstructionsBlock(_uiState.value.customInstructions))
                }

                promptBuilder.append(PromptTemplates.fewShotGeneral())
                if (allowSearchThisTurn) {
                    promptBuilder.append(PromptTemplates.fewShotSearch())
                }
                prependPersonalContextIfNeeded(promptBuilder)

                // Add memory context if enabled
                if (_uiState.value.memoryContextEnabled) {
                    val relevantMemories = PromptTemplates.buildMemoryContextFromQuery(
                        query = prompt,
                        allMemories = _uiState.value.memoryEntries
                    )

                    val bioInfo = if (_uiState.value.bioContextEnabled) _uiState.value.bioInformation else null
                    promptBuilder.append(PromptTemplates.memoryContextBlock(relevantMemories, bioInfo))
                }

                // Include prior image descriptions as context blocks
                if (_uiState.value.multimodalEnabled) {
                    _uiState.value.messages.forEach { msg ->
                        if (!msg.imageDescription.isNullOrBlank()) {
                            promptBuilder.append("[IMAGE_DESCRIPTION]\n${msg.imageDescription}\n[/IMAGE_DESCRIPTION]\n\n")
                        }
                    }
                }

                val history = _uiState.value.messages.takeLast(10)
                history.forEach { message ->
                    if (message.id == userMessage.id) return@forEach
                    if (message.isFromUser) promptBuilder.append("[USER]\n${message.text}\n[/USER]\n")
                    else promptBuilder.append("[ASSISTANT]\n${message.text}\n[/ASSISTANT]\n")
                }
                promptBuilder.append("[USER]\n$prompt\n[/USER]\n")
                promptBuilder.append("[ASSISTANT]\n")

                val fullPrompt = promptBuilder.toString()
                Log.d("ChatViewModel", "Sending prompt:\n$fullPrompt")

                var fullResponse = ""
                val stopTokens = listOf("[/ASSISTANT]", "[ASSISTANT]", "[/USER]", "[USER]")
                var searchTriggered = false
                var searchStartDetected = false
                var searchStartIndex = -1
                val streamStartMs = SystemClock.elapsedRealtime()

                generativeModel!!
                    .generateContentStream(fullPrompt)
                    .onStart {
                        _uiState.update {
                            it.copy(
                                isGenerating = true,
                                messages = it.messages + ChatMessage(text = "", isFromUser = false, isStreaming = true)
                            )
                        }
                    }
                    .onCompletion {
                        if (searchStartDetected && !searchTriggered) {
                            _uiState.update { it.copy(isSearchInProgress = false, currentSearchQuery = null) }
                        }
                        if (!searchTriggered) {
                            _uiState.update { currentState ->
                                val updated = currentState.messages.toMutableList()
                                if (updated.isNotEmpty() && updated.last().isStreaming) {
                                    val finalText = finalizeAssistantDisplayText(fullResponse, updated.last().text)
                                    updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                                }
                                currentState.copy(isGenerating = false, messages = updated)
                            }
                            _uiState.value.currentSessionId?.let { sid -> repository.replaceMessages(sid, _uiState.value.messages) }
                        }
                    }
                    .catch { e ->
                        Log.e("ChatViewModel", "Error generating content", e)
                        _uiState.update { currentState ->
                            val updated = currentState.messages.toMutableList()
                            if (updated.isNotEmpty() && updated.last().isStreaming) {
                                val errorText = "Error: ${e.message}"
                                val finalText = finalizeAssistantDisplayText(errorText, updated.last().text)
                                updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                            }
                            currentState.copy(isGenerating = false, messages = updated)
                        }
                        _uiState.value.currentSessionId?.let { sid -> repository.replaceMessages(sid, _uiState.value.messages) }
                    }
                    .collect { chunk ->
                        fullResponse += chunk.text

                        if (allowSearchThisTurn) {
                            val firstNonWs = fullResponse.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) Int.MAX_VALUE else it }
                            val searchToken = "[SEARCH]"
                            if (!searchStartDetected && firstNonWs != Int.MAX_VALUE) {
                                val after = fullResponse.substring(firstNonWs)
                                if (after.isNotEmpty() && after.length < searchToken.length && searchToken.startsWith(after)) {
                                    searchStartDetected = true
                                    searchStartIndex = firstNonWs
                                    _uiState.update { current ->
                                        val updated = current.messages.toMutableList()
                                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                                            updated[updated.lastIndex] = updated.last().copy(text = "")
                                        }
                                        current.copy(isSearchInProgress = true, currentSearchQuery = "", messages = updated)
                                    }
                                    return@collect
                                }
                            }
                            val startIdx = fullResponse.indexOf(searchToken)
                            if (!searchStartDetected && startIdx >= 0 && startIdx == firstNonWs) {
                                searchStartDetected = true
                                searchStartIndex = startIdx
                                val partialQuery = if (fullResponse.length >= startIdx + searchToken.length) {
                                    fullResponse.substring(startIdx + searchToken.length).substringBefore("[/SEARCH]").trim()
                                } else ""
                                _uiState.update { current ->
                                    val updated = current.messages.toMutableList()
                                    if (updated.isNotEmpty() && updated.last().isStreaming) {
                                        updated[updated.lastIndex] = updated.last().copy(text = "")
                                    }
                                    current.copy(isSearchInProgress = true, currentSearchQuery = partialQuery, messages = updated)
                                }
                            }
                            if (searchStartDetected) {
                                val tokenEndBase = searchStartIndex + searchToken.length
                                val partialQuery = if (fullResponse.length > tokenEndBase) fullResponse.substring(tokenEndBase).substringBefore("[/SEARCH]").trim() else ""
                                _uiState.update { it.copy(currentSearchQuery = partialQuery) }
                                val endIdx = if (fullResponse.length > tokenEndBase) fullResponse.indexOf("[/SEARCH]", tokenEndBase) else -1
                                if (endIdx != -1 && !searchTriggered) {
                                    searchTriggered = true
                                    val query = fullResponse.substring(tokenEndBase, endIdx).trim()
                                    viewModelScope.launch { continueWithSearchResultsUsingExistingBubble(userMessage, query) }
                                    generationJob?.cancel()
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
                            .map { token -> fullResponse.indexOf(token).let { idx -> if (idx >= 0) idx else Int.MAX_VALUE } }
                            .minOrNull() ?: Int.MAX_VALUE
                        delay(35)
                        _uiState.update { currentState ->
                            val updated = currentState.messages.toMutableList()
                            if (updated.isNotEmpty() && updated.last().isStreaming) {
                                val displayTextRaw = if (earliestIndex != Int.MAX_VALUE) fullResponse.substring(0, earliestIndex) else {
                                    val holdBack = findPartialStopSuffixLength(fullResponse, stopTokens)
                                    if (holdBack > 0 && fullResponse.length > holdBack) fullResponse.dropLast(holdBack) else fullResponse
                                }
                                val displayText = sanitizeAssistantText(displayTextRaw)
                                updated[updated.lastIndex] = updated.last().copy(text = displayText)
                            }
                            currentState.copy(messages = updated)
                        }
                        if (earliestIndex != Int.MAX_VALUE) {
                            generationJob?.cancel()
                        }
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("ChatViewModel", "Job cancelled as expected.")
                } else {
                    Log.e("ChatViewModel", "Exception during generation", e)
                    _uiState.update { it.copy(isGenerating = false, messages = it.messages + ChatMessage(text = "Error: ${e.message}", isFromUser = false)) }
                    _uiState.value.currentSessionId?.let { sid -> repository.replaceMessages(sid, _uiState.value.messages) }
                }
            }
        }
    }

    private suspend fun continueWithSearchResultsUsingExistingBubble(userMessage: ChatMessage, query: String) {
        try {
            val results = webSearchService.search(query)

            val promptBuilder = StringBuilder()
            promptBuilder.append(PromptTemplates.postSearchPreamble())

            // Add custom instructions if enabled
            if (_uiState.value.customInstructionsEnabled && _uiState.value.customInstructions.isNotBlank()) {
                promptBuilder.append(PromptTemplates.customInstructionsBlock(_uiState.value.customInstructions))
            }

            promptBuilder.append("[WEB_RESULTS]\n${results}\n[/WEB_RESULTS]\n\n")

            // Add memory context if enabled
            if (_uiState.value.memoryContextEnabled) {
                val relevantMemories = PromptTemplates.buildMemoryContextFromQuery(
                    query = userMessage.text,
                    allMemories = _uiState.value.memoryEntries
                )

                val bioInfo = if (_uiState.value.bioContextEnabled) _uiState.value.bioInformation else null
                promptBuilder.append(PromptTemplates.memoryContextBlock(relevantMemories, bioInfo))
            }
            // Build recent history WITHOUT any trailing streaming placeholder bubble (we never persisted it)
            val recent = _uiState.value.messages.filter { !it.isStreaming }
            (recent + userMessage).takeLast(10).forEach { m ->
                if (m.isFromUser) promptBuilder.append("[USER]\n${m.text}\n[/USER]\n")
                else promptBuilder.append("[ASSISTANT]\n${m.text}\n[/ASSISTANT]\n")
            }
            promptBuilder.append("[ASSISTANT]\n")

            val fullPrompt = promptBuilder.toString()
            Log.d("ChatViewModel", "Follow-up with web results (reuse bubble):\n$fullPrompt")

            var fullResponse = ""
            val stopTokens = listOf("[/ASSISTANT]", "[ASSISTANT]", "[/USER]", "[USER]")
            val streamStartMs = SystemClock.elapsedRealtime()

            generativeModel!!.generateContentStream(fullPrompt)
                .onStart {
                    // Hide searching flag and reuse the same streaming bubble
                    _uiState.update { it.copy(isSearchInProgress = false, currentSearchQuery = null, isGenerating = true) }
                }
                .onCompletion {
                    _uiState.update { current ->
                        val updated = current.messages.toMutableList()
                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                            val finalText = finalizeAssistantDisplayText(fullResponse, updated.last().text)
                            updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                        }
                        current.copy(isGenerating = false, messages = updated)
                    }
                    _uiState.value.currentSessionId?.let { sid -> repository.replaceMessages(sid, _uiState.value.messages) }
                }
                .catch { e ->
                    Log.e("ChatViewModel", "Error generating after search", e)
                    _uiState.update { current ->
                        val updated = current.messages.toMutableList()
                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                            val errorText = "Error: ${e.message}"
                            val finalText = finalizeAssistantDisplayText(errorText, updated.last().text)
                            updated[updated.lastIndex] = updated.last().copy(text = finalText, isStreaming = false)
                        }
                        current.copy(isGenerating = false, messages = updated)
                    }
                }
                .collect { chunk ->
                    fullResponse += chunk.text
                    // Initial suppression window for follow-up, too
                    val elapsed = SystemClock.elapsedRealtime() - streamStartMs
                    if (elapsed < 300 && fullResponse.length < 24) {
                        return@collect
                    }
                    val earliestIndex = stopTokens
                        .map { token -> fullResponse.indexOf(token).let { idx -> if (idx >= 0) idx else Int.MAX_VALUE } }
                        .minOrNull() ?: Int.MAX_VALUE
                    delay(35)
                    _uiState.update { current ->
                        val updated = current.messages.toMutableList()
                        if (updated.isNotEmpty() && updated.last().isStreaming) {
                            val displayTextRaw = if (earliestIndex != Int.MAX_VALUE) fullResponse.substring(0, earliestIndex) else {
                                val holdBack = findPartialStopSuffixLength(fullResponse, stopTokens)
                                if (holdBack > 0 && fullResponse.length > holdBack) fullResponse.dropLast(holdBack) else fullResponse
                            }
                            val displayText = sanitizeAssistantText(displayTextRaw)
                            updated[updated.lastIndex] = updated.last().copy(text = displayText)
                        }
                        current.copy(messages = updated)
                    }
                    if (earliestIndex != Int.MAX_VALUE) {
                        generationJob?.cancel()
                    }
                }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSearchInProgress = false, currentSearchQuery = null, modelError = e.message) }
        }
    }

    private fun findPartialStopSuffixLength(text: String, tokens: List<String>): Int {
        var maxLen = 0
        tokens.forEach { token ->
            val maxCheck = minOf(token.length - 1, text.length)
            for (k in maxCheck downTo 1) {
                if (text.endsWith(token.substring(0, k))) {
                    if (k > maxLen) maxLen = k
                    break
                }
            }
        }
        return maxLen
    }

    // Custom Instructions Management
    fun addCustomInstruction(title: String, instruction: String, category: String = "General") {
        viewModelScope.launch {
            try {
                val newInstruction = CustomInstruction(
                    title = title,
                    instruction = instruction,
                    category = category
                )
                memoryRepository.addCustomInstruction(newInstruction)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to add custom instruction: ${e.message}") }
            }
        }
    }

    fun updateCustomInstruction(instruction: CustomInstruction) {
        viewModelScope.launch {
            try {
                memoryRepository.updateCustomInstruction(instruction)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to update custom instruction: ${e.message}") }
            }
        }
    }

    fun deleteCustomInstruction(instructionId: String) {
        viewModelScope.launch {
            try {
                memoryRepository.deleteCustomInstruction(instructionId)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to delete custom instruction: ${e.message}") }
            }
        }
    }

    fun toggleCustomInstruction(instructionId: String) {
        viewModelScope.launch {
            try {
                memoryRepository.toggleCustomInstruction(instructionId)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to toggle custom instruction: ${e.message}") }
            }
        }
    }

    // Memory Entries Management
    fun addMemoryEntry(content: String) {
        viewModelScope.launch {
            try {
                val newMemory = MemoryEntry(
                    content = content
                )
                memoryRepository.addMemoryEntry(newMemory)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to add memory entry: ${e.message}") }
            }
        }
    }

    fun updateMemoryEntry(memory: MemoryEntry) {
        viewModelScope.launch {
            try {
                memoryRepository.updateMemoryEntry(memory)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to update memory entry: ${e.message}") }
            }
        }
    }

    fun deleteMemoryEntry(memoryId: String) {
        viewModelScope.launch {
            try {
                memoryRepository.deleteMemoryEntry(memoryId)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to delete memory entry: ${e.message}") }
            }
        }
    }

    fun toggleMemoryEntry(memoryId: String) {
        viewModelScope.launch {
            try {
                memoryRepository.toggleMemoryEntry(memoryId)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to toggle memory entry: ${e.message}") }
            }
        }
    }

    fun updateMemoryLastAccessed(memoryId: String) {
        viewModelScope.launch {
            try {
                memoryRepository.updateMemoryLastAccessed(memoryId)
                // Don't reload all data, just update the specific memory
                val updatedMemories = _uiState.value.memoryEntries.map { memory ->
                    if (memory.id == memoryId) {
                        memory.copy(lastAccessed = System.currentTimeMillis())
                    } else memory
                }
                _uiState.update { it.copy(memoryEntries = updatedMemories) }
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to update memory access time: ${e.message}") }
            }
        }
    }

    // Bio Information Management
    fun saveBioInformation(bio: BioInformation) {
        viewModelScope.launch {
            try {
                memoryRepository.saveBioInformation(bio)
                loadMemoryData()
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to save bio information: ${e.message}") }
            }
        }
    }

    fun deleteBioInformation() {
        viewModelScope.launch {
            try {
                memoryRepository.deleteBioInformation()
                _uiState.update { it.copy(bioInformation = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to delete bio information: ${e.message}") }
            }
        }
    }

    // Search and Filter Methods
    fun searchMemoryEntries(query: String) {
        viewModelScope.launch {
            try {
                val results = memoryRepository.searchMemoryEntries(query)
                _uiState.update { it.copy(memoryEntries = results, memorySearchQuery = query) }
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to search memories: ${e.message}") }
            }
        }
    }

    fun clearMemorySearch() {
        loadMemoryData()
        _uiState.update { it.copy(memorySearchQuery = "", selectedMemoryCategory = null) }
    }


    // Data Import/Export
    fun exportAllMemoryData(): String? {
        return try {
            memoryRepository.exportAllData()
        } catch (e: Exception) {
            _uiState.update { it.copy(memoryError = "Failed to export data: ${e.message}") }
            null
        }
    }

    fun importMemoryData(jsonData: String) {
        viewModelScope.launch {
            try {
                val result = memoryRepository.importData(jsonData)
                when (result) {
                    is MemoryRepository.ImportResult.Success -> {
                        loadMemoryData()
                        _uiState.update { it.copy(memoryError = null) }
                    }
                    is MemoryRepository.ImportResult.Error -> {
                        _uiState.update { it.copy(memoryError = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(memoryError = "Failed to import data: ${e.message}") }
            }
        }
    }

    // Utility Methods
    fun clearMemoryError() {
        _uiState.update { it.copy(memoryError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        generativeModel?.close()
        imageDescriptionService.close()
        llmManager.close()
    }
}
