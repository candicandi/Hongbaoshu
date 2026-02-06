package com.xuyutech.hongbaoshu.pack.storage

import android.content.Context
import java.io.File

data class PackInspection(
    val packExists: Boolean,
    val hasText: Boolean,
    val hasCover: Boolean,
    val hasFlipSound: Boolean,
    val hasNarration: Boolean
) {
    val isValid: Boolean get() = packExists && hasText
}

class PackFileStore(
    private val context: Context
) {
    fun packDir(packId: String): File = File(File(context.filesDir, "packs"), packId)

    fun pageCacheDir(packId: String): File = File(File(context.cacheDir, "page_cache"), packId)

    fun inspect(packId: String): PackInspection {
        val root = packDir(packId)
        val packExists = root.exists() && root.isDirectory

        val hasText = File(root, "text/book.json").exists()
        val hasCover = File(root, "images/cover.png").exists()
        val hasFlipSound = File(root, "sound/page_flip.wav.ogg").exists()

        val narrationDir = File(root, "audio/narration")
        val hasNarration = narrationDir.exists() && narrationDir.isDirectory && (narrationDir.list()?.isNotEmpty() == true)

        return PackInspection(
            packExists = packExists,
            hasText = hasText,
            hasCover = hasCover,
            hasFlipSound = hasFlipSound,
            hasNarration = hasNarration
        )
    }

    fun deletePack(packId: String) {
        packDir(packId).deleteRecursively()
        pageCacheDir(packId).deleteRecursively()
    }
}
