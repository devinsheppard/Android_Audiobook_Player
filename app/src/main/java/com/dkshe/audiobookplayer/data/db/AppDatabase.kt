package com.dkshe.audiobookplayer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dkshe.audiobookplayer.data.dao.AudiobookDao
import com.dkshe.audiobookplayer.data.dao.AudiobookContentItemDao
import com.dkshe.audiobookplayer.data.dao.BookmarkDao
import com.dkshe.audiobookplayer.data.dao.ChapterDao
import com.dkshe.audiobookplayer.data.dao.PlaybackStateDao
import com.dkshe.audiobookplayer.data.entity.AudiobookContentItemEntity
import com.dkshe.audiobookplayer.data.entity.AudiobookEntity
import com.dkshe.audiobookplayer.data.entity.BookmarkEntity
import com.dkshe.audiobookplayer.data.entity.ChapterEntity
import com.dkshe.audiobookplayer.data.entity.PlaybackStateEntity

@Database(
    entities = [
        AudiobookEntity::class,
        AudiobookContentItemEntity::class,
        PlaybackStateEntity::class,
        BookmarkEntity::class,
        ChapterEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audiobookDao(): AudiobookDao
    abstract fun audiobookContentItemDao(): AudiobookContentItemDao
    abstract fun playbackStateDao(): PlaybackStateDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun chapterDao(): ChapterDao
}
