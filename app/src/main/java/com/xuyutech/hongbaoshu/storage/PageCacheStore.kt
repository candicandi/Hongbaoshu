package com.xuyutech.hongbaoshu.storage

import android.content.Context
import com.xuyutech.hongbaoshu.reader.Page
import com.xuyutech.hongbaoshu.reader.PageSlice
import com.xuyutech.hongbaoshu.data.ParagraphType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 分页缓存持久化存储
 * 缓存 key 格式: "fontSizeLevel_widthPx_heightPx"
 */
class PageCacheStore(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheDir: File
        get() = File(context.cacheDir, "page_cache").also { it.mkdirs() }
    
    /**
     * 保存全书分页缓存
     * @param cacheKey 缓存 key（fontSizeLevel_widthPx_heightPx）
     * @param chapterPages 章节ID -> 分页列表
     */
    fun save(cacheKey: String, chapterPages: Map<String, List<Page>>) {
        try {
            val data = chapterPages.mapValues { (_, pages) ->
                pages.map { it.toSerializable() }
            }
            val file = File(cacheDir, "$cacheKey.json")
            file.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            // 忽略保存失败
        }
    }
    
    /**
     * 加载全书分页缓存
     * @return 章节ID -> 分页列表，如果缓存不存在返回 null
     */
    fun load(cacheKey: String): Map<String, List<Page>>? {
        return try {
            val file = File(cacheDir, "$cacheKey.json")
            if (!file.exists()) return null
            val data: Map<String, List<SerializablePage>> = json.decodeFromString(file.readText())
            data.mapValues { (_, pages) ->
                pages.map { it.toPage() }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 检查缓存是否存在
     */
    fun exists(cacheKey: String): Boolean {
        return File(cacheDir, "$cacheKey.json").exists()
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}

@Serializable
private data class SerializablePage(
    val index: Int,
    val slices: List<SerializableSlice>,
    val isFirstPage: Boolean,
    val globalIndex: Int,
    val totalPages: Int
)

@Serializable
private data class SerializableSlice(
    val paragraphId: String,
    val paragraphType: String,
    val startChar: Int,
    val endChar: Int,
    val isFirstSlice: Boolean,
    val isLastSlice: Boolean
)

private fun Page.toSerializable() = SerializablePage(
    index = index,
    slices = slices.map { it.toSerializable() },
    isFirstPage = isFirstPage,
    globalIndex = globalIndex,
    totalPages = totalPages
)

private fun PageSlice.toSerializable() = SerializableSlice(
    paragraphId = paragraphId,
    paragraphType = paragraphType.name,
    startChar = startChar,
    endChar = endChar,
    isFirstSlice = isFirstSlice,
    isLastSlice = isLastSlice
)

private fun SerializablePage.toPage() = Page(
    index = index,
    slices = slices.map { it.toSlice() },
    isFirstPage = isFirstPage,
    globalIndex = globalIndex,
    totalPages = totalPages
)

private fun SerializableSlice.toSlice() = PageSlice(
    paragraphId = paragraphId,
    paragraphType = ParagraphType.valueOf(paragraphType),
    startChar = startChar,
    endChar = endChar,
    isFirstSlice = isFirstSlice,
    isLastSlice = isLastSlice
)
