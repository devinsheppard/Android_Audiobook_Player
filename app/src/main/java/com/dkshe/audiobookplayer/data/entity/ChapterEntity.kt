package com.dkshe.audiobookplayer.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chapters",
    primaryKeys = ["audiobookId", "chapterIndex"],
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
data class ChapterEntity(
    val audiobookId: Long,
    val chapterIndex: Int,
    val title: String,
    val startMs: Long,
    val endMs: Long?,
)

