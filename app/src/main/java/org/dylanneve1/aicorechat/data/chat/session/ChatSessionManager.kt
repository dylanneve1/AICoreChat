package org.dylanneve1.aicorechat.data.chat.session

import org.dylanneve1.aicorechat.data.ChatRepository
import org.dylanneve1.aicorechat.data.chat.model.ChatMessage
import org.dylanneve1.aicorechat.data.chat.model.ChatSession
import org.dylanneve1.aicorechat.data.chat.model.ChatSessionMeta

data class SessionState(
    val sessions: List<ChatSessionMeta>,
    val currentSessionId: Long?,
    val currentSessionName: String,
    val messages: List<ChatMessage>,
)

class ChatSessionManager(private val repository: ChatRepository) {

    fun startInitialSession(): SessionState {
        purgeEmptySessions(null)
        val session = repository.createNewSession()
        val allSessions = repository.loadSessions()
        return SessionState(
            sessions = allSessions.map { ChatSessionMeta(it.id, it.name) },
            currentSessionId = session.id,
            currentSessionName = session.name,
            messages = emptyList(),
        )
    }

    fun createAndSelectNew(): SessionState {
        val session = repository.createNewSession()
        val allSessions = repository.loadSessions()
        return SessionState(
            sessions = allSessions.map { ChatSessionMeta(it.id, it.name) },
            currentSessionId = session.id,
            currentSessionName = session.name,
            messages = emptyList(),
        )
    }

    fun selectSession(sessionId: Long, previousSessionId: Long?, previousMessages: List<ChatMessage>): SessionState? {
        var sessions = repository.loadSessions()
        val selected = sessions.find { it.id == sessionId } ?: return null

        if (previousSessionId != null && previousSessionId != sessionId && previousMessages.none { it.isFromUser }) {
            repository.deleteSession(previousSessionId)
            sessions = repository.loadSessions()
        }

        return SessionState(
            sessions = sessions.map { ChatSessionMeta(it.id, it.name) },
            currentSessionId = selected.id,
            currentSessionName = selected.name,
            messages = selected.messages.toList(),
        )
    }

    fun renameSession(sessionId: Long, newName: String) {
        repository.renameSession(sessionId, newName)
    }

    fun deleteSession(sessionId: Long): SessionState {
        repository.deleteSession(sessionId)
        val remaining = repository.loadSessions()
        if (remaining.isEmpty()) {
            val replacement = repository.createNewSession()
            val refreshed = repository.loadSessions()
            return SessionState(
                sessions = refreshed.map { ChatSessionMeta(it.id, it.name) },
                currentSessionId = replacement.id,
                currentSessionName = replacement.name,
                messages = emptyList(),
            )
        }
        val next = remaining.first()
        return SessionState(
            sessions = remaining.map { ChatSessionMeta(it.id, it.name) },
            currentSessionId = next.id,
            currentSessionName = next.name,
            messages = next.messages.toList(),
        )
    }

    fun wipeAllSessions(): SessionState {
        repository.wipeAllSessions()
        return startInitialSession()
    }

    fun purgeEmptySessions(currentSessionId: Long?): List<ChatSessionMeta> {
        val sessions = repository.loadSessions()
        sessions.forEach { session ->
            if (session.id != currentSessionId && session.messages.none { it.isFromUser }) {
                repository.deleteSession(session.id)
            }
        }
        return repository.loadSessions().map { ChatSessionMeta(it.id, it.name) }
    }

    fun appendMessage(sessionId: Long, message: ChatMessage) {
        repository.appendMessage(sessionId, message)
    }

    fun replaceMessages(sessionId: Long, messages: List<ChatMessage>) {
        repository.replaceMessages(sessionId, messages)
    }

    fun loadSessions(): List<ChatSession> = repository.loadSessions()
}
