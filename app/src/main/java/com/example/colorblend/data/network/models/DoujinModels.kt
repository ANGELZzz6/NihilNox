package com.example.colorblend.data.network.models

import com.google.gson.annotations.SerializedName

/**
 * Modelos unificados para Doujinshi
 */
data class DoujinItem(
    val id: String,
    val title: String,
    val coverUrl: String,
    val source: String, // "MangaDex" o "nHentai"
    val mediaId: String? = null, // Para nHentai (como String para URLs)
    val tags: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val characters: List<String> = emptyList(),
    val totalPages: Int = 0,
    val pageExtensions: List<String> = emptyList() // "j", "p", "g" para nHentai
)

/**
 * MangaDex API Models
 */
data class MangaDexSearchResponse(
    val data: List<MangaDexManga>
)

data class MangaDexManga(
    val id: String,
    val attributes: MangaDexAttributes,
    val relationships: List<MangaDexRelationship>
)

data class MangaDexAttributes(
    val title: Map<String, String>,
    val description: Map<String, String>?,
    val tags: List<MangaDexTag>?,
    val contentRating: String?
)

data class MangaDexTag(
    val attributes: MangaDexTagAttributes
)

data class MangaDexTagAttributes(
    val name: Map<String, String>
)

data class MangaDexRelationship(
    val id: String,
    val type: String,
    val attributes: MangaDexRelationshipAttributes? = null
)

data class MangaDexRelationshipAttributes(
    val fileName: String? = null
)

data class MangaDexFeedResponse(
    val data: List<MangaDexChapter>
)

data class MangaDexChapter(
    val id: String,
    val attributes: MangaDexChapterAttributes
)

data class MangaDexChapterAttributes(
    val volume: String?,
    val chapter: String?,
    val title: String?,
    val translatedLanguage: String?,
    val pages: Int
)

/**
 * nHentai API Models
 */
data class NHentaiSearchResponse(
    val result: List<NHentaiGallery>?,
    @SerializedName("num_pages") val numPages: Int,
    @SerializedName("per_page") val perPage: Int
)

data class NHentaiGallery(
    val id: Int,
    @SerializedName("media_id") val mediaId: Any?,
    val title: NHentaiTitle?,
    val images: NHentaiImages?,
    @SerializedName("num_pages") val numPages: Int,
    val tags: List<NHentaiTag>?,
    
    // API V2 Search Fields
    @SerializedName("english_title") val englishTitleV2: String? = null,
    @SerializedName("japanese_title") val japaneseTitleV2: String? = null,
    @SerializedName("thumbnail") val thumbnailV2: Any? = null, // Puede ser String (Search) u Objeto (Details)
    
    // API V2 Details Fields
    val pages: List<NHentaiPageV2>? = null,
    val cover: NHentaiImageV2? = null
)

data class NHentaiPageV2(
    val number: Int,
    val path: String?
)

data class NHentaiImageV2(
    val path: String?
)

data class NHentaiTitle(
    val english: String?,
    val japanese: String?,
    val pretty: String?
)

data class NHentaiImages(
    val pages: List<NHentaiImage>?,
    val cover: NHentaiImage?,
    val thumbnail: NHentaiImage?
)

data class NHentaiImage(
    val t: String?, // "j" para jpg, "p" para png, "g" para gif
    val w: Int?,
    val h: Int?
)

data class NHentaiTag(
    val id: Int,
    val type: String?, // "artist", "tag", "character", "parody"
    val name: String?
)
