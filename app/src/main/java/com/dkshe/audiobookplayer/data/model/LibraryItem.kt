package com.dkshe.audiobookplayer.data.model

data class LibraryItem(
    val id: Long,
    val contentUri: String,
    val title: String,
    val author: String?,
    val coverArtPath: String?,
    val totalDurationMs: Long,
    val currentPositionMs: Long,
    val playbackDurationMs: Long,
    val completed: Boolean,
)

