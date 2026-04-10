package com.dkshe.audiobookplayer.ui

object UiMessages {
    const val bookmarkSaved = "Bookmark saved"
    const val currentPlaylistCleared = "Current playlist cleared"
    const val noChaptersOrTracks = "No chapters or tracks available"
    const val lastChapter = "You are at the last chapter"
    const val lastTrack = "You are at the last track"
    const val importFailed = "Unable to import those files."
    const val importFolderFailed = "Unable to import that folder."
    const val noSupportedFilesSelected = "No supported audiobook files were selected."
    const val noSupportedFilesInFolder = "No supported audiobook files were found in that folder."
    const val aaxSkippedWithImport = "AAX files were skipped because ClearListen does not support Audible AAX files directly. Use a supported format you are authorized to access, such as M4B, MP3, AAC, or WAV."
    const val aaxNotSupported = "AAX files are not supported directly. ClearListen can import M4B, MP3, AAC, and WAV files. If your audiobook is in Audible AAX format, use a supported version you are authorized to access."

    fun importedAudiobooks(count: Int): String = "Imported $count audiobooks"

    fun importedFolder(): String = "Imported 1 audiobook folder"
}
