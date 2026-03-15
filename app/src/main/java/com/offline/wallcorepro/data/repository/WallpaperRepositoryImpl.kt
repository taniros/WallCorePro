package com.offline.wallcorepro.data.repository

import androidx.paging.*
import androidx.room.withTransaction
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.ContentFilter
import com.offline.wallcorepro.data.local.WallCoreDatabase
import com.offline.wallcorepro.data.local.entity.WallpaperEntity
import com.offline.wallcorepro.data.mapper.toDomain
import com.offline.wallcorepro.data.mapper.toEntity
import com.offline.wallcorepro.data.network.WallpaperApiService
import com.offline.wallcorepro.data.paging.WallpaperRemoteMediator
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.domain.model.WallpaperCategory
import com.offline.wallcorepro.domain.repository.WallpaperRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// Secondary client-side filter — catches people/cross content that slipped through
// (e.g. old cached rows). Checks the full tag string, category, and title so nothing
// with a clean title but blocked tags (e.g. "cross, sunrise") can slip through.
private fun Wallpaper.containsPeopleContent(): Boolean =
    ContentFilter.containsPeople(tags = tags, alt = category, title = title)

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
    override fun getWallpapersFeed(niche: String, globalSeedBase: Int): Flow<PagingData<Wallpaper>> {
        return Pager(
            config = PagingConfig(
                pageSize           = AppConfig.PAGE_SIZE,
                prefetchDistance   = AppConfig.PREFETCH_DISTANCE,
                // 4 pages on first open (200 items) — fills several screens instantly
                // and gives the mediator time to fetch the next batch in the background.
                initialLoadSize    = AppConfig.PAGE_SIZE * 4,
                // false = the pager only allocates slots for actually-loaded items.
                // With 1,200+ items in the DB, true would force LazyStaggeredGrid to
                // reserve and track a null entry for every unloaded row, wasting RAM
                // and slowing Compose layout.  The shimmer overlay in HomeScreen
                // handles the "loading" visual without needing placeholder slots.
                enablePlaceholders = false
            ),
            remoteMediator = WallpaperRemoteMediator(
                database       = database,
                apiService     = apiService,
                niche          = niche,
                globalSeedBase = globalSeedBase
            ),
            pagingSourceFactory = { wallpaperDao.getWallpapersByNichePaged(niche) }
        ).flow.map { pagingData ->
            pagingData
                .map { it.toDomain() }
                .filter { !it.containsPeopleContent() }
        }
    }

    override suspend fun warmUpFeed(niche: String, globalSeedBase: Int) = coroutineScope {
        // Strategy: two-phase warm-up.
        //
        // Phase 1 — SEQUENTIAL wake-up (WARMUP_WAKE_SEEDS slots).
        //   The Render free-tier backend sleeps after ~15 min of inactivity.  Firing all
        //   24 requests in parallel against a sleeping server causes every request to
        //   timeout or fail together.  The first WARMUP_WAKE_SEEDS requests run one at a
        //   time; the first successful response confirms the server is awake.
        //
        // Phase 2 — PARALLEL blast (remaining slots).
        //   Once the server is confirmed awake the rest fire in parallel.  With
        //   maxRequestsPerHost=20 (NetworkModule) all run concurrently instead of
        //   queuing behind OkHttp's default limit of 5.
        //
        // Each slot fetches WARMUP_PAGES_PER_SEED pages → total:
        //   12 seeds × 2 pages × 50 items = 1,200 diverse wallpapers pre-loaded.
        //
        // Retry: a single retry with a 3 s delay is attempted for any failed request
        //   to handle transient network blips without hammering the server.

        val seedSpread = 10_000 / AppConfig.WARMUP_PARALLEL_SEEDS

        suspend fun fetchSlot(i: Int) {
            val seed = (globalSeedBase + i * seedSpread) % 10_000
            repeat(AppConfig.WARMUP_PAGES_PER_SEED) { pageOffset ->
                val page = i + 1 + (pageOffset * AppConfig.WARMUP_PARALLEL_SEEDS)
                var attempt = 0
                while (attempt < 2) {
                    try {
                        val response = apiService.getWallpapers(
                            niche   = niche,
                            page    = page,
                            perPage = AppConfig.PAGE_SIZE,
                            seed    = seed
                        )
                        if (response.isSuccessful) {
                            val items = response.body()?.wallpapers
                                ?.map { it.toEntity(niche) }
                                ?.filterNot { ContentFilter.containsPeople(tags = it.tags, alt = it.category, title = it.title) }
                                ?: emptyList()
                            if (items.isNotEmpty()) {
                                wallpaperDao.insertWallpapers(items)
                                Timber.d("WarmUp[$i-p$page]: seed=$seed inserted ${items.size}")
                            }
                        }
                        break  // success — no retry needed
                    } catch (e: Exception) {
                        attempt++
                        if (attempt < 2) {
                            Timber.w("WarmUp[$i-p$page] attempt $attempt failed, retrying in 3 s")
                            delay(3_000)
                        } else {
                            Timber.w(e, "WarmUp[$i-p$page] failed after retry (non-critical)")
                        }
                    }
                }
            }
        }

        // Phase 1: sequential wake-up seeds
        for (i in 0 until AppConfig.WARMUP_WAKE_SEEDS) {
            fetchSlot(i)
        }

        // Phase 2: parallel blast for remaining seeds
        val parallelJobs = (AppConfig.WARMUP_WAKE_SEEDS until AppConfig.WARMUP_PARALLEL_SEEDS)
            .map { i -> async { fetchSlot(i) } }
        parallelJobs.forEach { it.await() }

        Timber.d("WarmUp done: ${AppConfig.WARMUP_PARALLEL_SEEDS} seeds × ${AppConfig.WARMUP_PAGES_PER_SEED} pages")
    }

    override fun getTrendingWallpapers(niche: String): Flow<List<Wallpaper>> {
        return kotlinx.coroutines.flow.combine(
            wallpaperDao.getTrendingWallpapers(niche),
            wallpaperDao.getPopularWallpapers(niche),
            wallpaperDao.getWallpapersByNiche(niche, 24)
        ) { trending, popular, latest ->
            when {
                trending.isNotEmpty() -> trending
                popular.isNotEmpty()  -> popular
                else                  -> latest
            }
        }.map { list -> list.map { it.toDomain() } }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getWallpapersByCategory(niche: String, category: String): Flow<PagingData<Wallpaper>> {
        return Pager(
            config = PagingConfig(
                pageSize           = AppConfig.PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = com.offline.wallcorepro.data.paging.CategoryRemoteMediator(
                database   = database,
                apiService = apiService,
                niche      = niche,
                category   = category
            ),
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

    override suspend fun syncTrendingForHero(niche: String): Result<Int> {
        return try {
            val response = apiService.getTrending(niche = niche, page = 1, perPage = 15)
            if (response.isSuccessful) {
                val raw = response.body()?.wallpapers?.map { it.toEntity(niche) } ?: emptyList()
                val wallpapers = raw.filterNot {
                    ContentFilter.containsPeople(tags = it.tags, alt = it.category, title = it.title)
                }
                if (wallpapers.isNotEmpty()) {
                    wallpaperDao.insertWallpapers(wallpapers)
                    Timber.d("syncTrendingForHero: inserted ${wallpapers.size} trending (${raw.size - wallpapers.size} people filtered)")
                }
                Result.success(wallpapers.size)
            } else {
                Result.failure(Exception("Trending API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "syncTrendingForHero failed")
            Result.failure(e)
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
                val raw = apiResponse.body()?.wallpapers?.map { it.toEntity(niche) } ?: emptyList()
                // Filter using the ORIGINAL API tags/category before overriding the category label.
                // Overriding first caused ContentFilter to see alt="Family Wishes" → "family" token
                // matched → every result blocked → zero wallpapers shown for that category.
                val wallpapers = raw
                    .filterNot { ContentFilter.containsPeople(tags = it.tags, alt = it.category, title = it.title) }
                    .map { it.copy(category = category) }
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

    override suspend fun clearFeedCache(niche: String) {
        database.withTransaction {
            wallpaperDao.clearCacheForNiche(niche)
            database.remoteKeysDao().clearRemoteKeys()
        }
        Timber.d("clearFeedCache: non-favourite wallpapers + remote keys cleared for niche=$niche")
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

