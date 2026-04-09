package com.dkshe.audiobookplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dkshe.audiobookplayer.data.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE audiobookId = :audiobookId")
    suspend fun clearForAudiobook(audiobookId: Long)

    @Query("SELECT * FROM chapters WHERE audiobookId = :audiobookId ORDER BY chapterIndex ASC")
    fun observeForAudiobook(audiobookId: Long): Flow<List<ChapterEntity>>

    @Transaction
    suspend fun replaceForAudiobook(audiobookId: Long, chapters: List<ChapterEntity>) {
        clearForAudiobook(audiobookId)
        if (chapters.isNotEmpty()) {
            insertAll(chapters)
        }
    }
}

