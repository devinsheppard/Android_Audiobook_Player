package com.dkshe.audiobookplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dkshe.audiobookplayer.data.entity.AudiobookContentItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookContentItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AudiobookContentItemEntity>)

    @Query("DELETE FROM audiobook_content_items WHERE audiobookId = :audiobookId")
    suspend fun clearForAudiobook(audiobookId: Long)

    @Query("SELECT * FROM audiobook_content_items WHERE audiobookId = :audiobookId ORDER BY playOrder ASC")
    suspend fun getForAudiobook(audiobookId: Long): List<AudiobookContentItemEntity>

    @Query("SELECT * FROM audiobook_content_items WHERE audiobookId = :audiobookId ORDER BY playOrder ASC")
    fun observeForAudiobook(audiobookId: Long): Flow<List<AudiobookContentItemEntity>>

    @Transaction
    suspend fun replaceForAudiobook(audiobookId: Long, items: List<AudiobookContentItemEntity>) {
        clearForAudiobook(audiobookId)
        if (items.isNotEmpty()) {
            insertAll(items)
        }
    }
}
