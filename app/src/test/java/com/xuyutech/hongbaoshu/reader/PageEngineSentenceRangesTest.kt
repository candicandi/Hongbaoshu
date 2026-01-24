package com.xuyutech.hongbaoshu.reader

import com.xuyutech.hongbaoshu.data.Chapter
import com.xuyutech.hongbaoshu.data.Paragraph
import com.xuyutech.hongbaoshu.data.ParagraphType
import com.xuyutech.hongbaoshu.data.Sentence
import org.junit.Assert.assertEquals
import org.junit.Test

class PageEngineSentenceRangesTest {
    @Test
    fun `buildSentenceRanges tolerates gaps between sentences`() {
        val chapter = Chapter(
            id = "c1",
            title = "t",
            paragraphs = listOf(
                Paragraph(
                    id = "p1",
                    type = ParagraphType.text,
                    content = "A。 B。",
                    sentences = listOf(
                        Sentence(id = "s1", content = "A。"),
                        Sentence(id = "s2", content = "B。")
                    )
                )
            )
        )

        val engine = PageEngine()
        engine.buildSentenceRanges(chapter)

        val ranges = engine.getSentenceRanges("p1")!!
        assertEquals(0 until 2, ranges[0])
        assertEquals(3 until 5, ranges[1])
    }

    @Test
    fun `buildSentenceRanges tolerates newline gaps between sentences`() {
        val chapter = Chapter(
            id = "c1",
            title = "t",
            paragraphs = listOf(
                Paragraph(
                    id = "p1",
                    type = ParagraphType.text,
                    content = "A。\nB。",
                    sentences = listOf(
                        Sentence(id = "s1", content = "A。"),
                        Sentence(id = "s2", content = "B。")
                    )
                )
            )
        )

        val engine = PageEngine()
        engine.buildSentenceRanges(chapter)

        val ranges = engine.getSentenceRanges("p1")!!
        assertEquals(0 until 2, ranges[0])
        assertEquals(3 until 5, ranges[1])
    }

    @Test
    fun `buildSentenceRanges keeps order when sentence content repeats`() {
        val chapter = Chapter(
            id = "c1",
            title = "t",
            paragraphs = listOf(
                Paragraph(
                    id = "p1",
                    type = ParagraphType.text,
                    content = "A。B。A。",
                    sentences = listOf(
                        Sentence(id = "s1", content = "A。"),
                        Sentence(id = "s2", content = "B。"),
                        Sentence(id = "s3", content = "A。")
                    )
                )
            )
        )

        val engine = PageEngine()
        engine.buildSentenceRanges(chapter)

        val ranges = engine.getSentenceRanges("p1")!!
        assertEquals(0 until 2, ranges[0])
        assertEquals(2 until 4, ranges[1])
        assertEquals(4 until 6, ranges[2])
    }

    @Test
    fun `getSentenceIds returns only sentences intersecting page slices`() {
        val chapter = Chapter(
            id = "c1",
            title = "t",
            paragraphs = listOf(
                Paragraph(
                    id = "p1",
                    type = ParagraphType.text,
                    content = "A。 B。C。",
                    sentences = listOf(
                        Sentence(id = "s1", content = "A。"),
                        Sentence(id = "s2", content = "B。"),
                        Sentence(id = "s3", content = "C。")
                    )
                )
            )
        )

        val engine = PageEngine()
        engine.buildSentenceRanges(chapter)

        val page1 = Page(
            index = 0,
            slices = listOf(
                PageSlice(
                    paragraphId = "p1",
                    paragraphType = ParagraphType.text,
                    startChar = 0,
                    endChar = 2,
                    isFirstSlice = true,
                    isLastSlice = false
                )
            )
        )
        assertEquals(listOf("s1"), engine.getSentenceIds(page1, chapter))

        val page2 = Page(
            index = 1,
            slices = listOf(
                PageSlice(
                    paragraphId = "p1",
                    paragraphType = ParagraphType.text,
                    startChar = 3,
                    endChar = 5,
                    isFirstSlice = false,
                    isLastSlice = false
                )
            )
        )
        assertEquals(listOf("s2"), engine.getSentenceIds(page2, chapter))

        val page3 = Page(
            index = 2,
            slices = listOf(
                PageSlice(
                    paragraphId = "p1",
                    paragraphType = ParagraphType.text,
                    startChar = 5,
                    endChar = 7,
                    isFirstSlice = false,
                    isLastSlice = true
                )
            )
        )
        assertEquals(listOf("s3"), engine.getSentenceIds(page3, chapter))
    }

    @Test
    fun `buildSentenceRanges refuses to guess when data is inconsistent`() {
        val chapter = Chapter(
            id = "c1",
            title = "t",
            paragraphs = listOf(
                Paragraph(
                    id = "p1",
                    type = ParagraphType.text,
                    content = "A。B。",
                    sentences = listOf(
                        Sentence(id = "s1", content = "A。")
                    )
                )
            )
        )

        val engine = PageEngine()
        engine.buildSentenceRanges(chapter)

        val ranges = engine.getSentenceRanges("p1")!!
        assertEquals(1, ranges.size)
        assertEquals(0 until 0, ranges[0])

        val page = Page(
            index = 0,
            slices = listOf(
                PageSlice(
                    paragraphId = "p1",
                    paragraphType = ParagraphType.text,
                    startChar = 0,
                    endChar = 4,
                    isFirstSlice = true,
                    isLastSlice = true
                )
            )
        )
        assertEquals(emptyList<String>(), engine.getSentenceIds(page, chapter))
    }
}
