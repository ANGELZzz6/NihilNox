package com.example.colorblend.network

import com.example.colorblend.data.network.models.*
import retrofit2.http.*

interface MangaDexApi {
    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("contentRating[]") contentRating: List<String> = listOf("safe", "suggestive", "erotica", "pornographic"),
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaDexSearchResponse

    @GET("manga/{id}/feed")
    suspend fun getMangaFeed(
        @Path("id") mangaId: String,
        @Query("translatedLanguage[]") languages: List<String>?,
        @Query("contentRating[]") contentRating: List<String> = listOf("safe", "suggestive", "erotica", "pornographic"),
        @Query("order[chapter]") order: String = "asc",
        @Query("includeExternalUrl") includeExternal: Int = 0
    ): MangaDexFeedResponse

    @GET("at-home/server/{chapterId}")
    suspend fun getChapterPages(
        @Path("chapterId") chapterId: String
    ): MangaDexChapterPagesResponse
}

data class MangaDexChapterPagesResponse(
    val baseUrl: String,
    val chapter: MangaDexChapterData
)

data class MangaDexChapterData(
    val hash: String,
    val data: List<String>
)

interface NHentaiApi {
    @GET("api/v2/search")
    suspend fun searchGalleries(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): NHentaiSearchResponse

    @GET("api/v2/galleries/{galleryId}")
    suspend fun getGalleryDetails(
        @Header("Authorization") apiKey: String,
        @Path("galleryId") galleryId: Int
    ): NHentaiGallery
}

interface YandereApi {
    /**
     * Yande.re API: post.json devuelve una lista de posts.
     * page es el número de página (empezando en 1).
     */
    @GET("post.json")
    suspend fun searchPosts(
        @Query("tags") tags: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<YanderePost>
}

interface NekobotApi {
    @GET("api/image")
    suspend fun getImage(
        @Query("type") type: String = "pgif"
    ): NekobotResponse
}

