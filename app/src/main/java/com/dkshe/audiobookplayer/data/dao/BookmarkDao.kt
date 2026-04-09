package com.dkshe.audiobookplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dkshe.audiobookplayer.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("SELECT * FROM bookmarks WHERE audiobookId = :audiobookId ORDER BY positionMs ASC")
    fun observeForAudiobook(audiobookId: Long): Flow<List<BookmarkEntity>>
}

