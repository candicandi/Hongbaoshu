package com.xuyutech.hongbaoshu.pack.storage

import android.content.Context
import java.io.File

class PackFileStore(
    private val context: Context
) {
    fun packDir(packId: String): File = File(File(context.filesDir, "packs"), packId)

    fun pageCacheDir(packId: String): File = File(File(context.cacheDir, "page_cache"), packId)

    fun deletePack(packId: String) {
        packDir(packId).deleteRecursively()
        pageCacheDir(packId).deleteRecursively()
    }
}

