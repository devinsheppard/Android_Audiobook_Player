package com.dkshe.audiobookplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dkshe.audiobookplayer.R
import com.dkshe.audiobookplayer.data.model.LibraryItem
import com.dkshe.audiobookplayer.media.PlayerUiState
import com.dkshe.audiobookplayer.ui.components.AudiobookCoverArt
import com.dkshe.audiobookplayer.ui.viewmodel.LibraryUiState
import com.dkshe.audiobookplayer.util.formatClock
import com.dkshe.audiobookplayer.util.formatProgressPercent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onImportClick: () -> Unit,
    onImportFolderClick: () -> Unit,
    onBookClick: (Long) -> Unit,
    onNowPlayingClick: (Long) -> Unit,
    onDismissMessage: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.library_title))
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 900.dp),
                ) {
                    Text(
                        text = "ClearListen",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.library_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    FilledTonalButton(
                        onClick = onImportClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.import_books),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = onImportFolderClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.import_folder_as_book),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            uiState.nowPlaying?.let { playback ->
                playback.currentBookId?.let { bookId ->
                    item {
                        NowPlayingCard(
                            playback = playback,
                            onClick = { onNowPlayingClick(bookId) },
                        )
                    }
                }
            }

            if (uiState.isImporting) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 900.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.importing_books),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            if (uiState.items.isEmpty() && !uiState.isImporting) {
                item {
                    EmptyLibraryCard()
                }
            } else {
                items(uiState.items, key = { it.id }) { item ->
                    LibraryItemCard(
                        item = item,
                        onClick = { onBookClick(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCard(
    playback: PlayerUiState,
    onClick: () -> Unit,
) {
    val nowPlayingLabel = stringResource(R.string.now_playing)
    val fallbackTitle = stringResource(R.string.audiobook_fallback_title)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 900.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("$nowPlayingLabel. ")
                    append(playback.title.ifBlank { fallbackTitle })
                    if (playback.author.isNotBlank()) {
                        append(", ${playback.author}")
                    }
                }
            }
            .clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compactLayout = maxWidth < 420.dp
            if (compactLayout) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = nowPlayingLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = playback.title.ifBlank { fallbackTitle },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (playback.author.isNotBlank()) {
                        Text(
                            text = playback.author,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nowPlayingLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playback.title.ifBlank { fallbackTitle },
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (playback.author.isNotBlank()) {
                            Text(
                                text = playback.author,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 900.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.empty_library_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_library_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LibraryItemCard(
    item: LibraryItem,
    onClick: () -> Unit,
) {
    val duration = item.playbackDurationMs.takeIf { it > 0 } ?: item.totalDurationMs
    val progress = if (duration > 0L) item.currentPositionMs.toFloat() / duration.toFloat() else 0f
    val authorLabel = item.author ?: stringResource(R.string.author_unknown)
    val durationLabel = stringResource(R.string.duration_value, formatClock(duration))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 900.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(item.title)
                    append(". ")
                    append(authorLabel)
                    append(". $durationLabel. ")
                    append("${formatProgressPercent(item.currentPositionMs, duration)} listened.")
                }
            }
            .clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compactLayout = maxWidth < 520.dp
            if (compactLayout) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AudiobookCoverArt(
                        coverArtPath = item.coverArtPath,
                        contentDescription = null,
                        modifier = Modifier
                            .size(112.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                    LibraryItemDetails(
                        item = item,
                        duration = duration,
                        progress = progress,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AudiobookCoverArt(
                        coverArtPath = item.coverArtPath,
                        contentDescription = null,
                        modifier = Modifier.size(88.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    LibraryItemDetails(
                        item = item,
                        duration = duration,
                        progress = progress,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryItemDetails(
    item: LibraryItem,
    duration: Long,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.author ?: stringResource(R.string.author_unknown),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.duration_value, formatClock(duration)),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.listened_progress,
                formatClock(item.currentPositionMs),
                formatProgressPercent(item.currentPositionMs, duration),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
