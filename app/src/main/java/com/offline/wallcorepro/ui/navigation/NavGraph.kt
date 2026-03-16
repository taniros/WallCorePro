package com.offline.wallcorepro.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.ui.components.BannerAdView
import com.offline.wallcorepro.ui.components.ExitConfirmationDialog
import com.offline.wallcorepro.ui.components.InternetNoticeBannerFromContext
import com.offline.wallcorepro.ui.ai.AiScreen
import com.offline.wallcorepro.ui.detail.DetailScreen
import com.offline.wallcorepro.ui.favorites.FavoritesScreen
import com.offline.wallcorepro.ui.home.HomeScreen
import com.offline.wallcorepro.ui.legal.PrivacyPolicyScreen
import com.offline.wallcorepro.ui.legal.TermsOfServiceScreen
import com.offline.wallcorepro.ui.onboarding.OnboardingScreen
import com.offline.wallcorepro.ui.onboarding.OnboardingViewModel
import com.offline.wallcorepro.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home       : Screen("home")
    object Favorites  : Screen("favorites")
    object Settings   : Screen("settings")
    object Detail     : Screen("detail/{wallpaperId}") {
        fun createRoute(wallpaperId: String) = "detail/$wallpaperId"
    }
    object Category : Screen("category/{categoryName}") {
        fun createRoute(categoryName: String) = "category/$categoryName"
    }
    object Ai            : Screen("ai")
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsOfService: Screen("terms_of_service")
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route,      "Discover", Icons.Default.Home,          Icons.Default.Home),
    BottomNavItem(Screen.Favorites.route, "Saved",    Icons.Default.FavoriteBorder, Icons.Default.Favorite),
    BottomNavItem(Screen.Ai.route,        "WishAI",   Icons.Default.AutoAwesome,    Icons.Default.AutoAwesome),
    BottomNavItem(Screen.Settings.route,  "Settings", Icons.Default.Settings,       Icons.Default.Settings)
)

@Composable
fun WallCoreNavGraph(navController: NavHostController) {

    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isOnboardingDone by onboardingViewModel.isOnboardingDone.collectAsStateWithLifecycle(initialValue = null)

    if (isOnboardingDone == null) return

    val startDestination = if (isOnboardingDone == true) Screen.Home.route else Screen.Onboarding.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mainRoutes = remember { setOf(Screen.Home.route, Screen.Favorites.route, Screen.Ai.route, Screen.Settings.route) }
    val showBottomNav = currentRoute in mainRoutes

    // ── Exit dialog state ───────────────────────────────────────────────────
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Intercept back press on any root (main tab) screen
    BackHandler(enabled = showBottomNav) {
        showExitDialog = true
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onDismiss = { showExitDialog = false },
            onExit    = { (context as? Activity)?.finish() }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        InternetNoticeBannerFromContext()
        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter   = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(200)),
                exit    = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(tween(200))
            ) {
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    // Only show banner ad when ads are enabled AND remote config allows it
                    if (AppConfig.ADS_ENABLED && com.offline.wallcorepro.util.RemoteConfigManager.bannerAdEnabled) {
                        BannerAdView(modifier = Modifier.fillMaxWidth())
                    }
                    WallCoreBottomNavBar(
                        items        = bottomNavItems,
                        currentRoute = currentRoute,
                        onNavigate   = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(paddingValues)
        ) {
            // Onboarding (full-screen, no bottom nav)
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        onboardingViewModel.completeOnboarding()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home
            composable(Screen.Home.route) {
                HomeScreen(
                    onWallpaperClick = { navController.navigate(Screen.Detail.createRoute(it)) },
                    onCategoryClick  = { navController.navigate(Screen.Category.createRoute(it)) },
                    onAiClick = {
                        navController.navigate(Screen.Ai.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }

            // Favorites
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onWallpaperClick = { navController.navigate(Screen.Detail.createRoute(it)) },
                    onBackClick      = { navController.popBackStack() }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick           = { navController.popBackStack() },
                    onPrivacyPolicyClick  = { navController.navigate(Screen.PrivacyPolicy.route) },
                    onTermsOfServiceClick = { navController.navigate(Screen.TermsOfService.route) }
                )
            }

            // Privacy Policy (internal page)
            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen(onBackClick = { navController.popBackStack() })
            }

            // Terms of Service (internal page)
            composable(Screen.TermsOfService.route) {
                TermsOfServiceScreen(onBackClick = { navController.popBackStack() })
            }

            // Detail (full-screen, no bottom nav)
            composable(
                route     = Screen.Detail.route,
                arguments = listOf(navArgument("wallpaperId") { type = NavType.StringType })
            ) {
                DetailScreen(onBackClick = { navController.popBackStack() })
            }

            // Category filter view (reuses HomeScreen with pre-selected category)
            composable(
                route     = Screen.Category.route,
                arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("categoryName") ?: ""
                HomeScreen(
                    initialCategory  = category,
                    onWallpaperClick = { navController.navigate(Screen.Detail.createRoute(it)) },
                    onCategoryClick  = { navController.navigate(Screen.Category.createRoute(it)) },
                    onAiClick = {
                        navController.navigate(Screen.Ai.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }

            // WishMagic AI
            composable(Screen.Ai.route) {
                AiScreen(onBackClick = { navController.popBackStack() })
            }
        }
        }
    }
}

// ─── Beautiful Bottom Navigation Bar ─────────────────────────────────────────

@Composable
private fun WallCoreBottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick  = { onNavigate(item.route) },
                icon = {
                    AnimatedContent(
                        targetState  = selected,
                        transitionSpec = {
                            (scaleIn(tween(200)) + fadeIn(tween(200))) togetherWith
                            (scaleOut(tween(200)) + fadeOut(tween(200)))
                        },
                        label = "nav_icon_${item.label}"
                    ) { isSelected ->
                        Icon(
                            imageVector        = if (isSelected) item.selectedIcon else item.icon,
                            contentDescription = item.label
                        )
                    }
                },
                label = {
                    Text(
                        text       = item.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
