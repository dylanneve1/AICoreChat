package org.dylanneve1.aicorechat.data

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isFromUser: Boolean,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
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
    val currentSearchQuery: String? = null
)
