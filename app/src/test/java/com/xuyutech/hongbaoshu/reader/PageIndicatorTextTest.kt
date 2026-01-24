package com.xuyutech.hongbaoshu.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class PageIndicatorTextTest {
    @Test
    fun `hides when global pages not ready`() {
        val text = buildPageIndicatorText(
            globalPageIndex0 = null,
            globalTotalPages = null
        )
        assertEquals("", text)
    }

    @Test
    fun `uses global page when global pages ready`() {
        val text = buildPageIndicatorText(
            globalPageIndex0 = 12,
            globalTotalPages = 200
        )
        assertEquals("13/200", text)
    }

    @Test
    fun `returns empty when global total invalid`() {
        val text = buildPageIndicatorText(
            globalPageIndex0 = 0,
            globalTotalPages = 0
        )
        assertEquals("", text)
    }
}
