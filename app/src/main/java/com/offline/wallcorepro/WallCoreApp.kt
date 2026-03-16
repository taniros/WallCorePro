package com.offline.wallcorepro

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.offline.wallcorepro.ads.AdsManager
import com.offline.wallcorepro.ads.AppOpenAdManager
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.WallpaperDataSeed
import com.offline.wallcorepro.data.network.WallpaperApiService
import com.offline.wallcorepro.data.repository.PreferenceManager
import com.offline.wallcorepro.notifications.OneSignalManager
import com.offline.wallcorepro.util.RemoteConfigManager
import com.offline.wallcorepro.worker.NotificationWorker
import com.offline.wallcorepro.worker.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WallCoreApp : Application(), Configuration.Provider, Application.ActivityLifecycleCallbacks {

    @Inject lateinit var workerFactory:       HiltWorkerFactory
    @Inject lateinit var preferenceManager:   PreferenceManager
    @Inject lateinit var dataSeed:            WallpaperDataSeed
    @Inject lateinit var appOpenAdManager:    AppOpenAdManager
    @Inject lateinit var appResumeNotifier:   com.offline.wallcorepro.util.AppResumeNotifier
    @Inject lateinit var okHttpClient:        okhttp3.OkHttpClient
    @Inject lateinit var wallpaperApiService: WallpaperApiService

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR
            )
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            setupStrictMode()
        }

        // Coil — configure once, globally, before any image is loaded
        setupCoil()

        // Firebase Remote Config
        try {
            RemoteConfigManager.initialize()
        } catch (e: Exception) {
            Timber.w(e, "RemoteConfig init failed (check google-services.json)")
        }

        // OneSignal push notifications — initialise as early as possible
        OneSignalManager.initialize(this)
        OneSignalManager.tagUserPreferences()

        // AdMob — note: ads are loaded after ConsentManager resolves in MainActivity.
        // We only call initialize() here (which sets RequestConfiguration + SDK init).
        // The actual ad loading happens after consent, triggered from MainActivity.
        if (AppConfig.ADS_ENABLED) {
            AdsManager.initialize(this) { appOpenAdManager.loadAd() }
        }

        // App-Open Ad: track which activity is in the foreground so we can show the ad
        registerActivityLifecycleCallbacks(this)
        // Hook into the process lifecycle — AppOpenAdManager.onStart fires on every foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(appOpenAdManager)

        // Fire a lightweight health-check ping as early as possible so the Render
        // free-tier server starts waking up while the UI is still initialising.
        // By the time the user reaches HomeScreen (~2-3 s) the server is much closer
        // to ready, dramatically reducing the first-wallpaper wait time.
        MainScope().launch {
            try {
                wallpaperApiService.healthCheck()
                Timber.d("WallCore: server health-check OK")
            } catch (e: Exception) {
                Timber.w("WallCore: health-check failed — server will wake on first API call")
            }
        }

        // Data seeding & background scheduling
        MainScope().launch {
            // Ensure seeds are fully inserted before HomeViewModel warm-up starts
            // This prevents duplicate work and guarantees a smooth first-open experience
            dataSeed.seedIfNeeded(AppConfig.NICHE_TYPE)
            kotlinx.coroutines.delay(200) // Small buffer for DB transaction to complete

            val isAutoEnabled = preferenceManager.isAutoWallpaperEnabled.first()
            if (isAutoEnabled && AppConfig.FEATURE_AUTO_WALLPAPER) {
                val interval = preferenceManager.autoWallpaperIntervalHours.first()
                com.offline.wallcorepro.worker.AutoWallpaperWorker.schedule(this@WallCoreApp, interval)
            }
        }

        // Track this app open for Smart Reminder Intelligence
        MainScope().launch { preferenceManager.recordAppOpen() }

        // Schedule a DB cache clear for the NEXT open whenever the app goes to background.
        // HomeViewModel reads PENDING_CACHE_CLEAR on init/resume and wipes non-favourite
        // wallpapers + remote keys so every session opens with a genuinely fresh feed.
        if (AppConfig.CACHE_CLEAR_ON_EXIT) {
            MainScope().launch {
                appResumeNotifier.appBackgrounded.collect {
                    preferenceManager.setPendingCacheClear(true)
                    Timber.d("App backgrounded — cache clear scheduled for next open")
                }
            }
        }

        SyncWorker.schedule(this)
        NotificationWorker.schedule(this)
        com.offline.wallcorepro.worker.WeeklyShareRecapWorker.schedule(this)
        createNotificationChannels()

        Timber.d("WallCore Engine ${AppConfig.VERSION_NAME} initialized – Niche: ${AppConfig.NICHE_TYPE}")
    }

    // ─── ActivityLifecycleCallbacks ───────────────────────────────────────────
    // CRITICAL: Set currentActivity in onActivityStarted (NOT onResumed) because
    // ProcessLifecycleOwner.onStart fires when the first activity reaches STARTED.
    // If we only set in onResumed, currentActivity is still null when the ad tries to show.
    override fun onActivityStarted(activity: Activity) {
        appOpenAdManager.currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        appOpenAdManager.currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        // Don't clear currentActivity on pause — only on destroy.
        // Clearing here caused null when returning from background (onStart ran before our callback).
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (appOpenAdManager.currentActivity == activity) {
            appOpenAdManager.currentActivity = null
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Singleton Coil ImageLoader tuned for smooth, unlimited wallpaper scrolling:
     *
     *  Memory cache  — 25 % of RAM (capped at 192 MB on low-RAM devices).
     *                  Keeps the most-recently-seen thumbnails instantly available
     *                  when the user scrolls back up.
     *
     *  Disk cache    — 512 MB.  Images survived across app restarts so no
     *                  re-download on every fresh session.
     *
     *  allowHardware — true (default).  Hardware bitmaps are drawn directly by
     *                  the GPU driver, skipping a CPU → GPU copy every frame.
     *                  Gives a measurable frame-rate improvement on modern devices.
     *
     *  respectCacheHeaders = false — always serve from cache first; ignore
     *                  server Cache-Control headers that could force re-fetches.
     *
     *  crossfade(100) — very fast fade-in so images "snap" into place during
     *                  rapid scrolling instead of slowly dissolving.
     */
    private fun setupCoil() {
        // Dedicated OkHttp client for image downloads — completely separate from
        // Retrofit so API warm-up calls and thumbnail fetches never compete for
        // the same connection slots.  On first open this eliminates the queue where
        // images waited behind JSON API responses before starting to download.
        val coilHttpClient = OkHttpClient.Builder()
            .dispatcher(Dispatcher().apply {
                maxRequests        = 64
                maxRequestsPerHost = 24   // parallel thumbnail downloads per CDN host
            })
            .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Coil.setImageLoader {
            ImageLoader.Builder(this)
                .okHttpClient(coilHttpClient)
                .memoryCache {
                    MemoryCache.Builder(this)
                        // 20 % — RGB_565 thumbnails use 2 bytes/px instead of 4,
                        // so 20 % holds the same thumbnail count as the old 40 %.
                        // Frees ~10 % of the app heap for Firebase/AdMob/OneSignal.
                        .maxSizePercent(0.20)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("coil_image_cache"))
                        .maxSizeBytes(512L * 1024 * 1024)  // 512 MB
                        .build()
                }
                .respectCacheHeaders(false)
                .crossfade(100)                // snap, not dissolve
                .allowHardware(true)           // GPU-backed bitmaps — faster compositing
                .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
                .build()
        }
    }

    private fun setupStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().detectLeakedClosableObjects().penaltyLog().build()
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(listOf(
                NotificationChannel(
                    AppConfig.NOTIFICATION_CHANNEL_ID,
                    AppConfig.NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Good Morning & Good Night wishes" },
                NotificationChannel(
                    CHANNEL_SYNC_ID,
                    "Background Sync",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Background wallpaper sync" }
            ))
        }
    }

    companion object {
        const val CHANNEL_SYNC_ID = "wallcore_sync_channel"
    }
}
