package org.dylanneve1.aicorechat.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class MemoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("AICoreChatMemory", Context.MODE_PRIVATE)

    // Custom Instructions Management
    fun loadCustomInstructions(): List<CustomInstruction> {
        val json = prefs.getString(KEY_CUSTOM_INSTRUCTIONS, null) ?: return emptyList()
        return parseCustomInstructions(json)
    }

    fun saveCustomInstructions(instructions: List<CustomInstruction>) {
        prefs.edit().putString(KEY_CUSTOM_INSTRUCTIONS, customInstructionsToJson(instructions)).apply()
    }

    fun addCustomInstruction(instruction: CustomInstruction) {
        val instructions = loadCustomInstructions().toMutableList()
        instructions.add(instruction)
        saveCustomInstructions(instructions)
    }

    fun updateCustomInstruction(updatedInstruction: CustomInstruction) {
        val instructions = loadCustomInstructions().toMutableList()
        val index = instructions.indexOfFirst { it.id == updatedInstruction.id }
        if (index != -1) {
            instructions[index] = updatedInstruction.copy(updatedAt = System.currentTimeMillis())
            saveCustomInstructions(instructions)
        }
    }

    fun deleteCustomInstruction(instructionId: String) {
        val instructions = loadCustomInstructions().filter { it.id != instructionId }
        saveCustomInstructions(instructions)
    }

    fun toggleCustomInstruction(instructionId: String) {
        val instructions = loadCustomInstructions().toMutableList()
        val index = instructions.indexOfFirst { it.id == instructionId }
        if (index != -1) {
            val instruction = instructions[index]
            instructions[index] = instruction.copy(isEnabled = !instruction.isEnabled)
            saveCustomInstructions(instructions)
        }
    }

    // Memory Entries Management
    fun loadMemoryEntries(): List<MemoryEntry> {
        val json = prefs.getString(KEY_MEMORY_ENTRIES, null) ?: return emptyList()
        return parseMemoryEntries(json)
    }

    fun saveMemoryEntries(memories: List<MemoryEntry>) {
        prefs.edit().putString(KEY_MEMORY_ENTRIES, memoryEntriesToJson(memories)).apply()
    }

    fun addMemoryEntry(memory: MemoryEntry) {
        val memories = loadMemoryEntries().toMutableList()
        memories.add(memory)
        saveMemoryEntries(memories)
    }

    fun updateMemoryEntry(updatedMemory: MemoryEntry) {
        val memories = loadMemoryEntries().toMutableList()
        val index = memories.indexOfFirst { it.id == updatedMemory.id }
        if (index != -1) {
            memories[index] = updatedMemory.copy(updatedAt = System.currentTimeMillis())
            saveMemoryEntries(memories)
        }
    }

    fun deleteMemoryEntry(memoryId: String) {
        val memories = loadMemoryEntries().filter { it.id != memoryId }
        saveMemoryEntries(memories)
    }

    fun toggleMemoryEntry(memoryId: String) {
        val memories = loadMemoryEntries().toMutableList()
        val index = memories.indexOfFirst { it.id == memoryId }
        if (index != -1) {
            val memory = memories[index]
            memories[index] = memory.copy(isEnabled = !memory.isEnabled)
            saveMemoryEntries(memories)
        }
    }

    fun updateMemoryLastAccessed(memoryId: String) {
        val memories = loadMemoryEntries().toMutableList()
        val index = memories.indexOfFirst { it.id == memoryId }
        if (index != -1) {
            memories[index] = memories[index].copy(lastAccessed = System.currentTimeMillis())
            saveMemoryEntries(memories)
        }
    }

    // Bio Information Management
    fun loadBioInformation(): BioInformation? {
        val json = prefs.getString(KEY_BIO_INFORMATION, null) ?: return null
        return parseBioInformation(json)
    }

    fun saveBioInformation(bio: BioInformation) {
        prefs.edit().putString(KEY_BIO_INFORMATION, bioInformationToJson(bio)).apply()
    }

    fun deleteBioInformation() {
        prefs.edit().remove(KEY_BIO_INFORMATION).apply()
    }

    // Search and Filter Methods
    fun searchMemoryEntries(query: String): List<MemoryEntry> {
        val allMemories = loadMemoryEntries()
        return allMemories.filter { memory ->
            query.isBlank() || memory.content.contains(query, ignoreCase = true)
        }
    }

    fun getEnabledMemories(): List<MemoryEntry> {
        return loadMemoryEntries().filter { it.isEnabled }
    }

    // Export/Import functionality
    fun exportAllData(): String {
        val data = JSONObject()
        data.put("customInstructions", JSONArray(customInstructionsToJson(loadCustomInstructions())))
        data.put("memoryEntries", JSONArray(memoryEntriesToJson(loadMemoryEntries())))
        loadBioInformation()?.let { bio ->
            data.put("bioInformation", JSONObject(bioInformationToJson(bio)))
        }
        data.put("exportedAt", System.currentTimeMillis())
        return data.toString()
    }

    fun importData(jsonData: String): ImportResult {
        return try {
            val data = JSONObject(jsonData)
            val results = mutableListOf<String>()

            // Import custom instructions
            if (data.has("customInstructions")) {
                val instructionsJson = data.getString("customInstructions")
                val instructions = parseCustomInstructions(instructionsJson)
                saveCustomInstructions(instructions)
                results.add("Imported ${instructions.size} custom instructions")
            }

            // Import memory entries
            if (data.has("memoryEntries")) {
                val memoriesJson = data.getString("memoryEntries")
                val memories = parseMemoryEntries(memoriesJson)
                saveMemoryEntries(memories)
                results.add("Imported ${memories.size} memory entries")
            }

            // Import bio information
            if (data.has("bioInformation")) {
                val bioJson = data.getString("bioInformation")
                val bio = parseBioInformation(bioJson)
                if (bio != null) {
                    saveBioInformation(bio)
                    results.add("Imported bio information")
                }
            }

            ImportResult.Success(results)
        } catch (e: Exception) {
            ImportResult.Error("Failed to import data: ${e.message}")
        }
    }

    // JSON Parsing Methods
    private fun parseCustomInstructions(json: String): List<CustomInstruction> {
        val arr = JSONArray(json)
        val list = mutableListOf<CustomInstruction>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                CustomInstruction(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    instruction = o.getString("instruction"),
                    isEnabled = o.optBoolean("isEnabled", true),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    category = o.optString("category", "General")
                )
            )
        }
        return list
    }

    private fun parseMemoryEntries(json: String): List<MemoryEntry> {
        val arr = JSONArray(json)
        val list = mutableListOf<MemoryEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                MemoryEntry(
                    id = o.getString("id"),
                    content = o.getString("content"),
                    isEnabled = o.optBoolean("isEnabled", true),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    lastAccessed = o.optLong("lastAccessed", System.currentTimeMillis())
                )
            )
        }
        return list
    }

    private fun parseBioInformation(json: String): BioInformation? {
        return try {
            val o = JSONObject(json)

            val interestsArr = o.optJSONArray("interests") ?: JSONArray()
            val interests = mutableListOf<String>()
            for (i in 0 until interestsArr.length()) {
                interests.add(interestsArr.getString(i))
            }

            val personalityTraitsArr = o.optJSONArray("personalityTraits") ?: JSONArray()
            val personalityTraits = mutableListOf<String>()
            for (i in 0 until personalityTraitsArr.length()) {
                personalityTraits.add(personalityTraitsArr.getString(i))
            }

            val goalsArr = o.optJSONArray("goals") ?: JSONArray()
            val goals = mutableListOf<String>()
            for (i in 0 until goalsArr.length()) {
                goals.add(goalsArr.getString(i))
            }

            val preferences = mutableMapOf<String, String>()
            val preferencesObj = o.optJSONObject("preferences")
            preferencesObj?.keys()?.forEach { key ->
                preferences[key] = preferencesObj.getString(key)
            }

            val importantDates = mutableMapOf<String, String>()
            val datesObj = o.optJSONObject("importantDates")
            datesObj?.keys()?.forEach { key ->
                importantDates[key] = datesObj.getString(key)
            }

            val relationshipsArr = o.optJSONArray("relationships") ?: JSONArray()
            val relationships = mutableListOf<BioRelationship>()
            for (i in 0 until relationshipsArr.length()) {
                val relObj = relationshipsArr.getJSONObject(i)
                relationships.add(
                    BioRelationship(
                        name = relObj.getString("name"),
                        relationship = relObj.getString("relationship"),
                        details = relObj.optString("details")
                    )
                )
            }

            val achievementsArr = o.optJSONArray("achievements") ?: JSONArray()
            val achievements = mutableListOf<String>()
            for (i in 0 until achievementsArr.length()) {
                achievements.add(achievementsArr.getString(i))
            }

            BioInformation(
                id = o.getString("id"),
                name = o.getString("name"),
                age = if (o.has("age") && !o.isNull("age")) o.getInt("age") else null,
                occupation = o.optString("occupation"),
                location = o.optString("location"),
                interests = interests,
                personalityTraits = personalityTraits,
                goals = goals,
                preferences = preferences,
                importantDates = importantDates,
                relationships = relationships,
                achievements = achievements,
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    // JSON Serialization Methods
    private fun customInstructionsToJson(instructions: List<CustomInstruction>): String {
        val arr = JSONArray()
        instructions.forEach { instruction ->
            val o = JSONObject()
            o.put("id", instruction.id)
            o.put("title", instruction.title)
            o.put("instruction", instruction.instruction)
            o.put("isEnabled", instruction.isEnabled)
            o.put("createdAt", instruction.createdAt)
            o.put("updatedAt", instruction.updatedAt)
            o.put("category", instruction.category)
            arr.put(o)
        }
        return arr.toString()
    }

    private fun memoryEntriesToJson(memories: List<MemoryEntry>): String {
        val arr = JSONArray()
        memories.forEach { memory ->
            val o = JSONObject()
            o.put("id", memory.id)
            o.put("content", memory.content)
            o.put("isEnabled", memory.isEnabled)
            o.put("createdAt", memory.createdAt)
            o.put("updatedAt", memory.updatedAt)
            o.put("lastAccessed", memory.lastAccessed)
            arr.put(o)
        }
        return arr.toString()
    }

    private fun bioInformationToJson(bio: BioInformation): String {
        val o = JSONObject()
        o.put("id", bio.id)
        o.put("name", bio.name)
        bio.age?.let { o.put("age", it) }
        bio.occupation?.let { o.put("occupation", it) }
        bio.location?.let { o.put("location", it) }

        val interestsArr = JSONArray()
        bio.interests.forEach { interestsArr.put(it) }
        o.put("interests", interestsArr)

        val traitsArr = JSONArray()
        bio.personalityTraits.forEach { traitsArr.put(it) }
        o.put("personalityTraits", traitsArr)

        val goalsArr = JSONArray()
        bio.goals.forEach { goalsArr.put(it) }
        o.put("goals", goalsArr)

        val preferencesObj = JSONObject()
        bio.preferences.forEach { (key, value) -> preferencesObj.put(key, value) }
        o.put("preferences", preferencesObj)

        val datesObj = JSONObject()
        bio.importantDates.forEach { (key, value) -> datesObj.put(key, value) }
        o.put("importantDates", datesObj)

        val relationshipsArr = JSONArray()
        bio.relationships.forEach { relationship ->
            val relObj = JSONObject()
            relObj.put("name", relationship.name)
            relObj.put("relationship", relationship.relationship)
            relationship.details?.let { relObj.put("details", it) }
            relationshipsArr.put(relObj)
        }
        o.put("relationships", relationshipsArr)

        val achievementsArr = JSONArray()
        bio.achievements.forEach { achievementsArr.put(it) }
        o.put("achievements", achievementsArr)

        o.put("createdAt", bio.createdAt)
        o.put("updatedAt", bio.updatedAt)

        return o.toString()
    }

    sealed class ImportResult {
        data class Success(val messages: List<String>) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    companion object {
        private const val KEY_CUSTOM_INSTRUCTIONS = "custom_instructions"
        private const val KEY_MEMORY_ENTRIES = "memory_entries"
        private const val KEY_BIO_INFORMATION = "bio_information"
    }
}
