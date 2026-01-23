package com.xuyutech.hongbaoshu.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class NarrationSpeedClampTest {
    @Test
    fun `coerceNarrationSpeed clamps to range`() {
        assertEquals(0.75f, coerceNarrationSpeed(0.1f), 0.0001f)
        assertEquals(1.25f, coerceNarrationSpeed(3.0f), 0.0001f)
    }

    @Test
    fun `coerceNarrationSpeed keeps values within range`() {
        assertEquals(0.75f, coerceNarrationSpeed(0.75f), 0.0001f)
        assertEquals(1.0f, coerceNarrationSpeed(1.0f), 0.0001f)
        assertEquals(1.25f, coerceNarrationSpeed(1.25f), 0.0001f)
    }
}
