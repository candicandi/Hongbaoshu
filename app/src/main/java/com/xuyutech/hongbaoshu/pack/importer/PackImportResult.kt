package com.xuyutech.hongbaoshu.pack.importer

data class PackImportResult(
    val status: Status,
    val packId: String? = null,
    val message: String,
    val errorCode: ErrorCode? = null
) {
    enum class Status { SUCCESS, FAILED, SKIPPED }

    enum class ErrorCode {
        FORMAT_UNSUPPORTED,
        MANIFEST_INVALID,
        BOOK_INVALID,
        FILE_MISSING,
        VERSION_CONFLICT,
        IO_ERROR
    }
}

