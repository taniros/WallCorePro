package com.offline.wallcorepro.data.network

import com.offline.wallcorepro.data.model.CategoryListResponse
import com.offline.wallcorepro.data.model.PexelsResponse
import com.offline.wallcorepro.data.model.PixabayResponse
import com.offline.wallcorepro.data.model.WallpaperListResponse
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

// ─── AI Backend Proxy (Gemini key stays on server) ────────────────────────────
@Serializable
data class AiPromptRequest(val prompt: String)

@Serializable
data class AiTextResponse(val text: String = "")

@Serializable
data class AiKeywordsResponse(val keywords: List<String> = emptyList())

interface AiApiService {
    @POST("ai/generate-wish")
    suspend fun generateWish(@Body body: AiPromptRequest): Response<AiTextResponse>

    @POST("ai/rephrase-wish")
    suspend fun rephraseWish(@Body body: AiPromptRequest): Response<AiTextResponse>

    @POST("ai/generate-keywords")
    suspend fun generateKeywords(@Body body: AiPromptRequest): Response<AiKeywordsResponse>
}

interface WallpaperApiService {

    // Custom Backend endpoints
    @GET("wallpapers")
    suspend fun getWallpapers(
        @Query("niche")     niche: String,
        @Query("since")     since: Long = 0,
        @Query("page")      page: Int = 1,
        @Query("per_page")  perPage: Int = 30,   // matches AppConfig.PAGE_SIZE
        @Query("category")  category: String? = null,
        @Query("seed")      seed: Int = 0        // per-session random offset for infinite-wheel variety
    ): Response<WallpaperListResponse>

    /** Fire-and-forget health ping — wakes Render free-tier server before it is needed. */
    @GET("../health")
    suspend fun healthCheck(): Response<Unit>

    @GET("categories")
    suspend fun getCategories(
        @Query("niche") niche: String
    ): Response<CategoryListResponse>

    @GET("trending")
    suspend fun getTrending(
        @Query("niche") niche: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<WallpaperListResponse>

    @GET("wallpapers/{id}")
    suspend fun getWallpaperById(
        @Path("id") id: String
    ): Response<com.offline.wallcorepro.data.model.WallpaperDto>
}

interface PexelsApiService {
    @GET("search")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("orientation") orientation: String = "portrait"
    ): Response<PexelsResponse>

    @GET("curated")
    suspend fun getCuratedPhotos(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<PexelsResponse>
}

interface PixabayApiService {
    @GET(".")
    suspend fun searchImages(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("image_type") imageType: String = "photo",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("orientation") orientation: String = "vertical",
        @Query("safesearch") safeSearch: Boolean = true,
        // Restrict to nature or backgrounds to avoid people categories
        @Query("category") category: String = "nature",
        // Only show editorial-free images (no model releases needed = fewer people)
        @Query("editors_choice") editorsChoice: Boolean = false
    ): Response<PixabayResponse>
}

