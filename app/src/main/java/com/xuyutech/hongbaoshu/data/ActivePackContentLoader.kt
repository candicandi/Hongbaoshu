package com.xuyutech.hongbaoshu.data

import android.content.Context
import android.net.Uri

class ActivePackContentLoader(
    private val builtinLoader: ContentLoader,
    private val packLoaderProvider: (String) -> ContentLoader
) : ContentLoader {
    private var activePackId: String = "builtin"

    fun setActivePackId(packId: String) {
        activePackId = packId
    }

    fun currentPackId(): String = activePackId

    private fun currentLoader(): ContentLoader {
        return if (activePackId == "builtin") {
            builtinLoader
        } else {
            packLoaderProvider(activePackId)
        }
    }

    override suspend fun loadBook(context: Context): BookLoadResult = currentLoader().loadBook(context)

    override fun narrationUri(sentenceId: String): Uri? = currentLoader().narrationUri(sentenceId)

    override fun flipSound(): Uri? = currentLoader().flipSound()

    override fun coverImage(): Uri? = currentLoader().coverImage()
}
