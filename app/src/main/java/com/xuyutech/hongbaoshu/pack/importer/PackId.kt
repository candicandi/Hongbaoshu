package com.xuyutech.hongbaoshu.pack.importer

private val packIdRegex = Regex("^[a-zA-Z0-9._-]{1,120}$")

fun isValidPackId(packId: String): Boolean = packIdRegex.matches(packId)

