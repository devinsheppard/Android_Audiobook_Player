package com.dkshe.audiobookplayer.media

data class PlayerUiState(
    val isConnected: Boolean = false,
    val currentBookId: Long? = null,
    val title: String = "",
    val author: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val playbackSpeed: Float = 1f,
    val isPlaying: Boolean = false,
)

data class SleepTimerState(
    val isActive: Boolean = false,
    val endTimeEpochMs: Long? = null,
    val remainingMs: Long = 0,
)
