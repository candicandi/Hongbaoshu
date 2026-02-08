package com.xuyutech.hongbaoshu.pack.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
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
        Files.write(dir.toPath().resolve("text/book.json"), "{}".toByteArray(StandardCharsets.UTF_8))

        Files.createDirectories(dir.toPath().resolve("images"))
        Files.write(dir.toPath().resolve("images/cover.png"), "x".toByteArray(StandardCharsets.UTF_8))

        Files.createDirectories(dir.toPath().resolve("sound"))
        Files.write(dir.toPath().resolve("sound/page_flip.wav.ogg"), "x".toByteArray(StandardCharsets.UTF_8))

        Files.createDirectories(dir.toPath().resolve("audio/narration"))
        Files.write(dir.toPath().resolve("audio/narration/s1_hello.mp3"), "x".toByteArray(StandardCharsets.UTF_8))

        val result = PackInspector.inspectPackRoot(dir)
        assertTrue(result.isValid)
        assertTrue(result.hasCover)
        assertTrue(result.hasFlipSound)
        assertTrue(result.hasNarration)
    }

    @Test
    fun `inspectPackRoot supports cover path from manifest`() {
        val dir = Files.createTempDirectory("packtest").toFile()
        dir.deleteOnExit()

        Files.createDirectories(dir.toPath().resolve("text"))
        Files.write(dir.toPath().resolve("text/book.json"), "{}".toByteArray(StandardCharsets.UTF_8))

        val manifest = """
            {
              "formatVersion": 1,
              "packId": "p1",
              "packVersion": 1,
              "book": { "title": "t", "author": "a", "edition": "e" },
              "resources": {
                "text": { "path": "text/book.json" },
                "cover": { "path": "images\\custom_cover.jpg" }
              }
            }
        """.trimIndent()
        Files.write(dir.toPath().resolve("manifest.json"), manifest.toByteArray(StandardCharsets.UTF_8))

        Files.createDirectories(dir.toPath().resolve("images"))
        Files.write(dir.toPath().resolve("images/custom_cover.jpg"), "x".toByteArray(StandardCharsets.UTF_8))

        val result = PackInspector.inspectPackRoot(dir)
        assertTrue(result.hasCover)
    }

    @Test
    fun `inspectPackRoot resolves cover by basename when manifest path mismatches`() {
        val dir = Files.createTempDirectory("packtest").toFile()
        dir.deleteOnExit()

        Files.createDirectories(dir.toPath().resolve("text"))
        Files.write(dir.toPath().resolve("text/book.json"), "{}".toByteArray(StandardCharsets.UTF_8))

        val manifest = """
            {
              "formatVersion": 1,
              "packId": "p2",
              "packVersion": 1,
              "book": { "title": "t", "author": "a", "edition": "e" },
              "resources": {
                "text": { "path": "text/book.json" },
                "cover": { "path": "cover.jpg" }
              }
            }
        """.trimIndent()
        Files.write(dir.toPath().resolve("manifest.json"), manifest.toByteArray(StandardCharsets.UTF_8))

        Files.createDirectories(dir.toPath().resolve("images"))
        Files.write(dir.toPath().resolve("images/cover.jpg"), "x".toByteArray(StandardCharsets.UTF_8))

        val result = PackInspector.inspectPackRoot(dir)
        assertTrue(result.hasCover)
    }
}
