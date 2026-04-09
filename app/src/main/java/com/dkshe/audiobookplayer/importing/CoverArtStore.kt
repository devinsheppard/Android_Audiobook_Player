package com.dkshe.audiobookplayer.importing

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoverArtStore(
    context: Context,
) {
    private val coverDirectory = File(context.filesDir, "covers").apply {
        mkdirs()
    }

    suspend fun save(audiobookId: Long, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(coverDirectory, "cover_$audiobookId.jpg")
            file.writeBytes(bytes)
            file.absolutePath
        }.getOrNull()
    }
}

