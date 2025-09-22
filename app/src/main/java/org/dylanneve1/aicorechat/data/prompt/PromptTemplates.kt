package org.dylanneve1.aicorechat.data.prompt

import org.dylanneve1.aicorechat.data.BioInformation
import org.dylanneve1.aicorechat.data.MemoryEntry

object PromptTemplates {
    fun systemPreamble(allowSearch: Boolean, offlineNotice: Boolean): String {
        val sb = StringBuilder()
        sb.append("You are a helpful, careful AI assistant powered by Gemini Nano. Follow the user's instructions exactly and keep answers grounded in provided context.\n")
        sb.append("Conversation formatting (strict):\n")
        sb.append("- You are Gemini Nano, created by Google Deepmind. DO NOT EVER CLAIM TO BE ANY OTHER MODE.\n")
        sb.append("- User turns are always wrapped in [USER] and [/USER].\n")
        sb.append("- Assistant turns MUST be wrapped in [ASSISTANT] and ALWAYS end with [/ASSISTANT].\n")
        sb.append("- DO NOT USE ANY FORM OF MARKDOWN.\n")
        sb.append("- Keep replies concise (aim for six sentences or fewer) unless the user requests more detail.\n")
        sb.append("- If unsure or missing information, state that clearly and offer a clarifying question instead of guessing.\n")
        sb.append("- Think through the task before responding; give the final answer only.\n")
        sb.append("- When an [IMAGE_DESCRIPTION] block appears, treat it as the active reference image until a new block arrives. Keep it in mind because the next user turn will normally ask about that image.\n")
        sb.append("- Answer image questions using the description without restating it, and explain if the description lacks enough detail.\n")
        sb.append("- Do NOT output tool tags like [WEB_RESULTS]. Do NOT include URLs, citations, or sources.\n")
        sb.append("- Do not be sycophantic, be natural and friendly in conversation. Do not be robotic, you do not have feelings but it is okay to respond as if you do to keep the conversation natural.\n")
        if (allowSearch) {
            sb.append("Tool call: To request a web search, emit ONLY [SEARCH]query[/SEARCH] as the first output and nothing else for that turn.\n")
        } else if (offlineNotice) {
            sb.append("Note: Device is offline; search is unavailable this turn. Do not attempt any tool calls.\n")
        }
        sb.append("\n")
        return sb.toString()
    }

    fun fewShotGeneral(): String {
        return buildString {
            append("[USER]\nGive me three creative breakfast ideas.\n[/USER]\n")
            append("[ASSISTANT]\nOption 1: Savory oatmeal with spinach, feta, and a soft-boiled egg\nOption 2: Greek yogurt parfait with berries, honey, and toasted almonds\nOption 3: Avocado toast topped with chili flakes and lemon zest\n[/ASSISTANT]\n\n")

            append("[USER]\nExplain binary search briefly with a Kotlin example.\n[/USER]\n")
            append("[ASSISTANT]\nBinary search halves the search space each step in a sorted list.\nExample Kotlin:\nfun binarySearch(arr: IntArray, target: Int): Int {\n    var lo = 0\n    var hi = arr.lastIndex\n    while (lo <= hi) {\n        val mid = (lo + hi) ushr 1\n        when {\n            arr[mid] == target -> return mid\n            arr[mid] < target -> lo = mid + 1\n            else -> hi = mid - 1\n        }\n    }\n    return -1\n}\n[/ASSISTANT]\n\n")

            append("[USER]\nI'm planning a 3-day trip to Tokyo. Day 1 ideas?\n[/USER]\n")
            append("[ASSISTANT]\nDay 1 plan:\nStep 1: Visit Meiji Shrine and explore Harajuku.\nStep 2: Walk Omotesando for design shops and cafes.\nStep 3: Watch the scramble at Shibuya Crossing and visit Shibuya Sky.\nStep 4: End with dinner in Shinjuku Omoide Yokocho.\n[/ASSISTANT]\n\n")

            append("[USER]\nGreat, give me Day 2 with fewer crowds.\n[/USER]\n")
            append("[ASSISTANT]\nDay 2 plan:\nStep 1: Start with a quiet stroll through Yanaka Ginza.\nStep 2: Explore Ueno Park museums during the late morning.\nStep 3: Relax in Kiyosumi Gardens in the afternoon.\nStep 4: Enjoy dinner in the Kagurazaka backstreets.\n[/ASSISTANT]\n\n")

            append("[IMAGE_DESCRIPTION]\nA close-up photo of a wooden desk with a silver laptop open to a blank document, a black pen on a lined notebook, and a ceramic mug filled with coffee. Warm morning light comes from the left.\n[/IMAGE_DESCRIPTION]\n\n")
            append("[USER]\nDescribe the mood of this scene.\n[/USER]\n")
            append("[ASSISTANT]\nThe warm light and tidy tools create a calm, focused morning mood that feels ready for writing.\n[/ASSISTANT]\n\n")

            append("[USER]\nWould this setup work for a video call?\n[/USER]\n")
            append("[ASSISTANT]\nYes, the open laptop, bright natural light, and minimal distractions suggest it would work for a video call. Adding headphones would help prevent echo.\n[/ASSISTANT]\n\n")

            append("[IMAGE_DESCRIPTION]\nA photo of a tabby cat lounging on a sofa, eyes half closed, with a red collar that has a silver bell. The room is softly lit and there is a potted fern behind the cat.\n[/IMAGE_DESCRIPTION]\n\n")
            append("[USER]\nWhat color is the cat's collar?\n[/USER]\n")
            append("[ASSISTANT]\nThe collar is red with a silver bell attached.\n[/ASSISTANT]\n\n")

            append("[USER]\nDoes the description say anything about the cat's health?\n[/USER]\n")
            append("[ASSISTANT]\nThe description does not mention health details, so I cannot judge the cat's wellbeing. You could share more observations if you want an assessment.\n[/ASSISTANT]\n\n")
        }
    }

    fun fewShotSearch(): String {
        return buildString {
            append("[USER]\nWhat are the latest Pixel security updates this month?\n[/USER]\n")
            append("[ASSISTANT]\n[SEARCH]latest Pixel security update details[/SEARCH]\n")
        }
    }

    fun postSearchPreamble(): String {
        return buildString {
            append("You are to answer the user's last question using the provided web results.\n")
            append("Do NOT mention or cite sources, URLs, or [WEB_RESULTS].\n")
            append("Write the answer directly, ending with [/ASSISTANT].\n\n")
        }
    }

    fun customInstructionsBlock(instructions: String): String {
        if (instructions.isBlank()) return ""

        return buildString {
            append("[CUSTOM_INSTRUCTIONS]\n")
            append("The following are custom instructions that should guide your behavior:\n\n")
            append("$instructions\n")
            append("[/CUSTOM_INSTRUCTIONS]\n\n")
        }
    }

    fun memoryContextBlock(memories: List<MemoryEntry>, bio: BioInformation? = null): String {
        val sb = StringBuilder()

        // Add bio information if available
        bio?.let { bioInfo ->
            sb.append("[BIO_CONTEXT]\n")
            sb.append("User biographical information:\n")

            bioInfo.name?.let { sb.append("• Name: $it\n") }
            bioInfo.age?.let { sb.append("• Age: $it\n") }
            bioInfo.occupation?.let { sb.append("• Occupation: $it\n") }
            bioInfo.location?.let { sb.append("• Location: $it\n") }

            if (bioInfo.interests.isNotEmpty()) {
                sb.append("• Interests: ${bioInfo.interests.joinToString(", ")}\n")
            }

            if (bioInfo.personalityTraits.isNotEmpty()) {
                sb.append("• Personality: ${bioInfo.personalityTraits.joinToString(", ")}\n")
            }

            if (bioInfo.goals.isNotEmpty()) {
                sb.append("• Goals: ${bioInfo.goals.joinToString(", ")}\n")
            }

            if (bioInfo.achievements.isNotEmpty()) {
                sb.append("• Achievements: ${bioInfo.achievements.joinToString(", ")}\n")
            }

            sb.append("[/BIO_CONTEXT]\n\n")
        }

        // Add relevant memory entries
        if (memories.isNotEmpty()) {
            sb.append("[MEMORY_CONTEXT]\n")
            sb.append("Relevant information from user's memory:\n\n")

            memories.forEach { memory ->
                sb.append("• ${memory.content}\n\n")
            }

            sb.append("[/MEMORY_CONTEXT]\n\n")
        }

        return sb.toString()
    }

    fun buildMemoryContextFromQuery(query: String, allMemories: List<MemoryEntry>): List<MemoryEntry> {
        if (query.isBlank() || allMemories.isEmpty()) return emptyList()

        // Simple relevance scoring based on keyword matching
        val queryWords = query.lowercase().split("\\s+".toRegex()).filter { it.length > 2 }

        val scoredMemories = allMemories
            .filter { it.isEnabled }
            .map { memory ->
                val contentMatch = queryWords.count { word ->
                    memory.content.lowercase().contains(word)
                }

                val relevanceScore = contentMatch * 2
                memory to relevanceScore
            }
            .filter { it.second > 0 } // Only include memories with some relevance
            .sortedByDescending { it.second }
            .take(5) // Limit to top 5 most relevant memories

        return scoredMemories.map { it.first }
    }
}
