package com.xuyutech.hongbaoshu.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TocNarrationSwitchPlanTest {

    @Test
    fun `pending restart triggers needPlayFirstSentence when sentence list changes and not empty`() {
        val current = ReaderState(
            isLoading = false,
            narrationEnabled = true,
            currentPageSentenceIds = listOf("old"),
            needPlayFirstSentence = false
        )

        val plan = planSentenceIdsUpdate(
            current = current,
            sentenceIds = listOf("new1", "new2"),
            pendingNarrationRestart = true
        )

        assertEquals(listOf("new1", "new2"), plan.newState.currentPageSentenceIds)
        assertTrue(plan.newState.needPlayFirstSentence)
        assertTrue(plan.consumePendingRestart)
        assertTrue(plan.clearManualPageTurn)
    }

    @Test
    fun `pending restart is consumed even if new sentence list is empty`() {
        val current = ReaderState(
            isLoading = false,
            narrationEnabled = true,
            currentPageSentenceIds = listOf("old"),
            needPlayFirstSentence = false
        )

        val plan = planSentenceIdsUpdate(
            current = current,
            sentenceIds = emptyList(),
            pendingNarrationRestart = true
        )

        assertEquals(emptyList<String>(), plan.newState.currentPageSentenceIds)
        assertFalse(plan.newState.needPlayFirstSentence)
        assertTrue(plan.consumePendingRestart)
        assertTrue(plan.clearManualPageTurn)
    }

    @Test
    fun `pending restart is not consumed when sentence list does not change`() {
        val current = ReaderState(
            isLoading = false,
            narrationEnabled = true,
            currentPageSentenceIds = listOf("same"),
            needPlayFirstSentence = false
        )

        val plan = planSentenceIdsUpdate(
            current = current,
            sentenceIds = listOf("same"),
            pendingNarrationRestart = true
        )

        assertFalse(plan.consumePendingRestart)
        assertFalse(plan.clearManualPageTurn)
    }
}
