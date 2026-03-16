package com.offline.wallcorepro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
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
        registerDynamicShortcuts()

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Route deep-links that arrive while the app is already running
        // (e.g. user taps a shortcut while app is in back-stack).
        routeDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        // Route deep-link that launched THIS activity instance (cold start from shortcut).
        routeDeepLink(intent)
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

    // ── Deep-link routing ─────────────────────────────────────────────────────
    // Handles wallcorepro:// URIs coming from:
    //   • Static / dynamic app shortcuts (launcher long-press)
    //   • Rich notification action buttons
    //   • Future: shared wallpaper links
    private fun routeDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "wallcorepro") return
        if (!::navController.isInitialized) return
        // Consume so the same intent doesn't re-route on every onResume
        this.intent = Intent(this, MainActivity::class.java)
        when (data.host) {
            "home"      -> navController.navigate("home") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
            "favorites" -> navController.navigate("favorites") {
                launchSingleTop = true
            }
            "ai"        -> navController.navigate("ai") {
                launchSingleTop = true
            }
            "detail"    -> {
                val id = data.pathSegments.firstOrNull() ?: return
                navController.navigate("detail/$id") { launchSingleTop = true }
            }
        }
    }

    // ── Dynamic shortcuts ────────────────────────────────────────────────────
    // Programmatic shortcuts complement the static shortcuts.xml.
    // They can be updated at runtime (e.g. badge counts, recent wallpaper).
    private fun registerDynamicShortcuts() {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this) &&
            ShortcutManagerCompat.getMaxShortcutCountPerActivity(this) == 0) return

        val browse = ShortcutInfoCompat.Builder(this, "browse_wallpapers")
            .setShortLabel(getString(R.string.shortcut_browse_short))
            .setLongLabel(getString(R.string.shortcut_browse_long))
            .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("wallcorepro://home"), this, MainActivity::class.java))
            .setRank(0)
            .build()

        val favorites = ShortcutInfoCompat.Builder(this, "open_favorites")
            .setShortLabel(getString(R.string.shortcut_favorites_short))
            .setLongLabel(getString(R.string.shortcut_favorites_long))
            .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("wallcorepro://favorites"), this, MainActivity::class.java))
            .setRank(1)
            .build()

        val ai = ShortcutInfoCompat.Builder(this, "open_ai")
            .setShortLabel(getString(R.string.shortcut_ai_short))
            .setLongLabel(getString(R.string.shortcut_ai_long))
            .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("wallcorepro://ai"), this, MainActivity::class.java))
            .setRank(2)
            .build()

        try {
            ShortcutManagerCompat.setDynamicShortcuts(this, listOf(browse, favorites, ai))
        } catch (e: Exception) {
            Timber.w(e, "Dynamic shortcuts registration failed (non-critical)")
        }
    }
}
