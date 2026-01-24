package com.xuyutech.hongbaoshu.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageCacheCompletenessTest {
    @Test
    fun `treats book as complete only when each chapter cached for font`() {
        val chapterIds = listOf("ch01", "ch02", "ch03")
        val widthPx = 1080
        val heightPx = 1920

        val oldFont = 16
        val newFont = 18

        val cacheKeys = buildSet {
            chapterIds.forEach { id ->
                add("${id}_${oldFont}_${widthPx}_${heightPx}")
            }
            add("${chapterIds.first()}_${newFont}_${widthPx}_${heightPx}")
        }

        val oldComplete = areAllChaptersCachedForFontSize(
            chapterIds = chapterIds,
            fontSizeLevel = oldFont,
            widthPx = widthPx,
            heightPx = heightPx,
            pageCacheKeys = cacheKeys
        )
        assertTrue(oldComplete)

        val newComplete = areAllChaptersCachedForFontSize(
            chapterIds = chapterIds,
            fontSizeLevel = newFont,
            widthPx = widthPx,
            heightPx = heightPx,
            pageCacheKeys = cacheKeys
        )
        assertFalse(newComplete)
    }
}

