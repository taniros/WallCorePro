package com.offline.wallcorepro.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wallpapers",
    indices = [
        Index(value = ["niche"]),
        Index(value = ["category"]),
        Index(value = ["isTrending"]),
        Index(value = ["createdAt"]),
        Index(value = ["isFavorite"])
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
    val localPath: String? = null
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
