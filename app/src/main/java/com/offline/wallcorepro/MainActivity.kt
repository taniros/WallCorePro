package com.offline.wallcorepro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.offline.wallcorepro.ads.ConsentManager
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.notifications.OneSignalManager
import com.offline.wallcorepro.ui.navigation.WallCoreNavGraph
import com.offline.wallcorepro.ui.settings.SettingsViewModel
import com.offline.wallcorepro.ui.theme.WallCoreTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── UMP Consent (GDPR/CCPA) ───────────────────────────────────────────
        // Request consent ONCE per launch. The form only appears when required
        // (first open in EU/UK, or when consent expires). This is the #1 lever
        // for AdMob CPM: personalised ads earn 3–10× more than non-personalised.
        if (AppConfig.ADS_ENABLED) {
            ConsentManager.requestConsent(this) {
                Timber.d("Consent ready — canRequestAds=${ConsentManager.canRequestAds}")
                // Consent is settled; ads were already initialised in WallCoreApp,
                // the next load cycle will pick up the consent-aware AdRequest.
            }
        }

        // ── OneSignal Notification Permission ────────────────────────────────
        // Request on first launch (Android 13+ requires explicit POST_NOTIFICATIONS).
        OneSignalManager.requestPermission()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            // Energy-Based Dynamic Theme
            val effectiveDarkTheme = if (settingsState.autoTheme)
                !AppConfig.isMorningTime()
            else settingsState.isDarkMode

            navController = rememberNavController()

            WallCoreTheme(darkTheme = effectiveDarkTheme) {
                WallCoreNavGraph(navController = navController)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ── OneSignal Deep-Link Routing ───────────────────────────────────────
        // If the user tapped a OneSignal notification with a "screen" payload,
        // navigate them to the correct destination.
        OneSignalManager.consumeDeepLink()?.let { link ->
            Timber.d("OneSignal deep-link consumed: $link")
            when (link.screen) {
                "detail" -> {
                    if (link.wallpaperId.isNotBlank() && ::navController.isInitialized) {
                        navController.navigate("detail/${link.wallpaperId}")
                    }
                }
                "ai"       -> if (::navController.isInitialized) navController.navigate("ai")
                "favorites"-> if (::navController.isInitialized) navController.navigate("favorites")
                else       -> Timber.d("Unknown deep-link screen: ${link.screen}")
            }
        }
    }
}
