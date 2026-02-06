package com.xuyutech.hongbaoshu.pack.loader

import android.content.Context
import android.net.Uri
import com.xuyutech.hongbaoshu.data.Book
import com.xuyutech.hongbaoshu.data.BookJson
import com.xuyutech.hongbaoshu.data.BookLoadResult
import com.xuyutech.hongbaoshu.data.ContentLoader
import com.xuyutech.hongbaoshu.data.ParagraphType
import com.xuyutech.hongbaoshu.pack.model.PackManifest
import kotlinx.serialization.json.Json
import java.io.File

class FilePackContentLoader(
    private val packDir: File
) : ContentLoader {

    private val json = Json { ignoreUnknownKeys = true }

    private var cachedResult: BookLoadResult? = null
    private var manifest: PackManifest? = null
    private val narrationMap: MutableMap<String, Uri> = mutableMapOf()
    private var flipUri: Uri? = null
    private var coverUri: Uri? = null

    override suspend fun loadBook(context: Context): BookLoadResult {
        cachedResult?.let { return it }

        val mf = loadManifest()
        val bookPath = mf?.resources?.text?.path ?: "text/book.json"
        val bookFile = File(packDir, bookPath)
        val text = bookFile.readText(Charsets.UTF_8)
        val bookJson = json.decodeFromString(BookJson.serializer(), text)
        val book = Book.fromJson(bookJson)
        validateIds(book)

        buildResourceMaps()
        val missing = collectMissingSentenceAudio(book)
        val result = BookLoadResult(book = book, missingSentenceAudioIds = missing)
        cachedResult = result
        return result
    }

    override fun narrationUri(sentenceId: String): Uri? = narrationMap[sentenceId]

    override fun flipSound(): Uri? = flipUri

    override fun coverImage(): Uri? = coverUri

    private fun buildResourceMaps() {
        narrationMap.clear()

        val mf = loadManifest()
        val coverPath = mf?.resources?.cover?.path ?: "images/cover.png"
        val flipPath = mf?.resources?.flipSound?.path ?: "sound/page_flip.wav.ogg"

        coverUri = File(packDir, coverPath).takeIf { it.exists() }?.let { Uri.fromFile(it) }
        flipUri = File(packDir, flipPath).takeIf { it.exists() }?.let { Uri.fromFile(it) }

        val narrationDirPath = mf?.resources?.narration?.dir ?: "audio/narration"
        val narrationDir = File(packDir, narrationDirPath)
        val narrationFiles = narrationDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name }.orEmpty()
        narrationFiles.forEach { file ->
            val filename = file.name
            val prefix = filename.substringBefore('_').substringBeforeLast('.')
            if (prefix.isNotBlank()) {
                narrationMap[prefix] = Uri.fromFile(file)
            }
        }
    }

    private fun loadManifest(): PackManifest? {
        manifest?.let { return it }
        val manifestFile = File(packDir, "manifest.json")
        if (!manifestFile.exists()) return null
        val mf = runCatching {
            json.decodeFromString(PackManifest.serializer(), manifestFile.readText(Charsets.UTF_8))
        }.getOrNull()
        manifest = mf
        return mf
    }

    private fun collectMissingSentenceAudio(book: Book): Set<String> {
        // If no narration resources exist at all, treat as "no narration" rather than "everything missing".
        if (narrationMap.isEmpty()) return emptySet()

        val missing = mutableSetOf<String>()
        book.chapters.forEach { chapter ->
            chapter.paragraphs.forEach { paragraph ->
                if (paragraph.type == ParagraphType.text) {
                    paragraph.sentences.forEach { sentence ->
                        if (!narrationMap.containsKey(sentence.id)) {
                            missing.add(sentence.id)
                        }
                    }
                }
            }
        }
        return missing
    }

    private fun validateIds(book: Book) {
        val chapters = mutableSetOf<String>()
        val paragraphs = mutableSetOf<String>()
        val sentences = mutableSetOf<String>()
        book.chapters.forEach { ch ->
            require(chapters.add(ch.id)) { "Duplicate chapter id: ${ch.id}" }
            ch.paragraphs.forEach { p ->
                require(paragraphs.add(p.id)) { "Duplicate paragraph id: ${p.id}" }
                p.sentences.forEach { s ->
                    require(sentences.add(s.id)) { "Duplicate sentence id: ${s.id}" }
                }
            }
        }
    }
}
