package com.offline.wallcorepro.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wallpapers",
    indices = [
        // Composite index — covers the hot query: WHERE niche=? ORDER BY createdAt DESC
        Index(value = ["niche", "createdAt"]),
        // Composite index — covers category-filtered paged query
        Index(value = ["niche", "category", "createdAt"]),
        Index(value = ["isTrending"]),
        Index(value = ["isFavorite"]),
        // Covers the main paged feed: WHERE niche=? ORDER BY shuffleKey
        Index(value = ["niche", "shuffleKey"])
    ]
)
data class WallpaperEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val category: String,
    val niche: String,
    val dominantColor: String,
    val isTrending: Boolean,
    val isPremium: Boolean,
    val createdAt: Long,
    val downloadsCount: Int,
    val isFavorite: Boolean = false,
    val photographer: String? = null,
    val photographerUrl: String? = null,
    val cachedAt: Long = System.currentTimeMillis(),
    val localPath: String? = null,
    // Stable random sort key: assigned once at insert, used for ORDER BY shuffleKey
    // so the feed feels randomly ordered yet stays consistent within a scroll session.
    val shuffleKey: Int = kotlin.random.Random.nextInt(Int.MAX_VALUE),
    // Full comma-separated source tags (e.g. "flowers, sunrise, morning, nature").
    // Stored so people-content filtering can check ALL tags, not just the first one
    // that becomes the title.
    val tags: String = ""
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val imageUrl: String,
    val count: Int,
    val niche: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallpaper_remote_keys")
data class WallpaperRemoteKeys(
    @PrimaryKey val wallpaperId: String,
    val prevKey: Int?,
    val currentPage: Int,
    val nextKey: Int?,
    val createdAt: Long = System.currentTimeMillis()
)
