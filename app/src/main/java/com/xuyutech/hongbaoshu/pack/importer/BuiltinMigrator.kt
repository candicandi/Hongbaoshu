package com.xuyutech.hongbaoshu.pack.importer

import android.content.Context
import com.xuyutech.hongbaoshu.data.ContentLoaderImpl
import com.xuyutech.hongbaoshu.pack.index.PackIndexStore
import com.xuyutech.hongbaoshu.pack.model.BookMetadata
import com.xuyutech.hongbaoshu.pack.model.NarrationResource
import com.xuyutech.hongbaoshu.pack.model.PackIndex
import com.xuyutech.hongbaoshu.pack.model.PackManifest
import com.xuyutech.hongbaoshu.pack.model.PackResources
import com.xuyutech.hongbaoshu.pack.model.ResourceItem
import com.xuyutech.hongbaoshu.pack.storage.PackFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class BuiltinMigrator(
    private val context: Context,
    private val packIndexStore: PackIndexStore,
    private val packFileStore: PackFileStore
) {
    suspend fun invoke() = withContext(Dispatchers.IO) {
        val packId = "builtin"
        val existing = packIndexStore.find(packId)
        val targetDir = packFileStore.packDir(packId)

        // 若已迁移且有效，或者目录存在且清单存在，忽略
        if (existing != null && targetDir.exists() && File(targetDir, "manifest.json").exists()) {
            return@withContext
        }

        targetDir.mkdirs()

        // 复制 asserts 到 pack 目录
        copyAssetDir("text", File(targetDir, "text"))
        copyAssetDir("audio", File(targetDir, "audio"))
        copyAssetDir("images", File(targetDir, "images"))
        copyAssetDir("sound", File(targetDir, "sound"))

        // 使用 ContentLoader 读取信息
        val bookResult = ContentLoaderImpl().loadBook(context)
        val book = bookResult.book
        val missingCount = bookResult.missingSentenceAudioIds.size

        val manifest = PackManifest(
            formatVersion = 1,
            packId = packId,
            packVersion = 1,
            book = BookMetadata(
                title = book.title,
                author = book.author,
                edition = book.edition
            ),
            resources = PackResources(
                text = ResourceItem(path = "text/mao_quotes_1966.json"), // FIXME: Need to verify if this is the correct builtin text filename or standard book.json
                cover = ResourceItem(path = "images/cover.png"),
                flipSound = ResourceItem(path = "sound/page_flip.wav.ogg"),
                narration = NarrationResource(dir = "audio/narration", codec = "mp3")
            )
        )
        // Check actual filename from assets/text
        val textDir = File(targetDir, "text")
        val jsonFile = textDir.listFiles()?.firstOrNull { it.extension == "json" }
        if (jsonFile != null && jsonFile.name != "book.json") {
            // Rename to standard book.json for consistency in 2.0
            jsonFile.renameTo(File(textDir, "book.json"))
        }

        // Generate manifest.json
        val json = Json { prettyPrint = true }
        File(targetDir, "manifest.json").writeText(
            json.encodeToString(PackManifest.serializer(), manifest.copy(
                resources = manifest.resources.copy(
                    text = ResourceItem(path = "text/book.json")
                )
            ))
        )

        val index = PackIndex(
            packId = packId,
            packVersion = 1,
            formatVersion = 1,
            bookTitle = book.title,
            bookAuthor = book.author,
            bookEdition = book.edition,
            importedAt = System.currentTimeMillis(),
            hasCover = true,
            hasFlipSound = true,
            hasNarration = true,
            missingNarrationSentenceCount = missingCount,
            isValid = true
        )
        packIndexStore.upsert(index)
    }

    private fun copyAssetDir(srcPath: String, destDir: File) {
        val am = context.assets
        val list = am.list(srcPath)
        if (list.isNullOrEmpty()) {
            // It's a file
            destDir.parentFile?.mkdirs()
            am.open(srcPath).use { input ->
                FileOutputStream(destDir).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            // It's a directory
            destDir.mkdirs()
            for (child in list) {
                copyAssetDir("$srcPath/$child", File(destDir, child))
            }
        }
    }
}
