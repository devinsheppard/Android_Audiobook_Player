package com.dkshe.audiobookplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dkshe.audiobookplayer.data.entity.AudiobookEntity
import com.dkshe.audiobookplayer.data.model.LibraryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(audiobook: AudiobookEntity): Long

    @Update
    suspend fun update(audiobook: AudiobookEntity)

    @Query("SELECT * FROM audiobooks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AudiobookEntity?

    @Query("SELECT * FROM audiobooks WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<AudiobookEntity?>

    @Query("SELECT * FROM audiobooks WHERE contentUri = :contentUri LIMIT 1")
    suspend fun getByContentUri(contentUri: String): AudiobookEntity?

    @Query(
        """
        SELECT
            a.id AS id,
            a.contentUri AS contentUri,
            a.title AS title,
            a.author AS author,
            a.coverArtPath AS coverArtPath,
            a.totalDurationMs AS totalDurationMs,
            COALESCE(p.positionMs, 0) AS currentPositionMs,
            COALESCE(NULLIF(p.lastKnownDurationMs, 0), a.totalDurationMs) AS playbackDurationMs,
            COALESCE(p.completed, 0) AS completed
        FROM audiobooks a
        LEFT JOIN playback_state p ON p.audiobookId = a.id
        ORDER BY a.lastImportedAtEpochMs DESC, a.title COLLATE NOCASE ASC
        """,
    )
    fun observeLibraryItems(): Flow<List<LibraryItem>>

    @Query("UPDATE audiobooks SET coverArtPath = :coverArtPath WHERE id = :id")
    suspend fun updateCoverArtPath(id: Long, coverArtPath: String?)
}

