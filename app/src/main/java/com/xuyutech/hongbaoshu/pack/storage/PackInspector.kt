package com.xuyutech.hongbaoshu.pack.storage

import java.io.File

object PackInspector {
    fun inspectPackRoot(root: File): PackInspection {
        val packExists = root.exists() && root.isDirectory

        val hasText = File(root, "text/book.json").exists()
        val hasCover = File(root, "images/cover.png").exists()
        val hasFlipSound = File(root, "sound/page_flip.wav.ogg").exists()

        val narrationDir = File(root, "audio/narration")
        val hasNarration = narrationDir.exists() &&
            narrationDir.isDirectory &&
            (narrationDir.list()?.isNotEmpty() == true)

        return PackInspection(
            packExists = packExists,
            hasText = hasText,
            hasCover = hasCover,
            hasFlipSound = hasFlipSound,
            hasNarration = hasNarration
        )
    }
}

