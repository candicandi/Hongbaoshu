package com.xuyutech.hongbaoshu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Sentence(
    val id: String,
    val content: String
)

@Serializable
data class Paragraph(
    val id: String,
    val type: ParagraphType,
    val content: String,
    val ref: String? = null,
    val sentences: List<Sentence> = emptyList()
)

@Serializable
data class Chapter(
    val id: String,
    val title: String,
    val paragraphs: List<Paragraph>
)

@Serializable
data class BookMetadata(
    val title: String,
    val author: String,
    val edition: String
)

@Serializable
data class BookJson(
    val book: BookMetadata,
    val chapters: List<Chapter>
)

data class Book(
    val title: String,
    val author: String,
    val edition: String,
    val chapters: List<Chapter>
) {
    companion object {
        fun fromJson(json: BookJson): Book = Book(
            title = json.book.title,
            author = json.book.author,
            edition = json.book.edition,
            chapters = json.chapters
        )
    }
}

@Serializable
enum class ParagraphType {
    @SerialName("text")
    text,

    @SerialName("annotation")
    annotation
}

data class BookLoadResult(
    val book: Book,
    val missingSentenceAudioIds: Set<String> = emptySet()
)
