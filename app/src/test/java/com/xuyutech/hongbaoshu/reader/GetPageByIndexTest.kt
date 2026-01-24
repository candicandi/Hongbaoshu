package com.xuyutech.hongbaoshu.reader

import com.xuyutech.hongbaoshu.data.Book
import com.xuyutech.hongbaoshu.data.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetPageByIndexTest {
    private fun bookWithChapters(vararg ids: String): Book {
        val chapters = ids.map { id ->
            Chapter(id = id, title = id, paragraphs = emptyList())
        }
        return Book(title = "t", author = "a", edition = "e", chapters = chapters)
    }

    @Test
    fun `returns null when current pages empty even if next chapter exists`() {
        val book = bookWithChapters("ch01", "ch02")
        val result = getPageByIndex(
            book = book,
            currentChapterIndex = 0,
            currentPages = emptyList(),
            prevChapterPages = emptyList(),
            pageIndexToRender = 0,
            nextPagesProvider = { _ -> listOf(Page(index = 0, slices = emptyList(), isFirstPage = true)) }
        )
        assertNull(result)
    }

    @Test
    fun `returns next chapter first page when rendering past last page`() {
        val book = bookWithChapters("ch01", "ch02")
        val currentPages = listOf(Page(index = 0, slices = emptyList(), isFirstPage = true))
        val nextPages = listOf(Page(index = 0, slices = emptyList(), isFirstPage = true))
        val result = getPageByIndex(
            book = book,
            currentChapterIndex = 0,
            currentPages = currentPages,
            prevChapterPages = emptyList(),
            pageIndexToRender = 1,
            nextPagesProvider = { _ -> nextPages }
        )
        assertEquals("ch02", result?.first?.id)
        assertEquals(0, result?.second?.index)
    }
}
