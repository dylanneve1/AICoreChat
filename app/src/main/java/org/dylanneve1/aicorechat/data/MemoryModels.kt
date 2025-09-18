package org.dylanneve1.aicorechat.data

/**
 * Data models for Custom Instructions, Memory entries, and Bio information
 */

data class CustomInstruction(
    val id: String = newInstructionId(),
    val title: String,
    val instruction: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val category: String = "General",
)

data class MemoryEntry(
    val id: String = newMemoryId(),
    val content: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
)

enum class MemoryCategory {
    PERSONAL,
    WORK,
    HOBBIES,
    HEALTH,
    RELATIONSHIPS,
    TRAVEL,
    EDUCATION,
    FINANCE,
    OTHER,
}

enum class MemoryImportance {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

data class BioInformation(
    val id: String = newBioId(),
    val name: String? = null,
    val age: Int? = null,
    val occupation: String? = null,
    val location: String? = null,
    val interests: List<String> = emptyList(),
    val personalityTraits: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val preferences: Map<String, String> = emptyMap(),
    // e.g., "birthday" -> "1990-05-15"
    val importantDates: Map<String, String> = emptyMap(),
    val relationships: List<BioRelationship> = emptyList(),
    val achievements: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class BioRelationship(
    val name: String,
    // e.g., "spouse", "parent", "friend", "colleague"
    val relationship: String,
    // Additional context about the relationship
    val details: String? = null,
)

data class MemorySearchResult(
    val memory: MemoryEntry,
    val relevanceScore: Float,
    val matchedTags: List<String> = emptyList(),
    val matchedContent: List<String> = emptyList(),
)

data class MemoryContext(
    val relevantMemories: List<MemoryEntry> = emptyList(),
    val customInstructions: List<CustomInstruction> = emptyList(),
    val bioInformation: BioInformation? = null,
    val contextSummary: String = "",
)

@PublishedApi
internal fun newInstructionId(): String = "instruction_${System.nanoTime()}"

@PublishedApi
internal fun newMemoryId(): String = "memory_${System.nanoTime()}"

@PublishedApi
internal fun newBioId(): String = "bio_${System.nanoTime()}"
