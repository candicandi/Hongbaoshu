package com.xuyutech.hongbaoshu.data

import android.content.Context
import android.net.Uri

/**
 * Content loading contract.
 *
 * - Audio mapping uses sentenceId prefix match: files named `[sentenceId]_xxx.wav`.
 * - Implementations should report missing sentence audios to allow UI graceful fallback.
 */
interface ContentLoader {
    suspend fun loadBook(context: Context): BookLoadResult
    fun narrationUri(sentenceId: String): Uri?
    fun bgmPlaylist(): List<Uri>
    fun flipSound(): Uri?
    fun coverImage(): Uri?
}
