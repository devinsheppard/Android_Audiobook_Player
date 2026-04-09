package com.dkshe.audiobookplayer.data.repository

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
import com.dkshe.audiobookplayer.data.model.LibraryItem
import com.dkshe.audiobookplayer.importing.ImportedAudiobook
import kotlinx.coroutines.flow.Flow

class AudiobookRepository(
    private val audiobookDao: AudiobookDao,
    private val audiobookContentItemDao: AudiobookContentItemDao,
    private val playbackStateDao: PlaybackStateDao,
    private val bookmarkDao: BookmarkDao,
    private val chapterDao: ChapterDao,
) {
    fun observeLibraryItems(): Flow<List<LibraryItem>> = audiobookDao.observeLibraryItems()

    fun observeAudiobook(audiobookId: Long): Flow<AudiobookEntity?> = audiobookDao.observeById(audiobookId)

    suspend fun getAudiobook(audiobookId: Long): AudiobookEntity? = audiobookDao.getById(audiobookId)

    suspend fun getContentItems(audiobookId: Long): List<AudiobookContentItemEntity> =
        audiobookContentItemDao.getForAudiobook(audiobookId)

    fun observeContentItems(audiobookId: Long): Flow<List<AudiobookContentItemEntity>> =
        audiobookContentItemDao.observeForAudiobook(audiobookId)

    fun observePlaybackState(audiobookId: Long): Flow<PlaybackStateEntity?> =
        playbackStateDao.observeByAudiobookId(audiobookId)

    suspend fun getPlaybackState(audiobookId: Long): PlaybackStateEntity? =
        playbackStateDao.getByAudiobookId(audiobookId)

    suspend fun savePlaybackPosition(
        audiobookId: Long,
        positionMs: Long,
        durationMs: Long,
        completed: Boolean,
    ) {
        playbackStateDao.upsert(
            PlaybackStateEntity(
                audiobookId = audiobookId,
                positionMs = positionMs,
                lastKnownDurationMs = durationMs,
                completed = completed,
                lastUpdatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    fun observeBookmarks(audiobookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.observeForAudiobook(audiobookId)

    suspend fun addBookmark(
        audiobookId: Long,
        positionMs: Long,
        label: String,
    ) {
        bookmarkDao.insert(
            BookmarkEntity(
                audiobookId = audiobookId,
                positionMs = positionMs,
                label = label,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    fun observeChapters(audiobookId: Long): Flow<List<ChapterEntity>> =
        chapterDao.observeForAudiobook(audiobookId)

    suspend fun replaceChapters(audiobookId: Long, chapters: List<ChapterEntity>) {
        chapterDao.replaceForAudiobook(audiobookId, chapters)
    }

    suspend fun updateCoverArtPath(audiobookId: Long, coverArtPath: String?) {
        audiobookDao.updateCoverArtPath(audiobookId, coverArtPath)
    }

    suspend fun upsertImportedAudiobook(importedAudiobook: ImportedAudiobook): Long {
        val existing = audiobookDao.getByContentUri(importedAudiobook.contentUri)
        val now = System.currentTimeMillis()
        val audiobookId = if (existing == null) {
            audiobookDao.insert(
                AudiobookEntity(
                    contentUri = importedAudiobook.contentUri,
                    title = importedAudiobook.title,
                    author = importedAudiobook.author,
                    coverArtPath = null,
                    totalDurationMs = importedAudiobook.durationMs,
                    mimeType = importedAudiobook.mimeType,
                    fileName = importedAudiobook.fileName,
                    lastImportedAtEpochMs = now,
                ),
            )
        } else {
            audiobookDao.update(
                existing.copy(
                    title = importedAudiobook.title,
                    author = importedAudiobook.author ?: existing.author,
                    totalDurationMs = importedAudiobook.durationMs.takeIf { it > 0 } ?: existing.totalDurationMs,
                    mimeType = importedAudiobook.mimeType ?: existing.mimeType,
                    fileName = importedAudiobook.fileName,
                    lastImportedAtEpochMs = now,
                ),
            )
            existing.id
        }

        audiobookContentItemDao.replaceForAudiobook(
            audiobookId = audiobookId,
            items = importedAudiobook.contentItems.mapIndexed { index, item ->
                AudiobookContentItemEntity(
                    audiobookId = audiobookId,
                    playOrder = index,
                    contentUri = item.contentUri,
                    fileName = item.fileName,
                    durationMs = item.durationMs,
                    mimeType = item.mimeType,
                )
            },
        )

        return audiobookId
    }
}
