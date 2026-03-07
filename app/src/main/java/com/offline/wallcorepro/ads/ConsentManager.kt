package com.offline.wallcorepro.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import timber.log.Timber

/**
 * Handles UMP (User Messaging Platform) consent for GDPR / CCPA compliance.
 *
 * WHY THIS MATTERS FOR CPM:
 *   Without consent → AdMob shows non-personalised ads → CPM ≈ $0.30–$1.00
 *   With consent    → AdMob shows personalised ads     → CPM ≈ $2.00–$10.00+
 *   That's a 3–10× revenue difference from a single consent form.
 *
 * USAGE:
 *   Call requestConsent(activity) { AdsManager.loadAds(context) } in MainActivity.onCreate().
 *   The UMP SDK shows the consent form only once (or when consent expires/is needed again).
 *   Subsequent launches skip the form and call onReady immediately.
 */
object ConsentManager {

    /** True once consent flow is settled (form shown or not required). */
    @Volatile var isReady: Boolean = false
        private set

    /**
     * True if user consented (or is outside a regulated region).
     * If false, call AdsManager.buildAdRequest() which will use non-personalised mode.
     */
    @Volatile var canRequestAds: Boolean = true
        private set

    /**
     * Request consent info and show the form if required.
     * Must be called from an Activity (not Application context).
     * [onReady] fires once we know the consent state — safe to init/load ads after this.
     */
    fun requestConsent(activity: Activity, onReady: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val info = UserMessagingPlatform.getConsentInformation(activity)

        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Consent info up-to-date — load and show form if needed
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Timber.w("UMP form error [${formError.errorCode}]: ${formError.message}")
                    }
                    canRequestAds = info.canRequestAds()
                    isReady       = true
                    Timber.d("Consent settled — canRequestAds=$canRequestAds  status=${info.consentStatus}")
                    onReady()
                }
            },
            { requestError ->
                // Network error or misconfiguration — default to allowing ads
                Timber.w("UMP info request error [${requestError.errorCode}]: ${requestError.message}")
                canRequestAds = true
                isReady       = true
                onReady()
            }
        )
    }

    /**
     * Returns true if the form is available and consent has already been collected.
     * Use this to show a "Privacy Settings" option in your app's settings screen.
     */
    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return UserMessagingPlatform.getConsentInformation(context)
            .privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /**
     * Shows the privacy options form (if available).
     * Wire this to your Settings → Privacy Policy row so users can change consent.
     */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Timber.w("Privacy options form error: ${formError.message}")
            }
        }
    }

    /** Dev-only: reset consent so the form appears again on next launch. */
    fun resetForTesting(context: Context) {
        UserMessagingPlatform.getConsentInformation(context).reset()
    }
}
