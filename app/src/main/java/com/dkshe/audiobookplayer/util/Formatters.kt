package com.dkshe.audiobookplayer.util

import kotlin.math.roundToInt

fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1_000).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatProgressPercent(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return "0%"
    val percent = (positionMs.toDouble() / durationMs.toDouble() * 100).roundToInt()
    return "${percent.coerceIn(0, 100)}%"
}

fun formatBookmarkLabel(positionMs: Long): String = "Bookmark at ${formatClock(positionMs)}"

fun formatRemainingMinutes(ms: Long): String {
    val totalMinutes = (ms / 60_000.0).roundToInt().coerceAtLeast(1)
    return if (totalMinutes == 1) "1 minute" else "$totalMinutes minutes"
}
