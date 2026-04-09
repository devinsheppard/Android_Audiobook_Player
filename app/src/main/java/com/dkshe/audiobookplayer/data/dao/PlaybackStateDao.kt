package com.dkshe.audiobookplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dkshe.audiobookplayer.data.entity.PlaybackStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: PlaybackStateEntity)

    @Query("SELECT * FROM playback_state WHERE audiobookId = :audiobookId LIMIT 1")
    suspend fun getByAudiobookId(audiobookId: Long): PlaybackStateEntity?

    @Query("SELECT * FROM playback_state WHERE audiobookId = :audiobookId LIMIT 1")
    fun observeByAudiobookId(audiobookId: Long): Flow<PlaybackStateEntity?>
}

