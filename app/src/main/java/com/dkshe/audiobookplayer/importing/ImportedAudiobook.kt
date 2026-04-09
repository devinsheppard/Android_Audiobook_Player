package com.dkshe.audiobookplayer.importing

data class ImportedAudiobook(
    val contentUri: String,
    val title: String,
    val author: String?,
    val durationMs: Long,
    val mimeType: String?,
    val fileName: String,
    val coverArtBytes: ByteArray?,
    val contentItems: List<ImportedContentItem>,
    val chapters: List<ImportedChapter>,
)

data class ImportedContentItem(
    val contentUri: String,
    val fileName: String,
    val durationMs: Long,
    val mimeType: String?,
)

data class ImportedChapter(
    val title: String,
    val startMs: Long,
    val endMs: Long?,
)
