package com.offline.wallcorepro.domain.repository

import androidx.paging.PagingData
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.domain.model.WallpaperCategory
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {

    /** Paged wallpaper feed for the current niche.
     *  [globalSeedBase] is the persistent session seed (advanced each app open via
     *  PreferenceManager.getAndAdvanceGlobalFeedSeed) that selects a different
     *  cluster of backend queries so every session returns genuinely new content. */
    fun getWallpapersFeed(niche: String, globalSeedBase: Int = 0): Flow<PagingData<Wallpaper>>

    /** Trending wallpapers */
    fun getTrendingWallpapers(niche: String): Flow<List<Wallpaper>>

    /** Wallpapers by category, paged */
    fun getWallpapersByCategory(niche: String, category: String): Flow<PagingData<Wallpaper>>

    /** Search through wallpapers */
    fun searchWallpapers(niche: String, query: String): Flow<PagingData<Wallpaper>>

    /** Get a specific wallpaper by ID */
    suspend fun getWallpaperById(id: String): Wallpaper?

    /** Favorites */
    fun getFavoriteWallpapers(): Flow<List<Wallpaper>>
    suspend fun toggleFavorite(id: String, isFavorite: Boolean)

    /** Categories */
    fun getCategories(niche: String): Flow<List<WallpaperCategory>>
    suspend fun refreshCategories(niche: String)

    /** Sync */
    suspend fun syncWallpapers(niche: String, force: Boolean = false): Result<Int>
    /** Sync trending from /v1/trending API so Today's Pick has data even when main feed is empty */
    suspend fun syncTrendingForHero(niche: String): Result<Int>
    suspend fun syncCategoryWallpapers(niche: String, category: String, force: Boolean = false): Result<Int>
    suspend fun getNewWallpaperCount(niche: String, since: Long): Int

    /** Downloads */
    suspend fun incrementDownloadCount(id: String)

    /** Auto wallpaper */
    suspend fun getRandomWallpaper(niche: String): Wallpaper?

    /** Cache management */
    suspend fun cleanOldCache(niche: String, olderThanMs: Long)

    /**
     * Atomically clears all non-favourite wallpapers and remote paging keys for [niche].
     * Called at the start of each session when PENDING_CACHE_CLEAR flag is set so every
     * fresh open starts with an empty DB that warm-up fills with brand-new content.
     * Favourite wallpapers are always preserved.
     */
    suspend fun clearFeedCache(niche: String)

    /**
     * Background warm-up: fires [AppConfig.WARMUP_PARALLEL_SEEDS] parallel API requests
     * with seeds spread evenly across the 0–10,000 range.  Pre-fills the DB with
     * ~250 diverse images BEFORE the pager's first page appears, eliminating the
     * blank / shimmer-only first-open experience.
     */
    suspend fun warmUpFeed(niche: String, globalSeedBase: Int)

    /**
     * Background pre-fetch for ALL configured categories.
     * Runs in batches of 3 to avoid hammering the Render free-tier.
     * Skips any category that already has >= [AppConfig.PAGE_SIZE] wallpapers cached.
     * Call this in parallel with [warmUpFeed] at app start so every category
     * screen is populated before the user ever taps one.
     */
    suspend fun warmUpCategories(niche: String)
}
