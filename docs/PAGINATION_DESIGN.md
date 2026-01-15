# 分页与页码系统设计文档

## 概述

本文档描述了阅读器的分页和页码系统的设计思路、数据结构、核心算法和关键代码。

## 设计目标

1. **精确分页**：使用 `TextMeasurer` 进行精确测量，按字符级别分页，不估算
2. **无溢出**：文字不能被容器边缘遮挡
3. **填满容器**：除章节最后一页外，每页都要填满（80% 容器 + 3% 缓冲区）
4. **全书页码**：显示全书页码，不显示章节内页码
5. **性能优化**：预计算所有字号的分页结果，切换字号时瞬间响应

---

## 页面布局

```
┌─────────────────────────┐
│      上部留白 10%        │
├─────────────────────────┤
│                         │
│      文字容器 80%        │  ← 分页计算使用的高度
│                         │
├─────────────────────────┤
│      缓冲区 3%           │  ← 容纳计算误差，不遮挡文字
├─────────────────────────┤
│      底部留白 7%         │  ← 包含页码显示区域
└─────────────────────────┘
```

**关键代码** (`ReaderScreen.kt`):
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .padding(
            top = screenHeightDp * 0.1f,    // 上部 10% 留白
            bottom = screenHeightDp * 0.07f  // 底部 7% 留白
        )
)
```

---

## 数据结构

### PageSlice - 页面内容片段

一个段落可能跨多页，`PageSlice` 记录该段落在某一页上显示的字符范围。

```kotlin
data class PageSlice(
    val paragraphId: String,        // 原始段落 ID
    val paragraphType: ParagraphType,
    val startChar: Int,             // 起始字符索引（包含）
    val endChar: Int,               // 结束字符索引（不包含）
    val isFirstSlice: Boolean,      // 是否是段落的第一个片段（决定缩进）
    val isLastSlice: Boolean        // 是否是段落的最后一个片段（决定段落间距）
)
```

### Page - 页面数据

```kotlin
data class Page(
    val index: Int,                 // 章节内页码（0-based）
    val slices: List<PageSlice>,    // 本页包含的内容片段
    val isFirstPage: Boolean,       // 是否是章节第一页（显示标题）
    val globalIndex: Int,           // 全书页码（1-based）
    val totalPages: Int             // 全书总页数
)
```

### PageConfig - 分页配置

```kotlin
data class PageConfig(
    val availableHeightPx: Int,     // 可用高度（像素）
    val availableWidthPx: Int,      // 可用宽度（像素）
    val titleHeightPx: Int,         // 章节标题高度
    val textStyle: TextStyle,       // 正文样式（含 fontSize, lineHeight）
    val annotationStyle: TextStyle, // 注释样式
    val textParagraphSpacingPx: Int,    // 正文段落间距
    val annotationSpacingPx: Int        // 注释段落间距
)
```

---

## 核心算法

### 分页算法 (`PageEngine.paginate`)

```
输入：章节内容、分页配置、TextMeasurer
输出：List<Page>

对于每个段落：
    while 段落还有未处理的字符：
        1. 计算剩余可用高度
        2. 如果剩余高度不足一行，换页
        3. 尝试放入整个剩余内容，如果能放下则放入
        4. 否则，二分查找找到能放入的最大字符数（严格小于剩余高度）
        5. 贪心扩展：在高度不变的情况下尽量多放字符（同一行内）
        6. 创建 PageSlice，记录字符范围
        7. 如果不是段落最后一个片段，换页
```

**关键代码**:
```kotlin
// 二分查找：找到能放入剩余空间的最大字符数
while (low <= high) {
    val mid = (low + high) / 2
    val testHeight = measureTextHeight(content.substring(startChar, mid), ...)
    
    // 严格小于，留出余量避免边界溢出
    if (testHeight < remainingHeight) {
        bestEnd = mid
        low = mid + 1
    } else {
        high = mid - 1
    }
}

// 贪心扩展：只在高度完全不变时扩展（同一行内的字符）
while (bestEnd < content.length) {
    val testHeight = measureTextHeight(content.substring(startChar, bestEnd + 1), ...)
    if (testHeight == baseHeight) {
        bestEnd++
    } else {
        break
    }
}
```

### 全书页码计算 (`ReaderViewModel.computeAllChapters`)

```
输入：TextMeasurer、配置构建函数、字号档位
输出：缓存所有章节的分页结果（带全书页码）

步骤：
1. 检查是否已计算过（第一章第一页的 totalPages > 0）
2. 第一遍：对所有章节调用 paginate()，统计总页数
3. 第二遍：为每页设置 globalIndex 和 totalPages，存入缓存
```

**关键代码**:
```kotlin
fun computeAllChapters(textMeasurer, buildConfig, fontSizeLevel) {
    // 检查是否已计算过
    val firstChapterPages = pageCache[firstChapterKey]
    if (firstChapterPages?.firstOrNull()?.totalPages > 0) return
    
    // 第一遍：计算分页，统计总页数
    val chapterPages = mutableListOf<List<Page>>()
    var totalPages = 0
    book.chapters.forEach { chapter ->
        val pages = pageEngine.paginate(chapter, config, textMeasurer)
        chapterPages.add(pages)
        totalPages += pages.size
    }
    
    // 第二遍：更新全书页码
    var globalIndex = 0
    book.chapters.forEachIndexed { idx, chapter ->
        val updatedPages = chapterPages[idx].map { page ->
            globalIndex++
            page.copy(globalIndex = globalIndex, totalPages = totalPages)
        }
        pageCache[key] = updatedPages
    }
}
```

---

## 缓存机制

### 缓存 Key 格式

```
"${chapterId}_${fontSizeLevel}_${widthPx}_${heightPx}"
```

- **chapterId**: 章节 ID
- **fontSizeLevel**: 字号档位（0-4）
- **widthPx**: 内容区域宽度（像素）
- **heightPx**: 内容区域高度（像素）

### 缓存策略

1. **首次加载**：同步计算当前字号的所有章节分页
2. **后台预计算**：异步计算其他字号档位的分页
3. **缓存失效**：屏幕尺寸变化时清除所有缓存

**关键代码**:
```kotlin
// 同步计算当前字号
val pagesReady = remember(book, fontSizeLevel, contentWidthPx, contentHeightPx) {
    if (book != null && contentWidthPx > 0 && contentHeightPx > 0) {
        viewModel.computeAllChapters(textMeasurer, ::buildPageConfig, fontSizeLevel)
        true
    } else false
}

// 后台预计算其他字号
LaunchedEffect(pagesReady) {
    if (pagesReady) {
        viewModel.startPrecompute(textMeasurer, ::buildPageConfig)
    }
}
```

---

## 页码显示

页码直接从 `Page` 对象读取，不做任何计算或回退：

```kotlin
val currentPage = currentPages.getOrNull(currentPageIndex)
val globalPageInfo = Pair(currentPage.globalIndex, currentPage.totalPages)

// 显示
Text("${globalPageInfo.first} / ${globalPageInfo.second}")
```

---

## 文件结构

| 文件 | 职责 |
|------|------|
| `PageEngine.kt` | 分页算法、数据结构定义 |
| `ReaderViewModel.kt` | 缓存管理、预计算调度 |
| `ReaderScreen.kt` | UI 渲染、分页触发 |

---

## 字号档位

| 档位 | 字体大小 | 行高 |
|------|----------|------|
| 0 | 18sp | 27sp (1.5x) |
| 1 | 20sp | 30sp |
| 2 (默认) | 22sp | 33sp |
| 3 | 24sp | 36sp |
| 4 | 26sp | 39sp |

---

## 注意事项

1. **TextMeasurer 依赖**：只能在 Compose 中获取，因此分页计算在 UI 层触发
2. **同步计算**：首次加载必须同步完成，否则页面无法显示
3. **严格小于**：二分查找使用 `<` 而非 `<=`，避免边界溢出
4. **贪心扩展**：只在高度不变时扩展，确保同一行字符不被拆分
5. **全书页码**：分页时一次性计算好，显示时直接读取，零计算开销
