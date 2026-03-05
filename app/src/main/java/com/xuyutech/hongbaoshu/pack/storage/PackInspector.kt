package com.xuyutech.hongbaoshu.pack.storage

import com.xuyutech.hongbaoshu.data.BookJson
import com.xuyutech.hongbaoshu.pack.model.PackManifest
import kotlinx.serialization.json.Json
import java.io.File

object PackInspector {
    private val json = Json { ignoreUnknownKeys = true }

    fun inspectPackRoot(root: File): PackInspection {
        val packExists = root.exists() && root.isDirectory

        val manifest = loadManifest(root)
        val bookPath = manifest?.resources?.text?.path ?: "text/book.json"
        val hasText = File(root, bookPath).exists()
        val hasCover = resolveCoverPath(root) != null
        val flipPath = manifest?.resources?.flipSound?.path ?: "sound/page_flip.wav.ogg"
        val hasFlipSound = File(root, flipPath).exists()

        val narrationDirPath = manifest?.resources?.narration?.dir ?: "audio/narration"
        val narrationDir = File(root, narrationDirPath)
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

    /**
     * 计算朗读句子缺失数量：
     * - 若包没有 narration 目录或为空，返回 0（按“仅文本包”处理）。
     * - 若文本结构不可解析，返回 null（调用方按保守策略处理）。
     */
    fun inspectMissingNarrationSentenceCount(root: File): Int? {
        val manifest = loadManifest(root)
        val bookPath = manifest?.resources?.text?.path ?: "text/book.json"
        val narrationDirPath = manifest?.resources?.narration?.dir ?: "audio/narration"

        val narrationDir = File(root, narrationDirPath)
        val narrationFiles = narrationDir.listFiles()?.filter { it.isFile }.orEmpty()
        if (narrationFiles.isEmpty()) return 0

        val bookFile = File(root, bookPath)
        if (!bookFile.exists()) return null

        val bookJson = runCatching {
            json.decodeFromString(BookJson.serializer(), bookFile.readText(Charsets.UTF_8))
        }.getOrNull() ?: return null

        val availableSentenceIds = narrationFiles
            .mapNotNull { file ->
                val prefix = file.name.substringBefore('_').substringBeforeLast('.')
                prefix.takeIf { it.isNotBlank() }
            }
            .toSet()

        if (availableSentenceIds.isEmpty()) return 0

        val totalSentenceIds = buildSet {
            bookJson.chapters.forEach { chapter ->
                chapter.paragraphs.forEach { paragraph ->
                    paragraph.sentences.forEach { sentence ->
                        if (sentence.id.isNotBlank()) add(sentence.id)
                    }
                }
            }
        }

        if (totalSentenceIds.isEmpty()) return 0
        return totalSentenceIds.count { it !in availableSentenceIds }
    }

    fun resolveCoverPath(root: File): File? {
        val manifest = loadManifest(root)
        if (manifest != null) {
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

    private fun loadManifest(root: File): PackManifest? {
        val manifestFile = File(root, "manifest.json")
        if (!manifestFile.exists()) return null
        return runCatching {
            json.decodeFromString(PackManifest.serializer(), manifestFile.readText(Charsets.UTF_8))
        }.getOrNull()
    }
}
