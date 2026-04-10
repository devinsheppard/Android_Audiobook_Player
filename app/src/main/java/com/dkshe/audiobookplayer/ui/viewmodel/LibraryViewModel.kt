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
import com.dkshe.audiobookplayer.importing.ImportResult
import com.dkshe.audiobookplayer.media.PlaybackConnection
import com.dkshe.audiobookplayer.media.PlayerUiState
import com.dkshe.audiobookplayer.ui.UiMessages
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
            }.onSuccess { result ->
                message.value = formatImportMessage(
                    result = result,
                    successMessage = UiMessages::importedAudiobooks,
                    emptyMessage = UiMessages.noSupportedFilesSelected,
                )
            }.onFailure {
                message.value = UiMessages.importFailed
            }
            isImporting.value = false
        }
    }

    fun importAudiobookFolder(folderUri: Uri, persistableFlags: Int) {
        viewModelScope.launch {
            isImporting.value = true
            runCatching {
                importer.importFolder(folderUri, persistableFlags)
            }.onSuccess { result ->
                message.value = formatImportMessage(
                    result = result,
                    successMessage = { UiMessages.importedFolder() },
                    emptyMessage = UiMessages.noSupportedFilesInFolder,
                )
            }.onFailure {
                message.value = UiMessages.importFolderFailed
            }
            isImporting.value = false
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun formatImportMessage(
        result: ImportResult,
        successMessage: (Int) -> String,
        emptyMessage: String,
    ): String {
        return when {
            result.importedCount > 0 && result.unsupportedAaxFound ->
                "${successMessage(result.importedCount)}. ${UiMessages.aaxSkippedWithImport}"
            result.importedCount > 0 ->
                successMessage(result.importedCount)
            result.unsupportedAaxFound ->
                UiMessages.aaxNotSupported
            else ->
                emptyMessage
        }
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
