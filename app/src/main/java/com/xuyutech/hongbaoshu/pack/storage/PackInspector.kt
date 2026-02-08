package com.xuyutech.hongbaoshu.pack.storage

import com.xuyutech.hongbaoshu.pack.model.PackManifest
import kotlinx.serialization.json.Json
import java.io.File

object PackInspector {
    private val json = Json { ignoreUnknownKeys = true }

    fun inspectPackRoot(root: File): PackInspection {
        val packExists = root.exists() && root.isDirectory

        val hasText = File(root, "text/book.json").exists()
        val hasCover = resolveCoverPath(root) != null
        val hasFlipSound = File(root, "sound/page_flip.wav.ogg").exists()

        val narrationDir = File(root, "audio/narration")
        val hasNarration = narrationDir.exists() &&
            narrationDir.isDirectory &&
            (narrationDir.list()?.isNotEmpty() == true)

        return PackInspection(
            packExists = packExists,
            hasText = hasText,
            hasCover = hasCover,
            hasFlipSound = hasFlipSound,
            hasNarration = hasNarration
        )
    }

    fun resolveCoverPath(root: File): File? {
        val manifestFile = File(root, "manifest.json")
        if (manifestFile.exists()) {
            val manifest = runCatching {
                json.decodeFromString(PackManifest.serializer(), manifestFile.readText(Charsets.UTF_8))
            }.getOrNull()
            val manifestCover = manifest?.resources?.cover?.path
                ?.replace("\\", "/")
                ?.trim()
                ?.removePrefix("./")
                ?.removePrefix("/")
            if (!manifestCover.isNullOrBlank()) {
                val file = File(root, manifestCover)
                if (file.exists()) return file
                val manifestName = File(manifestCover).name
                val byName = sequenceOf(
                    File(root, manifestName),
                    File(File(root, "images"), manifestName)
                ).firstOrNull { it.exists() }
                if (byName != null) return byName
            }
        }

        val candidates = listOf(
            "images/cover.png",
            "images/cover.jpg",
            "images/cover.jpeg",
            "images/cover.webp",
            "cover.png",
            "cover.jpg",
            "cover.jpeg",
            "cover.webp"
        )
        val direct = candidates
            .asSequence()
            .map { rel -> File(root, rel) }
            .firstOrNull { it.exists() }
        if (direct != null) return direct

        val imageDir = File(root, "images")
        val imageMatch = imageDir.listFiles()
            ?.firstOrNull { it.isFile && it.name.substringBeforeLast('.').equals("cover", ignoreCase = true) }
        if (imageMatch != null) return imageMatch

        val rootMatch = root.listFiles()
            ?.firstOrNull { it.isFile && it.name.substringBeforeLast('.').equals("cover", ignoreCase = true) }
        if (rootMatch != null) return rootMatch

        val imageExt = setOf("png", "jpg", "jpeg", "webp")
        val anyImageInImages = imageDir.listFiles()
            ?.firstOrNull { it.isFile && it.extension.lowercase() in imageExt }
        if (anyImageInImages != null) return anyImageInImages

        val anyImageInRoot = root.listFiles()
            ?.firstOrNull { it.isFile && it.extension.lowercase() in imageExt }
        return anyImageInRoot
    }
}
