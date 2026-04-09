package com.dkshe.audiobookplayer.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "audiobook_content_items",
    primaryKeys = ["audiobookId", "playOrder"],
    foreignKeys = [
        ForeignKey(
            entity = AudiobookEntity::class,
            parentColumns = ["id"],
            childColumns = ["audiobookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("audiobookId")],
)
data class AudiobookContentItemEntity(
    val audiobookId: Long,
    val playOrder: Int,
    val contentUri: String,
    val fileName: String,
    val durationMs: Long,
    val mimeType: String?,
)

