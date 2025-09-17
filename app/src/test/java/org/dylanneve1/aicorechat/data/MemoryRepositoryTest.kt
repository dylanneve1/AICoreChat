package org.dylanneve1.aicorechat.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class MemoryRepositoryTest {

    private lateinit var repository: MemoryRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("AICoreChatMemory", Context.MODE_PRIVATE).edit().clear().commit()
        repository = MemoryRepository(context)
    }

    @Test
    fun addAndLoadCustomInstructions_persistsToPreferences() {
        val instruction = CustomInstruction(title = "Greeting", instruction = "Be cheerful")

        repository.addCustomInstruction(instruction)

        val loaded = repository.loadCustomInstructions()
        assertEquals(1, loaded.size)
        assertEquals("Greeting", loaded.first().title)
        assertEquals("Be cheerful", loaded.first().instruction)
    }

    @Test
    fun toggleCustomInstruction_flipsEnabledFlag() {
        val instruction = CustomInstruction(title = "Debug", instruction = "Log everything")
        repository.addCustomInstruction(instruction)

        repository.toggleCustomInstruction(instruction.id)

        val reloaded = repository.loadCustomInstructions().first()
        assertFalse(reloaded.isEnabled)
    }

    @Test
    fun updateMemoryLastAccessed_refreshesTimestamp() {
        val entry = MemoryEntry(content = "Tracks workouts")
        repository.addMemoryEntry(entry)

        repository.updateMemoryLastAccessed(entry.id)

        val updated = repository.loadMemoryEntries().first()
        assertTrue(updated.lastAccessed >= entry.lastAccessed)
    }

    @Test
    fun updateMemoryEntry_replacesContent() {
        val entry = MemoryEntry(content = "Prefers tea")
        repository.addMemoryEntry(entry)

        val updated = entry.copy(content = "Prefers espresso")
        repository.updateMemoryEntry(updated)

        val result = repository.loadMemoryEntries().first()
        assertEquals("Prefers espresso", result.content)
    }

    @Test
    fun toggleMemoryEntry_disablesEntry() {
        val entry = MemoryEntry(content = "Enjoys cycling")
        repository.addMemoryEntry(entry)

        repository.toggleMemoryEntry(entry.id)

        val result = repository.loadMemoryEntries().first()
        assertFalse(result.isEnabled)
    }

    @Test
    fun searchMemoryEntries_returnsMatchesCaseInsensitive() {
        val entry = MemoryEntry(content = "Enjoys Hiking in summer")
        repository.addMemoryEntry(entry)
        repository.addMemoryEntry(MemoryEntry(content = "Loves cooking"))

        val results = repository.searchMemoryEntries("hiking")

        assertEquals(1, results.size)
        assertEquals(entry.id, results.first().id)
    }

    @Test
    fun deleteBioInformation_clearsStoredBio() {
        val bio = BioInformation(id = "bio1", name = "Kai")
        repository.saveBioInformation(bio)

        repository.deleteBioInformation()

        assertEquals(null, repository.loadBioInformation())
    }

    @Test
    fun importData_restoresAllSections() {
        val payload = """
            {
              "customInstructions": "[{\"id\":\"ci1\",\"title\":\"Tone\",\"instruction\":\"Stay calm\",\"isEnabled\":true}]",
              "memoryEntries": "[{\"id\":\"m1\",\"content\":\"Allergic to peanuts\",\"isEnabled\":true}]",
              "bioInformation": "{\"id\":\"bio1\",\"name\":\"Alex\",\"location\":\"Seattle\"}"
            }
        """.trimIndent()

        val result = repository.importData(payload)

        assertTrue(result is MemoryRepository.ImportResult.Success)
        assertEquals(1, repository.loadCustomInstructions().size)
        assertEquals(1, repository.loadMemoryEntries().size)
        assertEquals("Alex", repository.loadBioInformation()?.name)
    }

    @Test
    fun importData_handlesMalformedJson() {
        val result = repository.importData("{ not valid json }")

        assertTrue(result is MemoryRepository.ImportResult.Error)
    }

    @Test
    fun exportAllData_includesStoredSections() {
        repository.saveCustomInstructions(
            listOf(CustomInstruction(id = "ci1", title = "Tone", instruction = "Friendly")),
        )
        repository.saveMemoryEntries(listOf(MemoryEntry(id = "m1", content = "Enjoys chess")))
        repository.saveBioInformation(BioInformation(id = "bio1", name = "Jamie"))

        val exported = repository.exportAllData()

        assertTrue(exported.contains("customInstructions"))
        assertTrue(exported.contains("memoryEntries"))
        assertTrue(exported.contains("bioInformation"))
    }
}
