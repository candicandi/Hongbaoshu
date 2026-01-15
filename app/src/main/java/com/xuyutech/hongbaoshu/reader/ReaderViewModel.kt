package com.xuyutech.hongbaoshu.reader

import android.app.Application
import androidx.compose.ui.text.TextMeasurer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xuyutech.hongbaoshu.audio.AudioManager
import com.xuyutech.hongbaoshu.data.Chapter
import com.xuyutech.hongbaoshu.data.ContentLoader
import com.xuyutech.hongbaoshu.storage.ProgressStore
import com.xuyutech.hongbaoshu.storage.ProgressState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 四元组数据类
 */
data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

class ReaderViewModel(
    application: Application,
    private val contentLoader: ContentLoader,
    private val progressStore: ProgressStore,
    private val audioManager: AudioManager,
    private val pageCacheStore: com.xuyutech.hongbaoshu.storage.PageCacheStore
) : AndroidViewModel(application) {

    private val _state = androidx.lifecycle.MutableLiveData(ReaderState())
    val state: androidx.lifecycle.LiveData<ReaderState> = _state

    val pageEngine = PageEngine()
    
    // 内存分页缓存：key = "chapterId_fontSizeLevel_widthPx_heightPx"
    private val pageCache: MutableMap<String, List<Page>> = mutableMapOf()
    
    // 当前配置参数（用于缓存 key 生成）
    private var currentWidthPx: Int = 0
    private var currentHeightPx: Int = 0
    
    // 预计算状态
    private var precomputeJob: Job? = null
    var isPrecomputing = false
        private set
    
    // 朗读模式是否开启（从 state 中读取）
    val narrationEnabled: Boolean
        get() = _state.value?.narrationEnabled ?: false
    // 延迟播放任务，用于取消之前的延迟
    private var delayedPlayJob: Job? = null
    // 是否正在手动翻页（用于防止自动播放干扰）
    private var isManualPageTurn = false

    init {
        load()
        setupNarrationCallback()
    }
    
    /**
     * 设置朗读完成回调，在句子播放完成后自动播放下一句
     * 这个回调在 ExoPlayer 的线程中执行，不依赖 UI 层
     */
    private fun setupNarrationCallback() {
        audioManager.setNarrationCompletionCallback { completedSentenceId ->
            // 在后台线程中执行，不受 UI 生命周期影响
            if (narrationEnabled && !isManualPageTurn) {
                viewModelScope.launch {
                    playNextSentenceAfter(completedSentenceId)
                }
            }
        }
    }
    
    /**
     * 播放指定句子之后的下一句
     */
    private fun playNextSentenceAfter(completedSentenceId: String) {
        val sentences = _state.value?.currentPageSentenceIds ?: return
        val idx = sentences.indexOf(completedSentenceId)
        
        if (idx >= 0 && idx + 1 < sentences.size) {
            // 当前页还有下一句
            playSentence(sentences[idx + 1])
        } else {
            // 当前页读完，需要翻页
            val current = _state.value ?: return
            val book = current.book ?: return
            val pageCount = getCachedPages(book.chapters[current.currentChapterIndex].id)?.size ?: return
            
            if (current.pageIndex + 1 < pageCount) {
                // 翻到下一页
                autoTurnToNextPage(pageCount)
            } else if (current.currentChapterIndex + 1 < book.chapters.size) {
                // 翻到下一章
                autoTurnToNextPage(pageCount)
            } else {
                // 全书读完
                toggleNarration(false)
            }
        }
    }
    
    /**
     * 自动翻到下一页并播放第一句
     */
    private fun autoTurnToNextPage(currentPageCount: Int) {
        val current = _state.value ?: return
        val book = current.book ?: return
        val newIndex = current.pageIndex + 1
        
        when {
            newIndex >= currentPageCount && current.currentChapterIndex < book.chapters.lastIndex -> {
                // 翻到下一章
                val nextChapter = current.currentChapterIndex + 1
                _state.postValue(current.copy(currentChapterIndex = nextChapter, pageIndex = 0))
                persistState(chapterIndex = nextChapter, pageIndex = 0)
                // 延迟后播放新页面第一句
                schedulePlayFirstSentence()
            }
            newIndex in 0 until currentPageCount -> {
                // 翻到下一页
                _state.postValue(current.copy(pageIndex = newIndex))
                persistState(pageIndex = newIndex)
                // 延迟后播放新页面第一句
                schedulePlayFirstSentence()
            }
        }
    }
    
    /**
     * 延迟播放新页面第一句（等待页面数据更新）
     */
    private fun schedulePlayFirstSentence() {
        viewModelScope.launch {
            delay(300L)
            _state.value = _state.value?.copy(needPlayFirstSentence = true)
        }
    }
    
    /**
     * 清除播放下一句的标记
     */
    fun clearPlayNextSentence() {
        _state.value = _state.value?.copy(needPlayNextSentence = false, lastPlayedSentenceId = null)
    }
    
    /**
     * 更新当前页的句子列表（由 UI 层调用）
     */
    fun updateCurrentPageSentences(sentenceIds: List<String>) {
        val current = _state.value ?: return
        if (current.currentPageSentenceIds != sentenceIds) {
            _state.value = current.copy(currentPageSentenceIds = sentenceIds)
        }
    }
    
    /**
     * 生成缓存 key
     */
    private fun cacheKey(chapterId: String, fontSizeLevel: Int, widthPx: Int, heightPx: Int): String {
        return "${chapterId}_${fontSizeLevel}_${widthPx}_${heightPx}"
    }
    
    /**
     * 获取缓存的分页数据
     */
    fun getCachedPages(chapterId: String): List<Page>? {
        val fontSizeLevel = _state.value?.fontSizeLevel ?: FONT_SIZE_DEFAULT
        val key = cacheKey(chapterId, fontSizeLevel, currentWidthPx, currentHeightPx)
        return pageCache[key]
    }
    
    /**
     * 获取指定字号的缓存分页数据
     */
    fun getCachedPages(chapterId: String, fontSizeLevel: Int): List<Page>? {
        val key = cacheKey(chapterId, fontSizeLevel, currentWidthPx, currentHeightPx)
        return pageCache[key]
    }
    
    /**
     * 缓存分页数据
     */
    fun cachePages(chapterId: String, pages: List<Page>) {
        val fontSizeLevel = _state.value?.fontSizeLevel ?: FONT_SIZE_DEFAULT
        val key = cacheKey(chapterId, fontSizeLevel, currentWidthPx, currentHeightPx)
        pageCache[key] = pages
    }
    
    /**
     * 缓存指定字号的分页数据
     */
    fun cachePages(chapterId: String, fontSizeLevel: Int, pages: List<Page>) {
        val key = cacheKey(chapterId, fontSizeLevel, currentWidthPx, currentHeightPx)
        pageCache[key] = pages
    }
    
    /**
     * 更新屏幕配置（屏幕尺寸变化时清除缓存）
     */
    fun updateScreenSize(widthPx: Int, heightPx: Int) {
        if (currentWidthPx != widthPx || currentHeightPx != heightPx) {
            currentWidthPx = widthPx
            currentHeightPx = heightPx
            pageCache.clear()
        }
    }
    
    /**
     * 更新配置哈希（已废弃，使用 updateScreenSize 替代）
     */
    @Deprecated("Use updateScreenSize instead")
    fun updateConfigHash(hash: Int) {
        // 保留兼容性
    }
    
    /**
     * 生成持久化缓存 key（不含章节ID，用于整本书）
     */
    private fun diskCacheKey(fontSizeLevel: Int, widthPx: Int, heightPx: Int): String {
        return "${fontSizeLevel}_${widthPx}_${heightPx}"
    }
    
    /**
     * 计算指定字号的所有章节分页（包含全书页码）
     * 优先从磁盘缓存加载，缓存不存在时计算并保存
     */
    fun computeAllChapters(
        textMeasurer: androidx.compose.ui.text.TextMeasurer,
        buildConfig: (Int) -> PageConfig,
        fontSizeLevel: Int
    ) {
        val book = _state.value?.book ?: return
        
        // 检查内存缓存
        val firstChapterKey = cacheKey(book.chapters.first().id, fontSizeLevel, currentWidthPx, currentHeightPx)
        val firstChapterPages = pageCache[firstChapterKey]
        if (firstChapterPages != null && firstChapterPages.isNotEmpty() && firstChapterPages.first().totalPages > 0) {
            return
        }
        
        // 尝试从磁盘加载缓存
        val diskKey = diskCacheKey(fontSizeLevel, currentWidthPx, currentHeightPx)
        val diskCache = pageCacheStore.load(diskKey)
        if (diskCache != null) {
            // 加载到内存缓存
            book.chapters.forEach { chapter ->
                val key = cacheKey(chapter.id, fontSizeLevel, currentWidthPx, currentHeightPx)
                diskCache[chapter.id]?.let { pages ->
                    pageCache[key] = pages
                }
                // 构建句子范围映射（朗读功能需要）
                pageEngine.buildSentenceRanges(chapter)
            }
            // 验证加载成功
            val loaded = pageCache[firstChapterKey]
            if (loaded != null && loaded.isNotEmpty() && loaded.first().totalPages > 0) {
                return
            }
        }
        
        // 磁盘缓存不存在，计算分页
        val config = buildConfig(fontSizeLevel)
        
        // 第一遍：计算所有章节的分页，统计总页数
        val chapterPages = mutableListOf<List<Page>>()
        var totalPages = 0
        
        book.chapters.forEach { chapter ->
            val pages = pageEngine.paginate(chapter, config, textMeasurer)
            chapterPages.add(pages)
            totalPages += pages.size
        }
        
        // 第二遍：更新全书页码并缓存到内存
        var globalIndex = 0
        val diskData = mutableMapOf<String, List<Page>>()
        book.chapters.forEachIndexed { idx, chapter ->
            val key = cacheKey(chapter.id, fontSizeLevel, currentWidthPx, currentHeightPx)
            val pages = chapterPages[idx]
            val updatedPages = pages.map { page ->
                globalIndex++
                page.copy(globalIndex = globalIndex, totalPages = totalPages)
            }
            pageCache[key] = updatedPages
            diskData[chapter.id] = updatedPages
        }
        
        // 保存到磁盘
        pageCacheStore.save(diskKey, diskData)
    }
    
    /**
     * 启动后台预计算其他字号的分页
     */
    fun startPrecompute(
        textMeasurer: androidx.compose.ui.text.TextMeasurer,
        buildConfig: (Int) -> PageConfig
    ) {
        val book = _state.value?.book ?: return
        val currentFontSize = _state.value?.fontSizeLevel ?: FONT_SIZE_DEFAULT
        
        precomputeJob?.cancel()
        precomputeJob = viewModelScope.launch(Dispatchers.Default) {
            isPrecomputing = true
            try {
                // 先计算当前字号的所有章节（带全书页码）
                computeAllChapters(textMeasurer, buildConfig, currentFontSize)
                
                // 预计算其他字号档位
                for (fontLevel in FONT_SIZE_MIN..FONT_SIZE_MAX) {
                    if (fontLevel == currentFontSize) continue
                    computeAllChapters(textMeasurer, buildConfig, fontLevel)
                }
            } finally {
                isPrecomputing = false
            }
        }
    }

    fun refresh() = load()

    private fun load() {
        _state.value = ReaderState(isLoading = true)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val bookResult = contentLoader.loadBook(getApplication())
                    val saved = progressStore.progress.first()
                    Pair(bookResult, saved)
                }
            }
            result.onSuccess { (bookResult, saved) ->
                val cappedChapterIndex =
                    saved.chapterIndex.coerceIn(0, bookResult.book.chapters.lastIndex)
                _state.value = ReaderState(
                    isLoading = false,
                    book = bookResult.book,
                    missingAudio = bookResult.missingSentenceAudioIds,
                    currentChapterIndex = cappedChapterIndex,
                    pageIndex = saved.pageIndex  // 由 UI 层校验
                )
                restoreAudioState(saved)
            }.onFailure { e ->
                _state.value = ReaderState(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun selectChapter(index: Int) {
        val book = _state.value?.book ?: return
        val safeIndex = index.coerceIn(0, book.chapters.lastIndex)
        _state.value = _state.value?.copy(currentChapterIndex = safeIndex, pageIndex = 0)
        persistState(chapterIndex = safeIndex, pageIndex = 0, narrationSentenceId = null)
    }

    private fun restoreAudioState(saved: ProgressState) {
        // 朗读模式默认关闭，不自动恢复朗读状态
        // narrationEnabled 保持 false，用户需要手动打开
        
        // 只恢复 BGM 状态
        if (saved.bgmEnabled) {
            audioManager.playBgm(saved.bgmIndex)
        }
        audioManager.setBgmVolume(saved.bgmVolume)
    }

    fun saveAudioState(narrationSentenceId: String?) {
        persistState(narrationSentenceId = narrationSentenceId)
    }

    fun playBgm(enable: Boolean) {
        if (enable) audioManager.playBgm() else audioManager.pauseBgm()
        persistState()
    }

    fun nextBgm() {
        audioManager.nextBgm()
        persistState()
    }

    fun setBgmVolume(volume: Float) {
        audioManager.setBgmVolume(volume)
        persistState()
    }

    fun playSentence(sentenceId: String) {
        android.util.Log.d("ReaderViewModel", "playSentence called: $sentenceId")
        val success = audioManager.playSentence(sentenceId)
        android.util.Log.d("ReaderViewModel", "playSentence result: $success")
        if (success) {
            persistState(narrationSentenceId = sentenceId)
        } else {
            showToast("该句子音频缺失")
        }
    }

    private fun showToast(message: String) {
        _state.value = _state.value?.copy(toastMessage = message)
    }

    fun clearToast() {
        _state.value = _state.value?.copy(toastMessage = null)
    }

    /**
     * 设置字体大小档位
     */
    fun setFontSize(level: Int) {
        val current = _state.value ?: return
        val safeLevel = level.coerceIn(FONT_SIZE_MIN, FONT_SIZE_MAX)
        if (current.fontSizeLevel != safeLevel) {
            // 重置页码到第一页（因为不同字号分页数量不同）
            _state.value = current.copy(fontSizeLevel = safeLevel, pageIndex = 0)
            // 不再清除缓存，因为现在按字号分别缓存
        }
    }

    fun pauseOrResumeSentence() {
        if (audioManager.state.value.narrationSentenceId != null) {
            if (audioManager.state.value.narrationPlaying) {
                audioManager.pauseSentence()
            } else {
                audioManager.resumeSentence()
            }
        }
        persistState()
    }

    fun stopSentence() {
        audioManager.stopSentence()
        persistState(narrationSentenceId = null)
    }

    /**
     * 切换朗读开关
     */
    fun toggleNarration(enabled: Boolean) {
        _state.value = _state.value?.copy(narrationEnabled = enabled)
        if (!enabled) {
            stopSentence()
        }
        // 如果开启，由 UI 层触发播放第一句
    }
    
    /**
     * 暂停朗读（保持开关状态，用于返回封面时）
     */
    fun pauseNarration() {
        audioManager.stopSentence()
    }
    
    /**
     * 设置手动翻页标记
     */
    fun setManualPageTurn(value: Boolean) {
        isManualPageTurn = value
    }
    
    /**
     * 重置朗读状态（用于手动翻页后）
     */
    fun resetNarrationState() {
        // 现在由回调机制处理，无需额外操作
    }

    private fun persistState(
        chapterIndex: Int? = null,
        pageIndex: Int? = null,
        narrationSentenceId: String? = null
    ) {
        val current = _state.value ?: return
        val finalChapter = chapterIndex ?: current.currentChapterIndex
        val finalPage = pageIndex ?: current.pageIndex
        viewModelScope.launch(Dispatchers.IO) {
            progressStore.save(
                ProgressState(
                    chapterIndex = finalChapter,
                    pageIndex = finalPage,
                    narrationSentenceId = narrationSentenceId
                        ?: audioManager.state.value.narrationSentenceId,
                    narrationPosition = audioManager.state.value.narrationPosition,
                    bgmIndex = audioManager.state.value.bgmIndex,
                    bgmEnabled = audioManager.state.value.bgmEnabled,
                    bgmVolume = audioManager.state.value.bgmVolume
                )
            )
        }
    }

    /**
     * 更新页码（由 UI 层调用，传入当前章节的总页数）
     */
    fun updatePage(delta: Int, currentChapterPageCount: Int, prevChapterPageCount: Int = 0) {
        val current = _state.value ?: return
        val book = current.book ?: return
        val newIndex = current.pageIndex + delta
        var pageChanged = false
        
        when {
            // 翻到下一章
            newIndex >= currentChapterPageCount && current.currentChapterIndex < book.chapters.lastIndex -> {
                val nextChapter = current.currentChapterIndex + 1
                audioManager.playFlip()
                _state.value = current.copy(currentChapterIndex = nextChapter, pageIndex = 0)
                persistState(chapterIndex = nextChapter, pageIndex = 0)
                pageChanged = true
            }
            // 翻到上一章最后一页
            newIndex < 0 && current.currentChapterIndex > 0 -> {
                val prevChapter = current.currentChapterIndex - 1
                audioManager.playFlip()
                _state.value = current.copy(
                    currentChapterIndex = prevChapter, 
                    pageIndex = maxOf(0, prevChapterPageCount - 1)
                )
                persistState(chapterIndex = prevChapter, pageIndex = maxOf(0, prevChapterPageCount - 1))
                pageChanged = true
            }
            // 正常翻页
            newIndex in 0 until currentChapterPageCount && newIndex != current.pageIndex -> {
                audioManager.playFlip()
                _state.value = current.copy(pageIndex = newIndex)
                persistState(pageIndex = newIndex)
                pageChanged = true
            }
        }
        
        // 如果正在朗读模式且翻页成功，延迟后从新页面第一句开始播放
        if (pageChanged && narrationEnabled) {
            delayedPlayJob?.cancel()
            isManualPageTurn = true
            audioManager.stopSentence()
            delayedPlayJob = viewModelScope.launch {
                delay(500L)
                // 触发 UI 层播放第一句
                _state.value = _state.value?.copy(needPlayFirstSentence = true)
                isManualPageTurn = false
            }
        }
    }
    
    /**
     * 清除播放第一句的标记
     */
    fun clearPlayFirstSentence() {
        _state.value = _state.value?.copy(needPlayFirstSentence = false)
    }
    
    /**
     * 静默翻页（用于朗读自动翻页）
     */
    fun updatePageSilent(delta: Int, currentChapterPageCount: Int, prevChapterPageCount: Int = 0) {
        val current = _state.value ?: return
        val book = current.book ?: return
        val newIndex = current.pageIndex + delta
        
        when {
            newIndex >= currentChapterPageCount && current.currentChapterIndex < book.chapters.lastIndex -> {
                val nextChapter = current.currentChapterIndex + 1
                _state.postValue(current.copy(currentChapterIndex = nextChapter, pageIndex = 0))
                persistState(chapterIndex = nextChapter, pageIndex = 0)
            }
            newIndex < 0 && current.currentChapterIndex > 0 -> {
                val prevChapter = current.currentChapterIndex - 1
                _state.postValue(current.copy(
                    currentChapterIndex = prevChapter, 
                    pageIndex = maxOf(0, prevChapterPageCount - 1)
                ))
                persistState(chapterIndex = prevChapter, pageIndex = maxOf(0, prevChapterPageCount - 1))
            }
            newIndex in 0 until currentChapterPageCount -> {
                _state.postValue(current.copy(pageIndex = newIndex))
                persistState(pageIndex = newIndex)
            }
        }
    }
    
    /**
     * 设置页码（用于校验恢复的页码）
     */
    fun setPageIndex(index: Int) {
        val current = _state.value ?: return
        if (current.pageIndex != index) {
            _state.value = current.copy(pageIndex = index)
            persistState(pageIndex = index)
        }
    }
}
