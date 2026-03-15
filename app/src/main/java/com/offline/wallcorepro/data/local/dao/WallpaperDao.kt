package com.offline.wallcorepro.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.offline.wallcorepro.data.local.entity.WallpaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {

    // Sort by cachedAt DESC first so every new batch inserted by the RemoteMediator
    // on REFRESH rises to the top of the feed — the user sees fresh content immediately
    // after pull-to-refresh without any blank screen.
    // shuffleKey is the secondary sort so items within the same insertion batch are
    // still randomly ordered (preventing duplicates looking like a sorted list).
    @Query("SELECT * FROM wallpapers WHERE niche = :niche ORDER BY cachedAt DESC, shuffleKey")
    fun getWallpapersByNichePaged(niche: String): PagingSource<Int, WallpaperEntity>

    @Query("SELECT * FROM wallpapers WHERE niche = :niche ORDER BY createdAt DESC LIMIT :limit")
    fun getWallpapersByNiche(niche: String, limit: Int = 50): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE niche = :niche AND isTrending = 1 ORDER BY createdAt DESC")
    fun getTrendingWallpapers(niche: String): Flow<List<WallpaperEntity>>

    /** Popular by downloads + recency — used as fallback when trending is empty */
    @Query("SELECT * FROM wallpapers WHERE niche = :niche ORDER BY downloadsCount DESC, createdAt DESC LIMIT 12")
    fun getPopularWallpapers(niche: String): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE niche = :niche AND category = :category ORDER BY createdAt DESC")
    fun getWallpapersByCategory(niche: String, category: String): PagingSource<Int, WallpaperEntity>

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY cachedAt DESC")
    fun getFavoriteWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE id = :id")
    suspend fun getWallpaperById(id: String): WallpaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpapers(wallpapers: List<WallpaperEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpaper(wallpaper: WallpaperEntity)

    @Query("UPDATE wallpapers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE wallpapers SET downloadsCount = downloadsCount + 1 WHERE id = :id")
    suspend fun incrementDownloads(id: String)

    @Query("UPDATE wallpapers SET localPath = :path WHERE id = :id")
    suspend fun updateLocalPath(id: String, path: String)

    @Query("DELETE FROM wallpapers WHERE niche = :niche AND isFavorite = 0 AND cachedAt < :olderThan")
    suspend fun deleteOldCache(niche: String, olderThan: Long)

    @Query("DELETE FROM wallpapers WHERE niche = :niche AND isFavorite = 0")
    suspend fun clearCacheForNiche(niche: String)

    @Query("SELECT COUNT(*) FROM wallpapers WHERE niche = :niche")
    suspend fun getWallpaperCount(niche: String): Int

    @Query("SELECT COUNT(*) FROM wallpapers WHERE niche = :niche AND category = :category")
    suspend fun getCategoryWallpaperCount(niche: String, category: String): Int

    @Query("SELECT COUNT(*) FROM wallpapers WHERE niche = :niche AND createdAt > :since")
    suspend fun getNewWallpaperCount(niche: String, since: Long): Int

    @Query("SELECT MAX(createdAt) FROM wallpapers WHERE niche = :niche")
    suspend fun getLatestTimestamp(niche: String): Long?

    @Query("SELECT * FROM wallpapers WHERE niche = :niche ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWallpaper(niche: String): WallpaperEntity?

    @Query("""
        SELECT * FROM wallpapers 
        WHERE niche = :niche 
        AND (title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchWallpapers(niche: String, query: String): PagingSource<Int, WallpaperEntity>
}
