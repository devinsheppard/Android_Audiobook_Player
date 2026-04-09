package com.dkshe.audiobookplayer.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dkshe.audiobookplayer.app.AppContainer
import com.dkshe.audiobookplayer.data.model.LibraryItem
import com.dkshe.audiobookplayer.data.repository.AudiobookRepository
import com.dkshe.audiobookplayer.importing.AudiobookImporter
import com.dkshe.audiobookplayer.media.PlaybackConnection
import com.dkshe.audiobookplayer.media.PlayerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val items: List<LibraryItem> = emptyList(),
    val isImporting: Boolean = false,
    val nowPlaying: PlayerUiState? = null,
    val message: String? = null,
)

class LibraryViewModel(
    private val repository: AudiobookRepository,
    private val importer: AudiobookImporter,
    private val playbackConnection: PlaybackConnection,
) : ViewModel() {
    private val isImporting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.observeLibraryItems(),
        playbackConnection.state,
        isImporting,
        message,
    ) { items, playbackState, importing, currentMessage ->
        LibraryUiState(
            items = items,
            isImporting = importing,
            nowPlaying = playbackState.takeIf { it.currentBookId != null },
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun importAudiobooks(uris: List<Uri>, persistableFlags: Int) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isImporting.value = true
            runCatching {
                importer.import(uris, persistableFlags)
            }.onSuccess { count ->
                message.value = if (count > 0) {
                    "Imported $count audiobooks"
                } else {
                    "No supported audiobook files were selected."
                }
            }.onFailure {
                message.value = "Unable to import those files."
            }
            isImporting.value = false
        }
    }

    fun importAudiobookFolder(folderUri: Uri, persistableFlags: Int) {
        viewModelScope.launch {
            isImporting.value = true
            runCatching {
                importer.importFolder(folderUri, persistableFlags)
            }.onSuccess { count ->
                message.value = if (count > 0) {
                    "Imported 1 audiobook folder"
                } else {
                    "No supported audiobook files were found in that folder."
                }
            }.onFailure {
                message.value = "Unable to import that folder."
            }
            isImporting.value = false
        }
    }

    fun clearMessage() {
        message.value = null
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LibraryViewModel(
                    repository = container.repository,
                    importer = container.importer,
                    playbackConnection = container.playbackConnection,
                )
            }
        }
    }
}
