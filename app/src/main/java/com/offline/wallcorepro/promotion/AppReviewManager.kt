package com.offline.wallcorepro.promotion

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.play.core.review.ReviewManagerFactory
import com.offline.wallcorepro.config.AppConfig
import timber.log.Timber

/**
 * Smart In-App Review manager using the Google Play Review API.
 *
 * WHY THIS MATTERS FOR GROWTH:
 *   Play Store ranking is heavily influenced by review count and rating.
 *   Most users never voluntarily rate apps — but if you ask at the right moment
 *   (while they're happy, not while they're frustrated), conversion is high.
 *
 *   More 5-star reviews → higher search ranking → more organic installs.
 *   This single feature can double organic growth over 6 months.
 *
 * TRIGGER STRATEGY:
 *   • After 3rd wallpaper share  → user clearly loves the app, ask now
 *   • After 5th download         → power user, perfect timing
 *   • 30-day cooldown            → Google's API may throttle more frequent requests
 *   • NEVER block the user flow  → fire-and-forget; if the form doesn't show, life continues
 *
 * IMPORTANT: Google controls whether the dialog actually appears.
 *   During development, use an internal test track to see the real dialog.
 *   In production, Google may suppress it if the user already reviewed or
 *   the API was called recently. This is by design — respect it.
 */
object AppReviewManager {

    private const val PREFS_NAME            = "wallcore_review_prefs"
    private const val KEY_SHARE_COUNT       = "review_share_count"
    private const val KEY_DOWNLOAD_COUNT    = "review_download_count"
    private const val KEY_LAST_REVIEW_MS    = "review_last_shown_ms"
    private const val KEY_HAS_REVIEWED      = "review_has_reviewed"

    // ─── Public Trigger Points ────────────────────────────────────────────────

    /**
     * Call after every successful wallpaper share.
     * Increments the counter and requests a review on the 3rd share.
     */
    fun onWallpaperShared(activity: ComponentActivity) {
        val count = incrementCounter(activity, KEY_SHARE_COUNT)
        Timber.d("Review: share count = $count")
        if (count >= AppConfig.REVIEW_MIN_SHARES) {
            maybeShowReview(activity)
        }
    }

    /**
     * Call after every successful wallpaper download.
     * Increments the counter and requests a review on the 5th download.
     */
    fun onWallpaperDownloaded(activity: ComponentActivity) {
        val count = incrementCounter(activity, KEY_DOWNLOAD_COUNT)
        Timber.d("Review: download count = $count")
        if (count >= AppConfig.REVIEW_MIN_DOWNLOADS) {
            maybeShowReview(activity)
        }
    }

    // ─── Core Logic ───────────────────────────────────────────────────────────

    private fun maybeShowReview(activity: ComponentActivity) {
        val prefs        = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShownMs  = prefs.getLong(KEY_LAST_REVIEW_MS, 0L)
        val hasReviewed  = prefs.getBoolean(KEY_HAS_REVIEWED, false)

        // If already reviewed (we track this optimistically), never ask again
        if (hasReviewed) return

        val daysSinceLast = (System.currentTimeMillis() - lastShownMs) / 86_400_000L
        if (daysSinceLast < AppConfig.REVIEW_COOLDOWN_DAYS) {
            Timber.d("Review: cooldown active ($daysSinceLast / ${AppConfig.REVIEW_COOLDOWN_DAYS} days)")
            return
        }

        // Mark that we attempted — prevents rapid-fire calls during same session
        prefs.edit()
            .putLong(KEY_LAST_REVIEW_MS, System.currentTimeMillis())
            .putBoolean(KEY_HAS_REVIEWED, true)
            .apply()

        requestReviewFlow(activity)
    }

    private fun requestReviewFlow(activity: ComponentActivity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow()
            .addOnSuccessListener { reviewInfo ->
                // Google granted us a token — launch the in-app dialog
                manager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener {
                        Timber.d("Review: flow completed (shown or suppressed by Google)")
                    }
            }
            .addOnFailureListener { e ->
                // Silently fail — never block the user experience over a review dialog
                Timber.w("Review: requestReviewFlow failed — ${e.message}")
            }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun incrementCounter(context: Context, key: String): Int {
        val prefs   = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, current).apply()
        return current
    }

    /** Reset all counters — useful for dev/testing. */
    fun resetForTesting(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
