package com.xuyutech.hongbaoshu.pack.index

import com.xuyutech.hongbaoshu.pack.model.PackIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackIndexCodecTest {

    @Test
    fun `decode null returns empty list`() {
        assertTrue(PackIndexCodec.decode(null).isEmpty())
    }

    @Test
    fun `encode then decode roundtrips`() {
        val now = 123456L
        val items = listOf(
            PackIndex(
                packId = "builtin",
                bookTitle = "t",
                bookAuthor = "a",
                importedAt = now,
                hasNarration = false
            )
        )
        val raw = PackIndexCodec.encode(items)
        val decoded = PackIndexCodec.decode(raw)
        assertEquals(items, decoded)
    }
}

