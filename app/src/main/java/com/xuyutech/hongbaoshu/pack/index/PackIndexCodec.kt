package com.xuyutech.hongbaoshu.pack.index

import com.xuyutech.hongbaoshu.pack.model.PackIndex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PackIndexCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(raw: String?): List<PackIndex> {
        val s = raw ?: return emptyList()
        return runCatching { json.decodeFromString<List<PackIndex>>(s) }.getOrDefault(emptyList())
    }

    fun encode(list: List<PackIndex>): String = json.encodeToString(list)
}

