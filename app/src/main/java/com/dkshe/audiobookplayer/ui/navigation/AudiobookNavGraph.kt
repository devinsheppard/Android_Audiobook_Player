package com.dkshe.audiobookplayer.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.dkshe.audiobookplayer.app.AppContainer
import com.dkshe.audiobookplayer.ui.screens.LibraryScreen
import com.dkshe.audiobookplayer.ui.screens.PlayerScreen
import com.dkshe.audiobookplayer.ui.viewmodel.LibraryViewModel
import com.dkshe.audiobookplayer.ui.viewmodel.PlayerViewModel

@Composable
fun AudiobookNavGraph(
    navController: NavHostController,
    container: AppContainer,
    onLaunchImport: () -> Unit,
    onLaunchFolderImport: () -> Unit,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val importUris by currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("import_uris", emptyList<String>())
        ?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(emptyList()) }
    val importFlags by currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("import_flags", 0)
        ?.collectAsStateWithLifecycle()
        ?: remember { mutableIntStateOf(0) }
    val importTreeUri by currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("import_tree_uri", "")
        ?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf("") }
    val importTreeFlags by currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("import_tree_flags", 0)
        ?.collectAsStateWithLifecycle()
        ?: remember { mutableIntStateOf(0) }

    NavHost(
        navController = navController,
        startDestination = "library",
    ) {
        composable("library") { backStackEntry ->
            val viewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.factory(container),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(importUris, importFlags) {
                if (importUris.isNotEmpty()) {
                    viewModel.importAudiobooks(importUris.map(Uri::parse), importFlags)
                    backStackEntry.savedStateHandle["import_uris"] = emptyList<String>()
                    backStackEntry.savedStateHandle["import_flags"] = 0
                }
            }

            LaunchedEffect(importTreeUri, importTreeFlags) {
                if (importTreeUri.isNotBlank()) {
                    viewModel.importAudiobookFolder(Uri.parse(importTreeUri), importTreeFlags)
                    backStackEntry.savedStateHandle["import_tree_uri"] = ""
                    backStackEntry.savedStateHandle["import_tree_flags"] = 0
                }
            }

            LibraryScreen(
                uiState = uiState,
                onImportClick = onLaunchImport,
                onImportFolderClick = onLaunchFolderImport,
                onBookClick = { audiobookId ->
                    navController.navigate("player/$audiobookId")
                },
                onNowPlayingClick = { audiobookId ->
                    navController.navigate("player/$audiobookId")
                },
                onDismissMessage = viewModel::clearMessage,
            )
        }

        composable(
            route = "player/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            val viewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.factory(container, bookId),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PlayerScreen(
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() },
                onPreparePlayback = viewModel::preparePlayback,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekTo = viewModel::seekTo,
                onSkipBack = { viewModel.seekBy(-15_000) },
                onSkipForward = { viewModel.seekBy(30_000) },
                onPreviousChapter = viewModel::previousChapter,
                onNextChapter = viewModel::nextChapter,
                onSelectSpeed = viewModel::setPlaybackSpeed,
                onSetSleepTimer = viewModel::setSleepTimerMinutes,
                onClearSleepTimer = viewModel::clearSleepTimer,
                onClearPlaylist = viewModel::clearPlaylist,
                onAddBookmark = viewModel::addBookmark,
                onPlayBookmark = viewModel::playFromBookmark,
                onPlayChapter = viewModel::playFromChapter,
                onDismissMessage = viewModel::clearMessage,
            )
        }
    }
}
