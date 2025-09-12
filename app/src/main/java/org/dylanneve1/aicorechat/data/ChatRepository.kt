package org.dylanneve1.aicorechat.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ChatRepository(context: Context) {
    private val prefs = context.getSharedPreferences("AICoreChatSessions", Context.MODE_PRIVATE)

    fun loadSessions(): MutableList<ChatSession> {
        val json = prefs.getString(KEY_SESSIONS, null) ?: return mutableListOf()
        return parseSessions(json)
    }

    fun saveSessions(sessions: List<ChatSession>) {
        prefs.edit().putString(KEY_SESSIONS, sessionsToJson(sessions)).apply()
    }

    fun createNewSession(): ChatSession {
        val session = ChatSession(id = System.nanoTime(), name = "New Chat")
        val sessions = loadSessions()
        sessions.add(0, session)
        saveSessions(sessions)
        return session
    }

    fun renameSession(sessionId: Long, newName: String) {
        val sessions = loadSessions()
        sessions.find { it.id == sessionId }?.let {
            it.name = newName
            it.updatedAt = System.currentTimeMillis()
        }
        saveSessions(sessions)
    }

    fun deleteSession(sessionId: Long) {
        val sessions = loadSessions()
        val updated = sessions.filter { it.id != sessionId }
        saveSessions(updated)
    }

    fun wipeAllSessions() {
        saveSessions(emptyList())
    }

    fun appendMessage(sessionId: Long, message: ChatMessage) {
        val sessions = loadSessions()
        sessions.find { it.id == sessionId }?.let {
            it.messages.add(message)
            it.updatedAt = System.currentTimeMillis()
        }
        saveSessions(sessions)
    }

    fun replaceMessages(sessionId: Long, messages: List<ChatMessage>) {
        val sessions = loadSessions()
        sessions.find { it.id == sessionId }?.let {
            it.messages.clear()
            it.messages.addAll(messages)
            it.updatedAt = System.currentTimeMillis()
        }
        saveSessions(sessions)
    }

    private fun parseSessions(json: String): MutableList<ChatSession> {
        val arr = JSONArray(json)
        val list = mutableListOf<ChatSession>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.getLong("id")
            val name = o.getString("name")
            val createdAt = o.optLong("createdAt", System.currentTimeMillis())
            val updatedAt = o.optLong("updatedAt", createdAt)
            val messagesArr = o.optJSONArray("messages") ?: JSONArray()
            val messages = mutableListOf<ChatMessage>()
            for (j in 0 until messagesArr.length()) {
                val m = messagesArr.getJSONObject(j)
                val imageUri: String? = if (m.has("imageUri")) m.optString("imageUri") else null
                val imageDescription: String? = if (m.has("imageDescription")) m.optString("imageDescription") else null
                messages.add(
                    ChatMessage(
                        id = m.optLong("id", System.nanoTime()),
                        text = m.getString("text"),
                        isFromUser = m.getBoolean("isFromUser"),
                        isStreaming = m.optBoolean("isStreaming", false),
                        timestamp = m.optLong("timestamp", System.currentTimeMillis()),
                        imageUri = imageUri,
                        imageDescription = imageDescription
                    )
                )
            }
            list.add(
                ChatSession(
                    id = id,
                    name = name,
                    messages = messages,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            )
        }
        return list
    }

    private fun sessionsToJson(sessions: List<ChatSession>): String {
        val arr = JSONArray()
        sessions.forEach { s ->
            val o = JSONObject()
            o.put("id", s.id)
            o.put("name", s.name)
            o.put("createdAt", s.createdAt)
            o.put("updatedAt", s.updatedAt)
            val mArr = JSONArray()
            s.messages.forEach { m ->
                val mo = JSONObject()
                mo.put("id", m.id)
                mo.put("text", m.text)
                mo.put("isFromUser", m.isFromUser)
                mo.put("isStreaming", m.isStreaming)
                mo.put("timestamp", m.timestamp)
                if (m.imageUri != null) mo.put("imageUri", m.imageUri)
                if (m.imageDescription != null) mo.put("imageDescription", m.imageDescription)
                mArr.put(mo)
            }
            o.put("messages", mArr)
            arr.put(o)
        }
        return arr.toString()
    }

    companion object {
        private const val KEY_SESSIONS = "sessions"
    }
} 