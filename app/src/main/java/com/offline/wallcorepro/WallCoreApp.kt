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
import com.offline.wallcorepro.ads.AdsManager
import com.offline.wallcorepro.ads.AppOpenAdManager
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.WallpaperDataSeed
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

    @Inject lateinit var workerFactory:     HiltWorkerFactory
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var dataSeed:          WallpaperDataSeed
    @Inject lateinit var appOpenAdManager:  AppOpenAdManager

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

        // Data seeding & background scheduling
        MainScope().launch {
            dataSeed.seedIfNeeded(AppConfig.NICHE_TYPE)

            val isAutoEnabled = preferenceManager.isAutoWallpaperEnabled.first()
            if (isAutoEnabled && AppConfig.FEATURE_AUTO_WALLPAPER) {
                val interval = preferenceManager.autoWallpaperIntervalHours.first()
                com.offline.wallcorepro.worker.AutoWallpaperWorker.schedule(this@WallCoreApp, interval)
            }
        }

        // Track this app open for Smart Reminder Intelligence
        MainScope().launch { preferenceManager.recordAppOpen() }

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
