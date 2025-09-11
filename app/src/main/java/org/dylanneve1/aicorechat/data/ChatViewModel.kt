package org.dylanneve1.aicorechat.data

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generativeModel: GenerativeModel? = null
    private var generationJob: Job? = null

    private val sharedPreferences =
        application.getSharedPreferences("AICoreChatPrefs", Context.MODE_PRIVATE)

    private val repository = ChatRepository(application.applicationContext)

    companion object {
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_TOP_K = "top_k"
    }

    init {
        loadSettings()
        initOrStartNewSession()
        reinitializeModel()
    }

    private fun loadSettings() {
        val temperature = sharedPreferences.getFloat(KEY_TEMPERATURE, 0.3f)
        val topK = sharedPreferences.getInt(KEY_TOP_K, 40)
        _uiState.update { it.copy(temperature = temperature, topK = topK) }
    }

    private fun initOrStartNewSession() {
        val sessions = repository.loadSessions()
        val session = repository.createNewSession()
        _uiState.update {
            it.copy(
                sessions = sessions.map { s -> ChatSessionMeta(s.id, s.name) }.let { existing ->
                    listOf(ChatSessionMeta(session.id, session.name)) + existing
                },
                currentSessionId = session.id,
                currentSessionName = session.name,
                messages = emptyList()
            )
        }
    }

    private fun reinitializeModel() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(modelError = "Initializing model…") }
                generativeModel?.close()

                val config = generationConfig {
                    context = getApplication<Application>().applicationContext
                    temperature = _uiState.value.temperature
                    topK = _uiState.value.topK
                }

                generativeModel = GenerativeModel(generationConfig = config)
                generativeModel?.prepareInferenceEngine()
                _uiState.update { it.copy(modelError = null) }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error initializing model", e)
                _uiState.update { it.copy(modelError = "Model initialization failed: ${e.message}") }
            }
        }
    }

    fun newChat() {
        val session = repository.createNewSession()
        val allSessions = repository.loadSessions()
        _uiState.update {
            it.copy(
                sessions = allSessions.map { s -> ChatSessionMeta(s.id, s.name) },
                currentSessionId = session.id,
                currentSessionName = session.name,
                messages = emptyList()
            )
        }
    }

    fun selectChat(sessionId: Long) {
        val sessions = repository.loadSessions()
        val selected = sessions.find { it.id == sessionId } ?: return
        _uiState.update {
            it.copy(
                currentSessionId = selected.id,
                currentSessionName = selected.name,
                messages = selected.messages.toList()
            )
        }
    }

    fun renameCurrentChat(newName: String) {
        val sessionId = _uiState.value.currentSessionId ?: return
        repository.renameSession(sessionId, newName)
        val updatedSessions = _uiState.value.sessions.map {
            if (it.id == sessionId) it.copy(name = newName) else it
        }
        _uiState.update { it.copy(currentSessionName = newName, sessions = updatedSessions) }
    }

    fun renameChat(sessionId: Long, newName: String) {
        repository.renameSession(sessionId, newName)
        val updatedSessions = _uiState.value.sessions.map {
            if (it.id == sessionId) it.copy(name = newName) else it
        }
        _uiState.update { state ->
            val currentName = if (state.currentSessionId == sessionId) newName else state.currentSessionName
            state.copy(currentSessionName = currentName, sessions = updatedSessions)
        }
    }

    fun deleteChat(sessionId: Long) {
        repository.deleteSession(sessionId)
        val remaining = repository.loadSessions()
        if (remaining.isEmpty()) {
            newChat()
        } else {
            val next = remaining.first()
            _uiState.update {
                it.copy(
                    sessions = remaining.map { s -> ChatSessionMeta(s.id, s.name) },
                    currentSessionId = next.id,
                    currentSessionName = next.name,
                    messages = next.messages.toList()
                )
            }
        }
    }

    fun wipeAllChats() {
        repository.wipeAllSessions()
        newChat()
    }

    fun updateTemperature(temperature: Float) {
        sharedPreferences.edit().putFloat(KEY_TEMPERATURE, temperature).apply()
        _uiState.update { it.copy(temperature = temperature) }
        reinitializeModel()
    }

    fun updateTopK(topK: Int) {
        sharedPreferences.edit().putInt(KEY_TOP_K, topK).apply()
        _uiState.update { it.copy(topK = topK) }
        reinitializeModel()
    }

    fun clearChat() {
        // Delete current chat session entirely and start a new one
        _uiState.value.currentSessionId?.let { repository.deleteSession(it) }
        newChat()
    }

    fun stopGeneration() {
        generationJob?.cancel()
    }

    fun generateChatTitle() {
        if (_uiState.value.isGenerating) {
            _uiState.update { it.copy(modelError = "Cannot rename while generating.") }
            return
        }
        val sessionId = _uiState.value.currentSessionId
        if (sessionId == null || _uiState.value.messages.none { it.isFromUser }) {
            _uiState.update { it.copy(modelError = "Chat is empty.") }
            return
        }
        val model = generativeModel
        if (model == null) {
            _uiState.update { it.copy(modelError = "Model not ready.") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isTitleGenerating = true, modelError = null) }
                val history = _uiState.value.messages
                val sb = StringBuilder()
                sb.append("You are to summarize the following chat into a very short, descriptive title.\n")
                sb.append("Rules: 3-4 words max, no quotes, no punctuation, Title Case, be specific.\n\n")
                history.forEach { m ->
                    if (m.isFromUser) sb.append("User: ${m.text}\n") else sb.append("Assistant: ${m.text}\n")
                }
                sb.append("\nReturn only the title.")
                val prompt = sb.toString()

                var result = ""
                model.generateContentStream(prompt)
                    .onStart { /* no-op */ }
                    .onCompletion {
                        val cleaned = result
                            .replace("\n", " ")
                            .replace("\"", "")
                            .trim()
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(4)
                            .joinToString(" ")
                            .replace(Regex("[.,!?:;]+$"), "")
                        if (cleaned.isNotBlank()) {
                            renameCurrentChat(cleaned)
                        }
                        _uiState.update { it.copy(isTitleGenerating = false) }
                    }
                    .catch { e ->
                        Log.e("ChatViewModel", "Error generating title", e)
                        _uiState.update { it.copy(isTitleGenerating = false, modelError = e.message) }
                    }
                    .collect { chunk ->
                        result += chunk.text
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isTitleGenerating = false, modelError = e.message) }
            }
        }
    }

    fun sendMessage(prompt: String) {
        generationJob?.cancel()

        if (generativeModel == null) {
            _uiState.update { it.copy(modelError = "Model is not initialized yet.") }
            return
        }

        val userMessage = ChatMessage(text = prompt, isFromUser = true)
        _uiState.update {
            it.copy(messages = it.messages + userMessage)
        }
        _uiState.value.currentSessionId?.let { repository.appendMessage(it, userMessage) }

        generationJob = viewModelScope.launch {
            try {
                val promptBuilder = StringBuilder()
                promptBuilder.append("You are a helpful AI assistant. Follow the user's instructions carefully.\n\n")
                promptBuilder.append("[USER]\nHello, who are you?\n")
                promptBuilder.append("[ASSISTANT]\nI am a helpful AI assistant built to answer your questions.\n\n")

                val history = _uiState.value.messages.takeLast(10)

                history.forEach { message ->
                    if (message.id == userMessage.id) return@forEach
                    if (message.isFromUser) {
                        promptBuilder.append("[USER]\n${message.text}\n")
                    } else {
                        promptBuilder.append("[ASSISTANT]\n${message.text}\n")
                    }
                }
                promptBuilder.append("[USER]\n$prompt\n")
                promptBuilder.append("[ASSISTANT]\n")

                val fullPrompt = promptBuilder.toString()
                Log.d("ChatViewModel", "Sending prompt:\n$fullPrompt")

                var fullResponse = ""
                generativeModel!!
                    .generateContentStream(fullPrompt)
                    .onStart {
                        _uiState.update {
                            it.copy(
                                isGenerating = true,
                                messages = it.messages + ChatMessage(
                                    text = "",
                                    isFromUser = false,
                                    isStreaming = true
                                )
                            )
                        }
                    }
                    .onCompletion {
                        Log.d("ChatViewModel", "Generation completed or cancelled.")
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.messages.toMutableList()
                            if (updatedMessages.isNotEmpty() && updatedMessages.last().isStreaming) {
                                updatedMessages[updatedMessages.lastIndex] = updatedMessages.last().copy(isStreaming = false)
                            }
                            val newState = currentState.copy(isGenerating = false, messages = updatedMessages)
                            newState
                        }
                        _uiState.value.currentSessionId?.let { sid ->
                            repository.replaceMessages(sid, _uiState.value.messages)
                        }
                    }
                    .catch { e ->
                        Log.e("ChatViewModel", "Error generating content", e)
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.messages.toMutableList()
                            if (updatedMessages.isNotEmpty() && updatedMessages.last().isStreaming) {
                                updatedMessages[updatedMessages.lastIndex] =
                                    updatedMessages.last().copy(text = "Error: ${e.message}", isStreaming = false)
                            }
                            currentState.copy(isGenerating = false, messages = updatedMessages)
                        }
                        _uiState.value.currentSessionId?.let { sid ->
                            repository.replaceMessages(sid, _uiState.value.messages)
                        }
                    }
                    .collect { chunk ->
                        fullResponse += chunk.text
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.messages.toMutableList()
                            if (updatedMessages.isNotEmpty() && updatedMessages.last().isStreaming) {
                                val cleanedText = fullResponse.substringBefore("[USER]").trim()
                                updatedMessages[updatedMessages.lastIndex] = updatedMessages.last().copy(text = cleanedText)
                            }
                            val newState = currentState.copy(messages = updatedMessages)
                            newState
                        }
                        _uiState.value.currentSessionId?.let { sid ->
                            repository.replaceMessages(sid, _uiState.value.messages)
                        }

                        if (fullResponse.contains("[USER]")) {
                            Log.d("ChatViewModel", "Stop sequence '[USER]' detected. Cancelling job.")
                            generationJob?.cancel()
                        }
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("ChatViewModel", "Job cancelled as expected.")
                } else {
                    Log.e("ChatViewModel", "Exception during generation", e)
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            messages = it.messages + ChatMessage(text = "Error: ${e.message}", isFromUser = false)
                        )
                    }
                    _uiState.value.currentSessionId?.let { sid ->
                        repository.replaceMessages(sid, _uiState.value.messages)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        generativeModel?.close()
    }
}
