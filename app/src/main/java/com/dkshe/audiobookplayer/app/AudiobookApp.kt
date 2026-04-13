package com.dkshe.audiobookplayer.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.dkshe.audiobookplayer.AudiobookApplication
import com.dkshe.audiobookplayer.ui.navigation.AudiobookNavGraph

@Composable
fun AudiobookApp() {
    val context = LocalContext.current
    val app = context.applicationContext as AudiobookApplication
    val navController = rememberNavController()
    val preferences by app.container.preferencesRepository.preferences.collectAsStateWithLifecycle()
    var hasHandledLaunchResume by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val flags = data.flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

        val uris = buildList {
            data.data?.let(::add)
            val clipData = data.clipData
            if (clipData != null) {
                for (index in 0 until clipData.itemCount) {
                    add(clipData.getItemAt(index).uri)
                }
            }
        }.distinct()

        if (uris.isNotEmpty()) {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("import_uris", uris.map { it.toString() })
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("import_flags", flags)
        }
    }

    val importFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val folderUri = data.data ?: return@rememberLauncherForActivityResult
        val flags = data.flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.set("import_tree_uri", folderUri.toString())
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.set("import_tree_flags", flags)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(hasHandledLaunchResume) {
        if (hasHandledLaunchResume) return@LaunchedEffect
        hasHandledLaunchResume = true
        if (!preferences.resumeOnLaunch) return@LaunchedEffect

        val playbackState = app.container.repository.getMostRecentIncompletePlaybackState() ?: return@LaunchedEffect
        val audiobook = app.container.repository.getAudiobook(playbackState.audiobookId) ?: return@LaunchedEffect
        navController.navigate("player/${audiobook.id}")
    }

    AudiobookNavGraph(
        navController = navController,
        container = app.container,
        onLaunchImport = {
            importLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    type = "audio/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        arrayOf("audio/mpeg", "audio/mp4", "audio/aac", "audio/wav", "audio/x-wav", "audio/*"),
                    )
                },
            )
        },
        onLaunchFolderImport = {
            importFolderLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                },
            )
        },
        onLaunchLibrivox = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://librivox.org/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        },
    )
}
