package org.dylanneve1.aicorechat.util

const val DEFAULT_PARTIAL_STOP_BUFFER = 4

@Suppress("NestedBlockDepth")
fun findPartialStopSuffixLength(
    text: String,
    tokens: List<String>,
    minBuffer: Int = DEFAULT_PARTIAL_STOP_BUFFER,
): Int {
    var maxLen = 0
    tokens.forEach { token ->
        val maxCheck = minOf(token.length - 1, text.length)
        for (k in maxCheck downTo 1) {
            val tokenPrefix = token.substring(0, k)
            val startIndex = text.length - k
            if (startIndex >= 0 && text.regionMatches(startIndex, tokenPrefix, 0, k, ignoreCase = true)) {
                if (k > maxLen) {
                    maxLen = k
                }
                break
            }
        }
    }
    if (maxLen == 0) return 0
    return maxOf(maxLen, minBuffer)
}

fun trimTrailingPartialStopToken(
    text: String,
    tokens: List<String>,
    minBuffer: Int = DEFAULT_PARTIAL_STOP_BUFFER,
): String {
    val holdBack = findPartialStopSuffixLength(text, tokens, minBuffer)
    if (holdBack <= 0 || text.isEmpty()) return text
    val safeHoldBack = holdBack.coerceAtMost(text.length)
    return text.dropLast(safeHoldBack).trimEnd()
}
