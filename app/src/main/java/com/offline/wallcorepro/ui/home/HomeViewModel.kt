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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val getWallpapersByCategoryUseCase: GetWallpapersByCategoryUseCase,
    private val searchWallpapersUseCase: SearchWallpapersUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val syncWallpapersUseCase: SyncWallpapersUseCase,
    private val syncCategoryWallpapersUseCase: SyncCategoryWallpapersUseCase,
    private val refreshCategoriesUseCase: RefreshCategoriesUseCase,
    private val getNewWallpaperCountUseCase: GetNewWallpaperCountUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _searchQuery      = MutableStateFlow("")

    // Main paged feed — priority: search query > category filter > all-feed
    @OptIn(ExperimentalCoroutinesApi::class)
    val wallpapersFeed: Flow<PagingData<Wallpaper>> =
        combine(_searchQuery, _selectedCategory) { q, cat -> Pair(q, cat) }
        .flatMapLatest { (query, category) ->
            when {
                query.isNotBlank() -> searchWallpapersUseCase(query.trim(), AppConfig.NICHE_TYPE)
                category != null   -> getWallpapersByCategoryUseCase(AppConfig.NICHE_TYPE, category)
                else               -> getWallpapersFeedUseCase(AppConfig.NICHE_TYPE)
            }
        }
        .cachedIn(viewModelScope)

    fun onSearch(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    init {
        loadCategories()
        loadTrending()
        checkNewWallpapers()
        initialSync()
        observeCategories()
        observeQuoteCategories()
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            refreshCategoriesUseCase()
            val result = syncWallpapersUseCase(force = false)
            _uiState.update { it.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message
            ) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            refreshCategoriesUseCase()
            val result = syncWallpapersUseCase(force = true)
            checkNewWallpapers()
            // Refresh time-based greeting on manual pull-to-refresh
            _uiState.update {
                it.copy(
                    isLoading        = false,
                    error            = result.exceptionOrNull()?.message,
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

    private fun loadTrending() {
        viewModelScope.launch {
            getTrendingWallpapersUseCase(AppConfig.NICHE_TYPE).collect { trending ->
                _uiState.update { it.copy(
                    trendingWallpapers = trending,
                    heroWallpaper      = trending.firstOrNull() ?: it.heroWallpaper
                ) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase(AppConfig.NICHE_TYPE).collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            getCategoriesUseCase(AppConfig.NICHE_TYPE).collect { categories ->
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

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun toggleFavorite(id: String, isFavorite: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(id, isFavorite)
        }
    }
}
