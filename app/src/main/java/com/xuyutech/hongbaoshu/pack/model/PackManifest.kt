package com.xuyutech.hongbaoshu.pack.model

import kotlinx.serialization.Serializable

@Serializable
data class PackManifest(
    val formatVersion: Int,
    val packId: String,
    val packVersion: Int,
    val book: BookMetadata,
    val resources: PackResources
)

@Serializable
data class BookMetadata(
    val title: String,
    val author: String = "",
    val edition: String = ""
)

@Serializable
data class PackResources(
    val text: ResourceItem,
    val cover: ResourceItem? = null,
    val flipSound: ResourceItem? = null,
    val narration: NarrationResource? = null
)

@Serializable
data class ResourceItem(
    val path: String,
    val sha256: String? = null
)

@Serializable
data class NarrationResource(
    val dir: String,
    val codec: String
)

