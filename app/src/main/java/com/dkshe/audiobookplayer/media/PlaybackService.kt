package com.dkshe.audiobookplayer.media

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.dkshe.audiobookplayer.AudiobookApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    private val repository
        get() = (application as AudiobookApplication).container.repository

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) {
                serviceScope.launch { persistCurrentProgress(forceCompleted = false) }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                serviceScope.launch { persistCurrentProgress(forceCompleted = true) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build(),
                    true,
                )
                setTrackSelectionParameters(
                    trackSelectionParameters
                        .buildUpon()
                        .setAudioOffloadPreferences(
                            TrackSelectionParameters.AudioOffloadPreferences.Builder()
                                .setAudioOffloadMode(
                                    TrackSelectionParameters.AudioOffloadPreferences
                                        .AUDIO_OFFLOAD_MODE_DISABLED,
                                )
                                .build(),
                        )
                        .build(),
                )
                setHandleAudioBecomingNoisy(true)
                setSkipSilenceEnabled(false)
                volume = 1f
                setWakeMode(C.WAKE_MODE_LOCAL)
                addListener(playerListener)
            }

        mediaSession = MediaSession.Builder(this, player).build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build(),
        )

        serviceScope.launch {
            while (isActive) {
                persistCurrentProgress(forceCompleted = false)
                delay(5_000)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        player.pause()
        serviceScope.launch {
            persistCurrentProgress(forceCompleted = false)
            stopSelf()
        }
    }

    override fun onDestroy() {
        runBlocking {
            persistCurrentProgress(forceCompleted = false)
        }
        player.removeListener(playerListener)
        mediaSession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun persistCurrentProgress(forceCompleted: Boolean) {
        val extras = player.currentMediaItem?.mediaMetadata?.extras
        val audiobookId = extras?.getLong("book_id")
            ?: player.currentMediaItem?.mediaId?.substringBefore(':')?.toLongOrNull()
            ?: return
        val itemOffsetMs = extras?.getLong("item_offset_ms") ?: 0L
        val duration = extras?.getLong("book_duration_ms")?.takeIf { it > 0 } ?: 0L
        val currentPosition = itemOffsetMs + player.currentPosition.coerceAtLeast(0L)
        val completed = forceCompleted || (duration > 0 && currentPosition >= duration - 1_500)
        repository.savePlaybackPosition(
            audiobookId = audiobookId,
            positionMs = if (completed) 0L else currentPosition,
            durationMs = duration,
            completed = completed,
        )
    }
}
