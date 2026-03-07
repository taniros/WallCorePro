package com.offline.wallcorepro.data.mapper

import com.offline.wallcorepro.data.local.entity.CategoryEntity
import com.offline.wallcorepro.data.local.entity.WallpaperEntity
import com.offline.wallcorepro.data.model.CategoryDto
import com.offline.wallcorepro.data.model.PixabayHit
import com.offline.wallcorepro.data.model.WallpaperDto
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.domain.model.WallpaperCategory

// ─── DTO → Entity ────────────────────────────────────────────────────────────

fun WallpaperDto.toEntity(niche: String): WallpaperEntity {
    val resolvedImageUrl = src?.large ?: src?.original ?: imageUrl
    val resolvedThumb = src?.medium ?: src?.small ?: thumbnailUrl.ifEmpty { resolvedImageUrl }
    val resolvedColor = avgColor ?: dominantColor

    return WallpaperEntity(
        id = id,
        title = title.ifEmpty { "Wallpaper" },
        imageUrl = resolvedImageUrl,
        thumbnailUrl = resolvedThumb,
        category = category,
        niche = niche,
        dominantColor = resolvedColor,
        isTrending = isTrending,
        isPremium = isPremium,
        createdAt = createdAt,
        downloadsCount = downloadsCount,
        photographer = photographer,
        photographerUrl = photographerUrl
    )
}

fun com.offline.wallcorepro.data.model.PixabayHit.toEntity(niche: String): WallpaperEntity {
    return WallpaperEntity(
        id = "pixabay_$id",
        title = tags.split(",").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Greeting Card",
        imageUrl = largeImageURL.ifEmpty { fullHDURL }.ifEmpty { webformatURL },
        thumbnailUrl = webformatURL.ifEmpty { previewURL },
        category = "Illustration",
        niche = niche,
        dominantColor = "#FF6F00",
        isTrending = false,
        isPremium = false,
        createdAt = System.currentTimeMillis(),
        downloadsCount = 0,
        photographer = user,
        photographerUrl = "https://pixabay.com/users/$user-$id/"
    )
}

fun CategoryDto.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    imageUrl = imageUrl,
    count = count,
    niche = niche
)

// ─── Entity → Domain ─────────────────────────────────────────────────────────

fun WallpaperEntity.toDomain(): Wallpaper = Wallpaper(
    id = id,
    title = title,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    category = category,
    niche = niche,
    dominantColor = dominantColor,
    isTrending = isTrending,
    isPremium = isPremium,
    createdAt = createdAt,
    downloadsCount = downloadsCount,
    isFavorite = isFavorite,
    photographer = photographer,
    photographerUrl = photographerUrl,
    localPath = localPath
)

fun CategoryEntity.toDomain(): WallpaperCategory = WallpaperCategory(
    id = id,
    name = name,
    imageUrl = imageUrl,
    count = count,
    niche = niche
)

// ─── Domain → Entity ─────────────────────────────────────────────────────────

fun Wallpaper.toEntity(): WallpaperEntity = WallpaperEntity(
    id = id,
    title = title,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    category = category,
    niche = niche,
    dominantColor = dominantColor,
    isTrending = isTrending,
    isPremium = isPremium,
    createdAt = createdAt,
    downloadsCount = downloadsCount,
    isFavorite = isFavorite,
    photographer = photographer,
    photographerUrl = photographerUrl,
    localPath = localPath
)
