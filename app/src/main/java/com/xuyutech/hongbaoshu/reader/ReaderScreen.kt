package com.xuyutech.hongbaoshu.reader

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.xuyutech.hongbaoshu.audio.AudioManager
import com.xuyutech.hongbaoshu.data.Chapter
import com.xuyutech.hongbaoshu.data.ParagraphType

@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    audioManager: AudioManager,
    onBack: () -> Unit
) {
    val state = viewModel.state.observeAsState(ReaderState())
    val audioState = audioManager.state.collectAsState()
    val showToc = remember { mutableStateOf(false) }
    val showMenu = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 显示 Toast 消息
    LaunchedEffect(state.value.toastMessage) {
        state.value.toastMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearToast()
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
        val fontSizeSp = (18 + fontSizeLevel * 2).sp
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
            val fSizeSp = (18 + fontLevel * 2).sp
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
        
        // 异步计算全书分页
        LaunchedEffect(book, fontSizeLevel, contentWidthPx, contentHeightPx) {
            if (book != null && contentWidthPx > 0 && contentHeightPx > 0) {
                pagesReady = false
                // 在后台线程计算全书分页
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    viewModel.computeAllChapters(textMeasurer, ::buildPageConfig, fontSizeLevel)
                }
                pagesReady = true
                // 继续预计算其他字号
                viewModel.startPrecompute(textMeasurer, ::buildPageConfig)
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
        
        // 校验页码
        LaunchedEffect(currentPageIndex, state.value.pageIndex) {
            if (state.value.pageIndex != currentPageIndex && pageCount > 0) {
                viewModel.setPageIndex(currentPageIndex)
            }
        }
        
        // 直接从 Page 中读取全书页码（分页时已计算好）
        val currentPage = currentPages.getOrNull(currentPageIndex)
        val globalPageInfo = if (currentPage != null && currentPage.totalPages > 0) {
            Pair(currentPage.globalIndex, currentPage.totalPages)
        } else {
            // 分页未完成时显示占位
            Pair(0, 0)
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
                viewModel.playSentence(currentPageSentences.first())
            }
        }
        
        // 处理朗读逻辑
        val currentNarrationId = audioState.value.narrationSentenceId
        
        // 处理翻页后播放第一句（自动翻页或手动翻页都会触发）
        LaunchedEffect(state.value.needPlayFirstSentence, currentPageSentences) {
            if (state.value.needPlayFirstSentence && currentPageSentences.isNotEmpty()) {
                viewModel.clearPlayFirstSentence()
                viewModel.resetNarrationState()
                viewModel.playSentence(currentPageSentences.first())
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.value.isLoading -> {
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
                book != null && currentChapter != null && pagesReady -> {
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
                                viewModel.updatePage(delta, pageCount, prevChapterPages.size)
                            }
                        },
                        onCenterTap = {
                            // 菜单打开时，点击中间区域关闭菜单
                            if (showMenu.value) {
                                showMenu.value = false
                            }
                        },
                        onTopDoubleTap = {
                            if (!showMenu.value) {
                                showMenu.value = true
                                viewModel.dismissMenuGuide()
                            }
                        }
                    ) { pageIndexToRender ->
                        val (targetChapter, targetPage, targetGlobalPage, targetTotalPages) = 
                            getPageByIndex(
                                book = book,
                                currentChapterIndex = currentChapterIndex,
                                currentPages = currentPages,
                                prevChapterPages = prevChapterPages,
                                pageIndexToRender = pageIndexToRender,
                                viewModel = viewModel,
                                pageConfig = pageConfig,
                                textMeasurer = textMeasurer
                            ) ?: return@SwipeablePageContainer
                        
                        PageContent(
                            chapter = targetChapter,
                            page = targetPage,
                            globalPageInfo = Pair(targetGlobalPage, targetTotalPages),
                            currentNarrationId = if (pageIndexToRender == currentPageIndex) currentNarrationId else null,
                            fontSizeLevel = fontSizeLevel,
                            textStyle = textStyle,
                            annotationStyle = annotationStyle,
                            textParagraphSpacingPx = textParagraphSpacingPx,
                            annotationSpacingPx = annotationSpacingPx,
                            pageEngine = viewModel.pageEngine
                        )
                    }
                }
            }



            // 用户引导层
            if (!state.value.hasShownMenuGuide && 
                state.value.currentChapterIndex == 0 && 
                state.value.pageIndex == 0 &&
                !state.value.isLoading &&
                state.value.error == null
            ) {
                MenuGuideOverlay(
                    onDismiss = { viewModel.dismissMenuGuide() }
                )
            }
            
            // 下拉菜单（带遮罩层）
            if (showMenu.value) {
                // 遮罩层：点击或滑动菜单以外区域关闭菜单
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { showMenu.value = false }
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, _ -> showMenu.value = false }
                        }
                )
            }
            
            AnimatedVisibility(
                visible = showMenu.value,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it }
            ) {
                MenuPanel(
                    audioState = audioState.value,
                    onBgmToggle = { viewModel.playBgm(it) },
                    onBgmNext = { viewModel.nextBgm() },
                    onBgmVolumeChange = { viewModel.setBgmVolume(it) },
                    onShowToc = { 
                        showToc.value = true 
                        // 如果在引导显示时打开了目录（也是通过菜单打开的），也应该关闭引导
                        viewModel.dismissMenuGuide()
                    },
                    narrationEnabled = state.value.narrationEnabled,
                    onNarrationToggle = { enabled ->
                        viewModel.toggleNarration(enabled)
                        // 开启朗读时播放当前页第一句
                        // 使用 state 中的句子列表，确保数据已同步
                        if (enabled) {
                            val sentences = state.value.currentPageSentenceIds.ifEmpty { currentPageSentences }
                            if (sentences.isNotEmpty()) {
                                viewModel.playSentence(sentences.first())
                            }
                        }
                    },
                    fontSizeLevel = state.value.fontSizeLevel,
                    onFontSizeChange = { level -> viewModel.setFontSize(level) },
                    onDismiss = { showMenu.value = false },
                    onToggleNightMode = { viewModel.toggleNightMode() },
                    isNightMode = state.value.isNightMode
                )
            }

            if (showToc.value) {
                TocDialog(
                    titles = state.value.book?.chapters?.map { it.title } ?: emptyList(),
                    currentIndex = state.value.currentChapterIndex,
                    onSelect = {
                        viewModel.selectChapter(it)
                        showToc.value = false
                        showMenu.value = false
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
 * 用户引导层
 */
/**
 * 用户引导层 (Ripple Animation)
 */
@Composable
private fun MenuGuideOverlay(
    onDismiss: () -> Unit
) {
    // 动画状态
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "GuidePulse")
    
    // 两个波纹动画，错开播放，形成连贯感
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "Scale1"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "Alpha1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2000, delayMillis = 1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "Scale2"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2000, delayMillis = 1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "Alpha2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 渐变背景，顶部深色，底部透明，不再全屏遮挡
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent
                    ),
                    endY = 600f // 只遮挡顶部一部分
                )
            )
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp), // 避开状态栏，定位到顶部交互区
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 波纹动画区域
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                // 波纹 1
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale1)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha1), androidx.compose.foundation.shape.CircleShape)
                )
                // 波纹 2
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale2)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha2), androidx.compose.foundation.shape.CircleShape)
                )
                
                // 中心手势图标
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                        .padding(6.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 提示卡片
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                   Text(
                        text = androidx.compose.ui.res.stringResource(id = com.xuyutech.hongbaoshu.R.string.guide_double_tap_menu),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


/**
 * 根据页码索引获取页面数据（支持跨章节预览）
 * 返回：(章节, 页面, 全书页码, 全书总页数)
 */
private fun getPageByIndex(
    book: com.xuyutech.hongbaoshu.data.Book,
    currentChapterIndex: Int,
    currentPages: List<Page>,
    prevChapterPages: List<Page>,
    pageIndexToRender: Int,
    viewModel: ReaderViewModel,
    pageConfig: PageConfig,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
): Tuple4<Chapter, Page, Int, Int>? {
    return when {
        // 上一章最后一页
        pageIndexToRender < 0 && currentChapterIndex > 0 -> {
            val prevChapter = book.chapters[currentChapterIndex - 1]
            val page = prevChapterPages.lastOrNull() ?: return null
            // 直接使用 page 中的全书页码
            Tuple4(prevChapter, page, page.globalIndex, page.totalPages)
        }
        // 下一章第一页
        pageIndexToRender >= currentPages.size && currentChapterIndex < book.chapters.lastIndex -> {
            val nextChapter = book.chapters[currentChapterIndex + 1]
            val nextPages = viewModel.getCachedPages(nextChapter.id) ?: return null
            val page = nextPages.firstOrNull() ?: return null
            // 直接使用 page 中的全书页码
            Tuple4(nextChapter, page, page.globalIndex, page.totalPages)
        }
        // 当前章节内
        pageIndexToRender in currentPages.indices -> {
            val chapter = book.chapters[currentChapterIndex]
            val page = currentPages[pageIndexToRender]
            // 直接使用 page 中的全书页码
            Tuple4(chapter, page, page.globalIndex, page.totalPages)
        }
        else -> null
    }
}



@Composable
private fun MenuPanel(
    audioState: com.xuyutech.hongbaoshu.audio.AudioState,
    onBgmToggle: (Boolean) -> Unit,
    onBgmNext: () -> Unit,
    onBgmVolumeChange: (Float) -> Unit,
    onShowToc: () -> Unit,
    narrationEnabled: Boolean,
    onNarrationToggle: (Boolean) -> Unit,
    fontSizeLevel: Int,
    onFontSizeChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onToggleNightMode: () -> Unit,
    isNightMode: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -20) onDismiss()
                }
            }
            .padding(16.dp)
            .padding(top = 8.dp) // Extra clear space for status bar
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }

        // Quick Actions Row (TOC + Night Mode)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "阅读设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            androidx.compose.material3.OutlinedButton(
                onClick = onShowToc,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                    contentDescription = "目录",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("目录")
            }
        }

        // Audio Control Card
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Section Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "听书 & 音效",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Narration Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("朗读模式", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "自动播放章节内容",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = narrationEnabled,
                        onCheckedChange = onNarrationToggle
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // BGM Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("背景白噪音", style = MaterialTheme.typography.bodyLarge)
                        if (audioState.bgmEnabled) {
                            Text(
                                "正在播放",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = audioState.bgmEnabled,
                            onCheckedChange = onBgmToggle
                        )
                        if (audioState.bgmEnabled) {
                            Spacer(modifier = Modifier.width(12.dp))
                            androidx.compose.material3.FilledTonalIconButton(
                                onClick = onBgmNext,
                                modifier = Modifier.size(36.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowForward,
                                    contentDescription = "下一首",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // BGM Volume Slider
                if (audioState.bgmEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Settings, // Should change to Volume icon but standard set limitations
                            contentDescription = "Vol",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = audioState.bgmVolume,
                            onValueChange = onBgmVolumeChange,
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }

        // Appearance Card
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "显示设置",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Night Mode Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("夜间模式", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "启用深色主题",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        modifier = Modifier.semantics { contentDescription = "夜间模式开关" },
                        checked = isNightMode,
                        onCheckedChange = { onToggleNightMode() }
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                // Font Size Slider
                var sliderValue by remember(fontSizeLevel) { androidx.compose.runtime.mutableFloatStateOf(fontSizeLevel.toFloat()) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "小",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { onFontSizeChange(sliderValue.toInt()) },
                        valueRange = 0f..4f,
                        steps = 3,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    Text(
                        "大",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
            LazyColumn {
                items(titles.indices.toList()) { idx ->
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
    globalPageInfo: Pair<Int, Int>,
    currentNarrationId: String?,
    fontSizeLevel: Int,
    textStyle: TextStyle,
    annotationStyle: TextStyle,
    textParagraphSpacingPx: Int,
    annotationSpacingPx: Int,
    pageEngine: PageEngine
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
                page.slices.forEach { slice ->
                    val para = paragraphMap[slice.paragraphId] ?: return@forEach
                    SliceContent(
                        slice = slice,
                        paragraph = para,
                        currentNarrationId = currentNarrationId,
                        textStyle = textStyle,
                        annotationStyle = annotationStyle,
                        textParagraphSpacingDp = textParagraphSpacingDp,
                        annotationSpacingDp = annotationSpacingDp,
                        pageEngine = pageEngine
                    )
                }
            }
        }
        // 页码指示
        Text(
            text = "${globalPageInfo.first} / ${globalPageInfo.second}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 渲染单个 PageSlice
 */
@Composable
private fun SliceContent(
    slice: PageSlice,
    paragraph: com.xuyutech.hongbaoshu.data.Paragraph,
    currentNarrationId: String?,
    textStyle: TextStyle,
    annotationStyle: TextStyle,
    textParagraphSpacingDp: androidx.compose.ui.unit.Dp,
    annotationSpacingDp: androidx.compose.ui.unit.Dp,
    pageEngine: PageEngine
) {
    val isText = slice.paragraphType == ParagraphType.text
    val spacing = if (slice.isLastSlice) {
        if (isText) textParagraphSpacingDp else annotationSpacingDp
    } else 0.dp
    
    if (isText) {
        // 获取句子范围映射
        val sentenceRanges = pageEngine.getSentenceRanges(slice.paragraphId)
        
        // 构建带高亮的文本（缩进通过 textIndent 样式实现，不要手动添加空格）
        val annotatedText = remember(slice, currentNarrationId) {
            buildAnnotatedString {
                val sliceText = paragraph.content.substring(
                    slice.startChar.coerceIn(0, paragraph.content.length),
                    slice.endChar.coerceIn(0, paragraph.content.length)
                )
                
                if (currentNarrationId != null && sentenceRanges != null) {
                    // 找到当前朗读句子的范围
                    val sentenceIdx = paragraph.sentences.indexOfFirst { it.id == currentNarrationId }
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
        
        Text(
            text = annotatedText,
            style = finalStyle,
            modifier = Modifier.padding(bottom = spacing)
        )
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
