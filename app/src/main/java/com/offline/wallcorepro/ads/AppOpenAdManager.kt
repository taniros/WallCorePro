package com.offline.wallcorepro.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.util.RemoteConfigManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages App-Open Ads — shown when the user brings the app back to the foreground.
 * App-Open ads are one of the highest-CPM formats ($5–20 CPM) and fire automatically
 * via ProcessLifecycleOwner.onStart, which triggers every time the app becomes visible.
 *
 * Strategy:
 *  - Load immediately on init and reload after each dismissal.
 *  - 4-hour freshness window: ads older than 4 hours are discarded and a new one is loaded.
 *  - 2-minute cooldown between shows to avoid user frustration.
 *  - Retry after 30 seconds on load failure (exponential-style, not tight loop).
 */
@Singleton
class AppOpenAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime = 0L
    private var lastShownTime = 0L
    /** Only show when returning from background — never on cold start (better UX). */
    private var hasBeenBackgrounded = false

    /** Set by WallCoreApp's ActivityLifecycleCallbacks — in onActivityStarted so it's ready before ProcessLifecycleOwner.onStart. */
    var currentActivity: Activity? = null

    // Load is triggered by AdsManager.initialize callback — after MobileAds SDK is ready

    // ─── Load ─────────────────────────────────────────────────────────────────

    fun loadAd() {
        if (!AppConfig.ADS_ENABLED || !RemoteConfigManager.appOpenAdEnabled) return
        if (isLoadingAd || isAdAvailable()) return

        isLoadingAd = true
        val request = AdsManager.buildAdRequestForAppOpen()

        AppOpenAd.load(
            context,
            AppConfig.ADMOB_APP_OPEN_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = System.currentTimeMillis()
                    Timber.d("AppOpenAd loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                    Timber.w("AppOpenAd failed to load: ${error.message}")
                    // Retry after 30s — prevents tight loop on server-side throttling
                    Handler(Looper.getMainLooper()).postDelayed({ loadAd() }, 30_000L)
                }
            }
        )
    }

    // ─── Show ─────────────────────────────────────────────────────────────────

    private fun showAdIfAvailable(activity: Activity) {
        if (!AppConfig.ADS_ENABLED || !RemoteConfigManager.appOpenAdEnabled) return
        if (isShowingAd) return
        if (!hasBeenBackgrounded) return  // never on cold start
        if (!canShowAd()) return
        if (!isAdAvailable()) {
            loadAd()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                lastShownTime = System.currentTimeMillis()
                Timber.d("AppOpenAd showed")
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                Timber.d("AppOpenAd dismissed — loading next")
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingAd = false
                Timber.e("AppOpenAd failed to show: ${error.message}")
                loadAd()
            }
        }
        appOpenAd?.show(activity)
    }

    // ─── ProcessLifecycleOwner hook ───────────────────────────────────────────

    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.let { activity ->
            // Short delay ensures activity is fully ready after process/return-from-background
            Handler(Looper.getMainLooper()).postDelayed({ showAdIfAvailable(activity) }, 150L)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        hasBeenBackgrounded = true
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Ad is available if it's loaded AND was loaded less than 4 hours ago. */
    private fun isAdAvailable(): Boolean {
        if (appOpenAd == null) return false
        val elapsed = System.currentTimeMillis() - loadTime
        return elapsed < 4 * 3_600_000L
    }

    /** Don't show more than once every 2 minutes to avoid back-press abuse. */
    private fun canShowAd(): Boolean {
        return System.currentTimeMillis() - lastShownTime > 2 * 60_000L
    }
}
