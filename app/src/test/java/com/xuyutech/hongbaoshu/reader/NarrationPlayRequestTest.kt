package com.xuyutech.hongbaoshu.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class NarrationPlayRequestTest {
    @Test
    fun `falls back to single sentence when id not in current list`() {
        val request = resolveNarrationPlayRequest(
            requestedSentenceId = "s2",
            currentPageSentenceIds = listOf("s1"),
            overrideSentenceIds = null
        )
        assertEquals(listOf("s2"), request.sentenceIds)
        assertEquals(0, request.startIndex)
    }

    @Test
    fun `uses current list when it contains requested id`() {
        val request = resolveNarrationPlayRequest(
            requestedSentenceId = "s2",
            currentPageSentenceIds = listOf("s1", "s2", "s3"),
            overrideSentenceIds = null
        )
        assertEquals(listOf("s1", "s2", "s3"), request.sentenceIds)
        assertEquals(1, request.startIndex)
    }

    @Test
    fun `override list wins and provides start index`() {
        val request = resolveNarrationPlayRequest(
            requestedSentenceId = "s2",
            currentPageSentenceIds = listOf("old1", "old2"),
            overrideSentenceIds = listOf("s1", "s2", "s3")
        )
        assertEquals(listOf("s1", "s2", "s3"), request.sentenceIds)
        assertEquals(1, request.startIndex)
    }

    @Test
    fun `override list falls back to single sentence when missing id`() {
        val request = resolveNarrationPlayRequest(
            requestedSentenceId = "s2",
            currentPageSentenceIds = listOf("s2"),
            overrideSentenceIds = listOf("s1", "s3")
        )
        assertEquals(listOf("s2"), request.sentenceIds)
        assertEquals(0, request.startIndex)
    }

    @Test
    fun `auto turn picks first sentence when not cross page`() {
        val next = pickAutoTurnSentenceToPlay(
            sentenceIds = listOf("s1", "s2"),
            lastCompletedSentenceId = "s0"
        )
        assertEquals("s1", next)
    }

    @Test
    fun `auto turn skips duplicated first sentence on cross page`() {
        val next = pickAutoTurnSentenceToPlay(
            sentenceIds = listOf("s1", "s2", "s3"),
            lastCompletedSentenceId = "s1"
        )
        assertEquals("s2", next)
    }

    @Test
    fun `auto turn returns null when page has no sentences`() {
        val next = pickAutoTurnSentenceToPlay(
            sentenceIds = emptyList(),
            lastCompletedSentenceId = "s1"
        )
        assertEquals(null, next)
    }
}
