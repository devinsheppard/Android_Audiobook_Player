package com.dkshe.audiobookplayer.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dkshe.audiobookplayer.data.entity.AudiobookContentItemEntity
import com.dkshe.audiobookplayer.data.entity.AudiobookEntity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackConnection(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _sleepTimerState = MutableStateFlow(SleepTimerState())
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

    private var controller: MediaController? = null
    private var sleepTimerJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refreshState()
        }
    }

    init {
        scope.launch {
            controller = runCatching {
                MediaController.Builder(appContext, sessionToken).buildAsync().await()
            }.getOrNull()
            controller?.addListener(listener)
            refreshState()
        }

        scope.launch {
            while (isActive) {
                refreshState()
                updateSleepTimerRemaining()
                delay(1_000)
            }
        }
    }

    fun openBook(
        audiobook: AudiobookEntity,
        contentItems: List<AudiobookContentItemEntity>,
        startPositionMs: Long,
        autoPlay: Boolean,
    ) {
        val currentController = controller ?: return
        if (contentItems.isEmpty()) return

        val totalDurationMs = contentItems.sumOf { it.durationMs.coerceAtLeast(0L) }
        var runningOffsetMs = 0L
        val mediaItems = contentItems.mapIndexed { index, item ->
            val itemOffsetMs = runningOffsetMs
            runningOffsetMs += item.durationMs.coerceAtLeast(0L)
            MediaItem.Builder()
                .setMediaId("${audiobook.id}:$index")
                .setUri(item.contentUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(audiobook.title)
                        .setArtist(audiobook.author)
                        .setArtworkUri(audiobook.coverArtPath?.let { Uri.fromFile(File(it)) })
                        .setExtras(
                            Bundle().apply {
                                putLong(EXTRA_BOOK_ID, audiobook.id)
                                putLong(EXTRA_ITEM_OFFSET_MS, itemOffsetMs)
                                putLong(EXTRA_BOOK_DURATION_MS, totalDurationMs)
                            },
                        )
                        .build(),
                )
                .build()
        }

        val (itemIndex, itemPositionMs) = resolvePlaylistSeek(contentItems, startPositionMs)
        currentController.setMediaItems(mediaItems, itemIndex, itemPositionMs)
        currentController.prepare()

        if (autoPlay) {
            currentController.play()
        } else {
            currentController.pause()
        }
        refreshState()
    }

    fun togglePlayPause() {
        controller?.let { mediaController ->
            if (mediaController.isPlaying) {
                mediaController.pause()
            } else {
                mediaController.play()
            }
            refreshState()
        }
    }

    fun seekTo(positionMs: Long) {
        val currentController = controller ?: return
        val (itemIndex, itemPositionMs) = resolvePlaylistSeek(
            currentController = currentController,
            overallPositionMs = positionMs.coerceAtLeast(0L),
        )
        currentController.seekTo(itemIndex, itemPositionMs)
        refreshState()
    }

    fun seekBy(offsetMs: Long) {
        val currentController = controller ?: return
        val currentOverallPosition = currentController.currentMediaItem
            ?.mediaMetadata
            ?.extras
            ?.getLong(EXTRA_ITEM_OFFSET_MS)
            ?.plus(currentController.currentPosition.coerceAtLeast(0L))
            ?: currentController.currentPosition.coerceAtLeast(0L)
        seekTo(currentOverallPosition + offsetMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackParameters(PlaybackParameters(speed))
        refreshState()
    }

    fun pause() {
        controller?.pause()
        refreshState()
    }

    fun clearPlaylist() {
        val currentController = controller ?: return
        clearSleepTimer()
        currentController.pause()
        currentController.stop()
        currentController.clearMediaItems()
        refreshState()
    }

    fun setSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        val endTime = System.currentTimeMillis() + durationMs
        _sleepTimerState.value = SleepTimerState(
            isActive = true,
            endTimeEpochMs = endTime,
            remainingMs = durationMs,
        )
        sleepTimerJob = scope.launch {
            delay(durationMs)
            pause()
            _sleepTimerState.value = SleepTimerState()
        }
    }

    fun clearSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerState.value = SleepTimerState()
    }

    private fun updateSleepTimerRemaining() {
        _sleepTimerState.update { state ->
            val endTime = state.endTimeEpochMs ?: return@update state
            val remaining = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remaining == 0L) {
                SleepTimerState()
            } else {
                state.copy(remainingMs = remaining)
            }
        }
    }

    private fun refreshState() {
        val currentController = controller
        if (currentController == null) {
            _state.value = PlayerUiState(isConnected = false)
            return
        }

        _state.value = PlayerUiState(
            isConnected = true,
            currentBookId = currentController.currentMediaItem
                ?.mediaMetadata
                ?.extras
                ?.getLong(EXTRA_BOOK_ID),
            title = currentController.mediaMetadata.title?.toString().orEmpty(),
            author = currentController.mediaMetadata.artist?.toString().orEmpty(),
            positionMs = currentController.currentMediaItem
                ?.mediaMetadata
                ?.extras
                ?.getLong(EXTRA_ITEM_OFFSET_MS)
                ?.plus(currentController.currentPosition.coerceAtLeast(0L))
                ?: currentController.currentPosition.coerceAtLeast(0L),
            durationMs = currentController.currentMediaItem
                ?.mediaMetadata
                ?.extras
                ?.getLong(EXTRA_BOOK_DURATION_MS)
                ?.takeIf { it > 0 }
                ?: currentController.duration.takeIf { it > 0 }
                ?: 0L,
            playbackSpeed = currentController.playbackParameters.speed,
            isPlaying = currentController.isPlaying,
        )
    }

    private fun resolvePlaylistSeek(
        contentItems: List<AudiobookContentItemEntity>,
        overallPositionMs: Long,
    ): Pair<Int, Long> {
        var remaining = overallPositionMs.coerceAtLeast(0L)
        contentItems.forEachIndexed { index, item ->
            val itemDuration = item.durationMs.coerceAtLeast(0L)
            if (itemDuration == 0L || remaining < itemDuration || index == contentItems.lastIndex) {
                return index to remaining
            }
            remaining -= itemDuration
        }
        return 0 to 0L
    }

    private fun resolvePlaylistSeek(
        currentController: MediaController,
        overallPositionMs: Long,
    ): Pair<Int, Long> {
        val mediaItemCount = currentController.mediaItemCount
        for (index in 0 until mediaItemCount) {
            val currentOffset = currentController.getMediaItemAt(index)
                .mediaMetadata
                .extras
                ?.getLong(EXTRA_ITEM_OFFSET_MS)
                ?: 0L
            val nextOffset = if (index < mediaItemCount - 1) {
                currentController.getMediaItemAt(index + 1)
                    .mediaMetadata
                    .extras
                    ?.getLong(EXTRA_ITEM_OFFSET_MS)
                    ?: currentOffset
            } else {
                currentController.getMediaItemAt(index)
                    .mediaMetadata
                    .extras
                    ?.getLong(EXTRA_BOOK_DURATION_MS)
                    ?: currentOffset
            }
            if (overallPositionMs < nextOffset || index == mediaItemCount - 1) {
                return index to (overallPositionMs - currentOffset).coerceAtLeast(0L)
            }
        }
        return 0 to 0L
    }

    private companion object {
        const val EXTRA_BOOK_ID = "book_id"
        const val EXTRA_ITEM_OFFSET_MS = "item_offset_ms"
        const val EXTRA_BOOK_DURATION_MS = "book_duration_ms"
    }
}
