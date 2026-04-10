package com.dkshe.audiobookplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dkshe.audiobookplayer.app.AppContainer
import com.dkshe.audiobookplayer.data.entity.AudiobookContentItemEntity
import com.dkshe.audiobookplayer.data.entity.AudiobookEntity
import com.dkshe.audiobookplayer.data.entity.BookmarkEntity
import com.dkshe.audiobookplayer.data.entity.ChapterEntity
import com.dkshe.audiobookplayer.data.entity.PlaybackStateEntity
import com.dkshe.audiobookplayer.data.repository.AudiobookRepository
import com.dkshe.audiobookplayer.media.PlaybackConnection
import com.dkshe.audiobookplayer.media.PlayerUiState
import com.dkshe.audiobookplayer.media.SleepTimerState
import com.dkshe.audiobookplayer.ui.UiMessages
import com.dkshe.audiobookplayer.util.formatBookmarkLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerScreenUiState(
    val audiobook: AudiobookEntity? = null,
    val contentItems: List<AudiobookContentItemEntity> = emptyList(),
    val playback: PlayerUiState = PlayerUiState(),
    val persistedPlayback: PlaybackStateEntity? = null,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val message: String? = null,
) {
    val isCurrentBook: Boolean
        get() = audiobook?.id == playback.currentBookId

    val effectivePositionMs: Long
        get() = if (isCurrentBook) playback.positionMs else persistedPlayback?.positionMs ?: 0L

    val effectiveDurationMs: Long
        get() = when {
            isCurrentBook && playback.durationMs > 0L -> playback.durationMs
            persistedPlayback?.lastKnownDurationMs?.let { it > 0L } == true -> persistedPlayback.lastKnownDurationMs
            else -> audiobook?.totalDurationMs ?: 0L
        }

    val currentChapterIndex: Int
        get() {
            if (chapters.isEmpty()) return -1
            val resolvedIndex = chapters.indexOfLast { it.startMs <= effectivePositionMs + 1_000L }
            return if (resolvedIndex >= 0) resolvedIndex else 0
        }

    val currentContentItemIndex: Int
        get() {
            if (contentItems.isEmpty()) return -1
            var runningOffsetMs = 0L
            contentItems.forEachIndexed { index, item ->
                val itemDurationMs = item.durationMs.coerceAtLeast(0L)
                val nextOffsetMs = runningOffsetMs + itemDurationMs
                if (itemDurationMs == 0L || effectivePositionMs < nextOffsetMs || index == contentItems.lastIndex) {
                    return index
                }
                runningOffsetMs = nextOffsetMs
            }
            return 0
        }

    val hasPreviousChapter: Boolean
        get() = when {
            chapters.isNotEmpty() -> currentChapterIndex > 0 || effectivePositionMs > chapters.first().startMs + 5_000L
            contentItems.size > 1 -> currentContentItemIndex > 0 || effectivePositionMs > 5_000L
            else -> false
        }

    val hasNextChapter: Boolean
        get() = when {
            chapters.isNotEmpty() -> currentChapterIndex in 0 until chapters.lastIndex
            contentItems.size > 1 -> currentContentItemIndex in 0 until contentItems.lastIndex
            else -> false
        }
}

class PlayerViewModel(
    private val audiobookId: Long,
    private val repository: AudiobookRepository,
    private val playbackConnection: PlaybackConnection,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private var hasPreparedPlayback = false

    private data class PlayerDataSnapshot(
        val audiobook: AudiobookEntity,
        val contentItems: List<AudiobookContentItemEntity>,
        val persistedPlayback: PlaybackStateEntity?,
        val bookmarks: List<BookmarkEntity>,
        val chapters: List<ChapterEntity>,
    )

    private val playerData = combine(
        repository.observeAudiobook(audiobookId).filterNotNull(),
        repository.observeContentItems(audiobookId),
        repository.observePlaybackState(audiobookId),
        repository.observeBookmarks(audiobookId),
        repository.observeChapters(audiobookId),
    ) { audiobook, contentItems, persistedPlayback, bookmarks, chapters ->
        PlayerDataSnapshot(
            audiobook = audiobook,
            contentItems = contentItems,
            persistedPlayback = persistedPlayback,
            bookmarks = bookmarks,
            chapters = chapters,
        )
    }

    val uiState: StateFlow<PlayerScreenUiState> = combine(
        playerData,
        playbackConnection.state,
        playbackConnection.sleepTimerState,
        message,
    ) { playerData, playback, sleepTimer, currentMessage ->
        PlayerScreenUiState(
            audiobook = playerData.audiobook,
            contentItems = playerData.contentItems.ifEmpty {
                fallbackContentItems(playerData.audiobook)
            },
            persistedPlayback = playerData.persistedPlayback,
            bookmarks = playerData.bookmarks,
            chapters = playerData.chapters,
            playback = playback,
            sleepTimer = sleepTimer,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerScreenUiState(),
    )

    fun preparePlayback() {
        if (hasPreparedPlayback) return
        viewModelScope.launch {
            awaitPlaybackConnection()
            val audiobook = repository.getAudiobook(audiobookId) ?: return@launch
            val contentItems = repository.getContentItems(audiobookId).ifEmpty {
                fallbackContentItems(audiobook)
            }
            val resumePosition = repository.getPlaybackState(audiobookId)?.positionMs ?: 0L
            playbackConnection.openBook(
                audiobook = audiobook,
                contentItems = contentItems,
                startPositionMs = resumePosition,
                autoPlay = true,
            )
            hasPreparedPlayback = true
        }
    }

    fun togglePlayPause() {
        val currentState = uiState.value
        if (!currentState.isCurrentBook) {
            prepareFromPosition(currentState.effectivePositionMs, autoPlay = true)
            return
        }
        playbackConnection.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        if (!uiState.value.isCurrentBook) {
            prepareFromPosition(positionMs, autoPlay = false)
            return
        }
        playbackConnection.seekTo(positionMs)
    }

    fun seekBy(offsetMs: Long) {
        if (!uiState.value.isCurrentBook) {
            prepareFromPosition((uiState.value.effectivePositionMs + offsetMs).coerceAtLeast(0L), autoPlay = true)
            return
        }
        playbackConnection.seekBy(offsetMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        if (!uiState.value.isCurrentBook) {
            prepareFromPosition(uiState.value.effectivePositionMs, autoPlay = false)
        }
        playbackConnection.setPlaybackSpeed(speed)
    }

    fun setSleepTimerMinutes(minutes: Int) {
        playbackConnection.setSleepTimer(minutes * 60_000L)
    }

    fun clearSleepTimer() {
        playbackConnection.clearSleepTimer()
    }

    fun clearPlaylist() {
        viewModelScope.launch {
            val activeBookId = uiState.value.playback.currentBookId
            if (activeBookId != null) {
                repository.savePlaybackPosition(
                    audiobookId = activeBookId,
                    positionMs = uiState.value.playback.positionMs,
                    durationMs = uiState.value.playback.durationMs,
                    completed = false,
                )
            }
            playbackConnection.clearPlaylist()
            message.value = UiMessages.currentPlaylistCleared
        }
    }

    fun addBookmark() {
        viewModelScope.launch {
            val position = uiState.value.effectivePositionMs
            repository.addBookmark(
                audiobookId = audiobookId,
                positionMs = position,
                label = formatBookmarkLabel(position),
            )
            message.value = UiMessages.bookmarkSaved
        }
    }

    fun playFromBookmark(bookmark: BookmarkEntity) {
        prepareFromPosition(bookmark.positionMs, autoPlay = true)
    }

    fun playFromChapter(chapter: ChapterEntity) {
        prepareFromPosition(chapter.startMs, autoPlay = true)
    }

    fun previousChapter() {
        val currentState = uiState.value
        val chapters = currentState.chapters
        if (chapters.isNotEmpty()) {
            val currentIndex = currentState.currentChapterIndex
            val currentChapter = chapters.getOrNull(currentIndex) ?: chapters.first()
            val targetChapter = if (currentState.effectivePositionMs > currentChapter.startMs + 5_000L) {
                currentChapter
            } else {
                chapters.getOrNull((currentIndex - 1).coerceAtLeast(0)) ?: chapters.first()
            }
            prepareFromPosition(targetChapter.startMs, autoPlay = true)
            return
        }

        val contentItems = currentState.contentItems
        if (contentItems.size > 1) {
            val currentIndex = currentState.currentContentItemIndex.coerceAtLeast(0)
            val targetIndex = if (currentState.effectivePositionMs > startOffsetForContentItem(contentItems, currentIndex) + 5_000L) {
                currentIndex
            } else {
                (currentIndex - 1).coerceAtLeast(0)
            }
            prepareFromPosition(startOffsetForContentItem(contentItems, targetIndex), autoPlay = true)
            return
        }

        message.value = UiMessages.noChaptersOrTracks
    }

    fun nextChapter() {
        val currentState = uiState.value
        val chapters = currentState.chapters
        if (chapters.isNotEmpty()) {
            val nextChapter = chapters.getOrNull(currentState.currentChapterIndex + 1)
            if (nextChapter == null) {
                message.value = UiMessages.lastChapter
                return
            }
            prepareFromPosition(nextChapter.startMs, autoPlay = true)
            return
        }

        val contentItems = currentState.contentItems
        if (contentItems.size > 1) {
            val nextIndex = currentState.currentContentItemIndex + 1
            if (nextIndex > contentItems.lastIndex) {
                message.value = UiMessages.lastTrack
                return
            }
            prepareFromPosition(startOffsetForContentItem(contentItems, nextIndex), autoPlay = true)
            return
        }

        message.value = UiMessages.noChaptersOrTracks
    }

    fun clearMessage() {
        message.value = null
    }

    private fun startOffsetForContentItem(
        contentItems: List<AudiobookContentItemEntity>,
        targetIndex: Int,
    ): Long {
        if (targetIndex <= 0) return 0L
        return contentItems
            .take(targetIndex)
            .sumOf { it.durationMs.coerceAtLeast(0L) }
    }

    private fun prepareFromPosition(positionMs: Long, autoPlay: Boolean) {
        viewModelScope.launch {
            awaitPlaybackConnection()
            val audiobook = repository.getAudiobook(audiobookId) ?: return@launch
            val contentItems = repository.getContentItems(audiobookId).ifEmpty {
                fallbackContentItems(audiobook)
            }
            playbackConnection.openBook(
                audiobook = audiobook,
                contentItems = contentItems,
                startPositionMs = positionMs,
                autoPlay = autoPlay,
            )
        }
    }

    private suspend fun awaitPlaybackConnection() {
        if (playbackConnection.state.value.isConnected) return
        playbackConnection.state.first { it.isConnected }
    }

    private fun fallbackContentItems(audiobook: AudiobookEntity): List<AudiobookContentItemEntity> {
        return listOf(
            AudiobookContentItemEntity(
                audiobookId = audiobook.id,
                playOrder = 0,
                contentUri = audiobook.contentUri,
                fileName = audiobook.fileName,
                durationMs = audiobook.totalDurationMs,
                mimeType = audiobook.mimeType,
            ),
        )
    }

    companion object {
        fun factory(
            container: AppContainer,
            audiobookId: Long,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PlayerViewModel(
                    audiobookId = audiobookId,
                    repository = container.repository,
                    playbackConnection = container.playbackConnection,
                )
            }
        }
    }
}
