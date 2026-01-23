package com.xuyutech.hongbaoshu.reader

import com.xuyutech.hongbaoshu.data.Book

/**
 * 字体大小：18-28，每 2 个字号一个档位
 * 默认 22
 */
const val FONT_SIZE_MIN = 18
const val FONT_SIZE_MAX = 28
const val FONT_SIZE_DEFAULT = 22  // 默认中间档

data class ReaderState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val missingAudio: Set<String> = emptySet(),
    val error: String? = null,
    val currentChapterIndex: Int = 0,
    val pageIndex: Int = 0,
    val toastMessage: String? = null,
    val fontSizeLevel: Int = FONT_SIZE_DEFAULT,  // 字体大小档位 0-4
    val needPlayNextSentence: Boolean = false,   // 需要播放下一句（朗读模式）
    val needPlayFirstSentence: Boolean = false,  // 需要播放当前页第一句（翻页后）
    val narrationEnabled: Boolean = false,       // 朗读模式是否开启
    val currentPageSentenceIds: List<String> = emptyList(),  // 当前页的句子 ID 列表
    val lastPlayedSentenceId: String? = null,    // 上一个播放完成的句子 ID（用于查找下一句）
    val narrationTimerMinutes: Int? = null,
    val narrationStopAtChapterEnd: Boolean = false,
    val isNightMode: Boolean = false,             // 夜间模式
    val hasShownMenuGuide: Boolean = false,       // 是否已显示过菜单引导（持久化）
    val isMenuGuideDismissedInSession: Boolean = false // 本次会话是否已关闭引导（临时）
)
