package org.dylanneve1.aicorechat.data.prompt

object PromptTemplates {
    fun systemPreamble(allowSearch: Boolean, offlineNotice: Boolean): String {
        val sb = StringBuilder()
        sb.append("You are a helpful AI assistant powered by Gemini Nano. Follow the user's instructions carefully.\n")
        sb.append("Conversation formatting (strict):\n")
        sb.append("- User turns MUST be wrapped in [USER] and [/USER].\n")
        sb.append("- Assistant turns MUST be wrapped in [ASSISTANT] and ALWAYS end with [/ASSISTANT].\n")
        sb.append("- If an [IMAGE_DESCRIPTION] block is present, use it as additional user-provided context. Do not repeat or quote it.\n")
        sb.append("- Do NOT output tool tags like [WEB_RESULTS]. Do NOT include URLs, citations, or sources.\n")
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
            append("[ASSISTANT]\nHere are three quick, tasty options:\n\n")
            append("1) Savory oatmeal with spinach, feta, and a soft‑boiled egg\n")
            append("2) Greek yogurt parfait with berries, honey, and toasted almonds\n")
            append("3) Avocado toast topped with chili flakes and lemon zest\n[/ASSISTANT]\n\n")

            append("[USER]\nExplain binary search briefly with a Kotlin example.\n[/USER]\n")
            append("[ASSISTANT]\nBinary search halves the search space each step in a sorted list.\n")
            append("Example Kotlin:\n\n")
            append("```kotlin\nfun binarySearch(arr: IntArray, target: Int): Int {\n    var lo = 0\n    var hi = arr.lastIndex\n    while (lo <= hi) {\n        val mid = (lo + hi) ushr 1\n        when {\n            arr[mid] == target -> return mid\n            arr[mid] < target -> lo = mid + 1\n            else -> hi = mid - 1\n        }\n    }\n    return -1\n}\n```\n[/ASSISTANT]\n\n")

            // Multi-turn consistency example
            append("[USER]\nI'm planning a 3‑day trip to Tokyo. Day 1 ideas?\n[/USER]\n")
            append("[ASSISTANT]\nDay 1 (central highlights):\n- Meiji Shrine and Harajuku\n- Omotesando walk\n- Shibuya Crossing and Sky\n- Dinner in Shinjuku Omoide Yokocho\n[/ASSISTANT]\n\n")
            append("[USER]\nGreat, give me Day 2 with fewer crowds.\n[/USER]\n")
            append("[ASSISTANT]\nDay 2 (quieter gems):\n- Yanaka Ginza morning stroll\n- Ueno Park museums\n- Kiyosumi Gardens\n- Kagurazaka alleys for dinner\n[/ASSISTANT]\n\n")
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
} 