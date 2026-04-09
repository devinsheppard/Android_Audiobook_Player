package com.dkshe.audiobookplayer.importing

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudiobookMetadataExtractor(
    private val context: Context,
) {
    suspend fun extract(uri: Uri): ImportedAudiobook = withContext(Dispatchers.IO) {
        val fileMetadata = extractFileMetadata(uri)
        ImportedAudiobook(
            contentUri = uri.toString(),
            title = fileMetadata.title,
            author = fileMetadata.author,
            durationMs = fileMetadata.durationMs,
            mimeType = fileMetadata.mimeType,
            fileName = fileMetadata.fileName,
            coverArtBytes = fileMetadata.coverArtBytes,
            contentItems = listOf(
                ImportedContentItem(
                    contentUri = uri.toString(),
                    fileName = fileMetadata.fileName,
                    durationMs = fileMetadata.durationMs,
                    mimeType = fileMetadata.mimeType,
                ),
            ),
            chapters = emptyList(),
        )
    }

    suspend fun extractFileMetadata(uri: Uri): FileMetadata = withContext(Dispatchers.IO) {
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        val fileName = documentFile?.name ?: uri.lastPathSegment ?: "Audiobook"
        val mimeType = documentFile?.type

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            FileMetadata(
                title = firstNonBlank(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    fileName.substringBeforeLast('.'),
                ) ?: "Audiobook",
                author = firstNonBlank(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                ),
                durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?: 0L,
                mimeType = mimeType,
                fileName = fileName,
                coverArtBytes = retriever.embeddedPicture,
            )
        } finally {
            retriever.release()
        }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()
}

data class FileMetadata(
    val title: String,
    val author: String?,
    val durationMs: Long,
    val mimeType: String?,
    val fileName: String,
    val coverArtBytes: ByteArray?,
)
