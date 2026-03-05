package com.xuyutech.hongbaoshu.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivePackContentLoaderTest {
    @Test
    fun `uses builtin loader by default`() {
        val builtin = FakeContentLoader()
        val packLoader = FakeContentLoader()
        val loader = ActivePackContentLoader(builtin) { packId ->
            if (packId == "pack1") packLoader else error("unexpected packId")
        }

        loader.narrationUri("s1")

        assertEquals(listOf("s1"), builtin.requestedIds)
        assertEquals(emptyList<String>(), packLoader.requestedIds)
    }

    @Test
    fun `switches loader when active pack changes`() {
        val builtin = FakeContentLoader()
        val packLoader = FakeContentLoader()
        val loader = ActivePackContentLoader(builtin) { packId ->
            if (packId == "pack1") packLoader else error("unexpected packId")
        }

        loader.setActivePackId("pack1")

        loader.narrationUri("p1")

        assertEquals(listOf("p1"), packLoader.requestedIds)
        assertEquals(emptyList<String>(), builtin.requestedIds)
    }
}

private class FakeContentLoader(
) : ContentLoader {
    val requestedIds: MutableList<String> = mutableListOf()

    override suspend fun loadBook(context: Context): BookLoadResult {
        val book = Book(
            title = "t",
            author = "a",
            edition = "e",
            chapters = emptyList()
        )
        return BookLoadResult(book = book)
    }

    override fun narrationUri(sentenceId: String): android.net.Uri? {
        requestedIds.add(sentenceId)
        return null
    }

    override fun flipSound(): android.net.Uri? = null

    override fun coverImage(): android.net.Uri? = null
}
