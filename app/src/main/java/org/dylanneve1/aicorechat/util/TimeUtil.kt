package org.dylanneve1.aicorechat.util

import kotlin.math.max

fun timeAgoShort(thenMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = max(0L, now - thenMillis)
    val s = diff / 1000
    val m = s / 60
    val h = m / 60
    val d = h / 24
    return when {
        d > 0 -> "${d}d ago"
        h > 0 -> "${h}h ago"
        m > 0 -> "${m}m ago"
        s > 5 -> "${s}s ago"
        else -> "just now"
    }
}
