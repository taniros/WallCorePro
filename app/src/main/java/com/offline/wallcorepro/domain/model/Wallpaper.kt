package com.offline.wallcorepro.domain.model

/**
 * Domain model – pure Kotlin, no Android/Room imports
 */
data class Wallpaper(
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
    val localPath: String? = null
)

data class WallpaperCategory(
    val id: String,
    val name: String,
    val imageUrl: String,
    val count: Int,
    val niche: String
)

enum class WallpaperTarget {
    HOME, LOCK, BOTH
}

enum class AutoWallpaperInterval(val hours: Long, val label: String) {
    ONE_HOUR(1, "Every hour"),
    SIX_HOURS(6, "Every 6 hours"),
    TWELVE_HOURS(12, "Every 12 hours"),
    DAILY(24, "Daily"),
    TWO_DAYS(48, "Every 2 days"),
    THREE_DAYS(72, "Every 3 days")
}
