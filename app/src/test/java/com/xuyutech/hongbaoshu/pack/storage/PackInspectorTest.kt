package com.xuyutech.hongbaoshu.pack.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class PackInspectorTest {

    @Test
    fun `inspectPackRoot returns invalid when missing text`() {
        val dir = Files.createTempDirectory("packtest").toFile()
        dir.deleteOnExit()

        val result = PackInspector.inspectPackRoot(dir)
        assertTrue(result.packExists)
        assertFalse(result.hasText)
        assertFalse(result.isValid)
    }

    @Test
    fun `inspectPackRoot detects optional resources`() {
        val dir = Files.createTempDirectory("packtest").toFile()
        dir.deleteOnExit()

        Files.createDirectories(dir.toPath().resolve("text"))
        Files.writeString(dir.toPath().resolve("text/book.json"), "{}")

        Files.createDirectories(dir.toPath().resolve("images"))
        Files.writeString(dir.toPath().resolve("images/cover.png"), "x")

        Files.createDirectories(dir.toPath().resolve("sound"))
        Files.writeString(dir.toPath().resolve("sound/page_flip.wav.ogg"), "x")

        Files.createDirectories(dir.toPath().resolve("audio/narration"))
        Files.writeString(dir.toPath().resolve("audio/narration/s1_hello.mp3"), "x")

        val result = PackInspector.inspectPackRoot(dir)
        assertTrue(result.isValid)
        assertTrue(result.hasCover)
        assertTrue(result.hasFlipSound)
        assertTrue(result.hasNarration)
    }
}

