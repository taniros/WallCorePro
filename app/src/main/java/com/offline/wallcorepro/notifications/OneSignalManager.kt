package com.offline.wallcorepro.notifications

import android.content.Context
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.offline.wallcorepro.BuildConfig
import com.offline.wallcorepro.config.AppConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * OneSignal push notification integration.
 *
 * Flow:
 *  1. initialize() → called once in WallCoreApp.onCreate()
 *  2. requestPermission() → called from MainActivity after onboarding completes
 *  3. tagUserPreferences() → sets segmentation tags so you can target campaigns
 *     from the OneSignal dashboard without any code change
 *
 * Deep-link routing:
 *  Notification payloads can carry an "additionalData" JSON object:
 *    { "screen": "detail", "wallpaper_id": "12345" }
 *  MainActivity polls pendingDeepLink on resume and navigates accordingly.
 *
 * Replace AppConfig.ONESIGNAL_APP_ID with your real ID from:
 *   https://app.onesignal.com → App Settings → Keys & IDs
 */
object OneSignalManager {

    @Volatile private var isInitialized = false

    fun initialize(context: Context) {
        if (!AppConfig.FEATURE_ONESIGNAL || isInitialized) return

        OneSignal.Debug.logLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.NONE

        OneSignal.initWithContext(context, AppConfig.ONESIGNAL_APP_ID)

        // Global click handler — routes notification taps to the right screen
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                handleNotificationClick(event)
            }
        })

        isInitialized = true
        Timber.d("OneSignal initialized — AppID: ${AppConfig.ONESIGNAL_APP_ID}")
    }

    /**
     * Requests POST_NOTIFICATIONS permission (mandatory on Android 13+).
     * Call this at an appropriate moment — after onboarding, not on cold open.
     * OneSignal 5.x requestPermission is a suspend function; we run it on MainScope.
     */
    fun requestPermission() {
        if (!AppConfig.FEATURE_ONESIGNAL) return
        MainScope().launch {
            val accepted = OneSignal.Notifications.requestPermission(fallbackToSettings = true)
            Timber.d("OneSignal notification permission: accepted=$accepted")
            if (accepted) tagUserPreferences()
        }
    }

    // ─── User Segmentation Tags ────────────────────────────────────────────────
    // Tags appear under each subscriber in the OneSignal dashboard.
    // Use them to build Segments for targeted campaigns:
    //   e.g. "morning_user=true AND language=ar" → Arabic morning users only

    fun tagUserPreferences(
        isMorningUser: Boolean = AppConfig.isMorningTime(),
        language: String       = java.util.Locale.getDefault().language
    ) {
        if (!AppConfig.FEATURE_ONESIGNAL) return
        with(OneSignal.User) {
            addTag("morning_user",  if (isMorningUser) "true" else "false")
            addTag("night_user",    if (!isMorningUser) "true" else "false")
            addTag("language",      language)
            addTag("niche",         AppConfig.NICHE_TYPE.lowercase())
            addTag("app_version",   AppConfig.VERSION_NAME)
        }
        Timber.d("OneSignal: tagged user (morning=$isMorningUser, lang=$language)")
    }

    /** Tag the last category the user browsed — for category-specific campaigns. */
    fun tagActiveCategory(category: String) {
        if (!AppConfig.FEATURE_ONESIGNAL) return
        OneSignal.User.addTag("last_category", category.lowercase().replace(" ", "_"))
    }

    /** Increment share count tag — identify power-sharers for loyalty campaigns. */
    fun tagShareAction() {
        if (!AppConfig.FEATURE_ONESIGNAL) return
        val current = OneSignal.User.getTags()["share_count"]?.toIntOrNull() ?: 0
        OneSignal.User.addTag("share_count", (current + 1).toString())
    }

    // ─── Notification Click Deep-Link ─────────────────────────────────────────

    private fun handleNotificationClick(event: INotificationClickEvent) {
        val data   = event.notification.additionalData
        val screen = data?.optString("screen")     ?: ""
        val id     = data?.optString("wallpaper_id") ?: ""
        Timber.d("OneSignal click — screen=$screen, wallpaper_id=$id")
        if (screen.isNotBlank()) {
            pendingDeepLink = PendingDeepLink(screen = screen, wallpaperId = id)
        }
    }

    data class PendingDeepLink(
        val screen: String,
        val wallpaperId: String = ""
    )

    /**
     * MainActivity calls this in onResume to consume any pending deep-link.
     * Returns the link once and clears it.
     */
    @Volatile var pendingDeepLink: PendingDeepLink? = null
        private set

    fun consumeDeepLink(): PendingDeepLink? {
        val link = pendingDeepLink
        pendingDeepLink = null
        return link
    }
}
