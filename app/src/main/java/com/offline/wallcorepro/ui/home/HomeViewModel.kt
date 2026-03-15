package com.offline.wallcorepro.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.repository.PreferenceManager
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.domain.model.WallpaperCategory
import com.offline.wallcorepro.domain.usecase.*
import timber.log.Timber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean             = false,
    val newWallpaperCount: Int         = 0,
    val categories: List<WallpaperCategory> = emptyList(),
    val trendingWallpapers: List<Wallpaper> = emptyList(),
    val selectedCategory: String?      = null,
    val selectedTab: HomeTab           = HomeTab.LATEST,
    val error: String?                 = null,
    val showNewBadge: Boolean          = false,
    // Time-aware greeting (4 periods: Morning, Afternoon, Evening, Night)
    val greeting: String               = AppConfig.getGreeting(),
    val greetingSubtitle: String       = getSubtitle(),
    val currentTimeOfDay: AppConfig.TimeOfDay = AppConfig.TimeOfDay.current(),
    val isMorningTime: Boolean         = AppConfig.isMorningTime(), // backward compat: true for morning only
    // Featured content
    val heroWallpaper: Wallpaper?      = null,
    val quote: AppConfig.Quote         = AppConfig.getQuoteOfDay(),
    // User's selected quote categories — passed to WallpaperCard for stable quote matching
    val selectedQuoteCategories: Set<String> = AppConfig.QuoteCategory.defaults,
    // Today's special occasion (Valentine's Day, Christmas, etc.) — null when none
    val currentOccasion: AppConfig.AppOccasion? = AppConfig.getCurrentOccasion(),
    // Personalised day-of-week subtitle (e.g. "Happy Friday! Weekend soon 🎉")
    val daySubtitle: String = AppConfig.getGreetingSubtitle(),
    // Consecutive days the user has shared/sent wishes
    val wishStreak: Int = 0,
    // Active search query — empty means no search active
    val searchQuery: String = ""
)

enum class HomeTab { LATEST, TRENDING }

private fun getSubtitle(): String = when (AppConfig.TimeOfDay.current()) {
    AppConfig.TimeOfDay.MORNING   -> "Beautiful morning greetings just for you ☀️"
    AppConfig.TimeOfDay.AFTERNOON -> "Sunny afternoon wishes to brighten your day 🌤️"
    AppConfig.TimeOfDay.EVENING   -> "Warm evening greetings for a peaceful night 🌅"
    AppConfig.TimeOfDay.NIGHT     -> "Heartfelt night wishes for sweet dreams 🌙"
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getWallpapersFeedUseCase: GetWallpapersFeedUseCase,
    private val getTrendingWallpapersUseCase: GetTrendingWallpapersUseCase,
    private val syncTrendingForHeroUseCase: SyncTrendingForHeroUseCase,
    private val getWallpapersByCategoryUseCase: GetWallpapersByCategoryUseCase,
    private val searchWallpapersUseCase: SearchWallpapersUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val syncWallpapersUseCase: SyncWallpapersUseCase,
    private val syncCategoryWallpapersUseCase: SyncCategoryWallpapersUseCase,
    private val refreshCategoriesUseCase: RefreshCategoriesUseCase,
    private val getNewWallpaperCountUseCase: GetNewWallpaperCountUseCase,
    private val getRandomWallpaperUseCase: GetRandomWallpaperUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val warmUpFeedUseCase: WarmUpFeedUseCase,
    private val warmUpCategoriesUseCase: WarmUpCategoriesUseCase,
    private val preferenceManager: PreferenceManager,
    private val appResumeNotifier: com.offline.wallcorepro.util.AppResumeNotifier,
    private val wallpaperRepository: com.offline.wallcorepro.domain.repository.WallpaperRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _searchQuery      = MutableStateFlow("")

    // Per-session seed — starts with a temporary random value and is immediately
    // replaced by the persistent global seed loaded from PreferenceManager in init{}.
    // The global seed advances by SEED_ADVANCE_PER_SESSION each session so the
    // backend never returns the same query cluster twice in a row.
    private val _sessionSeed = MutableStateFlow((0..9999).random())

    // Main paged feed — priority: search query > category filter > main feed.
    // Changing _sessionSeed creates a brand-new Pager + RemoteMediator with the
    // new seed, giving fresh content immediately without clearing the local cache.
    @OptIn(ExperimentalCoroutinesApi::class)
    val wallpapersFeed: Flow<PagingData<Wallpaper>> =
        combine(_searchQuery, _selectedCategory, _sessionSeed) { q, cat, seed ->
            Triple(q, cat, seed)
        }
        .flatMapLatest { (query, category, seed) ->
            when {
                query.isNotBlank() -> searchWallpapersUseCase(query.trim(), AppConfig.NICHE_TYPE)
                category != null   -> getWallpapersByCategoryUseCase(AppConfig.NICHE_TYPE, category)
                else               -> getWallpapersFeedUseCase(AppConfig.NICHE_TYPE, seed)
            }
        }
        .cachedIn(viewModelScope)

    fun onSearch(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    init {
        viewModelScope.launch {
            // ① Snapshot session start so we can prune only PRE-session items later.
            val sessionStartMs = System.currentTimeMillis()
            val needsCacheClear = AppConfig.CACHE_CLEAR_ON_EXIT &&
                                  preferenceManager.getPendingCacheClear()

            // ② Advance and apply the persistent seed IMMEDIATELY — before any network
            //   call — so the Pager starts at once and shows whatever is in the DB.
            //   On returning sessions the user sees the previous content instantly
            //   while the warm-up fetches fresh items in the background.
            val persistentSeed = preferenceManager.getAndAdvanceGlobalFeedSeed()
            _sessionSeed.value = persistentSeed
            Timber.d("HomeViewModel: seed=$persistentSeed needsClear=$needsCacheClear")

            // ③ Non-blocking age-based housekeeping.
            launch {
                val cutoff = sessionStartMs - AppConfig.CACHE_MAX_AGE_DAYS * 24 * 3600 * 1000L
                wallpaperRepository.cleanOldCache(AppConfig.NICHE_TYPE, cutoff)
            }

            // ④ PARALLEL backend tasks — categories and trending fire at the same time.
            //   The first request to reach the sleeping Render server wakes it; all
            //   others benefit immediately.  Neither blocks the Pager or each other.
            launch { try { refreshCategoriesUseCase()   } catch (e: Exception) { Timber.w(e, "Categories refresh failed") } }
            launch { try { syncTrendingForHeroUseCase() } catch (e: Exception) { Timber.w(e, "Trending sync failed") } }

            // ⑤ Warm-up runs in its own independent coroutine so it never blocks the
            //   Pager or the categories/trending tasks above.
            //   Cache is pruned ONLY if warm-up succeeds — prevents leaving DB empty
            //   when the server is completely unreachable.
            launch {
                var warmUpOk = false
                try {
                    warmUpFeedUseCase(globalSeedBase = persistentSeed)
                    warmUpOk = true
                } catch (e: Exception) { Timber.w(e, "WarmUp failed (non-critical)") }

                // ⑥ Prune stale content only after warm-up has filled the DB.
                //   Warm-up inserts have cachedAt >= sessionStartMs so they are kept.
                //   If warm-up failed we preserve existing cache for the next session.
                if (needsCacheClear && warmUpOk) {
                    wallpaperRepository.cleanOldCache(AppConfig.NICHE_TYPE, sessionStartMs)
                    preferenceManager.setPendingCacheClear(false)
                    Timber.d("HomeViewModel: pre-session items pruned, warm-up content kept")
                } else if (needsCacheClear) {
                    Timber.w("HomeViewModel: warm-up failed — preserving existing cache")
                }
            }

            // ⑦ Pre-fetch ALL categories in the background so every category screen
            //   is already populated before the user taps one.  Runs in its own
            //   coroutine so it never blocks the main feed warm-up or the Pager.
            launch {
                try { warmUpCategoriesUseCase() }
                catch (e: Exception) { Timber.w(e, "WarmUpCategories failed (non-critical)") }
            }
        }

        loadTrending()
        checkNewWallpapers()
        initialSync()
        observeCategories()
        observeQuoteCategories()
        observeAppResume()
    }

    private fun observeAppResume() {
        viewModelScope.launch {
            appResumeNotifier.appResumed.collect {
                refreshOnAppResume()
            }
        }
    }

    private fun refreshOnAppResume() {
        viewModelScope.launch {
            com.offline.wallcorepro.util.RemoteConfigManager.fetch()

            val sessionStartMs  = System.currentTimeMillis()
            val needsCacheClear = AppConfig.CACHE_CLEAR_ON_EXIT &&
                                  preferenceManager.getPendingCacheClear()

            // Advance seed FIRST so the Pager immediately shows existing DB content
            // while the warm-up runs in the background.
            val nextSeed = preferenceManager.getAndAdvanceGlobalFeedSeed()
            _sessionSeed.value = nextSeed

            // Parallel: categories and trending fire simultaneously.
            launch { try { refreshCategoriesUseCase()   } catch (e: Exception) { Timber.w(e, "Resume: categories failed") } }
            launch { try { syncTrendingForHeroUseCase() } catch (e: Exception) { Timber.w(e, "Resume: trending failed") } }

            // Warm-up in independent coroutine — prune only after it succeeds.
            launch {
                var warmUpOk = false
                try {
                    warmUpFeedUseCase(globalSeedBase = nextSeed)
                    warmUpOk = true
                } catch (e: Exception) { Timber.w(e, "WarmUp (resume) failed") }

                if (needsCacheClear && warmUpOk) {
                    wallpaperRepository.cleanOldCache(AppConfig.NICHE_TYPE, sessionStartMs)
                    preferenceManager.setPendingCacheClear(false)
                    Timber.d("refreshOnAppResume: pre-session items pruned, warm-up content kept")
                } else if (needsCacheClear) {
                    Timber.w("refreshOnAppResume: warm-up failed — preserving existing cache")
                }
            }

            // Re-fill any categories that got pruned since last session.
            launch {
                try { warmUpCategoriesUseCase() }
                catch (e: Exception) { Timber.w(e, "WarmUpCategories (resume) failed (non-critical)") }
            }

            checkNewWallpapers()
            _uiState.update {
                it.copy(
                    greeting         = AppConfig.getGreeting(),
                    greetingSubtitle = getSubtitle(),
                    daySubtitle      = AppConfig.getGreetingSubtitle(),
                    currentTimeOfDay = AppConfig.TimeOfDay.current(),
                    isMorningTime    = AppConfig.isMorningTime(),
                    currentOccasion  = AppConfig.getCurrentOccasion(),
                    quote            = AppConfig.getQuoteOfDay()
                )
            }
        }
    }

    private fun observeQuoteCategories() {
        viewModelScope.launch {
            preferenceManager.selectedQuoteCategories.collect { cats ->
                _uiState.update { it.copy(selectedQuoteCategories = cats) }
            }
        }
        viewModelScope.launch {
            preferenceManager.wishStreak.collect { streak ->
                _uiState.update { it.copy(wishStreak = streak) }
            }
        }
    }

    private fun initialSync() {
        // Categories are already refreshed in init {} above.
        // RemoteMediator.initialize() returns LAUNCH_INITIAL_REFRESH so the
        // paged feed handles the first wallpaper fetch with the correct seed.
        // Nothing else needed here.
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Advance the global seed so the new Pager queries a different backend cluster.
            // flatMapLatest creates a fresh Pager automatically — no orphan wallpapers.refresh() needed.
            val nextSeed = preferenceManager.getAndAdvanceGlobalFeedSeed()

            // Categories and trending fire in parallel (non-blocking for the seed advance).
            launch { try { refreshCategoriesUseCase()   } catch (e: Exception) { Timber.w(e, "refresh: categories failed") } }
            launch { try { syncTrendingForHeroUseCase() } catch (e: Exception) { Timber.w(e, "refresh: trending failed") } }

            // Apply seed AFTER launching background tasks so DB reads for the new
            // Pager start immediately with whatever is already cached.
            _sessionSeed.value = nextSeed

            checkNewWallpapers()
            _uiState.update {
                it.copy(
                    isLoading        = false,
                    error            = null,
                    greeting         = AppConfig.getGreeting(),
                    greetingSubtitle = getSubtitle(),
                    daySubtitle      = AppConfig.getGreetingSubtitle(),
                    currentTimeOfDay = AppConfig.TimeOfDay.current(),
                    isMorningTime    = AppConfig.isMorningTime(),
                    currentOccasion  = AppConfig.getCurrentOccasion()
                )
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun loadTrending() {
        viewModelScope.launch {
            getTrendingWallpapersUseCase(AppConfig.NICHE_TYPE)
                .distinctUntilChanged { a, b -> a.map { it.id }.toSet() == b.map { it.id }.toSet() }
                .debounce(400)
                .collect { trending ->
                    // Rotate "Today's Pick" daily — backend trending list is stable so
                    // firstOrNull() always returned the same image.  dayOfYear as the
                    // index gives a different hero every calendar day automatically.
                    val hero = if (trending.isNotEmpty()) {
                        val day = java.util.Calendar.getInstance()
                            .get(java.util.Calendar.DAY_OF_YEAR)
                        trending[day % trending.size]
                    } else {
                        _uiState.value.heroWallpaper
                            ?: getRandomWallpaperUseCase(AppConfig.NICHE_TYPE)
                    }
                    _uiState.update { it.copy(
                        trendingWallpapers = trending,
                        heroWallpaper      = hero
                    ) }
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeCategories() {
        viewModelScope.launch {
            getCategoriesUseCase(AppConfig.NICHE_TYPE)
                .distinctUntilChanged { a, b -> a.map { it.id }.toSet() == b.map { it.id }.toSet() }
                .debounce(400)
                .collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
        }
    }

    private fun checkNewWallpapers() {
        viewModelScope.launch {
            val lastViewed = preferenceManager.getLastViewedAt()
            val count = getNewWallpaperCountUseCase(since = lastViewed)
            _uiState.update {
                it.copy(
                    newWallpaperCount = count,
                    showNewBadge      = count > 0 && AppConfig.RC_SHOW_NEW_BADGE
                )
            }
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        _uiState.update { it.copy(selectedCategory = category) }
        category?.let { cat ->
            viewModelScope.launch {
                preferenceManager.incrementCategoryInteraction(cat)
                // Trigger an AI-driven sync for the specific category to ensure "Exactly" the right wallpapers
                _uiState.update { it.copy(isLoading = true) }
                syncCategoryWallpapersUseCase(cat)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectTab(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun clearNewBadge() {
        viewModelScope.launch {
            preferenceManager.setLastViewedAt(System.currentTimeMillis())
            _uiState.update { it.copy(newWallpaperCount = 0, showNewBadge = false) }
        }
    }

    // Timestamp of last wrap-around to prevent rapid-fire restarts when the
    // server is temporarily unreachable (2-second cooldown between restarts).
    // Note: with internal seed cycling in WallpaperRemoteMediator, this is only
    // reached after MAX_SEED_CYCLES (15) cycles — a very rare safety valve.
    private var lastWrapAroundMs = 0L

    /**
     * Called by HomeScreen when loadState.append.endOfPaginationReached becomes
     * true — i.e. the mediator exhausted all MAX_SEED_CYCLES internal cycles.
     * Emitting a new random seed causes flatMapLatest to cancel the old Pager
     * and create a fresh one starting at a different page range.
     * A 2-second cooldown prevents rapid restarts when the server is down.
     */
    fun wrapAroundScroll() {
        val now = System.currentTimeMillis()
        if (now - lastWrapAroundMs < 2_000L) return
        lastWrapAroundMs = now
        // Use a random wrap-around seed (not the persistent counter — that is reserved
        // for session starts to ensure cross-session uniqueness; intra-session cycling
        // just needs any seed different enough from the current one).
        _sessionSeed.value = (0..9999).random()
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun toggleFavorite(id: String, isFavorite: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(id, isFavorite)
        }
    }
}
