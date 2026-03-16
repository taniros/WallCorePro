package com.offline.wallcorepro.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.ContentFilter
import com.offline.wallcorepro.data.local.WallCoreDatabase
import com.offline.wallcorepro.data.local.entity.WallpaperEntity
import com.offline.wallcorepro.data.local.entity.WallpaperRemoteKeys
import com.offline.wallcorepro.data.mapper.toEntity
import com.offline.wallcorepro.data.network.WallpaperApiService
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Remote mediator for the infinite wallpaper feed.
 *
 * ── Why the old approach showed the same wallpapers ──────────────────────────
 *
 * The previous design cleared the cache on every REFRESH and used the same
 * seed for ALL page requests in a session.  The backend maps seed → query, so
 * one seed = one backend query = the same ~500 images served in the same order
 * every single time the app opened.
 *
 * ── New strategy: Per-Page Query Rotation + Accumulating Cache ───────────────
 *
 * 1. ACCUMULATING CACHE — REFRESH no longer clears wallpapers.  Content builds
 *    up in Room across every session.  New items are inserted with a fresh
 *    cachedAt timestamp so they appear FIRST in the feed (ORDER BY cachedAt DESC).
 *    Old items stay available for infinite scrolling below.
 *
 * 2. PER-PAGE QUERY ROTATION — Each API call uses a different seed derived from
 *    (globalSeedBase + virtualFetchIndex).  The backend maps seed → query, so
 *    consecutive pages draw from DIFFERENT backend queries → true variety.
 *
 *    virtualFetchIndex 0  → query 0,  Pixabay page 1  → 50 unique images
 *    virtualFetchIndex 1  → query 1,  Pixabay page 1  → 50 DIFFERENT images
 *    …
 *    virtualFetchIndex 29 → query 29, Pixabay page 1  → 50 DIFFERENT images
 *    virtualFetchIndex 30 → query 0,  Pixabay page 2  → 50 new images from q0
 *    …
 *    Total per session: 30 queries × ~20 Pixabay pages × 50 items ≈ 30,000
 *
 * 3. PERSISTENT GLOBAL SEED — globalSeedBase advances by SEED_ADVANCE_PER_SESSION
 *    each app open (managed by PreferenceManager).  Different base = different
 *    query cluster = genuinely new content on every session even if the virtual
 *    index wraps around.
 *
 * ── Remote-key usage ─────────────────────────────────────────────────────────
 *
 * currentPage / prevKey / nextKey all store virtualFetchIndex values (not real
 * backend page numbers).  The mapping to actual (apiSeed, apiPage) is done at
 * load time via virtualIndexToApiParams().
 *
 * ── People filter ────────────────────────────────────────────────────────────
 *
 * Whole-word matching on tags + title so "landscape" ≠ "and", etc.
 */
@OptIn(ExperimentalPagingApi::class)
class WallpaperRemoteMediator(
    private val database: WallCoreDatabase,
    private val apiService: WallpaperApiService,
    private val niche: String,
    private val globalSeedBase: Int = 0   // persistent, advanced each session
) : RemoteMediator<Int, WallpaperEntity>() {

    private val wallpaperDao  = database.wallpaperDao()
    private val remoteKeysDao = database.remoteKeysDao()

    // ── Query rotation ────────────────────────────────────────────────────────
    // Maps a monotonically increasing virtualFetchIndex to the actual (apiPage, apiSeed)
    // pair sent to the backend.  Cycling through all NUM_BACKEND_QUERIES on consecutive
    // pages before advancing to the next Pixabay page gives the most variety.
    private fun virtualIndexToApiParams(index: Int): Pair<Int, Int> {
        val queryIdx = index % AppConfig.NUM_BACKEND_QUERIES          // 0..29
        val apiPage  = index / AppConfig.NUM_BACKEND_QUERIES + 1      // 1, 2, 3 …
        val apiSeed  = (globalSeedBase + queryIdx) % 10_000
        return Pair(apiPage, apiSeed)
    }

    // Safety: stop hammering the server if it keeps returning empty
    private var consecutiveEmpty    = 0
    private val maxConsecutiveEmpty = 5

    // ── People filter ─────────────────────────────────────────────────────────
    // Use the comprehensive ContentFilter to strictly avoid women, girls,
    // portraits, couples, fashion, etc. — nature/flower/sky only.
    private fun WallpaperEntity.containsPeopleContent(): Boolean =
        ContentFilter.containsPeople(tags = tags, alt = category, title = title)

    // ── initialize ────────────────────────────────────────────────────────────
    override suspend fun initialize(): InitializeAction {
        val count = wallpaperDao.getWallpaperCount(niche)
        if (count < 50) {
            Timber.d("Feed: DB has $count items → LAUNCH_INITIAL_REFRESH (cold start)")
            return InitializeAction.LAUNCH_INITIAL_REFRESH
        }
        // Freshness check — if FRESHNESS_THRESHOLD_HOURS == 0 this always refreshes,
        // fetching new wallpapers on every app open without clearing the local cache.
        val newestMs  = wallpaperDao.getLatestTimestamp(niche) ?: 0L
        val ageHours  = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - newestMs)
        return if (ageHours >= AppConfig.FRESHNESS_THRESHOLD_HOURS) {
            Timber.d("Feed: $count items, cache ${ageHours}h old (threshold=${AppConfig.FRESHNESS_THRESHOLD_HOURS}h) → LAUNCH_INITIAL_REFRESH")
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            Timber.d("Feed: $count items, fresh (${ageHours}h old) → SKIP_INITIAL_REFRESH")
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    // ── load ──────────────────────────────────────────────────────────────────
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, WallpaperEntity>
    ): MediatorResult {
        return try {
            val virtualIndex: Int = when (loadType) {

                LoadType.REFRESH -> {
                    consecutiveEmpty = 0
                    // Start at a random offset within the first query rotation.
                    // Combined with the advanced globalSeedBase this means every
                    // session hits a genuinely different cluster of backend queries
                    // from position 0, maximising variety for the first visible page.
                    kotlin.random.Random.nextInt(AppConfig.NUM_BACKEND_QUERIES)
                }

                LoadType.PREPEND ->
                    return MediatorResult.Success(endOfPaginationReached = true)

                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    when {
                        remoteKeys == null         -> 0
                        remoteKeys.nextKey != null -> remoteKeys.nextKey!!
                        else                       ->
                            // nextKey == null means the backend returned 0 items for
                            // this index.  The consecutiveEmpty safety counter above
                            // will eventually trigger endOfPaginationReached → the UI's
                            // wrapAroundScroll() creates a new Pager with the next
                            // persistent seed → another fresh session without clearing.
                            return MediatorResult.Success(endOfPaginationReached = true)
                    }
                }
            }

            // ── Derive actual API parameters from the virtual index ────────────
            val (apiPage, apiSeed) = virtualIndexToApiParams(virtualIndex)

            // ── Network call ──────────────────────────────────────────────────
            val response = apiService.getWallpapers(
                niche   = niche,
                page    = apiPage,
                perPage = state.config.pageSize,
                seed    = apiSeed
            )

            if (!response.isSuccessful) {
                Timber.e("API error ${response.code()} vIdx=$virtualIndex page=$apiPage seed=$apiSeed")
                return MediatorResult.Error(Exception("API ${response.code()}"))
            }

            val rawItems = response.body()?.wallpapers?.map { it.toEntity(niche) } ?: emptyList()

            if (rawItems.isEmpty()) {
                consecutiveEmpty++
                Timber.w("Feed: empty response $consecutiveEmpty/$maxConsecutiveEmpty vIdx=$virtualIndex")
                if (consecutiveEmpty >= maxConsecutiveEmpty) {
                    Timber.w("Feed: $maxConsecutiveEmpty consecutive empty pages — pausing")
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
            } else {
                consecutiveEmpty = 0
            }

            val endOfPaginationReached = rawItems.isEmpty()
            val cleanItems = rawItems.filterNot { it.containsPeopleContent() }

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    // ── KEY CHANGE ────────────────────────────────────────────
                    // DO NOT clear wallpapers.  The library ACCUMULATES across sessions.
                    // New items get a fresh cachedAt (default = System.currentTimeMillis())
                    // so they appear first via ORDER BY cachedAt DESC in the DAO.
                    // Only clear remote keys so the virtual index chain restarts at 0.
                    remoteKeysDao.clearRemoteKeys()
                }

                val prevKey = if (virtualIndex == 0) null else virtualIndex - 1
                val nextKey = if (endOfPaginationReached) null else virtualIndex + 1

                // Store keys for ALL raw items so the chain survives people-filtering
                val keys = rawItems.map {
                    WallpaperRemoteKeys(
                        wallpaperId = it.id,
                        prevKey     = prevKey,
                        currentPage = virtualIndex,
                        nextKey     = nextKey
                    )
                }

                remoteKeysDao.insertAll(keys)
                wallpaperDao.insertWallpapers(cleanItems)
            }

            Timber.d(
                "Feed vIdx=$virtualIndex page=$apiPage seed=$apiSeed base=$globalSeedBase " +
                "raw=${rawItems.size} clean=${cleanItems.size} eop=$endOfPaginationReached"
            )
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)

        } catch (e: Exception) {
            Timber.e(e, "RemoteMediator error base=$globalSeedBase")
            MediatorResult.Error(e)
        }
    }

    // ── Remote key helpers ────────────────────────────────────────────────────

    private suspend fun getRemoteKeyForLastItem(
        state: PagingState<Int, WallpaperEntity>
    ): WallpaperRemoteKeys? =
        state.pages.lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { remoteKeysDao.remoteKeysById(it.id) }
}
