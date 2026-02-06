@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xuyutech.hongbaoshu.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward

import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import com.xuyutech.hongbaoshu.audio.AudioManager
import com.xuyutech.hongbaoshu.data.Chapter
import com.xuyutech.hongbaoshu.data.ParagraphType
import kotlin.math.roundToInt

data class ToolBarState(
    val isVisible: Boolean = false,
    val isInteracting: Boolean = false,
    val lastInteractionTs: Long = 0L,
    val autoHideMs: Long = 3000L
)


@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    audioManager: AudioManager,
    onBack: () -> Unit
) {
    val state = viewModel.state.observeAsState(ReaderState())
    val audioState = audioManager.state.collectAsState()

    // Intercept system back (including edge-swipe) so it navigates back to bookshelf
    // instead of finishing the Activity.
    BackHandler(enabled = true) {
        if (state.value.narrationEnabled) {
            viewModel.pauseNarration()
        }
        onBack()
    }

    val showToc = remember { mutableStateOf(false) }
    val showNarrationPanel = remember { mutableStateOf(false) }
    val showFontSettings = remember { mutableStateOf(false) }
    val toolbarState = remember { mutableStateOf(ToolBarState()) }
    var suppressNextCenterTap by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    // Tooltip State
    var activeTooltipSentenceId by remember { mutableStateOf<String?>(null) }
    // Clicked sentence for highlighting (same style as narration)
    var clickedSentenceId by remember { mutableStateOf<String?>(null) }

    // Manual tap signal for SwipeablePageContainer (from Text component)
    val manualTapSignal = remember { kotlinx.coroutines.flow.MutableSharedFlow<androidx.compose.ui.geometry.Offset>(extraBufferCapacity = 1) }
    
    // Auto-dismiss tooltip after 3 seconds
    LaunchedEffect(activeTooltipSentenceId) {
        if (activeTooltipSentenceId != null) {
            delay(3000)
            activeTooltipSentenceId = null
            clickedSentenceId = null
        }
    }

    // 显示 Toast 消息
    LaunchedEffect(state.value.toastMessage) {
        state.value.toastMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearToast()
        }
    }

    val updateToolbarInteraction = {
        toolbarState.value = toolbarState.value.copy(
            isVisible = true,
            lastInteractionTs = System.currentTimeMillis()
        )
    }

    val hideToolbar = {
        toolbarState.value = toolbarState.value.copy(
            isVisible = false,
            isInteracting = false,
            lastInteractionTs = System.currentTimeMillis()
        )
        showNarrationPanel.value = false
        showFontSettings.value = false
    }

    val openNarrationPanel = {
        toolbarState.value = toolbarState.value.copy(
            isVisible = true,
            isInteracting = true,
            lastInteractionTs = System.currentTimeMillis()
        )
        showNarrationPanel.value = true
        showFontSettings.value = false
    }

    val openFontSettings = {
        toolbarState.value = toolbarState.value.copy(
            isVisible = true,
            isInteracting = true,
            lastInteractionTs = System.currentTimeMillis()
        )
        showFontSettings.value = true
        showNarrationPanel.value = false
    }



    LaunchedEffect(
        toolbarState.value.isVisible,
        toolbarState.value.lastInteractionTs,
        toolbarState.value.isInteracting
    ) {
        val current = toolbarState.value
        if (current.isVisible && !current.isInteracting) {
            val token = current.lastInteractionTs
            delay(current.autoHideMs)
            val latest = toolbarState.value
            if (latest.isVisible && !latest.isInteracting && latest.lastInteractionTs == token) {
                toolbarState.value = latest.copy(isVisible = false)
            }
        }
    }



    com.xuyutech.hongbaoshu.ui.theme.HongbaoshuTheme(darkTheme = state.value.isNightMode) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
        val screenHeightPx = with(density) { maxHeight.toPx().toInt() }
        val screenWidthPx = with(density) { maxWidth.toPx().toInt() }
        // 页面正文区域上下各 10% 留白，中间 80% 填充文字
        val contentHeightPx = (screenHeightPx * 0.8f).toInt()
        // 文字区域扣掉 PageContent 的水平 padding 20.dp * 2
        val horizontalPaddingPx = with(density) { 20.dp.toPx().toInt() }
        val contentWidthPx = (screenWidthPx - horizontalPaddingPx * 2).coerceAtLeast(0)
        
        val fontSizeLevel = state.value.fontSizeLevel
        val fontSizeSp = fontSizeLevel.sp
        val lineHeightSp = fontSizeSp * 1.5f  // 行高为字体大小的 1.5 倍
        val textStyle = TextStyle(
            fontSize = fontSizeSp,
            fontWeight = FontWeight.Normal,
            lineHeight = lineHeightSp,
            color = MaterialTheme.colorScheme.onBackground
        )
        val annotationFontSizeSp = fontSizeSp * 0.85f
        val annotationStyle = TextStyle(
            fontSize = annotationFontSizeSp,
            color = Color.Gray,
            lineHeight = annotationFontSizeSp * 1.5f
        )
        val textParagraphSpacingPx = with(density) { 16.dp.toPx().toInt() }
        val annotationSpacingPx = with(density) { 12.dp.toPx().toInt() }
        
        // 测量章节标题高度
        val titleStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        val titlePaddingPx = with(density) { 24.dp.toPx().toInt() }
        val titleHeightPx = remember(fontSizeLevel, screenWidthPx) {
            val result = textMeasurer.measure(
                text = "测试标题",
                style = titleStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = contentWidthPx)
            )
            result.size.height + titlePaddingPx
        }
        
        // 构建分页配置的函数（用于预计算不同字号）
        fun buildPageConfig(fontLevel: Int): PageConfig {
            val fSizeSp = fontLevel.sp
            val lHeightSp = fSizeSp * 1.5f
            val tStyle = TextStyle(
                fontSize = fSizeSp,
                fontWeight = FontWeight.Normal,
                lineHeight = lHeightSp,
                color = Color.Black  // PageConfig is used for measurement, not rendering
            )
            val aFontSizeSp = fSizeSp * 0.85f
            val aStyle = TextStyle(
                fontSize = aFontSizeSp,
                color = Color.Gray,
                lineHeight = aFontSizeSp * 1.5f
            )
            return PageConfig(
                availableHeightPx = contentHeightPx,
                availableWidthPx = contentWidthPx,
                titleHeightPx = titleHeightPx,
                textStyle = tStyle,
                annotationStyle = aStyle,
                textParagraphSpacingPx = textParagraphSpacingPx,
                annotationSpacingPx = annotationSpacingPx
            )
        }
        
        // 当前字号的分页配置
        val pageConfig = remember(contentHeightPx, contentWidthPx, fontSizeLevel) {
            buildPageConfig(fontSizeLevel)
        }
        
        // 更新屏幕尺寸（用于缓存 key）
        LaunchedEffect(contentWidthPx, contentHeightPx) {
            viewModel.updateScreenSize(contentWidthPx, contentHeightPx)
        }
        
        val book = state.value.book
        val currentChapterIndex = state.value.currentChapterIndex
        val currentChapter = book?.chapters?.getOrNull(currentChapterIndex)
        
        // 分页计算状态
        var pagesReady by remember { mutableStateOf(false) }
        var globalPagesReady by remember { mutableStateOf(false) }
        
        LaunchedEffect(book, currentChapterIndex, fontSizeLevel, contentWidthPx, contentHeightPx) {
            if (book != null &&
                currentChapterIndex in book.chapters.indices &&
                contentWidthPx > 0 &&
                contentHeightPx > 0
            ) {
                pagesReady = false
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    viewModel.computeCurrentChapter(textMeasurer, ::buildPageConfig, fontSizeLevel)
                }
                pagesReady = true
            }
        }

        LaunchedEffect(book, fontSizeLevel, contentWidthPx, contentHeightPx) {
            if (book != null && contentWidthPx > 0 && contentHeightPx > 0) {
                globalPagesReady = false
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    viewModel.computeRemainingChapters(textMeasurer, ::buildPageConfig, fontSizeLevel)
                }
                globalPagesReady = true
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    viewModel.startPrecompute(textMeasurer, ::buildPageConfig)
                }
            }
        }
        
        // 从缓存获取当前章节的分页（已包含全书页码）
        val currentPages = if (pagesReady && currentChapter != null) {
            viewModel.getCachedPages(currentChapter.id) ?: emptyList()
        } else {
            emptyList()
        }
        
        // 从缓存获取上一章的分页（用于跨章节翻页）
        val prevChapter = book?.chapters?.getOrNull(currentChapterIndex - 1)
        val prevChapterPages = if (pagesReady && prevChapter != null) {
            viewModel.getCachedPages(prevChapter.id) ?: emptyList()
        } else {
            emptyList()
        }
        
        val pageCount = currentPages.size
        val currentPageIndex = state.value.pageIndex.coerceIn(0, maxOf(0, pageCount - 1))
        val globalPagination = remember(globalPagesReady, book, fontSizeLevel, contentWidthPx, contentHeightPx) {
            if (!globalPagesReady || book == null) return@remember null
            val chapterPageCounts = book.chapters.map { chapter ->
                viewModel.getCachedPages(chapter.id, fontSizeLevel)?.size
            }
            if (chapterPageCounts.any { it == null }) return@remember null
            val counts = chapterPageCounts.filterNotNull()
            val startByChapterId = linkedMapOf<String, Int>()
            var acc = 0
            book.chapters.forEachIndexed { idx, chapter ->
                startByChapterId[chapter.id] = acc
                acc += counts[idx]
            }
            GlobalPaginationInfo(
                chapterStartPageById = startByChapterId,
                totalPages = acc
            )
        }
        
        // 校验页码
        LaunchedEffect(currentPageIndex, state.value.pageIndex) {
            if (state.value.pageIndex != currentPageIndex && pageCount > 0) {
                viewModel.setPageIndex(currentPageIndex)
            }
        }
        
        // 获取当前页面的句子列表（用于朗读）
        val currentPageSentences = remember(currentChapter, currentPages, currentPageIndex) {
            if (currentChapter == null || currentPages.isEmpty()) emptyList()
            else {
                val page = currentPages.getOrNull(currentPageIndex) ?: return@remember emptyList()
                viewModel.pageEngine.getSentenceIds(page, currentChapter)
            }
        }
        
        // 当句子列表变化时，更新到 ViewModel
        LaunchedEffect(currentPageSentences) {
            viewModel.updateCurrentPageSentences(currentPageSentences)
        }
        
        // 标记是否已经初始化过朗读（用于区分首次进入和翻页）
        var narrationInitialized by remember { mutableStateOf(false) }
        
        // 进入页面时，如果朗读开关开着且当前没有在播放，自动播放第一句
        // 只在首次进入时触发，翻页由 needPlayFirstSentence 处理
        LaunchedEffect(pagesReady) {
            if (pagesReady && 
                !narrationInitialized &&
                state.value.narrationEnabled && 
                audioState.value.narrationSentenceId == null &&
                currentPageSentences.isNotEmpty()) {
                narrationInitialized = true
                val firstPlayable = currentPageSentences.firstOrNull { it !in state.value.missingAudio }
                if (firstPlayable != null) {
                    viewModel.playSentence(firstPlayable)
                } else {
                    viewModel.playSentence(currentPageSentences.first())
                }
            }
        }
        
        // 处理朗读逻辑
        val currentNarrationId = audioState.value.narrationSentenceId
        
        // 处理翻页后播放第一句（自动翻页或手动翻页都会触发）
        LaunchedEffect(state.value.needPlayFirstSentence, currentPageSentences) {
            if (state.value.needPlayFirstSentence && currentPageSentences.isNotEmpty()) {
                viewModel.clearPlayFirstSentence()
                viewModel.resetNarrationState()
                val firstPlayable = currentPageSentences.firstOrNull { it !in state.value.missingAudio }
                if (firstPlayable != null) {
                    viewModel.playSentence(firstPlayable)
                } else {
                    viewModel.playSentence(currentPageSentences.first())
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.value.isLoading || !pagesReady || currentPages.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("加载中...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                state.value.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "加载失败：${state.value.error}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                book != null && currentChapter != null -> {
                    // 第一章第一页时也可以向前翻（返回封面）
                    val isFirstPageOfBook = currentChapterIndex == 0 && currentPageIndex == 0
                    val canGoPrev = currentPageIndex > 0 || currentChapterIndex > 0 || isFirstPageOfBook
                    val canGoNext = currentPageIndex < pageCount - 1 || 
                                   currentChapterIndex < (book.chapters.size - 1)
                    
                    SwipeablePageContainer(
                        pageIndex = currentPageIndex,
                        pageCount = pageCount,
                        canGoPrev = canGoPrev,
                        canGoNext = canGoNext,
                        onPageChange = { delta -> 
                            // 在第一章第一页向前翻，返回封面
                            if (delta < 0 && isFirstPageOfBook) {
                                // 返回封面前暂停朗读（保持开关状态）
                                if (state.value.narrationEnabled) {
                                    viewModel.pauseNarration()
                                }
                                onBack()
                            } else {
                                // Dismiss tooltip on page turn
                                activeTooltipSentenceId = null
                                clickedSentenceId = null
                                viewModel.updatePage(delta, pageCount, prevChapterPages.size)
                            }
                        },
                        onCenterTap = {
                            if (suppressNextCenterTap) {
                                suppressNextCenterTap = false
                                return@SwipeablePageContainer
                            }
                            if (toolbarState.value.isVisible) {
                                hideToolbar()
                                showNarrationPanel.value = false
                                showFontSettings.value = false
                            } else {
                                updateToolbarInteraction()
                            }
                            // 点击任何位置都尝试关闭 Tooltip 和清除高亮
                            activeTooltipSentenceId = null
                            clickedSentenceId = null
                        },
                        onTopDoubleTap = {
                            updateToolbarInteraction()
                        },
                        onDragStart = {
                            activeTooltipSentenceId = null
                            clickedSentenceId = null
                        },
                        manualTapSignal = manualTapSignal
                    ) { pageIndexToRender ->
                        val (targetChapter, targetPage) =
                            getPageByIndex(
                                book = book,
                                currentChapterIndex = currentChapterIndex,
                                currentPages = currentPages,
                                prevChapterPages = prevChapterPages,
                                pageIndexToRender = pageIndexToRender,
                                nextPagesProvider = { id -> viewModel.getCachedPages(id) }
                            ) ?: return@SwipeablePageContainer
                        
                        PageContent(
                            chapter = targetChapter,
                            page = targetPage,
                            currentNarrationId = if (pageIndexToRender == currentPageIndex) currentNarrationId else null,
                            pageIndicatorText = run {
                                val globalStart = globalPagination?.chapterStartPageById?.get(targetChapter.id)
                                val globalPageIndex0 = if (globalStart != null) globalStart + targetPage.index else null
                                buildPageIndicatorText(
                                    globalPageIndex0 = globalPageIndex0,
                                    globalTotalPages = globalPagination?.totalPages
                                )
                            },
                            fontSizeLevel = fontSizeLevel,
                            textStyle = textStyle,
                            annotationStyle = annotationStyle,
                            textParagraphSpacingPx = textParagraphSpacingPx,
                            annotationSpacingPx = annotationSpacingPx,
                            pageEngine = viewModel.pageEngine,
                            activeTooltipSentenceId = activeTooltipSentenceId,
                            clickedSentenceId = clickedSentenceId,
                            onTooltipRequest = { id -> 
                                activeTooltipSentenceId = id
                                clickedSentenceId = id
                                suppressNextCenterTap = true
                            },
                            onSingleTap = { offset ->
                                manualTapSignal.tryEmit(offset)
                            },
                            onPlayNarration = { id, pageSentenceIds ->
                                activeTooltipSentenceId = null
                                clickedSentenceId = null
                                if (!state.value.narrationEnabled) {
                                    viewModel.toggleNarration(true)
                                }
                                viewModel.playSentence(id, pageSentenceIds)
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = toolbarState.value.isVisible,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopBar(
                    title = book?.title ?: "",
                    chapterTitle = currentChapter?.title ?: "",
                    onBack = {
                        updateToolbarInteraction()
                        if (state.value.narrationEnabled) {
                            viewModel.pauseNarration()
                        }
                        onBack()
                    }
                )
            }

            val playPauseIcon = if (audioState.value.narrationPlaying) {
                androidx.compose.material.icons.Icons.Default.Pause
            } else {
                androidx.compose.material.icons.Icons.Default.PlayArrow
            }
            val playPauseLabel = if (audioState.value.narrationPlaying) {
                "暂停"
            } else {
                "播放"
            }

            AnimatedVisibility(
                visible = toolbarState.value.isVisible && !showNarrationPanel.value && !showFontSettings.value,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderBottomBar(
                    onOpenToc = {
                        updateToolbarInteraction()
                        showToc.value = true
                    },
                    onPlayPause = {
                        updateToolbarInteraction()
                        if (!state.value.narrationEnabled) {
                            viewModel.toggleNarration(true)
                        }
                        if (audioState.value.narrationSentenceId == null) {
                            viewModel.playNextSentenceManual()
                        } else {
                            viewModel.pauseOrResumeSentence()
                        }
                    },
                    playPauseIcon = playPauseIcon,
                    playPauseLabel = playPauseLabel,
                    onOpenNarrationPanel = {
                        updateToolbarInteraction()
                        openNarrationPanel()
                    },
                    onOpenFontSettings = {
                        updateToolbarInteraction()
                        openFontSettings()
                    },
                    isNightMode = state.value.isNightMode,
                    onToggleNightMode = {
                        updateToolbarInteraction()
                        viewModel.toggleNightMode()
                    }
                )
            }

            if (showNarrationPanel.value) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { hideToolbar() },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.large
                ) {
                    NarrationPanelContent(
                        audioState = audioState.value,
                        narrationEnabled = state.value.narrationEnabled,
                        narrationTimerMinutes = state.value.narrationTimerMinutes,
                        narrationStopAtChapterEnd = state.value.narrationStopAtChapterEnd,
                        onPlayPause = {
                            updateToolbarInteraction()
                            if (!state.value.narrationEnabled) {
                                viewModel.toggleNarration(true)
                            }
                            if (audioState.value.narrationSentenceId == null) {
                                viewModel.playNextSentenceManual()
                            } else {
                                viewModel.pauseOrResumeSentence()
                            }
                        },
                        onPrev = {
                            updateToolbarInteraction()
                            viewModel.playPreviousSentenceManual()
                        },
                        onNext = {
                            updateToolbarInteraction()
                            viewModel.playNextSentenceManual()
                        },
                        onSpeedPreview = {
                            updateToolbarInteraction()
                            viewModel.previewNarrationSpeed(it)
                        },
                        onSpeedCommit = {
                            updateToolbarInteraction()
                            viewModel.setNarrationSpeed(it)
                        },
                        onTimerStart = {
                            updateToolbarInteraction()
                            viewModel.setNarrationStopAtChapterEnd(false)
                            viewModel.startNarrationTimer(it)
                        },
                        onStopAtChapterEnd = {
                            updateToolbarInteraction()
                            viewModel.clearNarrationTimer()
                            viewModel.setNarrationStopAtChapterEnd(true)
                        },
                        onTimerClear = {
                            updateToolbarInteraction()
                            viewModel.clearNarrationTimer()
                            viewModel.setNarrationStopAtChapterEnd(false)
                        },
                        onRetry = {
                            updateToolbarInteraction()
                            viewModel.retryLastSentence()
                        },
                        onDismiss = { hideToolbar() }
                    )
                }
            }

            if (showFontSettings.value) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { hideToolbar() },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.large
                ) {
                    FontSettingsPanelContent(
                        currentFontSize = state.value.fontSizeLevel,
                        onFontSizeChange = {
                            updateToolbarInteraction()
                            viewModel.setFontSize(it)
                        },
                        onDismiss = { hideToolbar() }
                    )
                }
            }




            
            if (showToc.value) {
                TocDialog(
                    titles = state.value.book?.chapters?.map { it.title } ?: emptyList(),
                    currentIndex = state.value.currentChapterIndex,
                    onSelect = {
                        viewModel.selectChapter(it)
                        showToc.value = false
                        hideToolbar()
                    },
                    onDismiss = { showToc.value = false }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
    }
    }
}



/**
 * 根据页码索引获取页面数据（支持跨章节预览）
 */
internal fun getPageByIndex(
    book: com.xuyutech.hongbaoshu.data.Book,
    currentChapterIndex: Int,
    currentPages: List<Page>,
    prevChapterPages: List<Page>,
    pageIndexToRender: Int,
    nextPagesProvider: (String) -> List<Page>?
): Pair<Chapter, Page>? {
    return when {
        // 上一章最后一页
        pageIndexToRender < 0 && currentChapterIndex > 0 -> {
            val prevChapter = book.chapters[currentChapterIndex - 1]
            val page = prevChapterPages.lastOrNull() ?: return null
            prevChapter to page
        }
        // 下一章第一页
        currentPages.isNotEmpty() &&
            pageIndexToRender >= currentPages.size &&
            currentChapterIndex < book.chapters.lastIndex -> {
            val nextChapter = book.chapters[currentChapterIndex + 1]
            val nextPages = nextPagesProvider(nextChapter.id) ?: return null
            val page = nextPages.firstOrNull() ?: return null
            nextChapter to page
        }
        // 当前章节内
        pageIndexToRender in currentPages.indices -> {
            val chapter = book.chapters[currentChapterIndex]
            val page = currentPages[pageIndexToRender]
            chapter to page
        }
        else -> null
    }
}

internal data class GlobalPaginationInfo(
    val chapterStartPageById: Map<String, Int>,
    val totalPages: Int
)

internal fun buildPageIndicatorText(
    globalPageIndex0: Int?,
    globalTotalPages: Int?
): String {
    if (globalPageIndex0 == null || globalTotalPages == null) return ""
    if (globalTotalPages <= 0 || globalPageIndex0 < 0) return ""
    return "${globalPageIndex0 + 1}/$globalTotalPages"
}

@Composable
private fun ReaderTopBar(
    title: String,
    chapterTitle: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold
                )
                if (chapterTitle.isNotEmpty()) {
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        },
        navigationIcon = {
            androidx.compose.material3.IconButton(onClick = onBack) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        windowInsets = WindowInsets.statusBars
    )
}

@Composable
private fun ReaderBottomBar(
    onOpenToc: () -> Unit,
    onPlayPause: () -> Unit,
    playPauseIcon: androidx.compose.ui.graphics.vector.ImageVector,
    playPauseLabel: String,
    onOpenNarrationPanel: () -> Unit,
    onOpenFontSettings: () -> Unit,
    isNightMode: Boolean,
    onToggleNightMode: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = false,
            onClick = onOpenToc,
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                    contentDescription = "目录"
                )
            },
            label = { Text("目录", style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onPlayPause,
            icon = { androidx.compose.material3.Icon(imageVector = playPauseIcon, contentDescription = playPauseLabel) },
            label = { Text(playPauseLabel, style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenNarrationPanel,
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Info,
                    contentDescription = "朗读"
                )
            },
            label = { Text("朗读", style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenFontSettings,
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                    contentDescription = "字体"
                )
            },
            label = { Text("字体", style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onToggleNightMode,
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = if (isNightMode) {
                        androidx.compose.material.icons.Icons.Filled.LightMode
                    } else {
                        androidx.compose.material.icons.Icons.Filled.DarkMode
                    },
                    contentDescription = if (isNightMode) "日间" else "夜间"
                )
            },
            label = { Text(if (isNightMode) "日间" else "夜间", style = MaterialTheme.typography.labelSmall) }
        )
    }
}

@Composable
private fun BottomBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FontSettingsPanel(
    currentFontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Content-only: ModalBottomSheet provides the outer container, shape, and background.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "字体大小",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.material3.IconButton(onClick = onDismiss) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = "关闭"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$currentFontSize",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Slider(
            value = currentFontSize.toFloat(),
            onValueChange = { onFontSizeChange(it.roundToInt()) },
            valueRange = com.xuyutech.hongbaoshu.reader.FONT_SIZE_MIN.toFloat()..com.xuyutech.hongbaoshu.reader.FONT_SIZE_MAX.toFloat(),
            steps = (com.xuyutech.hongbaoshu.reader.FONT_SIZE_MAX - com.xuyutech.hongbaoshu.reader.FONT_SIZE_MIN) / 2 - 1
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FontSettingsPanelContent(
    currentFontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    FontSettingsPanel(
        currentFontSize = currentFontSize,
        onFontSizeChange = onFontSizeChange,
        onDismiss = onDismiss
    )
}

@Composable
private fun NarrationPanel(
    audioState: com.xuyutech.hongbaoshu.audio.AudioState,
    narrationEnabled: Boolean,
    narrationTimerMinutes: Int?,
    narrationStopAtChapterEnd: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSpeedPreview: (Float) -> Unit,
    onSpeedCommit: (Float) -> Unit,
    onTimerStart: (Int) -> Unit,
    onStopAtChapterEnd: () -> Unit,
    onTimerClear: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    // Content-only: ModalBottomSheet provides the outer container, shape, and background.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "朗读",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val status = when {
                    !narrationEnabled -> "未开启"
                    audioState.narrationPlaying -> "正在播放"
                    audioState.narrationSentenceId != null -> "已暂停"
                    else -> "准备就绪"
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            androidx.compose.material3.IconButton(onClick = onDismiss) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = "关闭"
                )
            }
        }

        if (audioState.narrationError != null) {
            Text(
                text = audioState.narrationError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.FilledTonalIconButton(
                onClick = onPrev,
                modifier = Modifier.size(48.dp),
                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.SkipPrevious,
                    contentDescription = "上一句"
                )
            }

            androidx.compose.material3.FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = if (audioState.narrationPlaying) {
                        androidx.compose.material.icons.Icons.Default.Pause
                    } else {
                        androidx.compose.material.icons.Icons.Default.PlayArrow
                    },
                    contentDescription = "播放暂停",
                    modifier = Modifier.size(32.dp)
                )
            }

            androidx.compose.material3.FilledTonalIconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp),
                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.SkipNext,
                    contentDescription = "下一句"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "语速",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.2fx", audioState.narrationSpeed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val minSpeed = 0.75f
            val maxSpeed = 1.25f
            val step = 0.05f
            val steps = (((maxSpeed - minSpeed) / step).toInt() - 1).coerceAtLeast(0)
            var sliderValue by remember { mutableStateOf(audioState.narrationSpeed) }
            LaunchedEffect(audioState.narrationSpeed) {
                sliderValue = audioState.narrationSpeed
            }

            Slider(
                value = sliderValue.coerceIn(minSpeed, maxSpeed),
                onValueChange = { raw ->
                    val stepsFromMin = ((raw.coerceIn(minSpeed, maxSpeed) - minSpeed) / step).roundToInt()
                    val snapped = (minSpeed + stepsFromMin * step).coerceIn(minSpeed, maxSpeed)
                    sliderValue = snapped
                    onSpeedPreview(snapped)
                },
                onValueChangeFinished = {
                    onSpeedCommit(sliderValue.coerceIn(minSpeed, maxSpeed))
                },
                valueRange = minSpeed..maxSpeed,
                steps = steps
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "定时关闭",
                    style = MaterialTheme.typography.titleSmall
                )
                if (narrationTimerMinutes != null || narrationStopAtChapterEnd) {
                    TextButton(
                        onClick = onTimerClear,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("关闭定时")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val timerOptions = listOf(15, 30, 60)
                timerOptions.forEach { minutes ->
                    val selected = narrationTimerMinutes == minutes && !narrationStopAtChapterEnd
                    TimerChip(
                        label = "${minutes}分钟",
                        selected = selected,
                        onClick = { onTimerStart(minutes) },
                        modifier = Modifier.weight(1f)
                    )
                }

                val endChapterSelected = narrationStopAtChapterEnd
                TimerChip(
                    label = "读完本章",
                    selected = endChapterSelected,
                    onClick = onStopAtChapterEnd,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun NarrationPanelContent(
    audioState: com.xuyutech.hongbaoshu.audio.AudioState,
    narrationEnabled: Boolean,
    narrationTimerMinutes: Int?,
    narrationStopAtChapterEnd: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSpeedPreview: (Float) -> Unit,
    onSpeedCommit: (Float) -> Unit,
    onTimerStart: (Int) -> Unit,
    onStopAtChapterEnd: () -> Unit,
    onTimerClear: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    NarrationPanel(
        audioState = audioState,
        narrationEnabled = narrationEnabled,
        narrationTimerMinutes = narrationTimerMinutes,
        narrationStopAtChapterEnd = narrationStopAtChapterEnd,
        onPlayPause = onPlayPause,
        onPrev = onPrev,
        onNext = onNext,
        onSpeedPreview = onSpeedPreview,
        onSpeedCommit = onSpeedCommit,
        onTimerStart = onTimerStart,
        onStopAtChapterEnd = onStopAtChapterEnd,
        onTimerClear = onTimerClear,
        onRetry = onRetry,
        onDismiss = onDismiss
    )
}

@Composable
private fun TimerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}



@Composable
private fun TocDialog(
    titles: List<String>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "目录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            var query by remember { mutableStateOf("") }
            val normalizedQuery = query.trim()
            val filteredIndices = remember(titles, normalizedQuery) {
                if (normalizedQuery.isEmpty()) {
                    titles.indices.toList()
                } else {
                    titles.indices.filter { idx ->
                        titles[idx].contains(normalizedQuery, ignoreCase = true)
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索章节") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onSelect(currentIndex) }) {
                        Text("回到当前进度")
                    }
                    Text(
                        text = "当前第 ${currentIndex + 1} 章",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (filteredIndices.isEmpty()) {
                    Text(
                        text = "暂无匹配章节",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp)
                    ) {
                        items(filteredIndices) { idx ->
                            val isSelected = idx == currentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelect(idx) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(32.dp)
                                )
                                Text(
                                    text = titles[idx],
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("关闭") }
        }
    )
}

/**
 * 页面内容组件（基于 PageSlice 渲染）
 */
@Composable
private fun PageContent(
    chapter: Chapter,
    page: Page,
    currentNarrationId: String?,
    pageIndicatorText: String,
    fontSizeLevel: Int,
    textStyle: TextStyle,
    annotationStyle: TextStyle,
    textParagraphSpacingPx: Int,
    annotationSpacingPx: Int,
    pageEngine: PageEngine,
    activeTooltipSentenceId: String?,
    clickedSentenceId: String?,
    onTooltipRequest: (String) -> Unit,
    onSingleTap: (androidx.compose.ui.geometry.Offset) -> Unit,
    onPlayNarration: (String, List<String>) -> Unit
) {
    val density = LocalDensity.current
    val textParagraphSpacingDp = with(density) { textParagraphSpacingPx.toDp() }
    val annotationSpacingDp = with(density) { annotationSpacingPx.toDp() }
    
    // 构建段落 ID 到段落的映射
    val paragraphMap = remember(chapter.id) {
        chapter.paragraphs.associateBy { it.id }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // 正文区域布局：上部 10% 留白，中间 80% 容器，3% 缓冲区，底部 7% 留白
        // 使用 padding 来定位，而不是 fillMaxHeight + padding 组合
        // 上部 10% + 容器 80% + 缓冲 3% + 底部 7% = 100%
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(
                    top = LocalConfiguration.current.screenHeightDp.dp * 0.1f,   // 上部 10% 留白
                    bottom = LocalConfiguration.current.screenHeightDp.dp * 0.07f // 底部 7% 留白（3% 缓冲在容器内）
                )
        ) {
            // 章节标题只在第一页显示
            if (page.isFirstPage) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
            // 页面内容
            Column(modifier = Modifier.weight(1f)) {
                val pageSentenceIds = remember(chapter.id, page) {
                    pageEngine.getSentenceIds(page, chapter)
                }
                page.slices.forEach { slice ->
                    val para = paragraphMap[slice.paragraphId] ?: return@forEach
                    androidx.compose.runtime.key(slice.paragraphId, slice.startChar, slice.endChar) {
                        SliceContent(
                            slice = slice,
                            paragraph = para,
                            currentNarrationId = currentNarrationId,
                            textStyle = textStyle,
                            annotationStyle = annotationStyle,
                            textParagraphSpacingDp = textParagraphSpacingDp,
                            annotationSpacingDp = annotationSpacingDp,
                            pageEngine = pageEngine,
                            onTextTap = { _, id -> onTooltipRequest(id) },
                            onSingleTap = onSingleTap,
                            activeTooltipSentenceId = activeTooltipSentenceId,
                            clickedSentenceId = clickedSentenceId,
                            onPlayNarration = { id -> onPlayNarration(id, pageSentenceIds) }
                        )
                    }
                }
            }
        }

        if (pageIndicatorText.isNotEmpty()) {
            Text(
                text = pageIndicatorText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 6.dp)
            )
        }
    }
}

/**
 * 渲染单个 PageSlice
 */
// ... (existing helper methods if any)

/**
 * 提示框数据
 */
data class TooltipData(
    val isVisible: Boolean = false,
    val position: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    val sentenceId: String = "",
    val sentenceContent: String = "" // Optional, for debug
)

@Composable
private fun SliceContent(
    slice: PageSlice,
    paragraph: com.xuyutech.hongbaoshu.data.Paragraph,
    currentNarrationId: String?,
    textStyle: TextStyle,
    annotationStyle: TextStyle,
    textParagraphSpacingDp: androidx.compose.ui.unit.Dp,
    annotationSpacingDp: androidx.compose.ui.unit.Dp,
    pageEngine: PageEngine,
    onTextTap: (androidx.compose.ui.geometry.Offset, String) -> Unit,
    onSingleTap: (androidx.compose.ui.geometry.Offset) -> Unit,
    activeTooltipSentenceId: String?,
    clickedSentenceId: String?,
    onPlayNarration: (String) -> Unit
) {
    val viewConfiguration = LocalViewConfiguration.current
    val isText = slice.paragraphType == ParagraphType.text
    val spacing = if (slice.isLastSlice) {
        if (isText) textParagraphSpacingDp else annotationSpacingDp
    } else 0.dp
    
    if (isText) {
        // 获取句子范围映射
        val sentenceRanges = pageEngine.getSentenceRanges(slice.paragraphId)
        
        // 构建带高亮的文本（缩进通过 textIndent 样式实现，不要手动添加空格）
        val annotatedText = remember(slice, currentNarrationId, clickedSentenceId) {
            buildAnnotatedString {
                val sliceText = paragraph.content.substring(
                    slice.startChar.coerceIn(0, paragraph.content.length),
                    slice.endChar.coerceIn(0, paragraph.content.length)
                )
                
                // Determine which sentence to highlight (clicked takes precedence for visual feedback)
                val highlightSentenceId = clickedSentenceId ?: currentNarrationId
                
                if (highlightSentenceId != null && sentenceRanges != null) {
                    // 找到当前朗读/点击句子的范围
                    val sentenceIdx = paragraph.sentences.indexOfFirst { it.id == highlightSentenceId }
                    if (sentenceIdx >= 0 && sentenceIdx < sentenceRanges.size) {
                        val highlightRange = sentenceRanges[sentenceIdx]
                        // 计算高亮范围与当前片段的交集
                        val sliceRange = slice.startChar until slice.endChar
                        val intersectStart = maxOf(highlightRange.first, sliceRange.first) - slice.startChar
                        val intersectEnd = minOf(highlightRange.last + 1, sliceRange.last + 1) - slice.startChar
                        
                        if (intersectStart < intersectEnd && intersectStart >= 0 && intersectEnd <= sliceText.length) {
                            // 有交集，分段渲染
                            if (intersectStart > 0) {
                                append(sliceText.substring(0, intersectStart))
                            }
                            withStyle(SpanStyle(background = Color(0x40FFC107))) {
                                append(sliceText.substring(intersectStart, intersectEnd))
                            }
                            if (intersectEnd < sliceText.length) {
                                append(sliceText.substring(intersectEnd))
                            }
                        } else {
                            append(sliceText)
                        }
                    } else {
                        append(sliceText)
                    }
                } else {
                    append(sliceText)
                }
            }
        }
        
        val finalStyle = if (slice.isFirstSlice) {
            textStyle.copy(textIndent = TextIndent(firstLine = (textStyle.fontSize.value * 2).sp))
        } else {
            textStyle
        }
        

        
        Box {
            var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
            var localClickedCharOffset by remember { mutableStateOf<Int?>(null) }
            var localClickedSentenceId by remember { mutableStateOf<String?>(null) }
            var elementPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
            
            val handleSentencePress: (androidx.compose.ui.geometry.Offset) -> Unit = { pos ->
                textLayoutResult?.let { layoutResult ->
                    val offset = layoutResult.getOffsetForPosition(pos)
                    val paragraphCharIndex = slice.startChar + offset
                    if (sentenceRanges != null) {
                        val sentenceIdx = sentenceRanges.indexOfFirst { range ->
                            paragraphCharIndex in range
                        }
                        if (sentenceIdx >= 0 && sentenceIdx < paragraph.sentences.size) {
                            val sentence = paragraph.sentences[sentenceIdx]
                            onTextTap(pos, sentence.id)
                            localClickedCharOffset = offset
                            localClickedSentenceId = sentence.id
                        }
                    }
                }
            }

            Text(
                text = annotatedText,
                style = finalStyle,
                modifier = Modifier
                    .padding(bottom = spacing)
                    .onGloballyPositioned { elementPosition = it.positionInRoot() }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                handleSentencePress(offset)
                            },
                            onTap = { offset ->
                                onSingleTap(elementPosition + offset)
                            }
                        )
                    },
                onTextLayout = { textLayoutResult = it }
            )
            
            // Popup Logic - use character bounding box for accurate positioning
            if (activeTooltipSentenceId != null && 
                activeTooltipSentenceId == localClickedSentenceId && 
                localClickedCharOffset != null &&
                textLayoutResult != null) {
                
                // Get the bounding box of the clicked character
                val boundingBox = textLayoutResult!!.getBoundingBox(localClickedCharOffset!!)
                
                // Measure tooltip width (approximate)
                val tooltipWidth = 100.dp
                val tooltipWidthPx = with(LocalDensity.current) { tooltipWidth.toPx() }
                
                val tooltipHeightPx = with(LocalDensity.current) { 60.dp.toPx() }
                
                // Position tooltip: centered horizontally above the character
                // Since we are in a Box wrapping the Text, these coordinates are local to the Text
                val popupOffset = androidx.compose.ui.unit.IntOffset(
                    x = (boundingBox.left + boundingBox.width / 2 - tooltipWidthPx / 2).toInt(),
                    y = (boundingBox.top - tooltipHeightPx).toInt()
                )
                
                androidx.compose.ui.window.Popup(
                    alignment = Alignment.TopStart,
                    offset = popupOffset,
                    onDismissRequest = { /* Dismissal handled by parent */ }
                ) {
                    ReadingTooltip(
                        onClick = { onPlayNarration(localClickedSentenceId!!) }
                    )
                }
            }
        }

    } else {
        // annotation 类型
        val prefix = if (slice.isFirstSlice) "【注】" else ""
        val sliceText = paragraph.content.substring(
            slice.startChar.coerceIn(0, paragraph.content.length),
            slice.endChar.coerceIn(0, paragraph.content.length)
        )
        
        Text(
            text = "$prefix$sliceText",
            style = annotationStyle,
            modifier = Modifier.padding(bottom = spacing)
        )
    }
}


@Composable
private fun ReadingTooltip(
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "开始朗读",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}
