package org.dylanneve1.aicorechat.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.dylanneve1.aicorechat.data.chat.model.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ChatRepositoryTest {

    private lateinit var repository: ChatRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("AICoreChatSessions", Context.MODE_PRIVATE).edit().clear().commit()
        repository = ChatRepository(context)
    }

    @Test
    fun createNewSession_prependsAndPersists() {
        val first = repository.createNewSession()
        val second = repository.createNewSession()

        val sessions = repository.loadSessions()
        assertEquals(2, sessions.size)
        assertEquals(second.id, sessions.first().id)
        assertTrue(sessions.any { it.id == first.id })
    }

    @Test
    fun renameSession_updatesName() {
        val session = repository.createNewSession()

        repository.renameSession(session.id, "Renamed")

        val updated = repository.loadSessions().first { it.id == session.id }
        assertEquals("Renamed", updated.name)
    }

    @Test
    fun appendMessage_addsToSessionHistory() {
        val session = repository.createNewSession()
        val message = ChatMessage(text = "Hello", isFromUser = true)

        repository.appendMessage(session.id, message)

        val updated = repository.loadSessions().first { it.id == session.id }
        assertEquals(1, updated.messages.size)
        assertEquals("Hello", updated.messages.first().text)
    }

    @Test
    fun deleteSession_removesEntry() {
        val session = repository.createNewSession()

        repository.deleteSession(session.id)

        val sessions = repository.loadSessions()
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun wipeAllSessions_clearsStorage() {
        repository.createNewSession()
        repository.createNewSession()

        repository.wipeAllSessions()

        assertTrue(repository.loadSessions().isEmpty())
    }

    @Test
    fun replaceMessages_overwritesExistingHistory() {
        val session = repository.createNewSession()
        repository.appendMessage(session.id, ChatMessage(text = "Old", isFromUser = true))

        repository.replaceMessages(session.id, listOf(ChatMessage(text = "New", isFromUser = false)))

        val updated = repository.loadSessions().first()
        assertEquals(1, updated.messages.size)
        assertFalse(updated.messages.first().isFromUser)
        assertEquals("New", updated.messages.first().text)
    }
}
