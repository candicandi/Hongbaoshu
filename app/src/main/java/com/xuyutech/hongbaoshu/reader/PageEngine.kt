package com.xuyutech.hongbaoshu.reader

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.xuyutech.hongbaoshu.data.Chapter
import com.xuyutech.hongbaoshu.data.Paragraph
import com.xuyutech.hongbaoshu.data.ParagraphType
import com.xuyutech.hongbaoshu.data.Sentence

/**
 * 页面内容片段：记录原始段落 ID + 字符范围
 * 一个段落可能跨多页，每页只包含该段落的一部分字符
 */
data class PageSlice(
    val paragraphId: String,
    val paragraphType: ParagraphType,
    val startChar: Int,      // 在原始段落 content 中的起始字符索引（包含）
    val endChar: Int,        // 在原始段落 content 中的结束字符索引（不包含）
    val isFirstSlice: Boolean,  // 是否是该段落的第一个片段（用于决定是否显示缩进/前缀）
    val isLastSlice: Boolean    // 是否是该段落的最后一个片段（用于决定是否显示段落间距）
)

/**
 * 页面数据
 */
data class Page(
    val index: Int,                    // 章节内页码（0-based）
    val slices: List<PageSlice>,
    val isFirstPage: Boolean = false,  // 是否是章节第一页（需要显示标题）
    val globalIndex: Int = 0,          // 全书页码（1-based）
    val totalPages: Int = 0            // 全书总页数
)

/**
 * 分页配置（用于传递测量参数）
 */
data class PageConfig(
    val availableHeightPx: Int = 0,
    val availableWidthPx: Int = 0,
    val titleHeightPx: Int = 0,
    val textStyle: TextStyle = TextStyle.Default,
    val annotationStyle: TextStyle = TextStyle.Default,
    val textParagraphSpacingPx: Int = 0,
    val annotationSpacingPx: Int = 0
)

/**
 * 分页引擎：基于 TextMeasurer 精确测量，按字符级别分页
 */
class PageEngine {

    private val sentenceToPageIndex = mutableMapOf<String, Int>()
    private val paragraphSentenceRanges = mutableMapOf<String, List<IntRange>>()

    /**
     * 为章节构建句子范围映射（从磁盘缓存加载时需要调用）
     */
    fun buildSentenceRanges(chapter: Chapter) {
        chapter.paragraphs.forEach { para ->
            if (para.type == ParagraphType.text && para.sentences.isNotEmpty()) {
                if (paragraphSentenceRanges.containsKey(para.id)) return@forEach
                val ranges = mutableListOf<IntRange>()
                var offset = 0
                para.sentences.forEach { sentence ->
                    val start = offset
                    val end = offset + sentence.content.length
                    ranges.add(start until end)
                    offset = end
                }
                paragraphSentenceRanges[para.id] = ranges
            }
        }
    }

    /**
     * 使用 TextMeasurer 进行精确分页
     */
    fun paginate(
        chapter: Chapter,
        config: PageConfig,
        textMeasurer: TextMeasurer
    ): List<Page> {
        sentenceToPageIndex.clear()
        paragraphSentenceRanges.clear()
        
        // 预处理：为每个段落建立句子到字符范围的映射
        chapter.paragraphs.forEach { para ->
            if (para.type == ParagraphType.text && para.sentences.isNotEmpty()) {
                val ranges = mutableListOf<IntRange>()
                var offset = 0
                para.sentences.forEach { sentence ->
                    val start = offset
                    val end = offset + sentence.content.length
                    ranges.add(start until end)
                    offset = end
                }
                paragraphSentenceRanges[para.id] = ranges
            }
        }
        
        val pages = mutableListOf<Page>()
        var currentSlices = mutableListOf<PageSlice>()
        var usedHeightPx = 0
        var isFirstPage = true
        
        fun availableHeight(): Int {
            return if (isFirstPage) config.availableHeightPx - config.titleHeightPx else config.availableHeightPx
        }
        
        fun flushPage() {
            if (currentSlices.isNotEmpty()) {
                val pageIndex = pages.size
                pages.add(Page(
                    index = pageIndex,
                    slices = currentSlices.toList(),
                    isFirstPage = isFirstPage
                ))
                // 记录句子到页码的映射
                currentSlices.forEach { slice ->
                    if (slice.paragraphType == ParagraphType.text) {
                        val ranges = paragraphSentenceRanges[slice.paragraphId] ?: return@forEach
                        val para = chapter.paragraphs.find { it.id == slice.paragraphId } ?: return@forEach
                        para.sentences.forEachIndexed { idx, sentence ->
                            val range = ranges.getOrNull(idx) ?: return@forEachIndexed
                            // 如果句子范围与当前片段有交集，记录映射
                            if (range.first < slice.endChar && range.last >= slice.startChar) {
                                sentenceToPageIndex[sentence.id] = pageIndex
                            }
                        }
                    }
                }
                currentSlices = mutableListOf()
                usedHeightPx = 0
                isFirstPage = false
            }
        }
        
        fun measureTextHeight(
            text: String,
            style: TextStyle,
            width: Int,
            hasIndent: Boolean = false
        ): Int {
            val finalStyle = if (hasIndent) {
                style.copy(textIndent = TextIndent(firstLine = (style.fontSize.value * 2).sp))
            } else {
                style
            }
            val result = textMeasurer.measure(
                text = text,
                style = finalStyle,
                constraints = Constraints(maxWidth = width)
            )
            return result.size.height
        }
        
        fun addParagraph(para: Paragraph) {
            val isText = para.type == ParagraphType.text
            val style = if (isText) config.textStyle else config.annotationStyle
            val spacing = if (isText) config.textParagraphSpacingPx else config.annotationSpacingPx
            val content = if (isText) para.content else "【注】${para.content}"
            
            var startChar = 0
            var isFirstSlice = true
            
            while (startChar < content.length) {
                val remainingHeight = availableHeight() - usedHeightPx
                
                // 如果剩余空间不足一行高度，先换页
                val singleCharHeight = measureTextHeight(
                    text = content.substring(startChar, minOf(startChar + 1, content.length)),
                    style = style,
                    width = config.availableWidthPx,
                    hasIndent = isText && isFirstSlice
                )
                if (remainingHeight < singleCharHeight && currentSlices.isNotEmpty()) {
                    flushPage()
                    continue
                }
                
                // 先尝试放入整个剩余内容
                val fullText = content.substring(startChar)
                val fullHeight = measureTextHeight(
                    text = fullText,
                    style = style,
                    width = config.availableWidthPx,
                    hasIndent = isText && isFirstSlice
                )
                val fullTotalHeight = fullHeight + spacing
                
                val endChar: Int
                if (fullTotalHeight <= remainingHeight) {
                    // 整个剩余内容都能放下
                    endChar = content.length
                } else {
                    // 二分查找：找到能放入剩余空间的最大字符数
                    // 使用严格小于（<）而不是小于等于（<=），避免边界情况溢出
                    var low = startChar + 1
                    var high = content.length
                    var bestEnd = startChar + 1
                    
                    while (low <= high) {
                        val mid = (low + high) / 2
                        val testText = content.substring(startChar, mid)
                        val testHeight = measureTextHeight(
                            text = testText,
                            style = style,
                            width = config.availableWidthPx,
                            hasIndent = isText && isFirstSlice
                        )
                        
                        // 严格小于，留出余量
                        if (testHeight < remainingHeight) {
                            bestEnd = mid
                            low = mid + 1
                        } else if (testHeight == remainingHeight && mid == content.length) {
                            // 刚好是段落末尾且高度相等，需要检查加上 spacing 后是否超出
                            if (testHeight + spacing <= remainingHeight) {
                                bestEnd = mid
                            }
                            break
                        } else {
                            high = mid - 1
                        }
                    }
                    
                    // 贪心扩展：只在高度完全不变时扩展（同一行内的字符）
                    val baseHeight = measureTextHeight(
                        text = content.substring(startChar, bestEnd),
                        style = style,
                        width = config.availableWidthPx,
                        hasIndent = isText && isFirstSlice
                    )
                    
                    while (bestEnd < content.length) {
                        val nextEnd = bestEnd + 1
                        val testText = content.substring(startChar, nextEnd)
                        val testHeight = measureTextHeight(
                            text = testText,
                            style = style,
                            width = config.availableWidthPx,
                            hasIndent = isText && isFirstSlice
                        )
                        
                        // 只有高度完全不变时才扩展（确保是同一行）
                        if (testHeight == baseHeight) {
                            bestEnd = nextEnd
                        } else {
                            // 高度变化，停止扩展
                            break
                        }
                    }
                    
                    endChar = bestEnd
                }
                val isLastSlice = endChar >= content.length
                
                // 对于 annotation，需要调整 startChar/endChar 去掉前缀
                val slice = if (isText) {
                    PageSlice(
                        paragraphId = para.id,
                        paragraphType = para.type,
                        startChar = startChar,
                        endChar = endChar,
                        isFirstSlice = isFirstSlice,
                        isLastSlice = isLastSlice
                    )
                } else {
                    // annotation 的 content 加了 "【注】" 前缀，需要调整
                    val prefixLen = 3  // "【注】".length
                    PageSlice(
                        paragraphId = para.id,
                        paragraphType = para.type,
                        startChar = maxOf(0, startChar - prefixLen),
                        endChar = maxOf(0, endChar - prefixLen),
                        isFirstSlice = isFirstSlice,
                        isLastSlice = isLastSlice
                    )
                }
                
                val sliceText = content.substring(startChar, endChar)
                val sliceHeight = measureTextHeight(
                    text = sliceText,
                    style = style,
                    width = config.availableWidthPx,
                    hasIndent = isText && isFirstSlice
                )
                val totalSliceHeight = sliceHeight + (if (isLastSlice) spacing else 0)
                
                currentSlices.add(slice)
                usedHeightPx += totalSliceHeight
                startChar = endChar
                isFirstSlice = false
                
                // 如果不是最后一个片段，换页继续
                if (!isLastSlice) {
                    flushPage()
                }
            }
        }
        
        chapter.paragraphs.forEach { para ->
            addParagraph(para)
        }
        flushPage()
        
        if (pages.isEmpty()) {
            pages.add(Page(index = 0, slices = emptyList(), isFirstPage = true))
        }
        return pages
    }

    /**
     * 根据句子 ID 查找所在页码
     */
    fun findPageBySentenceId(sentenceId: String): Int? = sentenceToPageIndex[sentenceId]

    /**
     * 获取页面中所有句子 ID 列表
     */
    fun getSentenceIds(page: Page, chapter: Chapter): List<String> {
        val result = mutableListOf<String>()
        page.slices.forEach { slice ->
            if (slice.paragraphType == ParagraphType.text) {
                val para = chapter.paragraphs.find { it.id == slice.paragraphId } ?: return@forEach
                val ranges = paragraphSentenceRanges[slice.paragraphId] ?: return@forEach
                para.sentences.forEachIndexed { idx, sentence ->
                    val range = ranges.getOrNull(idx) ?: return@forEachIndexed
                    if (range.first < slice.endChar && range.last >= slice.startChar) {
                        if (sentence.id !in result) {
                            result.add(sentence.id)
                        }
                    }
                }
            }
        }
        return result
    }
    
    /**
     * 获取段落中句子的字符范围映射
     */
    fun getSentenceRanges(paragraphId: String): List<IntRange>? = paragraphSentenceRanges[paragraphId]
}
