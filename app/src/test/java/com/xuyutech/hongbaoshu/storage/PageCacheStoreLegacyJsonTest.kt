package com.xuyutech.hongbaoshu.storage

import com.xuyutech.hongbaoshu.data.ParagraphType
import com.xuyutech.hongbaoshu.reader.Page
import com.xuyutech.hongbaoshu.reader.PageSlice
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PageCacheStoreLegacyJsonTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `SerializablePage parses legacy json with global fields`() {
        val jsonStr = """
            {
              "index": 0,
              "slices": [
                {
                  "paragraphId": "p1",
                  "paragraphType": "text",
                  "startChar": 0,
                  "endChar": 2,
                  "isFirstSlice": true,
                  "isLastSlice": true
                }
              ],
              "isFirstPage": true,
              "globalIndex": 12,
              "totalPages": 100
            }
        """.trimIndent()

        val parsed = json.decodeFromString(SerializablePage.serializer(), jsonStr)
        val page = parsed.toPage()

        assertEquals(0, page.index)
        assertEquals(true, page.isFirstPage)
        assertEquals(1, page.slices.size)
        assertEquals("p1", page.slices[0].paragraphId)
        assertEquals(ParagraphType.text, page.slices[0].paragraphType)
        assertEquals(0, page.slices[0].startChar)
        assertEquals(2, page.slices[0].endChar)
        assertEquals(true, page.slices[0].isFirstSlice)
        assertEquals(true, page.slices[0].isLastSlice)
    }

    @Test
    fun `SerializablePage encoding omits removed global fields`() {
        val page = Page(
            index = 0,
            slices = listOf(
                PageSlice(
                    paragraphId = "p1",
                    paragraphType = ParagraphType.text,
                    startChar = 0,
                    endChar = 2,
                    isFirstSlice = true,
                    isLastSlice = true
                )
            ),
            isFirstPage = true
        )

        val encoded = json.encodeToString(page.toSerializable())
        assertFalse(encoded.contains("globalIndex"))
        assertFalse(encoded.contains("totalPages"))
    }
}

