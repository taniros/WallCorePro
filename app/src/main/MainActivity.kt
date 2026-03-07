package com.offline.wallcorepro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.ui.navigation.Screen
import com.offline.wallcorepro.ui.navigation.WallCoreNavGraph
import com.offline.wallcorepro.ui.settings.SettingsViewModel
import com.offline.wallcorepro.ui.theme.WallCoreTheme
import com.offline.wallcorepro.worker.AutoWallpaperWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            // Wire auto wallpaper worker from settings changes
            LaunchedEffect(settingsState.isAutoWallpaperEnabled, settingsState.autoWallpaperIntervalHours) {
                if (settingsState.isAutoWallpaperEnabled) {
                    AutoWallpaperWorker.schedule(
                        context = this@MainActivity,
                        intervalHours = settingsState.autoWallpaperIntervalHours
                    )
                } else {
                    AutoWallpaperWorker.cancel(this@MainActivity)
                }
            }

            WallCoreTheme(darkTheme = settingsState.isDarkMode) {
                WallCoreMainApp()
            }
        }
    }
}

// ─── Main Navigation Shell ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallCoreMainApp() {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf(Screen.Home.route) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStack ->
            currentRoute = backStack.destination.route ?: Screen.Home.route
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home),
        BottomNavItem(Screen.Favorites.route, "Favorites", Icons.Default.Favorite),
        BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
    )

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Favorites.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { _ ->
        WallCoreNavGraph(navController = navController)
    }
}

// ─── Data class ───────────────────────────────────────────────────────────────

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
