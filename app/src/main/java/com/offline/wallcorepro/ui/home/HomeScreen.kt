package com.offline.wallcorepro.ui.home

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.offline.wallcorepro.ads.AdsManager
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.domain.model.WallpaperCategory
import com.offline.wallcorepro.ui.components.HeroWallpaperCard
import com.offline.wallcorepro.ui.components.ShimmerWallpaperCard
import com.offline.wallcorepro.ui.components.WallpaperCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Staggered card heights for Pinterest-like layout
private val cardHeightCycle = listOf(250.dp, 200.dp, 235.dp, 215.dp, 265.dp)

// Emoji map per category name
private val categoryEmojiMap = mapOf(
    "Morning Greetings"  to "☀️",
    "Afternoon Wishes"   to "🌤️",
    "Evening Greetings"  to "🌅",
    "Night Wishes"       to "🌙",
    "Morning Love"       to "💕",
    "Afternoon Love"     to "🌤️",
    "Evening Love"       to "🌅",
    "Nightly Love"       to "💫",
    "Family Wishes"      to "👨‍👩‍👧",
    "Friends Greetings"  to "🤝",
    "Daily Blessings"    to "🙏",
    "Spiritual Morning"  to "✨",
    "Sweet Night Dreams" to "😴",
    "Romantic Greetings" to "🌹"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialCategory: String? = null,
    onWallpaperClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onAiClick: () -> Unit,
    navUiStateHolder: com.offline.wallcorepro.ui.navigation.NavUiStateHolder? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val wallpapers = viewModel.wallpapersFeed.collectAsLazyPagingItems()

    // ── Empty-state debounce ──────────────────────────────────────────────
    // During pager transitions (wrapAroundScroll / seed change) itemCount
    // briefly hits 0 with refresh=NotLoading. Wait 400 ms before committing
    // to the empty-state so those flashes show shimmer instead.
    var showEmptyState by remember { mutableStateOf(false) }
    LaunchedEffect(wallpapers.itemCount) {
        if (wallpapers.itemCount == 0) {
            delay(400)
            showEmptyState = true
        } else {
            showEmptyState = false
        }
    }

    // ── Infinite scroll restart ───────────────────────────────────────────────
    // Paging 3 permanently stops calling APPEND once endOfPaginationReached=true.
    // We watch for that condition and emit a new sessionSeed in the ViewModel,
    // which causes flatMapLatest to cancel the old Pager and create a fresh one
    // starting at a different backend page range — seamless infinite scroll.
    val appendState = wallpapers.loadState.append
    LaunchedEffect(appendState) {
        if (appendState is androidx.paging.LoadState.NotLoading &&
            appendState.endOfPaginationReached
        ) {
            viewModel.wrapAroundScroll()
        }
    }

    // ── User-initiated refresh tracking ─────────────────────────────────────
    // Stays true from the moment the user pulls/taps refresh until the pager
    // finishes its network load — keeps the spinner visible for the full cycle.
    var isUserRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(wallpapers.loadState.refresh) {
        if (wallpapers.loadState.refresh is LoadState.NotLoading) {
            isUserRefreshing = false
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showSearch  by remember { mutableStateOf(false) }

    // Notify ViewModel whenever the query changes (debounce via the Flow in VM)
    LaunchedEffect(searchQuery) { viewModel.onSearch(searchQuery) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialCategory) {
        initialCategory?.let { viewModel.selectCategory(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.clearNewBadge()
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    val context = LocalContext.current

    // Notify NavGraph when content is loaded (controls bottom banner visibility)
    LaunchedEffect(wallpapers.itemCount) {
        if (wallpapers.itemCount > 0) {
            navUiStateHolder?.markHomeContentLoaded()
        }
    }

    // ── Proactive image prefetch ──────────────────────────────────────────────
    // Every time the paging list grows (new page loaded from DB/network), we
    // immediately enqueue the thumbnail URLs of the freshly-added items into
    // Coil.  Coil checks its memory/disk cache first (O(1)) — already-cached
    // items cost nothing.  Only genuinely new URLs start a background download,
    // so by the time those cards scroll into the viewport the images are already
    // in the disk cache and appear instantly instead of loading on demand.
    val coilLoader = remember(context) { coil.Coil.imageLoader(context) }
    LaunchedEffect(wallpapers.itemCount) {
        if (wallpapers.itemCount == 0) return@LaunchedEffect
        // Prefetch the last (PAGE_SIZE * 3) items — 3 pages ahead.
        // Taking only the tail avoids re-enqueueing thousands of already-cached items
        // as the paging list grows to 2000+ entries.
        wallpapers.itemSnapshotList.items
            // 2 pages ahead is enough — shrinking from 3 avoids re-enqueueing
            // 150 items on every page load and reduces redundant cache lookups.
            .takeLast(AppConfig.PAGE_SIZE * 2)
            .forEach { wallpaper ->
                val url = wallpaper.thumbnailUrl.ifEmpty { wallpaper.imageUrl }
                if (url.isBlank()) return@forEach
                coilLoader.enqueue(
                    coil.request.ImageRequest.Builder(context)
                        .data(url)
                        .size(320, 480)
                        .memoryCacheKey(url)
                        .diskCacheKey(url)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        // Must match WallpaperCard: same bitmap config = same cache entry.
                        .allowRgb565(true)
                        .build()
                )
            }
    }

    val coroutineScope    = rememberCoroutineScope()
    val configuration     = LocalConfiguration.current
    // Material Design adaptive breakpoints:
    //   < 600 dp  → compact phone   → 2 columns
    //   600–839 dp → medium (foldable / small tablet / landscape phone) → 3 columns
    //   ≥ 840 dp  → expanded (large tablet) → 4 columns
    val gridColumns = when {
        configuration.screenWidthDp >= 840 -> 4
        configuration.screenWidthDp >= 600 -> 3
        else                               -> 2
    }
    val pullToRefreshState = rememberPullToRefreshState()
    val gridState = rememberLazyStaggeredGridState()
    var lastInterstitialScrollIndex by remember { mutableStateOf(0) }

    // Show scroll-to-top FAB only after the user has scrolled past the header items.
    // derivedStateOf ensures recomposition fires only when the boolean flips (not on
    // every scroll pixel), keeping this completely free of frame-budget impact.
    val showScrollToTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 3 } }

    // Interstitial on scroll — every N wallpapers to avoid OOM restart frustration
    LaunchedEffect(gridState.firstVisibleItemIndex) {
        if (!AppConfig.ADS_ENABLED) return@LaunchedEffect
        val idx = gridState.firstVisibleItemIndex
        val threshold = lastInterstitialScrollIndex + AppConfig.INTERSTITIAL_SCROLL_ITEMS
        if (idx >= threshold && idx > 0) {
            lastInterstitialScrollIndex = idx
            (context as? Activity)?.let {
                AdsManager.showInterstitialIfReady(it)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter   = scaleIn(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit    = scaleOut(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                    },
                    shape          = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                    elevation      = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Scroll to top"
                    )
                }
            }
        },
        topBar = {
            HomeTopBar(
                appName             = AppConfig.APP_NAME_SHORT,
                newCount            = uiState.newWallpaperCount,
                showSearch          = showSearch,
                searchQuery         = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearchToggle      = {
                    showSearch = !showSearch
                    if (!showSearch) { searchQuery = ""; viewModel.onSearch("") }
                },
                onRefresh           = {
                    isUserRefreshing = true
                    viewModel.refresh()
                    if (com.offline.wallcorepro.BuildConfig.DEBUG) {
                        val activity = context as? android.app.Activity
                        activity?.let { AdsManager.showInterstitialIfReady(it) }
                    }
                },
                onAiClick = onAiClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isUserRefreshing || uiState.isLoading,
            onRefresh = {
                isUserRefreshing = true
                viewModel.refresh()
            },
            state = pullToRefreshState,
            modifier = Modifier.padding(paddingValues),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state           = pullToRefreshState,
                    isRefreshing    = isUserRefreshing || uiState.isLoading,
                    modifier        = Modifier.align(Alignment.TopCenter),
                    containerColor  = MaterialTheme.colorScheme.primaryContainer,
                    color           = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            // ── Single staggered grid for everything — scroll naturally ──
            LazyVerticalStaggeredGrid(
                state                 = gridState,
                columns               = StaggeredGridCells.Fixed(gridColumns),
                modifier              = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing   = 8.dp,
                contentPadding        = PaddingValues(bottom = 16.dp)
            ) {

                // ── 0. Social Proof Banner (optional) ─────────────────────────
                if (AppConfig.FEATURE_SOCIAL_PROOF_BANNER) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        SocialProofBanner()
                    }
                }

                // ── 1. Animated Greeting Banner (Full Width) ───────────────
                if (AppConfig.FEATURE_TIME_GREETING) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        EnhancedGreetingBanner(
                            greeting    = uiState.greeting,
                            subtitle    = uiState.daySubtitle,
                            timeOfDay   = uiState.currentTimeOfDay,
                            wishStreak  = uiState.wishStreak
                        )
                    }
                }

                // ── 1b. Special Occasion Banner ────────────────────────────
                uiState.currentOccasion?.let { occasion ->
                    item(span = StaggeredGridItemSpan.FullLine) {
                        OccasionBanner(occasion = occasion)
                    }
                }

                // ── 1c. Recommended For You ────────────────────────────────
                if (AppConfig.FEATURE_RECOMMENDATION_ENGINE) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        RecommendedForYouCard(
                            timeOfDay  = uiState.currentTimeOfDay,
                            occasion   = uiState.currentOccasion,
                            onAiClick  = onAiClick
                        )
                    }
                }

                // ── 2. Hero Card / Today's Pick (Full Width) ───────────────
                uiState.heroWallpaper?.let { hero ->
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            HeroWallpaperCard(
                                wallpaper        = hero,
                                onClick          = { onWallpaperClick(hero.id) },
                                onFavoriteToggle = { w, fav -> viewModel.toggleFavorite(w.id, fav) }
                            )
                        }
                    }
                }

                // ── 3. Quote of the Day (Full Width) ──────────────────────
                item(span = StaggeredGridItemSpan.FullLine) {
                    HomeQuoteCard(quote = uiState.quote)
                }

                // ── 4. Tab Row (Full Width) ────────────────────────────────
                if (AppConfig.FEATURE_TRENDING) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        HomeTabRow(
                            selectedTab   = uiState.selectedTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                }

                // ── 5. Category Chips (Full Width) ─────────────────────────
                if (AppConfig.FEATURE_CATEGORIES && uiState.categories.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        CategoryChips(
                            categories         = uiState.categories,
                            selectedCategory   = uiState.selectedCategory,
                            onCategorySelected = { category ->
                                viewModel.selectCategory(
                                    if (uiState.selectedCategory == category) null else category
                                )
                            }
                        )
                    }
                }

                // ── 6. New Wallpapers Banner (Full Width) ──────────────────
                if (uiState.showNewBadge) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        NewWallpapersBanner(
                            count   = uiState.newWallpaperCount,
                            onClick = { viewModel.clearNewBadge() }
                        )
                    }
                }

                // ── 6b. Native Ad (Full Width) ─────────────────────────────
                // Only show when wallpapers are loaded — avoids "just ads" view
                // on first open while the server is waking up (cold start).
                if (AppConfig.ADS_ENABLED &&
                    com.offline.wallcorepro.util.RemoteConfigManager.nativeAdEnabled &&
                    wallpapers.itemCount > 0
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            com.offline.wallcorepro.ui.components.NativeAdCard()
                        }
                    }
                }

                // ── 7. Main Grid Content ───────────────────────────────────
                when (uiState.selectedTab) {
                    HomeTab.LATEST -> {
                        if (wallpapers.itemCount == 0) {
                            // Show shimmer while loading OR during the 400 ms debounce;
                            // only commit to the empty-state once both conditions clear.
                            if (wallpapers.loadState.refresh is LoadState.Loading || !showEmptyState) {
                                items(8) { i ->
                                    ShimmerWallpaperCard(
                                        height   = cardHeightCycle[i % cardHeightCycle.size],
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            } else {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    if (searchQuery.isNotBlank()) {
                                        SearchEmptyState(query = searchQuery, onClear = {
                                            searchQuery = ""; viewModel.onSearch("")
                                        })
                                    } else {
                                        EmptyWallpapersState(onRetry = { viewModel.refresh() })
                                    }
                                }
                            }
                        } else {
                            // Build a filtered promo list for round-robin rotation
                            val activePromos = AppConfig.PROMO_APPS.filter { it.isEnabled }

                            items(
                                count = wallpapers.itemCount,
                                key   = wallpapers.itemKey { it.id },
                                span  = { index ->
                                    val isNativeAd = AppConfig.ADS_ENABLED && com.offline.wallcorepro.util.RemoteConfigManager.nativeAdEnabled &&
                                                     (index + 1) % com.offline.wallcorepro.util.RemoteConfigManager.nativeAdInterval == 0
                                    val isInlineBanner = AppConfig.ADS_ENABLED && com.offline.wallcorepro.util.RemoteConfigManager.inlineBannerEnabled &&
                                                         (index + 1) % AppConfig.INLINE_BANNER_INTERVAL == 0
                                    val isPromoCard = AppConfig.FEATURE_CROSS_PROMOTION &&
                                                      activePromos.isNotEmpty() &&
                                                      AppConfig.PROMO_IN_FEED_INTERVAL > 0 &&
                                                      (index + 1) % AppConfig.PROMO_IN_FEED_INTERVAL == 0
                                    if (isNativeAd || isInlineBanner || isPromoCard) StaggeredGridItemSpan.FullLine
                                    else StaggeredGridItemSpan.SingleLane
                                }
                            ) { index ->
                                val isNativeAd = AppConfig.ADS_ENABLED && com.offline.wallcorepro.util.RemoteConfigManager.nativeAdEnabled &&
                                                 (index + 1) % com.offline.wallcorepro.util.RemoteConfigManager.nativeAdInterval == 0
                                val isInlineBanner = AppConfig.ADS_ENABLED && com.offline.wallcorepro.util.RemoteConfigManager.inlineBannerEnabled &&
                                                     (index + 1) % AppConfig.INLINE_BANNER_INTERVAL == 0
                                val isPromoCard = AppConfig.FEATURE_CROSS_PROMOTION &&
                                                  activePromos.isNotEmpty() &&
                                                  AppConfig.PROMO_IN_FEED_INTERVAL > 0 &&
                                                  (index + 1) % AppConfig.PROMO_IN_FEED_INTERVAL == 0

                                when {
                                    isNativeAd -> Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        com.offline.wallcorepro.ui.components.NativeAdCard()
                                    }
                                    isInlineBanner -> Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        com.offline.wallcorepro.ui.components.InlineAdaptiveBannerAd()
                                    }
                                    isPromoCard -> {
                                        // Round-robin through the promo apps list
                                        val promoIndex = ((index + 1) / AppConfig.PROMO_IN_FEED_INTERVAL - 1)
                                        val promo = activePromos[promoIndex % activePromos.size]
                                        com.offline.wallcorepro.ui.components.PromoAppCard(app = promo)
                                    }
                                    else -> {
                                        val wallpaper = wallpapers[index]
                                        if (wallpaper != null) {
                                            WallpaperCard(
                                                wallpaper          = wallpaper,
                                                cardHeight         = cardHeightCycle[index % cardHeightCycle.size],
                                                onClick            = { onWallpaperClick(wallpaper.id) },
                                                onFavoriteToggle   = { w, fav -> viewModel.toggleFavorite(w.id, fav) },
                                                selectedCategories = uiState.selectedQuoteCategories
                                            )
                                        } else {
                                            ShimmerWallpaperCard(
                                                height   = cardHeightCycle[index % cardHeightCycle.size],
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HomeTab.TRENDING -> {
                        items(
                            items = uiState.trendingWallpapers,
                            key   = { it.id }
                        ) { wallpaper ->
                            WallpaperCard(
                                wallpaper          = wallpaper,
                                cardHeight         = cardHeightCycle[uiState.trendingWallpapers.indexOf(wallpaper) % cardHeightCycle.size],
                                onClick            = { onWallpaperClick(wallpaper.id) },
                                onFavoriteToggle   = { w, fav -> viewModel.toggleFavorite(w.id, fav) },
                                showTrendingBadge  = true,
                                selectedCategories = uiState.selectedQuoteCategories
                            )
                        }
                    }
                }

                // Load more shimmer
                if (wallpapers.loadState.append is LoadState.Loading) {
                    items(2) { i ->
                        ShimmerWallpaperCard(
                            height   = if (i % 2 == 0) 220.dp else 250.dp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Social Proof Banner ────────────────────────────────────────────────────

@Composable
private fun SocialProofBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("✨", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Join 50,000+ users sharing daily wishes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
            )
        }
    }
}

// ─── Enhanced Animated Greeting Banner ───────────────────────────────────────

@Composable
private fun EnhancedGreetingBanner(
    greeting: String,
    subtitle: String,
    timeOfDay: AppConfig.TimeOfDay,
    wishStreak: Int = 0
) {
    // Live clock – updates every minute
    var currentTime by remember { mutableStateOf(getLiveTime()) }
    var dayLabel    by remember { mutableStateOf(getDayLabel()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            currentTime = getLiveTime()
            dayLabel    = getDayLabel()
        }
    }

    val infiniteAnim = rememberInfiniteTransition(label = "banner")
    val emojiOffsetY by infiniteAnim.animateFloat(
        initialValue = 0f,
        targetValue  = -12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_float"
    )
    val glowAlpha by infiniteAnim.animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.60f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val gradientBrush = when (timeOfDay) {
        AppConfig.TimeOfDay.MORNING   -> Brush.linearGradient(listOf(Color(0xFFFF8F00), Color(0xFFFF6F00), Color(0xFFE65100)))
        AppConfig.TimeOfDay.AFTERNOON -> Brush.linearGradient(listOf(Color(0xFFFFB74D), Color(0xFFFFA726), Color(0xFFFF9800)))
        AppConfig.TimeOfDay.EVENING   -> Brush.linearGradient(listOf(Color(0xFF7B1FA2), Color(0xFF512DA8), Color(0xFF311B92)))
        AppConfig.TimeOfDay.NIGHT     -> Brush.linearGradient(listOf(Color(0xFF0D1B3E), Color(0xFF1A1035), Color(0xFF2D1B69)))
    }
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.68f)) {
                Text(
                    text       = currentTime,
                    style      = MaterialTheme.typography.displaySmall,
                    color      = Color.White,
                    fontWeight = FontWeight.Black,
                    lineHeight = 40.sp
                )
                Text(
                    text  = "$dayLabel  ·  ${timeOfDay.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text       = greeting,
                    style      = MaterialTheme.typography.titleMedium,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f)
                )
                if (wishStreak >= 2) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.30f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("🔥", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text  = "$wishStreak day wish streak!",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(0.32f)
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text     = timeOfDay.emoji,
                    fontSize = 58.sp,
                    modifier = Modifier.offset(y = emojiOffsetY.dp)
                )
            }
        }
    }
}

// ─── Recommended For You Card ────────────────────────────────────────────────

@Composable
private fun RecommendedForYouCard(
    timeOfDay: AppConfig.TimeOfDay,
    occasion: AppConfig.AppOccasion?,
    onAiClick: () -> Unit
) {
    val cal = java.util.Calendar.getInstance()
    val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
    val isDaytime = timeOfDay == AppConfig.TimeOfDay.MORNING || timeOfDay == AppConfig.TimeOfDay.AFTERNOON
    val categoryKey = when (timeOfDay) {
        AppConfig.TimeOfDay.MORNING   -> "good_morning"
        AppConfig.TimeOfDay.AFTERNOON -> "good_afternoon"
        AppConfig.TimeOfDay.EVENING   -> "good_evening"
        AppConfig.TimeOfDay.NIGHT     -> "good_night"
    }

    // Determine the best contextual recommendation
    val isPeakShare = AppConfig.isPeakShareTime()
    val (headline, reason, quote) = when {
        isPeakShare && occasion == null -> Triple(
            "✨ Perfect Time to Send a Wish!",
            "People love sharing at this hour — make someone's day",
            com.offline.wallcorepro.data.local.WishQuotePool.getNextQuote(
                AppConfig.QuoteCategory.defaults, categoryKey)
        )
        occasion != null -> Triple(
            "✨ Perfect for ${occasion.name.substringBefore("!")}",
            "Today is a special day — make it unforgettable",
            com.offline.wallcorepro.data.local.WishQuotePool.getOccasionQuote(occasion.key)
        )
        dow == java.util.Calendar.MONDAY && isDaytime -> Triple(
            "💪 Monday Motivation Picked for You",
            "Start your week with the right energy",
            com.offline.wallcorepro.data.local.WishQuotePool.getDayOfWeekQuote("monday_seed")
        )
        dow == java.util.Calendar.FRIDAY -> Triple(
            "🎉 It's Friday — Celebrate!",
            "The most shared day of the week",
            com.offline.wallcorepro.data.local.WishQuotePool.getDayOfWeekQuote("friday_seed")
        )
        !isDaytime -> Triple(
            "🌙 Tonight's Wish, Just for You",
            "Most opened at this hour by people like you",
            com.offline.wallcorepro.data.local.WishQuotePool.getNextQuote(
                AppConfig.QuoteCategory.defaults, "good_night")
        )
        else -> Triple(
            "${timeOfDay.emoji} ${timeOfDay.displayName}'s Top Pick",
            "Based on what people love sending right now",
            com.offline.wallcorepro.data.local.WishQuotePool.getNextQuote(
                AppConfig.QuoteCategory.defaults, categoryKey)
        )
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        if (isDaytime)
                            listOf(Color(0xFF1B2A4A), Color(0xFF0D3B2E))
                        else
                            listOf(Color(0xFF1A1035), Color(0xFF0D1B3E))
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7C4DFF).copy(alpha = 0.25f)
                    ) {
                        Text(
                            text     = "Recommended for You",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color(0xFFD4BFFF),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text       = headline,
                    style      = MaterialTheme.typography.titleSmall,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text       = "\"$quote\"",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onAiClick,
                        shape   = RoundedCornerShape(14.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Use WishAI →", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Special Occasion Banner ─────────────────────────────────────────────────

@Composable
private fun OccasionBanner(occasion: AppConfig.AppOccasion) {
    val infiniteAnim = rememberInfiniteTransition(label = "occasion")
    val shimmerAlpha by infiniteAnim.animateFloat(
        initialValue  = 0.85f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    val emojiScale by infiniteAnim.animateFloat(
        initialValue  = 1.0f,
        targetValue   = 1.18f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_pulse"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(occasion.gradientStart).copy(alpha = shimmerAlpha),
                            Color(occasion.gradientEnd).copy(alpha = shimmerAlpha)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier             = Modifier.fillMaxWidth()
            ) {
                Text(
                    text     = occasion.emoji,
                    fontSize = (40 * emojiScale).sp
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text       = occasion.name,
                        style      = MaterialTheme.typography.titleMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text  = "Special wallpapers & wishes for today",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text     = occasion.emoji,
                    fontSize = (40 * emojiScale).sp
                )
            }
        }
    }
}

private fun getLiveTime(): String {
    val cal    = java.util.Calendar.getInstance()
    val hour   = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = cal.get(java.util.Calendar.MINUTE)
    return "%02d:%02d".format(hour, minute)
}

private fun getDayLabel(): String {
    val sdf = java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault())
    return sdf.format(java.util.Calendar.getInstance().time)
}

// ─── Home Quote Card ──────────────────────────────────────────────────────────

@Composable
private fun HomeQuoteCard(quote: AppConfig.Quote) {
    val context          = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.FormatQuote,
                        null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = "Daily Wish",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                    // Copy
                    IconButton(
                        onClick  = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(quote.text))
                            android.widget.Toast.makeText(context, "Copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            null,
                            tint     = MaterialTheme.colorScheme.primary.copy(0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    // Share
                    IconButton(
                        onClick  = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "${quote.text}\n\n— Sent via ${AppConfig.APP_NAME}")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Wish"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            null,
                            tint     = MaterialTheme.colorScheme.primary.copy(0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "\"${quote.text}\"",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontStyle  = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = "— ${quote.author}",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    appName: String,
    newCount: Int,
    showSearch: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onRefresh: () -> Unit,
    onAiClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            if (showSearch) {
                OutlinedTextField(
                    value              = searchQuery,
                    onValueChange      = onSearchQueryChange,
                    placeholder        = { Text("Search wallpapers…") },
                    singleLine         = true,
                    modifier           = Modifier.fillMaxWidth(0.95f),
                    shape              = RoundedCornerShape(28.dp),
                    colors             = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = appName,
                        style      = MaterialTheme.typography.headlineMedium,
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (newCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(
                                text  = if (newCount > 99) "99+" else "$newCount",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        },
        actions = {
            if (AppConfig.FEATURE_SEARCH) {
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector        = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
            }
            IconButton(onClick = onAiClick) {
                Icon(
                    imageVector        = Icons.Default.AutoAwesome,
                    contentDescription = "WishMagic AI",
                    tint               = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// ─── Tab Row ──────────────────────────────────────────────────────────────────

@Composable
private fun HomeTabRow(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor   = Color.Transparent,
        contentColor     = MaterialTheme.colorScheme.primary,
        modifier         = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        divider          = {}
    ) {
        HomeTab.values().forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick  = { onTabSelected(tab) },
                text = {
                    Text(
                        text       = when (tab) {
                            HomeTab.LATEST   -> "✨ Discover"
                            HomeTab.TRENDING -> "🔥 Popular"
                        },
                        fontWeight = if (selectedTab == tab) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            )
        }
    }
}

// ─── Category Chips with Emojis ───────────────────────────────────────────────

@Composable
private fun CategoryChips(
    categories: List<WallpaperCategory>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val emoji     = categoryEmojiMap[category.name] ?: "🖼️"
            val isSelected = selectedCategory == category.name
            FilterChip(
                selected = isSelected,
                onClick  = { onCategorySelected(category.name) },
                label    = {
                    Text(
                        text       = "$emoji  ${category.name}",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                shape  = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                    containerColor         = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = if (isSelected) null else FilterChipDefaults.filterChipBorder(
                    borderColor        = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = Color.Transparent,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

// ─── New Wallpapers Banner ────────────────────────────────────────────────────

@Composable
private fun NewWallpapersBanner(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.NewReleases, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(
                text       = "$count fresh wallpapers are waiting! Refresh now.",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier   = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun SearchEmptyState(query: String, onClear: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🔍", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text       = "No results for \"$query\"",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Try a different keyword like \"morning love\", \"good night nature\" or a category name.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onClear, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Clear Search")
        }
    }
}

@Composable
private fun EmptyWallpapersState(onRetry: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🖼️", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text       = "No Wallpapers Found",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Couldn't load wallpapers. Check your internet connection or API key.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape   = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

