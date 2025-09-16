package org.dylanneve1.aicorechat.data

import org.dylanneve1.aicorechat.data.model.ModelBackend

enum class ModelDownloadStatus {
    NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, FAILED
}

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isFromUser: Boolean,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val imageDescription: String? = null
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val modelError: String? = null,
    val temperature: Float = 0.3f,
    val topK: Int = 40,
    val sessions: List<ChatSessionMeta> = emptyList(),
    val currentSessionId: Long? = null,
    val currentSessionName: String = "New Chat",
    val isTitleGenerating: Boolean = false,
    val isBulkTitleGenerating: Boolean = false,
    val userName: String = "",
    val personalContextEnabled: Boolean = false,
    val webSearchEnabled: Boolean = false,
    val isSearchInProgress: Boolean = false,
    val currentSearchQuery: String? = null,
    val isDescribingImage: Boolean = false,
    val pendingImageDescription: String? = null,
    val pendingImageUri: String? = null,
    val multimodalEnabled: Boolean = true,
    // Memory and Custom Instructions
    val customInstructions: String = "",
    val memoryEntries: List<MemoryEntry> = emptyList(),
    val bioInformation: BioInformation? = null,
    val memoryContextEnabled: Boolean = true,
    val customInstructionsEnabled: Boolean = true,
    val bioContextEnabled: Boolean = true,
    val memorySearchQuery: String = "",
    val selectedMemoryCategory: MemoryCategory? = null,
    val isMemoryLoading: Boolean = false,
    val memoryError: String? = null,

    // New state for model selection and download
    val selectedBackend: ModelBackend = ModelBackend.AICORE_GEMINI_NANO,
    val gemmaDownloadStatus: ModelDownloadStatus = ModelDownloadStatus.NOT_DOWNLOADED,
    val gemmaDownloadProgress: Float = 0f,
    val isModelSwitching: Boolean = false,
    val huggingFaceToken: String = ""
)
