package com.dkshe.audiobookplayer.app

import android.content.Context
import androidx.room.Room
import com.dkshe.audiobookplayer.data.db.AppDatabase
import com.dkshe.audiobookplayer.data.repository.AudiobookRepository
import com.dkshe.audiobookplayer.importing.AudiobookImporter
import com.dkshe.audiobookplayer.importing.AudiobookMetadataExtractor
import com.dkshe.audiobookplayer.importing.CoverArtStore
import com.dkshe.audiobookplayer.media.PlaybackConnection

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "audiobook-player.db",
        ).fallbackToDestructiveMigration().build()
    }

    val repository: AudiobookRepository by lazy {
        AudiobookRepository(
            audiobookDao = database.audiobookDao(),
            audiobookContentItemDao = database.audiobookContentItemDao(),
            playbackStateDao = database.playbackStateDao(),
            bookmarkDao = database.bookmarkDao(),
            chapterDao = database.chapterDao(),
        )
    }

    private val coverArtStore: CoverArtStore by lazy {
        CoverArtStore(appContext)
    }

    private val metadataExtractor: AudiobookMetadataExtractor by lazy {
        AudiobookMetadataExtractor(appContext)
    }

    val importer: AudiobookImporter by lazy {
        AudiobookImporter(
            context = appContext,
            repository = repository,
            metadataExtractor = metadataExtractor,
            coverArtStore = coverArtStore,
        )
    }

    val playbackConnection: PlaybackConnection by lazy {
        PlaybackConnection(appContext)
    }
}
