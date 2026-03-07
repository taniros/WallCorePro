package com.offline.wallcorepro.util

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.offline.wallcorepro.config.AppConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Firebase Remote Config wrapper.
 * Allows controlling app behavior from the Firebase Console without app updates.
 *
 * Usage in AppConfig: AppConfig.RC_ADS_ENABLED → read from here at runtime
 *
 * Quotes of the day can be updated remotely: set `quotes_of_day` in Firebase Console
 * to a JSON array like [{"text":"Good morning! ...","author":"Morning Wishes"}, ...]
 */
object RemoteConfigManager {

    private val remoteConfig by lazy { Firebase.remoteConfig }

    private val json = Json { ignoreUnknownKeys = true }

    // Remote config keys
    private const val KEY_ADS_ENABLED = "ads_enabled"
    private const val KEY_INTERSTITIAL_ENABLED = "interstitial_enabled"
    private const val KEY_NATIVE_AD_ENABLED = "native_ad_enabled"
    private const val KEY_BANNER_AD_ENABLED = "banner_ad_enabled"
    private const val KEY_APP_OPEN_AD_ENABLED = "app_open_ad_enabled"
    private const val KEY_REWARDED_ENABLED = "rewarded_enabled"
    private const val KEY_SHOW_NEW_BADGE = "show_new_badge"
    private const val KEY_FORCE_FRESH_ON_OPEN = "force_fresh_on_open"
    private const val KEY_PREMIUM_ENABLED = "premium_enabled"
    private const val KEY_NATIVE_AD_INTERVAL = "native_ad_interval"
    private const val KEY_INTERSTITIAL_COUNT = "interstitial_count"
    private const val KEY_QUOTES_OF_DAY = "quotes_of_day"

    @Serializable
    data class QuoteDto(val text: String, val author: String)

    fun initialize() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (AppConfig.RC_ADS_ENABLED) 3600L else 0L
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Default values from AppConfig
        val defaults = mapOf(
            KEY_ADS_ENABLED to AppConfig.RC_ADS_ENABLED,
            KEY_INTERSTITIAL_ENABLED to AppConfig.RC_INTERSTITIAL_ENABLED,
            KEY_NATIVE_AD_ENABLED to AppConfig.RC_NATIVE_AD_ENABLED,
            KEY_BANNER_AD_ENABLED to AppConfig.RC_BANNER_AD_ENABLED,
            KEY_APP_OPEN_AD_ENABLED to AppConfig.RC_APP_OPEN_AD_ENABLED,
            KEY_REWARDED_ENABLED to AppConfig.RC_REWARDED_ENABLED,
            KEY_SHOW_NEW_BADGE to AppConfig.RC_SHOW_NEW_BADGE,
            KEY_FORCE_FRESH_ON_OPEN to AppConfig.RC_FORCE_FRESH_ON_OPEN,
            KEY_PREMIUM_ENABLED to AppConfig.RC_PREMIUM_ENABLED,
            KEY_NATIVE_AD_INTERVAL to AppConfig.NATIVE_AD_INTERVAL.toLong(),
            KEY_INTERSTITIAL_COUNT to AppConfig.INTERSTITIAL_APPLY_COUNT.toLong(),
            KEY_QUOTES_OF_DAY to "[]"
        )
        remoteConfig.setDefaultsAsync(defaults)

        fetch()
    }

    fun fetch() {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                Timber.d("RemoteConfig: Fetch success, activated=$it")
            }
            .addOnFailureListener {
                Timber.w(it, "RemoteConfig: Fetch failed, using defaults")
            }
    }

    // ─── Getters ────────────────────────────────────────────────────────────────
    val adsEnabled: Boolean get() = remoteConfig.getBoolean(KEY_ADS_ENABLED)
    val interstitialEnabled: Boolean get() = remoteConfig.getBoolean(KEY_INTERSTITIAL_ENABLED)
    val nativeAdEnabled: Boolean get() = remoteConfig.getBoolean(KEY_NATIVE_AD_ENABLED)
    val bannerAdEnabled: Boolean get() = remoteConfig.getBoolean(KEY_BANNER_AD_ENABLED)
    val appOpenAdEnabled: Boolean get() = remoteConfig.getBoolean(KEY_APP_OPEN_AD_ENABLED)
    val rewardedEnabled: Boolean get() = remoteConfig.getBoolean(KEY_REWARDED_ENABLED)
    val showNewBadge: Boolean get() = remoteConfig.getBoolean(KEY_SHOW_NEW_BADGE)
    val forceFreshOnOpen: Boolean get() = remoteConfig.getBoolean(KEY_FORCE_FRESH_ON_OPEN)
    val premiumEnabled: Boolean get() = remoteConfig.getBoolean(KEY_PREMIUM_ENABLED)
    val nativeAdInterval: Int get() = remoteConfig.getLong(KEY_NATIVE_AD_INTERVAL).toInt()
    val interstitialCount: Int get() = remoteConfig.getLong(KEY_INTERSTITIAL_COUNT).toInt()

    /**
     * Remote quotes for "Quote of the Day". Update in Firebase Console without app release.
     * Format: [{"text":"Good morning! ...","author":"Morning Wishes"}, ...]
     * Returns empty list when not set or parse fails — use local fallback then.
     */
    val quotesOfDay: List<AppConfig.Quote>
        get() {
            val raw = remoteConfig.getString(KEY_QUOTES_OF_DAY)
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                json.decodeFromString<List<QuoteDto>>(raw).map { AppConfig.Quote(it.text, it.author) }
            }.getOrElse {
                Timber.w(it, "RemoteConfig: Failed to parse quotes_of_day, using local fallback")
                emptyList()
            }
        }
}
