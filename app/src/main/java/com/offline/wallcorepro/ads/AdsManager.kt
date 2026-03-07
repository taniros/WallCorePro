package com.offline.wallcorepro.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.util.RemoteConfigManager
import timber.log.Timber

/**
 * Centralised AdMob manager — Interstitial + Rewarded.
 * App-Open ads are managed separately in AppOpenAdManager.
 *
 * High-CPM strategy applied here:
 *  1. RequestConfiguration — signals non-child, non-COPPA, content rating G to AdMob
 *     so demand-side partners bid with full audience targeting → higher eCPM.
 *  2. Content URL + Neighbouring URLs — contextual signals tell bidders your content
 *     is "good morning wishes / inspirational quotes" → premium lifestyle advertisers bid.
 *  3. Consent-aware AdRequest — personalised ad request when UMP consent is granted,
 *     non-personalised fallback otherwise.  Personalised ads earn 3–10× more.
 *  4. Preloading — always load the next ad BEFORE it's needed so there's zero wait.
 *  5. Retry backoff — failed loads retry at 30 s, preventing tight-loop quota burn.
 */
object AdsManager {

    private var isInitialized       = false
    private var interstitialCount   = 0
    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd?         = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // ─── Initialization ────────────────────────────────────────────────────────

    /**
     * Call this from WallCoreApp.onCreate().
     * [onInitialized] fires when AdMob is ready — use for App Open ad load.
     */
    fun initialize(context: Context, onInitialized: (() -> Unit)? = null) {
        if (!AppConfig.ADS_ENABLED) {
            Timber.d("Ads disabled via AppConfig")
            return
        }
        try {
            // ── High-CPM Step 1: Global Request Configuration ─────────────────
            // These flags tell AdMob (and every mediation partner) that your app is
            // NOT directed at children, enabling the full targeting stack.
            val requestConfig = RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build()
            MobileAds.setRequestConfiguration(requestConfig)

            MobileAds.initialize(context) { initStatus ->
                isInitialized = true
                Timber.d("AdMob initialised: $initStatus")
                loadInterstitial(context)
                loadRewarded(context)
                onInitialized?.invoke()
            }
        } catch (e: Exception) {
            Timber.e(e, "AdsManager: Failed to initialise")
            onInitialized?.invoke()
        }
    }

    // ─── Shared Ad Request Builder ─────────────────────────────────────────────

    /** App Open ads use the same high-CPM request (content URL, consent). */
    fun buildAdRequestForAppOpen(): AdRequest = buildAdRequest()

    /** Use for Banner/Native/any display ad — same high-CPM request (content URL + consent). */
    fun getAdRequest(): AdRequest = buildAdRequest()

    /**
     * Builds an AdRequest with:
     *  • Content URL  → contextual signal for the "morning wishes" niche
     *  • Neighbouring URLs → secondary context for bidders
     *  • Non-personalised flag only when consent was denied (GDPR fallback)
     *
     * The content URL is the single biggest code-side CPM lever after consent.
     */
    private fun buildAdRequest(): AdRequest {
        val builder = AdRequest.Builder()
            .setContentUrl(AppConfig.ADMOB_CONTENT_URL)
            .setNeighboringContentUrls(AppConfig.ADMOB_NEIGHBORING_URLS)

        // If user is in a regulated region and denied consent, signal non-personalised
        if (ConsentManager.isReady && !ConsentManager.canRequestAds) {
            val extras = android.os.Bundle().apply {
                putString("npa", "1")   // non-personalised ads flag
            }
            builder.addNetworkExtrasBundle(
                com.google.ads.mediation.admob.AdMobAdapter::class.java,
                extras
            )
        }
        return builder.build()
    }

    // ─── Interstitial ──────────────────────────────────────────────────────────

    private fun loadInterstitial(context: Context) {
        if (!AppConfig.ADS_ENABLED || !RemoteConfigManager.interstitialEnabled) return
        if (mInterstitialAd != null) return

        val loadContext = (context as? Activity)?.applicationContext ?: context
        InterstitialAd.load(
            loadContext,
            AppConfig.ADMOB_INTERSTITIAL_ID,
            buildAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    mInterstitialAd = ad
                    Timber.d("Interstitial loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    mInterstitialAd = null
                    Timber.w("Interstitial failed: ${error.message}")
                    mainHandler.postDelayed({ loadInterstitial(loadContext) }, 30_000L)
                }
            }
        )
    }

    /**
     * Shows the interstitial if ready, then calls [onAdDismissed] AFTER the ad closes.
     * The throttle (every [INTERSTITIAL_APPLY_COUNT] actions) keeps UX smooth while
     * maximising fill rate — showing too often hurts both CPM and retention.
     */
    fun showInterstitialIfReady(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (!AppConfig.ADS_ENABLED || !RemoteConfigManager.interstitialEnabled) {
            onAdDismissed(); return
        }
        interstitialCount++
        if (interstitialCount < RemoteConfigManager.interstitialCount) {
            onAdDismissed(); return
        }
        val ad = mInterstitialAd
        if (ad != null) {
            interstitialCount = 0
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadInterstitial(activity)
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Timber.e("Interstitial show failed: ${error.message}")
                    mInterstitialAd = null
                    loadInterstitial(activity)
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            Timber.d("Interstitial not ready — action runs immediately")
            loadInterstitial(activity)
            onAdDismissed()
        }
    }

    // ─── Rewarded ──────────────────────────────────────────────────────────────

    private fun loadRewarded(context: Context) {
        if (!AppConfig.ADS_ENABLED || !RemoteConfigManager.rewardedEnabled) return
        if (mRewardedAd != null) return

        val loadContext = (context as? Activity)?.applicationContext ?: context
        RewardedAd.load(
            loadContext,
            AppConfig.ADMOB_REWARDED_ID,
            buildAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    mRewardedAd = ad
                    Timber.d("Rewarded ad loaded")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            mRewardedAd = null
                            loadRewarded(loadContext)
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            mRewardedAd = null
                            loadRewarded(loadContext)
                            Timber.e("Rewarded show failed: ${error.message}")
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    mRewardedAd = null
                    Timber.w("Rewarded failed: ${error.message}")
                    mainHandler.postDelayed({ loadRewarded(loadContext) }, 30_000L)
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit = {}) {
        if (!AppConfig.ADS_ENABLED || !RemoteConfigManager.rewardedEnabled) { onFailed(); return }
        val ad = mRewardedAd
        if (ad != null) {
            ad.show(activity) { reward ->
                Timber.d("Reward earned: ${reward.amount} ${reward.type}")
                onRewarded()
            }
        } else {
            Timber.d("Rewarded not ready")
            loadRewarded(activity)
            onFailed()
        }
    }
}
