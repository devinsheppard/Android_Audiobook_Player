package com.dkshe.audiobookplayer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audiobooks")
data class AudiobookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentUri: String,
    val title: String,
    val author: String?,
    val coverArtPath: String?,
    val totalDurationMs: Long,
    val mimeType: String?,
    val fileName: String,
    val lastImportedAtEpochMs: Long,
)

