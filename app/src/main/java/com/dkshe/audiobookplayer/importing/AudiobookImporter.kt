package com.dkshe.audiobookplayer.importing

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.dkshe.audiobookplayer.data.entity.ChapterEntity
import com.dkshe.audiobookplayer.data.repository.AudiobookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(
    val importedCount: Int = 0,
    val unsupportedAaxFound: Boolean = false,
)

class AudiobookImporter(
    private val context: Context,
    private val repository: AudiobookRepository,
    private val metadataExtractor: AudiobookMetadataExtractor,
    private val coverArtStore: CoverArtStore,
) {
    suspend fun import(uris: List<Uri>, persistableFlags: Int): ImportResult = withContext(Dispatchers.IO) {
        var importedCount = 0
        var unsupportedAaxFound = false
        uris.forEach { uri ->
            if (isAax(uri)) {
                unsupportedAaxFound = true
                return@forEach
            }
            if (!isSupported(uri)) return@forEach

            runCatching {
                val flags = persistableFlags and
                    (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                if (flags != 0) {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                }
            }

            runCatching {
                val importedAudiobook = metadataExtractor.extract(uri)
                val audiobookId = repository.upsertImportedAudiobook(importedAudiobook)

                importedAudiobook.coverArtBytes?.let { bytes ->
                    coverArtStore.save(audiobookId, bytes)?.let { path ->
                        repository.updateCoverArtPath(audiobookId, path)
                    }
                }

                repository.replaceChapters(
                    audiobookId = audiobookId,
                    chapters = importedAudiobook.chapters.mapIndexed { index, chapter ->
                        ChapterEntity(
                            audiobookId = audiobookId,
                            chapterIndex = index,
                            title = chapter.title,
                            startMs = chapter.startMs,
                            endMs = chapter.endMs,
                        )
                    },
                )
                importedCount += 1
            }
        }
        ImportResult(
            importedCount = importedCount,
            unsupportedAaxFound = unsupportedAaxFound,
        )
    }

    suspend fun importFolder(folderUri: Uri, persistableFlags: Int): ImportResult = withContext(Dispatchers.IO) {
        val flags = persistableFlags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        if (flags != 0) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(folderUri, flags)
            }
        }

        val rootFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext ImportResult()
        if (!rootFolder.isDirectory) return@withContext ImportResult()

        val unsupportedAaxFound = containsAax(rootFolder)

        val audioFiles = collectAudioFiles(rootFolder)
            .sortedWith { left, right ->
                compareNaturalNames(left.name.orEmpty(), right.name.orEmpty())
            }

        if (audioFiles.isEmpty()) {
            return@withContext ImportResult(unsupportedAaxFound = unsupportedAaxFound)
        }

        val fileMetadata = audioFiles.mapNotNull { file ->
            val uri = file.uri
            runCatching { metadataExtractor.extractFileMetadata(uri) }
                .getOrNull()
                ?.let { metadata -> file to metadata }
        }

        if (fileMetadata.isEmpty()) {
            return@withContext ImportResult(unsupportedAaxFound = unsupportedAaxFound)
        }

        val folderName = rootFolder.name ?: "Audiobook"
        val title = folderName
        val author = fileMetadata
            .mapNotNull { (_, metadata) -> metadata.author?.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        val totalDurationMs = fileMetadata.sumOf { (_, metadata) -> metadata.durationMs }
        val coverArtBytes = fileMetadata.firstNotNullOfOrNull { (_, metadata) -> metadata.coverArtBytes }
        val importedAudiobook = ImportedAudiobook(
            contentUri = folderUri.toString(),
            title = title,
            author = author,
            durationMs = totalDurationMs,
            mimeType = rootFolder.type,
            fileName = folderName,
            coverArtBytes = coverArtBytes,
            contentItems = fileMetadata.map { (file, metadata) ->
                ImportedContentItem(
                    contentUri = file.uri.toString(),
                    fileName = metadata.fileName,
                    durationMs = metadata.durationMs,
                    mimeType = metadata.mimeType,
                )
            },
            chapters = emptyList(),
        )

        val audiobookId = repository.upsertImportedAudiobook(importedAudiobook)
        coverArtBytes?.let { bytes ->
            coverArtStore.save(audiobookId, bytes)?.let { path ->
                repository.updateCoverArtPath(audiobookId, path)
            }
        }
        repository.replaceChapters(audiobookId, emptyList())
        ImportResult(
            importedCount = 1,
            unsupportedAaxFound = unsupportedAaxFound,
        )
    }

    private fun isSupported(uri: Uri): Boolean {
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        val type = documentFile?.type.orEmpty().lowercase()
        val name = documentFile?.name.orEmpty().lowercase()
        return type in supportedMimeTypes || supportedExtensions.any(name::endsWith)
    }

    private fun isSupported(documentFile: DocumentFile): Boolean {
        val type = documentFile.type.orEmpty().lowercase()
        val name = documentFile.name.orEmpty().lowercase()
        return type in supportedMimeTypes || supportedExtensions.any(name::endsWith)
    }

    private fun isAax(uri: Uri): Boolean {
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        return documentFile?.name.orEmpty().lowercase().endsWith(".aax")
    }

    private fun isAax(documentFile: DocumentFile): Boolean =
        documentFile.name.orEmpty().lowercase().endsWith(".aax")

    private fun collectAudioFiles(folder: DocumentFile): List<DocumentFile> {
        val audioFiles = mutableListOf<DocumentFile>()
        folder.listFiles().forEach { file ->
            when {
                file.isDirectory -> audioFiles += collectAudioFiles(file)
                isSupported(file) -> audioFiles += file
            }
        }
        return audioFiles
    }

    private fun containsAax(folder: DocumentFile): Boolean {
        folder.listFiles().forEach { file ->
            when {
                file.isDirectory && containsAax(file) -> return true
                isAax(file) -> return true
            }
        }
        return false
    }

    private fun compareNaturalNames(left: String, right: String): Int {
        val leftTokens = "\\d+|\\D+".toRegex().findAll(left.lowercase()).map { it.value }.toList()
        val rightTokens = "\\d+|\\D+".toRegex().findAll(right.lowercase()).map { it.value }.toList()
        val maxIndex = minOf(leftTokens.size, rightTokens.size)

        for (index in 0 until maxIndex) {
            val leftToken = leftTokens[index]
            val rightToken = rightTokens[index]
            val leftNumber = leftToken.toIntOrNull()
            val rightNumber = rightToken.toIntOrNull()
            val comparison = when {
                leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                else -> leftToken.compareTo(rightToken)
            }
            if (comparison != 0) {
                return comparison
            }
        }

        return leftTokens.size.compareTo(rightTokens.size).takeIf { it != 0 } ?: left.compareTo(right)
    }

    private companion object {
        val supportedMimeTypes = setOf(
            "audio/mpeg",
            "audio/mp4",
            "audio/aac",
            "audio/x-m4b",
            "audio/mp3",
            "audio/wav",
            "audio/x-wav",
            "audio/wave",
        )

        val supportedExtensions = setOf(".mp3", ".m4b", ".aac", ".m4a", ".wav")
    }
}
