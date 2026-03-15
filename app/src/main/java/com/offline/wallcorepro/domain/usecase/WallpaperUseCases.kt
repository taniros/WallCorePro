package com.offline.wallcorepro.domain.usecase

import androidx.paging.PagingData
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.domain.model.WallpaperCategory
import com.offline.wallcorepro.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWallpapersFeedUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    operator fun invoke(
        niche: String = AppConfig.NICHE_TYPE,
        seed: Int = 0
    ): Flow<PagingData<Wallpaper>> {
        return repository.getWallpapersFeed(niche, seed)
    }
}

class GetTrendingWallpapersUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    operator fun invoke(niche: String = AppConfig.NICHE_TYPE): Flow<List<Wallpaper>> {
        return repository.getTrendingWallpapers(niche)
    }
}

class GetWallpapersByCategoryUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    operator fun invoke(niche: String, category: String): Flow<PagingData<Wallpaper>> {
        return repository.getWallpapersByCategory(niche, category)
    }
}

class SearchWallpapersUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    operator fun invoke(query: String, niche: String = AppConfig.NICHE_TYPE): Flow<PagingData<Wallpaper>> {
        return repository.searchWallpapers(niche, query)
    }
}

class GetWallpaperByIdUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(id: String): Wallpaper? {
        return repository.getWallpaperById(id)
    }
}

class GetFavoriteWallpapersUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    operator fun invoke(): Flow<List<Wallpaper>> {
        return repository.getFavoriteWallpapers()
    }
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(id: String, isFavorite: Boolean) {
        repository.toggleFavorite(id, isFavorite)
    }
}

class GetCategoriesUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    operator fun invoke(niche: String = AppConfig.NICHE_TYPE): Flow<List<WallpaperCategory>> {
        return repository.getCategories(niche)
    }
}

class SyncWallpapersUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(niche: String = AppConfig.NICHE_TYPE, force: Boolean = false): Result<Int> {
        return repository.syncWallpapers(niche, force)
    }
}

/** Sync trending from /v1/trending so Today's Pick has data even when main feed is empty */
class SyncTrendingForHeroUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(niche: String = AppConfig.NICHE_TYPE): Result<Int> {
        return repository.syncTrendingForHero(niche)
    }
}

class SyncCategoryWallpapersUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(category: String, niche: String = AppConfig.NICHE_TYPE, force: Boolean = false): Result<Int> {
        return repository.syncCategoryWallpapers(niche, category, force)
    }
}

class GetNewWallpaperCountUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(niche: String = AppConfig.NICHE_TYPE, since: Long): Int {
        return repository.getNewWallpaperCount(niche, since)
    }
}

class GetRandomWallpaperUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(niche: String = AppConfig.NICHE_TYPE): Wallpaper? {
        return repository.getRandomWallpaper(niche)
    }
}

class IncrementDownloadUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(id: String) {
        repository.incrementDownloadCount(id)
    }
}

class RefreshCategoriesUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(niche: String = AppConfig.NICHE_TYPE) {
        repository.refreshCategories(niche)
    }
}

class WarmUpFeedUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(niche: String = AppConfig.NICHE_TYPE, globalSeedBase: Int) {
        repository.warmUpFeed(niche, globalSeedBase)
    }
}

class WarmUpCategoriesUseCase @Inject constructor(
    private val repository: WallpaperRepository
) {
    suspend operator fun invoke(niche: String = AppConfig.NICHE_TYPE) {
        repository.warmUpCategories(niche)
    }
}
