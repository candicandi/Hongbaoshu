package com.xuyutech.hongbaoshu.pack.model

import kotlinx.serialization.Serializable

@Serializable
data class PackIndex(
    val packId: String,
    val packVersion: Int = 1,
    val formatVersion: Int = 1,
    val bookTitle: String,
    val bookAuthor: String = "",
    val bookEdition: String? = null,
    val importedAt: Long,
    val lastOpenedAt: Long? = null,
    val hasCover: Boolean = true,
    val hasFlipSound: Boolean = true,
    val hasNarration: Boolean = true,
    val missingNarrationSentenceCount: Int = 0,
    val isValid: Boolean = true
)

