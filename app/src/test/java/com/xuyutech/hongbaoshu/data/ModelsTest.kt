package com.xuyutech.hongbaoshu.data

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parse sentence from json`() {
        val jsonStr = """{"id": "s001", "content": "测试句子"}"""
        val sentence = json.decodeFromString(Sentence.serializer(), jsonStr)
        assertEquals("s001", sentence.id)
        assertEquals("测试句子", sentence.content)
    }

    @Test
    fun `parse paragraph with sentences`() {
        val jsonStr = """
        {
            "id": "p001",
            "type": "text",
            "content": "段落内容",
            "sentences": [
                {"id": "s001", "content": "句子1"},
                {"id": "s002", "content": "句子2"}
            ]
        }
        """.trimIndent()
        val paragraph = json.decodeFromString(Paragraph.serializer(), jsonStr)
        assertEquals("p001", paragraph.id)
        assertEquals(ParagraphType.text, paragraph.type)
        assertEquals(2, paragraph.sentences.size)
        assertEquals("s001", paragraph.sentences[0].id)
    }

    @Test
    fun `parse annotation paragraph`() {
        val jsonStr = """
        {
            "id": "p002",
            "type": "annotation",
            "content": "注释内容",
            "ref": "p001"
        }
        """.trimIndent()
        val paragraph = json.decodeFromString(Paragraph.serializer(), jsonStr)
        assertEquals(ParagraphType.annotation, paragraph.type)
        assertEquals("p001", paragraph.ref)
        assertTrue(paragraph.sentences.isEmpty())
    }

    @Test
    fun `parse chapter`() {
        val jsonStr = """
        {
            "id": "ch01",
            "title": "第一章",
            "paragraphs": [
                {"id": "p001", "type": "text", "content": "内容", "sentences": []}
            ]
        }
        """.trimIndent()
        val chapter = json.decodeFromString(Chapter.serializer(), jsonStr)
        assertEquals("ch01", chapter.id)
        assertEquals("第一章", chapter.title)
        assertEquals(1, chapter.paragraphs.size)
    }

    @Test
    fun `parse book json`() {
        val jsonStr = """
        {
            "book": {
                "title": "测试书籍",
                "author": "作者",
                "edition": "1966"
            },
            "chapters": []
        }
        """.trimIndent()
        val bookJson = json.decodeFromString(BookJson.serializer(), jsonStr)
        val book = Book.fromJson(bookJson)
        assertEquals("测试书籍", book.title)
        assertEquals("作者", book.author)
        assertEquals("1966", book.edition)
        assertTrue(book.chapters.isEmpty())
    }
}
