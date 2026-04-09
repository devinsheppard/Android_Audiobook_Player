package com.dkshe.audiobookplayer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val audiobookId: Long,
    val positionMs: Long,
    val lastKnownDurationMs: Long,
    val completed: Boolean,
    val lastUpdatedAtEpochMs: Long,
)

