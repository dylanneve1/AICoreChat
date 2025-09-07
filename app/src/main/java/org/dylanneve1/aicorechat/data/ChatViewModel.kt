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

    companion object {
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_TOP_K = "top_k"
    }

    init {
        loadSettings()
        reinitializeModel()
    }

    private fun loadSettings() {
        val temperature = sharedPreferences.getFloat(KEY_TEMPERATURE, 0.3f)
        val topK = sharedPreferences.getInt(KEY_TOP_K, 40)
        _uiState.update { it.copy(temperature = temperature, topK = topK) }
    }

    private fun reinitializeModel() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(modelError = "Initializing model…") }
                generativeModel?.close() // Clean up the old model if it exists

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
        _uiState.update { it.copy(messages = emptyList(), modelError = null) }
    }

    fun stopGeneration() {
        generationJob?.cancel()
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
                            currentState.copy(isGenerating = false, messages = updatedMessages)
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
                    }
                    .collect { chunk ->
                        fullResponse += chunk.text
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.messages.toMutableList()
                            if (updatedMessages.isNotEmpty() && updatedMessages.last().isStreaming) {
                                val cleanedText = fullResponse.substringBefore("[USER]").trim()
                                updatedMessages[updatedMessages.lastIndex] = updatedMessages.last().copy(text = cleanedText)
                            }
                            currentState.copy(messages = updatedMessages)
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
