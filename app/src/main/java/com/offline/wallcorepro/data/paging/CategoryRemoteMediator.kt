package com.offline.wallcorepro.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.WallCoreDatabase
import com.offline.wallcorepro.data.local.entity.WallpaperEntity
import com.offline.wallcorepro.data.mapper.toEntity
import com.offline.wallcorepro.data.network.WallpaperApiService
import timber.log.Timber

/**
 * RemoteMediator for category-filtered wallpaper feeds.
 *
 * Unlike the main feed mediator, this passes the [category] parameter to the
 * backend so the server returns wallpapers specifically tagged for that category.
 * This ensures every category always has wallpapers even on a cold DB.
 *
 * Pagination strategy: page number is derived from the count of already-cached
 * wallpapers for that category, so APPEND always fetches the next logical page.
 * A unique seed derived from the category name and page number guarantees diverse
 * results across consecutive pages.
 */
@OptIn(ExperimentalPagingApi::class)
class CategoryRemoteMediator(
    private val database: WallCoreDatabase,
    private val apiService: WallpaperApiService,
    private val niche: String,
    private val category: String
) : RemoteMediator<Int, WallpaperEntity>() {

    private val wallpaperDao = database.wallpaperDao()

    override suspend fun initialize(): InitializeAction {
        val count = wallpaperDao.getCategoryWallpaperCount(niche, category)
        return if (count < AppConfig.PAGE_SIZE) {
            Timber.d("Category[$category]: $count items in DB → LAUNCH_INITIAL_REFRESH")
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            Timber.d("Category[$category]: $count items in DB → SKIP_INITIAL_REFRESH")
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, WallpaperEntity>
    ): MediatorResult {
        return try {
            val page = when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND  -> {
                    val count = wallpaperDao.getCategoryWallpaperCount(niche, category)
                    (count / state.config.pageSize) + 1
                }
            }

            // Vary seed per page so the backend returns a different slice each time.
            // Using a hash of the category name keeps seeds consistent per category
            // while still being different across categories and pages.
            val seed = ((kotlin.math.abs(category.hashCode()) + page * 37) % 10_000)

            val response = apiService.getWallpapers(
                niche    = niche,
                page     = page,
                perPage  = state.config.pageSize,
                category = category,
                seed     = seed
            )

            if (!response.isSuccessful) {
                Timber.e("Category[$category] API error ${response.code()} page=$page")
                return MediatorResult.Error(Exception("API ${response.code()}"))
            }

            val items = response.body()?.wallpapers
                ?.map { it.toEntity(niche).copy(category = category) }
                ?: emptyList()

            Timber.d("Category[$category] page=$page seed=$seed fetched=${items.size}")

            database.withTransaction {
                wallpaperDao.insertWallpapers(items)
            }

            MediatorResult.Success(endOfPaginationReached = items.isEmpty())

        } catch (e: Exception) {
            Timber.e(e, "CategoryRemoteMediator[$category] error")
            MediatorResult.Error(e)
        }
    }
}
