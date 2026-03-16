package com.offline.wallcorepro.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.offline.wallcorepro.config.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallcore_prefs")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Keys
    private object Keys {
        val ONBOARDING_DONE           = booleanPreferencesKey("onboarding_done")
        val DARK_MODE                  = booleanPreferencesKey("dark_mode")
        val AUTO_WALLPAPER_ENABLED     = booleanPreferencesKey("auto_wallpaper_enabled")
        val AUTO_WALLPAPER_INTERVAL    = longPreferencesKey("auto_wallpaper_interval_hours")
        val AUTO_WALLPAPER_TARGET      = stringPreferencesKey("auto_wallpaper_target")
        val LAST_VIEWED_AT             = longPreferencesKey("last_viewed_at")
        val INTERSTITIAL_COUNT         = intPreferencesKey("interstitial_count")
        val ADS_CONSENT_GIVEN          = booleanPreferencesKey("ads_consent_given")
        val SELECTED_NICHE             = stringPreferencesKey("selected_niche")
        val PREMIUM_UNLOCKED           = booleanPreferencesKey("premium_unlocked")
        val MORNING_NOTIF_ENABLED      = booleanPreferencesKey("morning_notif_enabled")
        val AFTERNOON_NOTIF_ENABLED    = booleanPreferencesKey("afternoon_notif_enabled")
        val EVENING_NOTIF_ENABLED      = booleanPreferencesKey("evening_notif_enabled")
        val NIGHT_NOTIF_ENABLED        = booleanPreferencesKey("night_notif_enabled")
        val USER_NAME                  = stringPreferencesKey("user_name")
        val SELECTED_QUOTE_CATEGORIES  = stringSetPreferencesKey("selected_quote_categories")
        val WISH_STREAK                = intPreferencesKey("wish_streak")
        val LAST_SHARE_DATE_DAY        = intPreferencesKey("last_share_date_day") // day-of-year
        val SHARES_THIS_WEEK           = intPreferencesKey("shares_this_week")
        val WEEK_NUMBER                = intPreferencesKey("week_number")        // year*52 + week
        val OVERLAY_STYLE_INDEX        = intPreferencesKey("overlay_style_index")
        // New feature keys
        val SELECTED_TONE              = stringPreferencesKey("selected_tone")
        val AUTO_THEME                 = booleanPreferencesKey("auto_theme")
        val FAVORITES_LOCKED           = booleanPreferencesKey("favorites_locked")
        val APP_OPEN_HOURS             = stringPreferencesKey("app_open_hours") // comma-sep ints
        val SMART_REMINDER_DISMISSED   = booleanPreferencesKey("smart_reminder_dismissed")
        val DETAIL_FULLSCREEN_BY_DEFAULT = booleanPreferencesKey("detail_fullscreen_by_default")
        // Monotonically increasing counter advanced by SEED_ADVANCE_PER_SESSION each session.
        // Ensures every session starts on a different backend query rotation → no repeats.
        val GLOBAL_FEED_SEED             = intPreferencesKey("global_feed_seed")
        // Set to true when the app goes to background; cleared after cache wipe on next open.
        val PENDING_CACHE_CLEAR          = booleanPreferencesKey("pending_cache_clear")
        fun lastSyncKey(niche: String)         = longPreferencesKey("last_sync_$niche")
        fun categoryInteraction(category: String) = intPreferencesKey("interaction_$category")
    }

    // ─── Sync ─────────────────────────────────────────────────────────────────
    suspend fun getLastSyncTime(niche: String): Long {
        return dataStore.data.first()[Keys.lastSyncKey(niche)] ?: 0L
    }

    suspend fun setLastSyncTime(niche: String, time: Long) {
        dataStore.edit { it[Keys.lastSyncKey(niche)] = time }
    }

    // ─── App State ────────────────────────────────────────────────────────────
    val isOnboardingDone: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    suspend fun setOnboardingDone() { dataStore.edit { it[Keys.ONBOARDING_DONE] = true } }

    val isDarkMode: Flow<Boolean> = dataStore.data.map { it[Keys.DARK_MODE] ?: true }
    suspend fun setDarkMode(enabled: Boolean) { dataStore.edit { it[Keys.DARK_MODE] = enabled } }

    // ─── Auto Wallpaper ───────────────────────────────────────────────────────
    val isAutoWallpaperEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.AUTO_WALLPAPER_ENABLED] ?: false }

    suspend fun setAutoWallpaperEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_WALLPAPER_ENABLED] = enabled }
    }

    val autoWallpaperIntervalHours: Flow<Long> =
        dataStore.data.map { it[Keys.AUTO_WALLPAPER_INTERVAL] ?: 24L }

    suspend fun setAutoWallpaperInterval(hours: Long) {
        dataStore.edit { it[Keys.AUTO_WALLPAPER_INTERVAL] = hours }
    }

    val autoWallpaperTarget: Flow<String> =
        dataStore.data.map { it[Keys.AUTO_WALLPAPER_TARGET] ?: "HOME" }

    suspend fun setAutoWallpaperTarget(target: String) {
        dataStore.edit { it[Keys.AUTO_WALLPAPER_TARGET] = target }
    }

    // ─── Ads ──────────────────────────────────────────────────────────────────
    val interstitialCount: Flow<Int> = dataStore.data.map { it[Keys.INTERSTITIAL_COUNT] ?: 0 }

    suspend fun incrementInterstitialCount() {
        dataStore.edit { prefs ->
            prefs[Keys.INTERSTITIAL_COUNT] = (prefs[Keys.INTERSTITIAL_COUNT] ?: 0) + 1
        }
    }

    suspend fun resetInterstitialCount() {
        dataStore.edit { it[Keys.INTERSTITIAL_COUNT] = 0 }
    }

    val adsConsentGiven: Flow<Boolean> = dataStore.data.map { it[Keys.ADS_CONSENT_GIVEN] ?: false }
    suspend fun setAdsConsentGiven(given: Boolean) {
        dataStore.edit { it[Keys.ADS_CONSENT_GIVEN] = given }
    }

    // ─── Last Viewed ──────────────────────────────────────────────────────────
    suspend fun getLastViewedAt(): Long = dataStore.data.first()[Keys.LAST_VIEWED_AT] ?: 0L
    suspend fun setLastViewedAt(time: Long) { dataStore.edit { it[Keys.LAST_VIEWED_AT] = time } }

    // ─── Personalization ──────────────────────────────────────────────────────
    suspend fun incrementCategoryInteraction(category: String) {
        dataStore.edit { prefs ->
            val key = Keys.categoryInteraction(category)
            prefs[key] = (prefs[key] ?: 0) + 1
        }
    }

    suspend fun getCategoryInteractionScore(category: String): Int {
        return dataStore.data.first()[Keys.categoryInteraction(category)] ?: 0
    }

    // ─── Notification granular toggles ────────────────────────────────────────
    val isMorningNotifEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.MORNING_NOTIF_ENABLED] ?: true }
    suspend fun setMorningNotifEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.MORNING_NOTIF_ENABLED] = enabled }
    }

    val isAfternoonNotifEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.AFTERNOON_NOTIF_ENABLED] ?: true }
    suspend fun setAfternoonNotifEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AFTERNOON_NOTIF_ENABLED] = enabled }
    }

    val isEveningNotifEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.EVENING_NOTIF_ENABLED] ?: true }
    suspend fun setEveningNotifEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.EVENING_NOTIF_ENABLED] = enabled }
    }

    val isNightNotifEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.NIGHT_NOTIF_ENABLED] ?: true }
    suspend fun setNightNotifEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NIGHT_NOTIF_ENABLED] = enabled }
    }

    // ─── User Name (for personalized wishes) ─────────────────────────────────
    val userName: Flow<String> = dataStore.data.map { it[Keys.USER_NAME] ?: "" }
    suspend fun setUserName(name: String) { dataStore.edit { it[Keys.USER_NAME] = name.trim() } }

    // ─── Quote Category Selection ─────────────────────────────────────────────
    // Stores the set of QuoteCategory.key values the user has enabled.
    // Defaults to the built-in default set defined in QuoteCategory.defaults.
    val selectedQuoteCategories: Flow<Set<String>> =
        dataStore.data.map { it[Keys.SELECTED_QUOTE_CATEGORIES] ?: AppConfig.QuoteCategory.defaults }

    suspend fun setSelectedQuoteCategories(categories: Set<String>) {
        dataStore.edit { it[Keys.SELECTED_QUOTE_CATEGORIES] = categories }
    }

    // ─── Wish Streak ──────────────────────────────────────────────────────────
    val wishStreak: Flow<Int>       = dataStore.data.map { it[Keys.WISH_STREAK] ?: 0 }
    val lastShareDayOfYear: Flow<Int> = dataStore.data.map { it[Keys.LAST_SHARE_DATE_DAY] ?: -1 }

    /** Call this every time a wallpaper is shared or downloaded. */
    suspend fun recordShareAndUpdateStreak() {
        dataStore.edit { prefs ->
            val cal        = java.util.Calendar.getInstance()
            val todayDoy   = cal.get(java.util.Calendar.DAY_OF_YEAR)
            val year       = cal.get(java.util.Calendar.YEAR)
            val week       = cal.get(java.util.Calendar.WEEK_OF_YEAR)
            val currentWeek = year * 53 + week
            val lastDoy    = prefs[Keys.LAST_SHARE_DATE_DAY] ?: -1
            val streak     = prefs[Keys.WISH_STREAK] ?: 0
            val lastWeek   = prefs[Keys.WEEK_NUMBER] ?: -1
            val sharesWeek = prefs[Keys.SHARES_THIS_WEEK] ?: 0
            val newStreak  = when {
                lastDoy == todayDoy         -> streak
                lastDoy == todayDoy - 1     -> streak + 1
                else                        -> 1
            }
            val (newSharesWeek, newWeekNum) = if (currentWeek != lastWeek) Pair(1, currentWeek)
                                               else Pair(sharesWeek + 1, currentWeek)
            prefs[Keys.WISH_STREAK]       = newStreak
            prefs[Keys.LAST_SHARE_DATE_DAY] = todayDoy
            prefs[Keys.SHARES_THIS_WEEK]  = newSharesWeek
            prefs[Keys.WEEK_NUMBER]       = newWeekNum
        }
    }

    suspend fun getSharesThisWeek(): Int = dataStore.data.first()[Keys.SHARES_THIS_WEEK] ?: 0

    suspend fun resetWeeklyShareCount() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.SHARES_THIS_WEEK)
            prefs.remove(Keys.WEEK_NUMBER)
        }
    }

    // ─── Overlay Style ────────────────────────────────────────────────────────
    val overlayStyleIndex: Flow<Int> = dataStore.data.map { it[Keys.OVERLAY_STYLE_INDEX] ?: 0 }
    suspend fun setOverlayStyleIndex(index: Int) { dataStore.edit { it[Keys.OVERLAY_STYLE_INDEX] = index } }

    // ─── Emotional Tone ───────────────────────────────────────────────────────
    val selectedTone: Flow<String> = dataStore.data.map {
        it[Keys.SELECTED_TONE] ?: com.offline.wallcorepro.config.AppConfig.EmotionalTone.default.key
    }
    suspend fun setSelectedTone(key: String) { dataStore.edit { it[Keys.SELECTED_TONE] = key } }

    // ─── Auto Theme ───────────────────────────────────────────────────────────
    val autoTheme: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_THEME] ?: false }
    suspend fun setAutoTheme(enabled: Boolean) { dataStore.edit { it[Keys.AUTO_THEME] = enabled } }

    // ─── Private Favorites Lock ───────────────────────────────────────────────
    val isFavoritesLocked: Flow<Boolean> = dataStore.data.map { it[Keys.FAVORITES_LOCKED] ?: false }
    suspend fun setFavoritesLocked(locked: Boolean) { dataStore.edit { it[Keys.FAVORITES_LOCKED] = locked } }

    // ─── Smart Reminder Intelligence ─────────────────────────────────────────
    val appOpenHours: Flow<String> = dataStore.data.map { it[Keys.APP_OPEN_HOURS] ?: "" }
    val isSmartReminderDismissed: Flow<Boolean> = dataStore.data.map {
        it[Keys.SMART_REMINDER_DISMISSED] ?: false
    }
    /** Records the current hour-of-day as an app open event. Keeps last 14 opens. */
    suspend fun recordAppOpen() {
        dataStore.edit { prefs ->
            val hour     = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val existing = (prefs[Keys.APP_OPEN_HOURS] ?: "").split(",").filter { it.isNotBlank() }
            val updated  = (existing + hour.toString()).takeLast(14)
            prefs[Keys.APP_OPEN_HOURS] = updated.joinToString(",")
        }
    }
    suspend fun dismissSmartReminder() { dataStore.edit { it[Keys.SMART_REMINDER_DISMISSED] = true } }

    // ─── Detail Full Screen ───────────────────────────────────────────────────
    val detailFullScreenByDefault: Flow<Boolean> =
        dataStore.data.map { it[Keys.DETAIL_FULLSCREEN_BY_DEFAULT] ?: false }
    suspend fun setDetailFullScreenByDefault(enabled: Boolean) {
        dataStore.edit { it[Keys.DETAIL_FULLSCREEN_BY_DEFAULT] = enabled }
    }

    // ─── Premium ──────────────────────────────────────────────────────────────
    val isPremiumUnlocked: Flow<Boolean> =
        dataStore.data.map { it[Keys.PREMIUM_UNLOCKED] ?: false }

    suspend fun setPremiumUnlocked(unlocked: Boolean) {
        dataStore.edit { it[Keys.PREMIUM_UNLOCKED] = unlocked }
    }

    // ─── Global Feed Seed (persistent, ever-advancing) ────────────────────────
    // Returns the current seed and atomically advances it for the next session.
    // Session 1 → seed 0, session 2 → seed 60, session 3 → seed 120 …
    // After ~166 sessions the counter wraps (mod 10,000) — acceptable repetition.
    suspend fun getAndAdvanceGlobalFeedSeed(): Int {
        var current = 0
        dataStore.edit { prefs ->
            current = prefs[Keys.GLOBAL_FEED_SEED] ?: 0
            prefs[Keys.GLOBAL_FEED_SEED] =
                (current + AppConfig.SEED_ADVANCE_PER_SESSION) % 10_000
        }
        return current
    }

    // ─── Pending Cache Clear ──────────────────────────────────────────────────
    // Set to true when the app goes to background (via AppResumeNotifier).
    // HomeViewModel reads this on the next open and wipes non-favourite wallpapers
    // from the DB before starting the warm-up, ensuring every session is fresh.
    suspend fun getPendingCacheClear(): Boolean =
        dataStore.data.first()[Keys.PENDING_CACHE_CLEAR] ?: false

    suspend fun setPendingCacheClear(value: Boolean) {
        dataStore.edit { it[Keys.PENDING_CACHE_CLEAR] = value }
    }
}

