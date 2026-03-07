package com.offline.wallcorepro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WallpaperDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String = "",
    @SerialName("imageUrl") val imageUrl: String = "",
    @SerialName("thumbnailUrl") val thumbnailUrl: String = "",
    @SerialName("category") val category: String = "General",
    @SerialName("niche") val niche: String = "",
    @SerialName("dominantColor") val dominantColor: String = "#1A1A1A",
    @SerialName("isTrending") val isTrending: Boolean = false,
    @SerialName("isPremium") val isPremium: Boolean = false,
    @SerialName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("downloadsCount") val downloadsCount: Int = 0,
    // Pexels API fields
    @SerialName("photographer") val photographer: String? = null,
    @SerialName("photographerUrl") val photographerUrl: String? = null,
    @SerialName("src") val src: PexelsSrc? = null,
    @SerialName("avg_color") val avgColor: String? = null,
    @SerialName("url") val url: String? = null,
    // alt = Pexels photo description — used for people-content filtering
    @SerialName("alt") val alt: String = ""
)

@Serializable
data class PexelsSrc(
    @SerialName("original") val original: String = "",
    @SerialName("large2x") val large2x: String = "",
    @SerialName("large") val large: String = "",
    @SerialName("medium") val medium: String = "",
    @SerialName("small") val small: String = "",
    @SerialName("portrait") val portrait: String = "",
    @SerialName("landscape") val landscape: String = "",
    @SerialName("tiny") val tiny: String = ""
)

@Serializable
data class PexelsResponse(
    @SerialName("photos") val photos: List<WallpaperDto> = emptyList(),
    @SerialName("total_results") val totalResults: Int = 0,
    @SerialName("page") val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    @SerialName("next_page") val nextPage: String? = null
)

@Serializable
data class CategoryDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("imageUrl") val imageUrl: String = "",
    @SerialName("count") val count: Int = 0,
    @SerialName("niche") val niche: String = ""
)

@Serializable
data class WallpaperListResponse(
    @SerialName("wallpapers") val wallpapers: List<WallpaperDto> = emptyList(),
    @SerialName("total") val total: Int = 0,
    @SerialName("page") val page: Int = 1,
    @SerialName("hasNext") val hasNext: Boolean = false
)

@Serializable
data class CategoryListResponse(
    @SerialName("categories") val categories: List<CategoryDto> = emptyList()
)

@Serializable
data class PixabayResponse(
    @SerialName("total") val total: Int = 0,
    @SerialName("totalHits") val totalHits: Int = 0,
    @SerialName("hits") val hits: List<PixabayHit> = emptyList()
)

@Serializable
data class PixabayHit(
    @SerialName("id") val id: Long,
    @SerialName("pageURL") val pageURL: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("tags") val tags: String = "",
    @SerialName("previewURL") val previewURL: String = "",
    @SerialName("largeImageURL") val largeImageURL: String = "",
    @SerialName("fullHDURL") val fullHDURL: String = "",
    @SerialName("webformatURL") val webformatURL: String = "",
    @SerialName("imageWidth") val imageWidth: Int = 0,
    @SerialName("imageHeight") val imageHeight: Int = 0,
    @SerialName("user") val user: String = "",
    @SerialName("userImageURL") val userImageURL: String = ""
)
