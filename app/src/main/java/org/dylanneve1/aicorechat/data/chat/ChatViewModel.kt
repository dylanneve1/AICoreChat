package org.dylanneve1.aicorechat.data.chat

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dylanneve1.aicorechat.data.BioInformation
import org.dylanneve1.aicorechat.data.ChatRepository
import org.dylanneve1.aicorechat.data.CustomInstruction
import org.dylanneve1.aicorechat.data.MemoryEntry
import org.dylanneve1.aicorechat.data.MemoryRepository
import org.dylanneve1.aicorechat.data.MemoryRepository.ImportResult
import org.dylanneve1.aicorechat.data.chat.generation.ChatGenerationManager
import org.dylanneve1.aicorechat.data.chat.generation.ChatModelManager
import org.dylanneve1.aicorechat.data.chat.generation.ChatTitleManager
import org.dylanneve1.aicorechat.data.chat.media.ChatMediaHandler
import org.dylanneve1.aicorechat.data.chat.memory.ChatMemoryManager
import org.dylanneve1.aicorechat.data.chat.memory.MemorySnapshot
import org.dylanneve1.aicorechat.data.chat.model.ChatUiState
import org.dylanneve1.aicorechat.data.chat.prompt.ChatPromptBuilder
import org.dylanneve1.aicorechat.data.chat.session.ChatSessionManager
import org.dylanneve1.aicorechat.data.chat.session.SessionState
import org.dylanneve1.aicorechat.data.chat.settings.ChatPreferences
import org.dylanneve1.aicorechat.data.chat.settings.ChatSettingsManager
import org.dylanneve1.aicorechat.data.context.PersonalContextBuilder
import org.dylanneve1.aicorechat.data.image.ImageDescriptionService
import org.dylanneve1.aicorechat.data.search.WebSearchService

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val sharedPreferences =
        application.getSharedPreferences("AICoreChatPrefs", Context.MODE_PRIVATE)

    private val chatRepository = ChatRepository(application.applicationContext)
    private val memoryRepository = MemoryRepository(application.applicationContext)

    private val settingsManager = ChatSettingsManager(sharedPreferences)
    private val sessionManager = ChatSessionManager(chatRepository)
    private val memoryManager = ChatMemoryManager(memoryRepository)

    private val personalContextBuilder = PersonalContextBuilder(application)
    private val promptBuilder = ChatPromptBuilder(personalContextBuilder)

    private val mediaHandler = ChatMediaHandler(
        application = application,
        scope = viewModelScope,
        state = _uiState,
        imageDescriptionService = ImageDescriptionService(application),
    )
    private val webSearchService = WebSearchService(application)
    private val modelManager = ChatModelManager(application)
    private val generationManager = ChatGenerationManager(
        application = application,
        scope = viewModelScope,
        state = _uiState,
        sessionManager = sessionManager,
        promptBuilder = promptBuilder,
        modelManager = modelManager,
        webSearchService = webSearchService,
    )
    private val titleManager = ChatTitleManager(
        scope = viewModelScope,
        state = _uiState,
        sessionManager = sessionManager,
        modelManager = modelManager,
    )

    init {
        loadSettings()
        refreshMemoryData()
        initializeSession()
        reinitializeModel()
    }

    // region Initialization helpers

    private fun loadSettings() {
        val prefs: ChatPreferences = settingsManager.load()
        _uiState.update {
            it.copy(
                temperature = prefs.temperature,
                topK = prefs.topK,
                userName = prefs.userName,
                personalContextEnabled = prefs.personalContextEnabled,
                webSearchEnabled = prefs.webSearchEnabled,
                multimodalEnabled = prefs.multimodalEnabled,
                memoryContextEnabled = prefs.memoryContextEnabled,
                customInstructionsEnabled = prefs.customInstructionsEnabled,
                customInstructions = prefs.customInstructions,
                bioContextEnabled = prefs.bioContextEnabled,
            )
        }
    }

    private fun initializeSession() {
        val sessionState = sessionManager.startInitialSession()
        applySessionState(sessionState)
    }

    private fun reinitializeModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(modelError = "Initializing model…") }
            val result = modelManager.reinitialize(
                temperature = _uiState.value.temperature,
                topK = _uiState.value.topK,
            )
            _uiState.update { state ->
                if (result.isSuccess) {
                    state.copy(modelError = null)
                } else {
                    state.copy(modelError = "Model initialization failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    private fun refreshMemoryData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMemoryLoading = true, memoryError = null) }
            val result = memoryManager.refreshSnapshot()
            applyMemorySnapshot(result, "Failed to load memory data")
        }
    }

    private fun applySessionState(sessionState: SessionState) {
        _uiState.update {
            it.copy(
                sessions = sessionState.sessions,
                currentSessionId = sessionState.currentSessionId,
                currentSessionName = sessionState.currentSessionName,
                messages = sessionState.messages,
            )
        }
    }

    private fun applyMemorySnapshot(result: Result<MemorySnapshot>, failureMessage: String) {
        result.onSuccess { snapshot ->
            _uiState.update {
                it.copy(
                    memoryEntries = snapshot.entries,
                    bioInformation = snapshot.bioInformation,
                    isMemoryLoading = false,
                    memoryError = null,
                )
            }
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(
                    isMemoryLoading = false,
                    memoryError = "$failureMessage: ${throwable.message}",
                )
            }
        }
    }

    private fun applyMemoryEntries(result: Result<List<MemoryEntry>>, failureMessage: String) {
        result.onSuccess { entries ->
            _uiState.update { it.copy(memoryEntries = entries, memoryError = null) }
        }.onFailure { throwable ->
            _uiState.update { it.copy(memoryError = "$failureMessage: ${throwable.message}") }
        }
    }

    // endregion

    // region Settings management

    fun updateUserName(name: String) {
        settingsManager.setUserName(name)
        _uiState.update { it.copy(userName = name) }
    }

    fun updatePersonalContextEnabled(enabled: Boolean) {
        settingsManager.setPersonalContextEnabled(enabled)
        _uiState.update { it.copy(personalContextEnabled = enabled) }
    }

    fun updateWebSearchEnabled(enabled: Boolean) {
        settingsManager.setWebSearchEnabled(enabled)
        _uiState.update { it.copy(webSearchEnabled = enabled) }
    }

    fun updateMultimodalEnabled(enabled: Boolean) {
        settingsManager.setMultimodalEnabled(enabled)
        _uiState.update { it.copy(multimodalEnabled = enabled) }
        if (!enabled) {
            mediaHandler.clearPendingImage()
        }
    }

    fun updateMemoryContextEnabled(enabled: Boolean) {
        settingsManager.setMemoryContextEnabled(enabled)
        _uiState.update { it.copy(memoryContextEnabled = enabled) }
    }

    fun updateCustomInstructionsEnabled(enabled: Boolean) {
        settingsManager.setCustomInstructionsEnabled(enabled)
        _uiState.update { it.copy(customInstructionsEnabled = enabled) }
    }

    fun updateBioContextEnabled(enabled: Boolean) {
        settingsManager.setBioContextEnabled(enabled)
        _uiState.update { it.copy(bioContextEnabled = enabled) }
    }

    fun updateBioInformation(name: String, age: String, occupation: String, location: String) {
        val bio = if (name.isNotBlank() || age.isNotBlank() || occupation.isNotBlank() || location.isNotBlank()) {
            BioInformation(
                id = "user_bio",
                name = name.takeIf { it.isNotBlank() },
                age = age.toIntOrNull(),
                occupation = occupation.takeIf { it.isNotBlank() },
                location = location.takeIf { it.isNotBlank() },
            )
        } else {
            null
        }

        bio?.let {
            viewModelScope.launch {
                val result = memoryManager.saveBioInformation(it)
                applyMemorySnapshot(result, "Failed to save bio information")
            }
        }
        _uiState.update { it.copy(bioInformation = bio) }
    }

    fun updateCustomInstructions(instructions: String, enabled: Boolean) {
        settingsManager.setCustomInstructions(instructions)
        settingsManager.setCustomInstructionsEnabled(enabled)
        _uiState.update {
            it.copy(
                customInstructionsEnabled = enabled,
                customInstructions = instructions,
            )
        }
    }

    fun updateTemperature(temperature: Float) {
        settingsManager.setTemperature(temperature)
        _uiState.update { it.copy(temperature = temperature) }
        reinitializeModel()
    }

    fun updateTopK(topK: Int) {
        settingsManager.setTopK(topK)
        _uiState.update { it.copy(topK = topK) }
        reinitializeModel()
    }

    fun resetModelSettings() {
        settingsManager.resetModelDefaults()
        _uiState.update {
            it.copy(
                temperature = ChatSettingsManager.defaultTemperature(),
                topK = ChatSettingsManager.defaultTopK(),
            )
        }
        reinitializeModel()
    }

    // endregion

    // region Media

    fun clearPendingImage() = mediaHandler.clearPendingImage()

    fun onImageSelected(uri: Uri) = mediaHandler.onImageSelected(uri)

    fun onImagePicked(bitmap: Bitmap) = mediaHandler.onImagePicked(bitmap)

    // endregion

    // region Sessions

    fun newChat() {
        val sessionState = sessionManager.createAndSelectNew()
        applySessionState(sessionState)
    }

    fun selectChat(sessionId: Long) {
        val previousSessionId = _uiState.value.currentSessionId
        val previousMessages = _uiState.value.messages
        val sessionState = sessionManager.selectSession(sessionId, previousSessionId, previousMessages) ?: return
        applySessionState(sessionState)
    }

    fun renameCurrentChat(newName: String) {
        val sessionId = _uiState.value.currentSessionId ?: return
        sessionManager.renameSession(sessionId, newName)
        _uiState.update { state ->
            val updatedSessions = state.sessions.map { if (it.id == sessionId) it.copy(name = newName) else it }
            state.copy(currentSessionName = newName, sessions = updatedSessions)
        }
    }

    fun renameChat(sessionId: Long, newName: String) {
        sessionManager.renameSession(sessionId, newName)
        _uiState.update { state ->
            val updatedSessions = state.sessions.map { if (it.id == sessionId) it.copy(name = newName) else it }
            val currentName = if (state.currentSessionId == sessionId) newName else state.currentSessionName
            state.copy(currentSessionName = currentName, sessions = updatedSessions)
        }
    }

    fun deleteChat(sessionId: Long) {
        val sessionState = sessionManager.deleteSession(sessionId)
        applySessionState(sessionState)
    }

    fun wipeAllChats() {
        val sessionState = sessionManager.wipeAllSessions()
        applySessionState(sessionState)
    }

    fun purgeEmptyChats() {
        val currentId = _uiState.value.currentSessionId
        val sessions = sessionManager.purgeEmptySessions(currentId)
        _uiState.update { it.copy(sessions = sessions) }
    }

    fun clearChat() {
        val sessionId = _uiState.value.currentSessionId ?: return
        val sessionState = sessionManager.deleteSession(sessionId)
        applySessionState(sessionState)
    }

    // endregion

    // region Generation & titles

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        generationManager.sendMessage(prompt)
    }

    fun regenerateAssistantResponse(messageId: Long) {
        viewModelScope.launch {
            val stateSnapshot = _uiState.value
            val index = stateSnapshot.messages.indexOfFirst { it.id == messageId }
            if (index == -1) return@launch

            val targetMessage = stateSnapshot.messages[index]
            if (targetMessage.isFromUser) return@launch

            generationManager.stopGeneration()

            val retainedMessages = stateSnapshot.messages.take(index)
            val userMessage = retainedMessages.lastOrNull()
            if (userMessage == null || !userMessage.isFromUser) {
                _uiState.update { it.copy(modelError = "Cannot regenerate without a preceding user message.") }
                return@launch
            }

            _uiState.update {
                it.copy(
                    messages = retainedMessages,
                    isGenerating = false,
                    isSearchInProgress = false,
                    currentSearchQuery = null,
                    modelError = null,
                )
            }

            stateSnapshot.currentSessionId?.let { sessionId ->
                sessionManager.replaceMessages(sessionId, retainedMessages)
            }

            generationManager.regenerateFromUserMessage(userMessage)
        }
    }

    fun editUserMessage(messageId: Long, newText: String) {
        val sanitized = newText.trim()
        if (sanitized.isBlank()) return

        viewModelScope.launch {
            val stateSnapshot = _uiState.value
            val index = stateSnapshot.messages.indexOfFirst { it.id == messageId }
            if (index == -1) return@launch

            val original = stateSnapshot.messages[index]
            if (!original.isFromUser) return@launch

            generationManager.stopGeneration()

            val updatedUserMessage = original.copy(text = sanitized, timestamp = System.currentTimeMillis())
            val retainedMessages = stateSnapshot.messages.take(index + 1).toMutableList().apply {
                this[index] = updatedUserMessage
            }.toList()

            _uiState.update {
                it.copy(
                    messages = retainedMessages,
                    isGenerating = false,
                    isSearchInProgress = false,
                    currentSearchQuery = null,
                    modelError = null,
                )
            }

            stateSnapshot.currentSessionId?.let { sessionId ->
                sessionManager.replaceMessages(sessionId, retainedMessages)
            }

            generationManager.regenerateFromUserMessage(updatedUserMessage)
        }
    }

    fun stopGeneration() = generationManager.stopGeneration()

    fun generateChatTitle() = titleManager.generateCurrentChatTitle()

    fun generateTitlesForAllChats() = titleManager.generateAllChatTitles()

    // endregion

    // region Memory & custom instructions

    fun addCustomInstruction(title: String, instruction: String, category: String = "General") {
        viewModelScope.launch {
            val result = memoryManager.addCustomInstruction(
                CustomInstruction(title = title, instruction = instruction, category = category),
            )
            applyMemorySnapshot(result, "Failed to add custom instruction")
        }
    }

    fun updateCustomInstruction(instruction: CustomInstruction) {
        viewModelScope.launch {
            val result = memoryManager.updateCustomInstruction(instruction)
            applyMemorySnapshot(result, "Failed to update custom instruction")
        }
    }

    fun deleteCustomInstruction(instructionId: String) {
        viewModelScope.launch {
            val result = memoryManager.deleteCustomInstruction(instructionId)
            applyMemorySnapshot(result, "Failed to delete custom instruction")
        }
    }

    fun toggleCustomInstruction(instructionId: String) {
        viewModelScope.launch {
            val result = memoryManager.toggleCustomInstruction(instructionId)
            applyMemorySnapshot(result, "Failed to toggle custom instruction")
        }
    }

    fun addMemoryEntry(content: String) {
        viewModelScope.launch {
            val result = memoryManager.addMemoryEntry(content)
            applyMemorySnapshot(result, "Failed to add memory entry")
        }
    }

    fun updateMemoryEntry(memory: MemoryEntry) {
        viewModelScope.launch {
            val result = memoryManager.updateMemoryEntry(memory)
            applyMemorySnapshot(result, "Failed to update memory entry")
        }
    }

    fun deleteMemoryEntry(memoryId: String) {
        viewModelScope.launch {
            val result = memoryManager.deleteMemoryEntry(memoryId)
            applyMemorySnapshot(result, "Failed to delete memory entry")
        }
    }

    fun toggleMemoryEntry(memoryId: String) {
        viewModelScope.launch {
            val result = memoryManager.toggleMemoryEntry(memoryId)
            applyMemorySnapshot(result, "Failed to toggle memory entry")
        }
    }

    fun updateMemoryLastAccessed(memoryId: String) {
        viewModelScope.launch {
            val result = memoryManager.updateMemoryLastAccessed(memoryId)
            applyMemoryEntries(result, "Failed to update memory access time")
        }
    }

    fun saveBioInformation(bio: BioInformation) {
        viewModelScope.launch {
            val result = memoryManager.saveBioInformation(bio)
            applyMemorySnapshot(result, "Failed to save bio information")
        }
    }

    fun deleteBioInformation() {
        viewModelScope.launch {
            val result = memoryManager.deleteBioInformation()
            applyMemorySnapshot(result, "Failed to delete bio information")
        }
    }

    fun searchMemoryEntries(query: String) {
        viewModelScope.launch {
            val result = memoryManager.searchMemoryEntries(query)
            result.onSuccess { entries ->
                _uiState.update {
                    it.copy(
                        memoryEntries = entries,
                        memorySearchQuery = query,
                        memoryError = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(memoryError = "Failed to search memories: ${throwable.message}") }
            }
        }
    }

    fun clearMemorySearch() {
        _uiState.update { it.copy(memorySearchQuery = "", selectedMemoryCategory = null) }
        refreshMemoryData()
    }

    fun exportAllMemoryData(): String? {
        val result = memoryManager.exportAllMemoryData()
        return result.getOrElse { throwable ->
            _uiState.update { it.copy(memoryError = "Failed to export data: ${throwable.message}") }
            null
        }
    }

    fun importMemoryData(jsonData: String) {
        viewModelScope.launch {
            val result = memoryManager.importMemoryData(jsonData)
            result.onSuccess { importResult ->
                when (importResult) {
                    is ImportResult.Success -> {
                        refreshMemoryData()
                        _uiState.update { it.copy(memoryError = null) }
                    }
                    is ImportResult.Error -> {
                        _uiState.update { it.copy(memoryError = importResult.message) }
                    }
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(memoryError = "Failed to import data: ${throwable.message}") }
            }
        }
    }

    fun clearMemoryError() {
        _uiState.update { it.copy(memoryError = null) }
    }

    // endregion

    override fun onCleared() {
        super.onCleared()
        generationManager.stopGeneration()
        modelManager.close()
        mediaHandler.close()
    }
}
