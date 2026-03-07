package com.offline.wallcorepro.domain.repository

import androidx.paging.PagingData
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.domain.model.WallpaperCategory
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {

    /** Paged wallpaper feed for the current niche */
    fun getWallpapersFeed(niche: String): Flow<PagingData<Wallpaper>>

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
    suspend fun syncCategoryWallpapers(niche: String, category: String, force: Boolean = false): Result<Int>
    suspend fun getNewWallpaperCount(niche: String, since: Long): Int

    /** Downloads */
    suspend fun incrementDownloadCount(id: String)

    /** Auto wallpaper */
    suspend fun getRandomWallpaper(niche: String): Wallpaper?

    /** Cache management */
    suspend fun cleanOldCache(niche: String, olderThanMs: Long)
}
