package com.dkshe.audiobookplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dkshe.audiobookplayer.R
import com.dkshe.audiobookplayer.data.entity.BookmarkEntity
import com.dkshe.audiobookplayer.data.entity.ChapterEntity
import com.dkshe.audiobookplayer.ui.components.AudiobookCoverArt
import com.dkshe.audiobookplayer.ui.viewmodel.PlayerScreenUiState
import com.dkshe.audiobookplayer.util.formatClock
import com.dkshe.audiobookplayer.util.formatRemainingMinutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uiState: PlayerScreenUiState,
    onNavigateBack: () -> Unit,
    onPreparePlayback: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onClearSleepTimer: () -> Unit,
    onClearPlaylist: () -> Unit,
    onAddBookmark: () -> Unit,
    onPlayBookmark: (BookmarkEntity) -> Unit,
    onPlayChapter: (ChapterEntity) -> Unit,
    onDismissMessage: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showClearPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onPreparePlayback()
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissMessage()
    }

    if (showSpeedDialog) {
        SelectionDialog(
            title = stringResource(R.string.speed_dialog_title),
            options = listOf(
                "0.75x" to 0.75f,
                "1.0x" to 1.0f,
                "1.25x" to 1.25f,
                "1.5x" to 1.5f,
                "1.75x" to 1.75f,
                "2.0x" to 2.0f,
            ),
            onDismiss = { showSpeedDialog = false },
            onSelected = {
                onSelectSpeed(it)
                showSpeedDialog = false
            },
        )
    }

    if (showSleepDialog) {
        SelectionDialog(
            title = stringResource(R.string.sleep_dialog_title),
            options = listOf(
                "10 minutes" to 10f,
                "15 minutes" to 15f,
                "30 minutes" to 30f,
                "45 minutes" to 45f,
                "60 minutes" to 60f,
            ),
            onDismiss = { showSleepDialog = false },
            onSelected = {
                onSetSleepTimer(it.toInt())
                showSleepDialog = false
            },
            showClearAction = uiState.sleepTimer.isActive,
            onClear = {
                onClearSleepTimer()
                showSleepDialog = false
            },
        )
    }

    if (showClearPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showClearPlaylistDialog = false },
            title = { Text(text = stringResource(R.string.clear_playlist_title)) },
            text = {
                Text(text = stringResource(R.string.clear_playlist_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearPlaylist()
                        showClearPlaylistDialog = false
                    },
                ) {
                    Text(text = stringResource(R.string.clear_playlist_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearPlaylistDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.player_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.back_to_library),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val audiobook = uiState.audiobook

        if (audiobook == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.playback_unavailable),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            return@Scaffold
        }

        val sliderValue = if (uiState.effectiveDurationMs > 0L) {
            uiState.effectivePositionMs.toFloat() / uiState.effectiveDurationMs.toFloat()
        } else {
            0f
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 20.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AudiobookCoverArt(
                        coverArtPath = audiobook.coverArtPath,
                        modifier = Modifier.size(112.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = audiobook.title,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = audiobook.author ?: stringResource(R.string.author_unknown),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.position),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Slider(
                            value = sliderValue.coerceIn(0f, 1f),
                            onValueChange = { fraction ->
                                onSeekTo((uiState.effectiveDurationMs * fraction).toLong())
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatClock(uiState.effectivePositionMs),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = formatClock(uiState.effectiveDurationMs),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.playback_controls),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top,
                    ) {
                        ControlButton(
                            label = stringResource(R.string.skip_back),
                            onClick = onSkipBack,
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Replay30,
                                    contentDescription = stringResource(R.string.skip_back),
                                )
                            },
                        )
                        ControlButton(
                            label = stringResource(R.string.skip_forward),
                            onClick = onSkipForward,
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.FastForward,
                                    contentDescription = stringResource(R.string.skip_forward),
                                )
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(108.dp),
                    ) {
                        Icon(
                            imageVector = if (uiState.playback.isPlaying && uiState.isCurrentBook) {
                                Icons.Rounded.PauseCircle
                            } else {
                                Icons.Rounded.PlayCircle
                            },
                            contentDescription = if (uiState.playback.isPlaying && uiState.isCurrentBook) {
                                stringResource(R.string.pause_label)
                            } else {
                                stringResource(R.string.play_label)
                            },
                            modifier = Modifier.size(56.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.playback.isPlaying && uiState.isCurrentBook) {
                            stringResource(R.string.pause_label)
                        } else {
                            stringResource(R.string.play_label)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        ControlButton(
                            label = stringResource(R.string.previous_chapter_compact),
                            onClick = onPreviousChapter,
                            enabled = uiState.hasPreviousChapter,
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = stringResource(R.string.previous_chapter),
                                )
                            },
                        )
                        ControlButton(
                            label = stringResource(R.string.next_chapter_compact),
                            onClick = onNextChapter,
                            enabled = uiState.hasNextChapter,
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = stringResource(R.string.next_chapter),
                                )
                            },
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = { showSpeedDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 58.dp),
                    ) {
                        Icon(imageVector = Icons.Rounded.Speed, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(
                                R.string.playback_speed_value,
                                uiState.playback.playbackSpeed.toString(),
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FilledTonalButton(
                        onClick = { showSleepDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 58.dp),
                    ) {
                        Icon(imageVector = Icons.Rounded.Hotel, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (uiState.sleepTimer.isActive) {
                                stringResource(
                                    R.string.sleep_timer_remaining,
                                    formatRemainingMinutes(uiState.sleepTimer.remainingMs),
                                )
                            } else {
                                stringResource(R.string.set_sleep_timer)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FilledTonalButton(
                        onClick = onAddBookmark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 58.dp),
                    ) {
                        Icon(imageVector = Icons.Rounded.BookmarkAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.bookmark_here),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FilledTonalButton(
                        onClick = { showClearPlaylistDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 58.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ClearAll,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.clear_current_playlist),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            item {
                SectionCard(title = stringResource(R.string.chapters)) {
                    if (uiState.chapters.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_chapters),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.chapters.forEach { chapter ->
                                TextButton(
                                    onClick = { onPlayChapter(chapter) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = chapter.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = formatClock(chapter.startMs),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = stringResource(R.string.bookmarks)) {
                    if (uiState.bookmarks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_bookmarks),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.bookmarks.forEach { bookmark ->
                                BookmarkRow(
                                    bookmark = bookmark,
                                    onClick = { onPlayBookmark(bookmark) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(84.dp),
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatClock(bookmark.positionMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.bookmark_play),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<Pair<String, Float>>,
    onDismiss: () -> Unit,
    onSelected: (Float) -> Unit,
    showClearAction: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (label, value) ->
                    TextButton(
                        onClick = { onSelected(value) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (showClearAction && onClear != null) {
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.turn_off_timer),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}
