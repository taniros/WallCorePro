package com.offline.wallcorepro.domain.model

import androidx.compose.runtime.Immutable

/**
 * Domain model – pure Kotlin, no Android/Room imports.
 *
 * @Immutable tells the Compose compiler that every property of this object is
 * deeply immutable and will never change after construction.  Without this,
 * Compose treats Wallpaper as "unstable" and recomposes every WallpaperCard
 * whenever ANY state in the parent screen changes — even unrelated state like
 * the search bar or greeting text.  With @Immutable, cards whose wallpaper data
 * did not change are skipped entirely during recomposition → dramatically fewer
 * GPU draw calls during fast scrolling.
 */
@Immutable
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
    val localPath: String? = null,
    // Full comma-separated source tags (e.g. "flowers, sunrise, nature").
    // Carried through from WallpaperEntity so all filter checkpoints have
    // the complete tag string, not just the title.
    val tags: String = ""
)

@Immutable
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
