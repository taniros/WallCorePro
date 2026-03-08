package com.offline.wallcorepro.data.repository

import androidx.paging.*
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.WallCoreDatabase
import com.offline.wallcorepro.data.local.entity.WallpaperEntity
import com.offline.wallcorepro.data.mapper.toDomain
import com.offline.wallcorepro.data.mapper.toEntity
import com.offline.wallcorepro.data.network.WallpaperApiService
import com.offline.wallcorepro.data.paging.WallpaperRemoteMediator
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.domain.model.WallpaperCategory
import com.offline.wallcorepro.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepositoryImpl @Inject constructor(
    private val database: WallCoreDatabase,
    private val apiService: WallpaperApiService,
    private val preferenceManager: PreferenceManager,
    private val aiService: com.offline.wallcorepro.data.network.AiService
) : WallpaperRepository {

    private val wallpaperDao = database.wallpaperDao()
    private val categoryDao = database.categoryDao()

    @OptIn(ExperimentalPagingApi::class)
    override fun getWallpapersFeed(niche: String): Flow<PagingData<Wallpaper>> {
        return Pager(
            config = PagingConfig(
                pageSize = AppConfig.PAGE_SIZE,
                prefetchDistance = AppConfig.PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            remoteMediator = WallpaperRemoteMediator(
                database = database,
                apiService = apiService,
                niche = niche
            ),
            pagingSourceFactory = { wallpaperDao.getWallpapersByNichePaged(niche) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getTrendingWallpapers(niche: String): Flow<List<Wallpaper>> {
        return kotlinx.coroutines.flow.combine(
            wallpaperDao.getTrendingWallpapers(niche),
            wallpaperDao.getPopularWallpapers(niche)
        ) { trending, popular ->
            if (trending.isNotEmpty()) trending else popular
        }.map { list -> list.map { it.toDomain() } }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getWallpapersByCategory(niche: String, category: String): Flow<PagingData<Wallpaper>> {
        return Pager(
            config = PagingConfig(pageSize = AppConfig.PAGE_SIZE),
            pagingSourceFactory = { wallpaperDao.getWallpapersByCategory(niche, category) }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun searchWallpapers(niche: String, query: String): Flow<PagingData<Wallpaper>> {
        return Pager(
            config = PagingConfig(pageSize = AppConfig.PAGE_SIZE),
            pagingSourceFactory = { wallpaperDao.searchWallpapers(niche, query) }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override suspend fun getWallpaperById(id: String): Wallpaper? {
        return wallpaperDao.getWallpaperById(id)?.toDomain()
    }

    override fun getFavoriteWallpapers(): Flow<List<Wallpaper>> {
        return wallpaperDao.getFavoriteWallpapers().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        wallpaperDao.setFavorite(id, isFavorite)
    }

    override fun getCategories(niche: String): Flow<List<WallpaperCategory>> {
        return categoryDao.getCategoriesFlow(niche).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshCategories(niche: String) {
        try {
            val response = apiService.getCategories(niche)
            if (response.isSuccessful && !response.body()?.categories.isNullOrEmpty()) {
                val categories = response.body()!!.categories.map { it.toEntity() }
                categoryDao.clearCategories(niche)
                categoryDao.insertCategories(categories)
            } else {
                categoryDao.clearCategories(niche)
                categoryDao.insertCategories(getNicheCategories(niche))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh categories")
            categoryDao.clearCategories(niche)
            categoryDao.insertCategories(getNicheCategories(niche))
        }
    }

    override suspend fun syncWallpapers(niche: String, force: Boolean): Result<Int> {
        return try {
            val lastSync = preferenceManager.getLastSyncTime(niche)
            val since = if (force) 0L else lastSync

            val apiResponse = apiService.getWallpapers(niche = niche, since = since)
            if (apiResponse.isSuccessful) {
                val wallpapers = apiResponse.body()?.wallpapers?.map { it.toEntity(niche) } ?: emptyList()
                wallpaperDao.insertWallpapers(wallpapers)
                preferenceManager.setLastSyncTime(niche, System.currentTimeMillis())
                Result.success(wallpapers.size)
            } else {
                Result.failure(Exception("API error: ${apiResponse.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Sync failed")
            Result.failure(e)
        }
    }

    override suspend fun syncCategoryWallpapers(niche: String, category: String, force: Boolean): Result<Int> {
        return try {
            val apiResponse = apiService.getWallpapers(niche = niche, category = category, perPage = AppConfig.INITIAL_WALLPAPER_COUNT)
            if (apiResponse.isSuccessful) {
                val wallpapers = apiResponse.body()?.wallpapers?.map { it.toEntity(niche).copy(category = category) } ?: emptyList()
                if (wallpapers.isNotEmpty()) {
                    wallpaperDao.insertWallpapers(wallpapers)
                    Result.success(wallpapers.size)
                } else Result.success(0)
            } else Result.failure(Exception("API error: ${apiResponse.code()}"))
        } catch (e: Exception) {
            Timber.e(e, "Category sync failed")
            Result.failure(e)
        }
    }

    override suspend fun getNewWallpaperCount(niche: String, since: Long): Int {
        return wallpaperDao.getNewWallpaperCount(niche, since)
    }

    override suspend fun incrementDownloadCount(id: String) {
        wallpaperDao.incrementDownloads(id)
    }

    override suspend fun getRandomWallpaper(niche: String): Wallpaper? {
        return wallpaperDao.getRandomWallpaper(niche)?.toDomain()
    }

    override suspend fun cleanOldCache(niche: String, olderThanMs: Long) {
        wallpaperDao.deleteOldCache(niche, olderThanMs)
    }

    private fun getNicheCategories(niche: String): List<com.offline.wallcorepro.data.local.entity.CategoryEntity> {
        val categories = when (niche.uppercase()) {
            "GOOD_MORNING" -> AppConfig.NICHE_CATEGORIES
            "NATURE"       -> listOf("Forest", "Ocean", "Mountains", "Sunset", "Flowers", "Wildlife")
            "AMOLED"       -> listOf("Dark", "Abstract", "Neon", "Stars", "Minimal", "Geometric")
            "AI_ART"       -> listOf("Digital Art", "Fantasy", "Sci-Fi", "Portrait", "Landscape", "Abstract")
            "ANIME"        -> listOf("Action", "Romance", "Fantasy", "Slice of Life", "Mecha", "Horror")
            "FLOWERS"      -> listOf("Roses", "Sunflowers", "Orchids", "Cherry Blossom", "Tulips", "Wildflowers")
            "SPACE"        -> listOf("Galaxies", "Nebulae", "Planets", "Stars", "Astronauts", "Black Holes")
            "MINIMAL"      -> listOf("Lines", "Geometric", "Monochrome", "Pastel", "Abstract", "Typography")
            else           -> listOf("Good Morning", "Good Night", "Sunrise", "Moon & Stars", "Motivational", "Nature")
        }
        return categories.map { name ->
            com.offline.wallcorepro.data.local.entity.CategoryEntity(
                id       = "${niche}_${name.lowercase().replace(" ", "_").replace("&", "and")}",
                name     = name,
                imageUrl = "",
                count    = 0,
                niche    = niche
            )
        }
    }
}

