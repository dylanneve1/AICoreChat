package org.dylanneve1.aicorechat.util

import android.util.Log

private const val PARTIAL_BUFFER = DEFAULT_PARTIAL_STOP_BUFFER

/**
 * Normalizes streamed assistant responses coming from Gemini so the UI never
 * gets stuck waiting on a missing closing tag and strips tool annotations that
 * should remain invisible to the user.
 */
object AssistantResponseFormatter {
    private const val TAG = "AssistantResponseFormatter"
    private val newlineCollapseRegex = Regex("\n{3,}")
    private val assistantCloseRegex = Regex("""\[/assistant\]\s*$""", RegexOption.IGNORE_CASE)
    private val assistantCloseAnywhereRegex = Regex("""\[/assistant\]""", RegexOption.IGNORE_CASE)

    private val removableTags = listOf(
        "[WEB_RESULTS]",
        "[/WEB_RESULTS]",
        "[SEARCH]",
        "[/SEARCH]",
    )

    private val removableBlocks = listOf(
        Regex("""\[WEB_RESULTS].*?\[/WEB_RESULTS]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("""\[SEARCH].*?\[/SEARCH]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
    )

    private val stopTokens = listOf("[/ASSISTANT]", "[ASSISTANT]", "[/USER]", "[USER]")

    private fun String.endsWithIgnoreCase(suffix: String): Boolean = this.endsWith(suffix, ignoreCase = true)

    private fun safeWarn(message: String) {
        try {
            Log.w(TAG, message)
        } catch (_: Throwable) {
            // android.util.Log is a no-op stub in unit tests; ignore failures there.
        }
    }

    private fun replaceAssistantCloseWithCanonical(text: String): String {
        return assistantCloseRegex.replace(text) { _ -> "[/ASSISTANT]" }
    }

    fun sanitizeAssistantText(text: String): String {
        var sanitized = text
        removableBlocks.forEach { pattern ->
            sanitized = pattern.replace(sanitized, "")
        }
        removableTags.forEach { tag ->
            sanitized = sanitized.replace(tag, "", ignoreCase = true)
        }
        sanitized = sanitized.replace('\u00A0', ' ')
        sanitized = sanitized.replace(newlineCollapseRegex, "\n\n")
        sanitized = sanitized.trim()
        return trimTrailingPartialStopToken(sanitized, stopTokens, PARTIAL_BUFFER)
    }

    fun stripAssistantEndTokens(text: String): String {
        var result = text.trimEnd()
        while (assistantCloseRegex.containsMatchIn(result)) {
            result = assistantCloseRegex.replace(result) { _ -> "" }.trimEnd()
        }
        return result
    }

    fun ensureAssistantEndToken(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "[/ASSISTANT]"

        if (trimmed.endsWithIgnoreCase("[ASSISTANT]")) {
            return "$trimmed\n\n[/ASSISTANT]"
        }

        if (trimmed.endsWithIgnoreCase("[/ASSISTANT]")) {
            return replaceAssistantCloseWithCanonical(trimmed)
        }

        if (stopTokens.filterNot { it == "[/ASSISTANT]" || it == "[ASSISTANT]" }
                .any { trimmed.endsWithIgnoreCase(it) }
        ) {
            return trimmed
        }

        val lastClose = assistantCloseAnywhereRegex.findAll(trimmed).lastOrNull()
        if (lastClose != null) {
            val endIndex = lastClose.range.last + 1
            val after = trimmed.substring(endIndex).trim()
            return if (after.isEmpty()) {
                replaceAssistantCloseWithCanonical(trimmed.substring(0, endIndex))
            } else {
                replaceAssistantCloseWithCanonical(trimmed.substring(0, endIndex))
            }
        }

        safeWarn("Response missing [/ASSISTANT] token, appending automatically: ${trimmed.take(50)}...")
        return "$trimmed\n\n[/ASSISTANT]"
    }

    fun finalizeAssistantDisplayText(rawText: String, fallback: String = ""): String {
        if (rawText.isBlank() && fallback.isBlank()) return ""
        val source = if (rawText.isBlank()) fallback else rawText
        val ensured = ensureAssistantEndToken(source)
        val withoutToken = stripAssistantEndTokens(ensured)
        return sanitizeAssistantText(withoutToken)
    }
}
