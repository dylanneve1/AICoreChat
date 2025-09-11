package org.dylanneve1.aicorechat.data

data class ChatSession(
    val id: Long,
    var name: String,
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

data class ChatSessionMeta(
    val id: Long,
    val name: String
) 