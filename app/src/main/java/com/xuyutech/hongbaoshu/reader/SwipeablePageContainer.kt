package com.xuyutech.hongbaoshu.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 支持左右滑动翻页的容器组件（翻书效果）
 * 使用三页缓存机制：prev, current, next
 */
@Composable
fun SwipeablePageContainer(
    pageIndex: Int,
    pageCount: Int,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPageChange: (Int) -> Unit,
    onCenterTap: (() -> Unit)? = null,  // 点击中间区域的回调（可选）
    onTopDoubleTap: (() -> Unit)? = null,  // 双击顶部区域的回调（打开菜单）
    content: @Composable (Int) -> Unit  // 接收页码，渲染对应页面
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val swipeThreshold = screenWidthPx * 0.15f
    
    var dragOffset by remember(pageIndex) { mutableFloatStateOf(0f) }
    val animatedOffset = remember(pageIndex) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // 是否正在拖动或动画中（用于判断是否渲染相邻页面）
    var isDragging by remember(pageIndex) { mutableStateOf(false) }
    // 点击翻页的方向：-1 上一页，1 下一页，0 无
    var tapDirection by remember(pageIndex) { mutableIntStateOf(0) }

    // 页码变化时重置偏移和拖动状态（已通过 remember(pageIndex) 处理）

    // 处理点击翻页动画
    LaunchedEffect(tapDirection) {
        if (tapDirection != 0) {
            val targetOffset = if (tapDirection < 0) screenWidthPx else -screenWidthPx
            animatedOffset.animateTo(targetOffset, tween(200))
            onPageChange(tapDirection)
            tapDirection = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF5EE))  // 底色，模拟纸张
            .pointerInput(pageIndex, canGoPrev, canGoNext, onCenterTap, onTopDoubleTap) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        // 双击顶部 20% 区域打开菜单
                        val topZone = size.height * 0.2f
                        if (offset.y <= topZone) {
                            onTopDoubleTap?.invoke()
                        }
                    },
                    onTap = { offset ->
                        val leftZone = size.width * 0.4f
                        val rightZone = size.width * 0.6f
                        when {
                            offset.x < leftZone && canGoPrev -> {
                                isDragging = true
                                tapDirection = -1
                            }
                            offset.x > rightZone && canGoNext -> {
                                isDragging = true
                                tapDirection = 1
                            }
                            else -> onCenterTap?.invoke()
                        }
                    }
                )
            }
            .pointerInput(pageIndex, canGoPrev, canGoNext) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        scope.launch {
                            when {
                                dragOffset > swipeThreshold && canGoPrev -> {
                                    animatedOffset.animateTo(screenWidthPx, tween(150))
                                    onPageChange(-1)
                                    // isDragging 由 LaunchedEffect(pageIndex) 重置
                                }
                                dragOffset < -swipeThreshold && canGoNext -> {
                                    animatedOffset.animateTo(-screenWidthPx, tween(150))
                                    onPageChange(1)
                                    // isDragging 由 LaunchedEffect(pageIndex) 重置
                                }
                                else -> {
                                    // 回弹，不翻页
                                    animatedOffset.animateTo(0f, tween(100))
                                    dragOffset = 0f
                                    isDragging = false
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            animatedOffset.animateTo(0f, tween(100))
                            dragOffset = 0f
                            isDragging = false
                        }
                    },
                    onHorizontalDrag = { _, amount ->
                        val newOffset = dragOffset + amount
                        dragOffset = when {
                            newOffset > 0 && !canGoPrev -> 0f
                            newOffset < 0 && !canGoNext -> 0f
                            else -> newOffset
                        }
                        scope.launch {
                            animatedOffset.snapTo(dragOffset)
                        }
                    }
                )
            }
    ) {
        val offset = animatedOffset.value
        
        // 渲染三页：上一页、当前页、下一页
        // 只在正在拖动时渲染相邻页面
        
        // 上一页（向右滑动或点击左侧翻页时显示）
        if (isDragging && (offset > 0 || tapDirection < 0) && canGoPrev && pageIndex > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            ) {
                content(pageIndex - 1)
            }
        }
        
        // 下一页（向左滑动或点击右侧翻页时显示）
        if (isDragging && (offset < 0 || tapDirection > 0) && canGoNext) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            ) {
                content(pageIndex + 1)
            }
        }
        
        // 当前页（最上层）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .offset { IntOffset(offset.roundToInt(), 0) }
                .shadow(
                    elevation = if (offset != 0f) 4.dp else 0.dp,
                    clip = false
                )
                .background(Color(0xFFFAF5EE))
        ) {
            content(pageIndex)
        }
    }
}
