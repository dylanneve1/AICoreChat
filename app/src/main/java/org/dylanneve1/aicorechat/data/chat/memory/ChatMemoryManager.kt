package org.dylanneve1.aicorechat.data.chat.memory

import org.dylanneve1.aicorechat.data.BioInformation
import org.dylanneve1.aicorechat.data.CustomInstruction
import org.dylanneve1.aicorechat.data.MemoryEntry
import org.dylanneve1.aicorechat.data.MemoryRepository

data class MemorySnapshot(
    val entries: List<MemoryEntry>,
    val bioInformation: BioInformation?,
)

class ChatMemoryManager(private val repository: MemoryRepository) {

    suspend fun refreshSnapshot(): Result<MemorySnapshot> = runCatching { loadSnapshot() }

    suspend fun addCustomInstruction(instruction: CustomInstruction): Result<MemorySnapshot> = runCatching {
        repository.addCustomInstruction(instruction)
        loadSnapshot()
    }

    suspend fun updateCustomInstruction(instruction: CustomInstruction): Result<MemorySnapshot> = runCatching {
        repository.updateCustomInstruction(instruction)
        loadSnapshot()
    }

    suspend fun deleteCustomInstruction(id: String): Result<MemorySnapshot> = runCatching {
        repository.deleteCustomInstruction(id)
        loadSnapshot()
    }

    suspend fun toggleCustomInstruction(id: String): Result<MemorySnapshot> = runCatching {
        repository.toggleCustomInstruction(id)
        loadSnapshot()
    }

    suspend fun addMemoryEntry(content: String): Result<MemorySnapshot> = runCatching {
        repository.addMemoryEntry(MemoryEntry(content = content))
        loadSnapshot()
    }

    suspend fun updateMemoryEntry(entry: MemoryEntry): Result<MemorySnapshot> = runCatching {
        repository.updateMemoryEntry(entry)
        loadSnapshot()
    }

    suspend fun deleteMemoryEntry(id: String): Result<MemorySnapshot> = runCatching {
        repository.deleteMemoryEntry(id)
        loadSnapshot()
    }

    suspend fun toggleMemoryEntry(id: String): Result<MemorySnapshot> = runCatching {
        repository.toggleMemoryEntry(id)
        loadSnapshot()
    }

    suspend fun updateMemoryLastAccessed(id: String): Result<List<MemoryEntry>> = runCatching {
        repository.updateMemoryLastAccessed(id)
        repository.loadMemoryEntries().map { entry ->
            if (entry.id == id) entry.copy(lastAccessed = System.currentTimeMillis()) else entry
        }
    }

    suspend fun saveBioInformation(bio: BioInformation): Result<MemorySnapshot> = runCatching {
        repository.saveBioInformation(bio)
        loadSnapshot()
    }

    suspend fun deleteBioInformation(): Result<MemorySnapshot> = runCatching {
        repository.deleteBioInformation()
        loadSnapshot()
    }

    suspend fun searchMemoryEntries(query: String): Result<List<MemoryEntry>> = runCatching {
        repository.searchMemoryEntries(query)
    }

    fun exportAllMemoryData(): Result<String> = runCatching {
        repository.exportAllData() ?: throw IllegalStateException("No data to export")
    }

    suspend fun importMemoryData(json: String): Result<MemoryRepository.ImportResult> = runCatching {
        repository.importData(json)
    }

    private suspend fun loadSnapshot(): MemorySnapshot = MemorySnapshot(
        entries = repository.loadMemoryEntries(),
        bioInformation = repository.loadBioInformation(),
    )
}
