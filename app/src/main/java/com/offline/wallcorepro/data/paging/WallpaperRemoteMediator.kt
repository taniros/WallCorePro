package com.offline.wallcorepro.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.WallCoreDatabase
import com.offline.wallcorepro.data.local.entity.WallpaperEntity
import com.offline.wallcorepro.data.local.entity.WallpaperRemoteKeys
import com.offline.wallcorepro.data.mapper.*
import com.offline.wallcorepro.data.network.WallpaperApiService
import timber.log.Timber
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPagingApi::class)
class WallpaperRemoteMediator(
    private val database: WallCoreDatabase,
    private val apiService: WallpaperApiService,
    private val niche: String
) : RemoteMediator<Int, WallpaperEntity>() {

    private val wallpaperDao = database.wallpaperDao()
    private val remoteKeysDao = database.remoteKeysDao()

    override suspend fun initialize(): InitializeAction {
        val count = wallpaperDao.getWallpaperCount(niche)
        val cacheAge = remoteKeysDao.getLastCreationTime()?.createdAt ?: 0L
        val cacheAgeHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - cacheAge)
        
        // Force refresh if DB is nearly empty (e.g. only seed data) or cache is old
        return if (count < 10 || cacheAgeHours >= AppConfig.FRESHNESS_THRESHOLD_HOURS) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, WallpaperEntity>
    ): MediatorResult {
        return try {
            val page = when (loadType) {
                LoadType.REFRESH -> STARTING_PAGE_INDEX
                LoadType.PREPEND -> {
                    val remoteKeys = getRemoteKeyForFirstItem(state)
                    val prevKey = remoteKeys?.prevKey
                        ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                    prevKey
                }
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    val nextKey = remoteKeys?.nextKey
                        ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                    nextKey
                }
            }

            val wallpapers = mutableListOf<WallpaperEntity>()

            // Backend-only = Play-safe (no Pexels/Pixabay in app)
            val response = apiService.getWallpapers(
                niche = niche,
                page = page,
                perPage = state.config.pageSize
            )
            if (!response.isSuccessful) return MediatorResult.Error(Exception("API error: ${response.code()}"))
            response.body()?.wallpapers?.map { it.toEntity(niche) }?.let { wallpapers.addAll(it) }

            val endOfPaginationReached = wallpapers.isEmpty()

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    remoteKeysDao.clearRemoteKeys()
                    if (AppConfig.RC_FORCE_FRESH_ON_OPEN) {
                        wallpaperDao.clearCacheForNiche(niche)
                    }
                }

                val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1

                val keys = wallpapers.map { wallpaper ->
                    WallpaperRemoteKeys(
                        wallpaperId = wallpaper.id,
                        prevKey = prevKey,
                        currentPage = page,
                        nextKey = nextKey
                    )
                }

                remoteKeysDao.insertAll(keys)
                wallpaperDao.insertWallpapers(wallpapers)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            Timber.e(e, "RemoteMediator load failed")
            MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, WallpaperEntity>
    ): WallpaperRemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { id ->
                remoteKeysDao.remoteKeysById(id)
            }
        }
    }

    private suspend fun getRemoteKeyForFirstItem(
        state: PagingState<Int, WallpaperEntity>
    ): WallpaperRemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()?.let {
            remoteKeysDao.remoteKeysById(it.id)
        }
    }

    private suspend fun getRemoteKeyForLastItem(
        state: PagingState<Int, WallpaperEntity>
    ): WallpaperRemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let {
            remoteKeysDao.remoteKeysById(it.id)
        }
    }

    companion object {
        private const val STARTING_PAGE_INDEX = 1
    }
}
