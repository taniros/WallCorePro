package com.offline.wallcorepro.ui.components

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.offline.wallcorepro.ads.AdsManager
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.util.RemoteConfigManager
import timber.log.Timber
import android.graphics.drawable.GradientDrawable

// ─── Adaptive Banner Ad ────────────────────────────────────────────────────────

/**
 * Adaptive anchored banner — fills the full device width.
 * Earns 10-20% more per impression than the fixed 50dp standard banner
 * because the ad size is optimised per device at runtime.
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    if (!AppConfig.ADS_ENABLED || !RemoteConfigManager.bannerAdEnabled) return

    val context = LocalContext.current

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            val displayMetrics = ctx.resources.displayMetrics
            val adWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            val adSize   = AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(ctx, adWidthDp)

            AdView(ctx).apply {
                adUnitId = AppConfig.ADMOB_INLINE_BANNER_ID
                setAdSize(adSize)
                loadAd(AdRequest.Builder().build())
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Timber.w("BannerAd failed to load: ${error.message}")
                    }
                }
            }
        }
    )
}

// ─── Inline Adaptive Banner Ad ────────────────────────────────────────────────

/**
 * Inline Adaptive Banner — designed to live INSIDE scrollable content (feeds, grids).
 *
 * Key differences from [BannerAdView] (anchored):
 *  • Uses [AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize] — AdMob picks the
 *    optimal height (up to the max supplied) rather than capping at ~90 dp.
 *  • Taller slots earn significantly more per impression because they give advertisers
 *    more creative space (video, rich-media) → higher-paying demand competes for the slot.
 *  • Must sit inside a LazyColumn/LazyVerticalGrid item, never fixed to screen edge.
 *
 * Placement: one ad every [AppConfig.INLINE_BANNER_INTERVAL] wallpaper cards, offset
 * from native ads so the two ad types never appear on the same index.
 */
@Composable
fun InlineAdaptiveBannerAd(modifier: Modifier = Modifier) {
    if (!AppConfig.ADS_ENABLED || !RemoteConfigManager.inlineBannerEnabled) return

    val context = LocalContext.current

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory  = { ctx ->
            val displayMetrics = ctx.resources.displayMetrics
            val adWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            // Inline adaptive — no fixed height cap; AdMob returns the best-fit size
            val adSize = AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(ctx, adWidthDp)

            AdView(ctx).apply {
                adUnitId = AppConfig.ADMOB_INLINE_BANNER_ID
                setAdSize(adSize)
                loadAd(AdsManager.getAdRequest())
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Timber.w("InlineBannerAd failed: ${error.message}")
                    }
                }
            }
        }
    )
}

// ─── Native Ad Card ────────────────────────────────────────────────────────────

/**
 * Real Google Native Ad rendered as a wallpaper-card-style tile.
 * Native ads blend naturally with content and deliver 3–5× higher CTR
 * than banner ads, making them one of the most valuable placements in the feed.
 *
 * The ad is loaded asynchronously; nothing is rendered until the ad arrives
 * so the layout never shows a blank placeholder.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    if (!AppConfig.ADS_ENABLED || !RemoteConfigManager.nativeAdEnabled) return

    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(context) {
        val adLoader = AdLoader.Builder(context, AppConfig.ADMOB_NATIVE_ID)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("NativeAd failed to load: ${error.message}")
                }
            })
            .build()
        adLoader.loadAd(AdsManager.getAdRequest())

        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    nativeAd?.let { ad ->
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp)),
            factory  = { ctx -> buildNativeAdView(ctx) },
            update   = { adView -> populateNativeAdView(ad, adView as NativeAdView) }
        )
    }
}

// ─── Programmatic NativeAdView builder ────────────────────────────────────────

private fun buildNativeAdView(context: Context): NativeAdView {
    val dm  = context.resources.displayMetrics
    fun Int.px() = (this * dm.density).toInt()

    val nativeAdView = NativeAdView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // ── Root card container ─────────────────────────────────────────────────
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        background = GradientDrawable().apply {
            setColor(0xFF1A1015.toInt())
            cornerRadius = 16.px().toFloat()
        }
        clipToOutline = true
        outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
    }

    // ── Media View (main image/video) ───────────────────────────────────────
    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            150.px()
        )
    }
    root.addView(mediaView)

    // ── Info section ────────────────────────────────────────────────────────
    val infoLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        setPadding(12.px(), 8.px(), 12.px(), 12.px())
    }

    // Headline row: app icon + headline + "AD" badge
    val headlineRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 4.px() }
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(32.px(), 32.px()).apply {
            rightMargin = 8.px()
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = GradientDrawable().apply {
            setColor(0xFF2D1B00.toInt())
            cornerRadius = 8.px().toFloat()
        }
    }

    val headlineView = TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        setTextColor(0xFFFFFFFF.toInt())
        textSize = 13f
        setTypeface(null, Typeface.BOLD)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val adBadge = TextView(context).apply {
        text = "AD"
        textSize = 9f
        setTextColor(0xFFFF6F00.toInt())
        background = GradientDrawable().apply {
            setColor(0x33FF6F00)
            cornerRadius = 4.px().toFloat()
        }
        setPadding(5.px(), 2.px(), 5.px(), 2.px())
    }

    headlineRow.addView(iconView)
    headlineRow.addView(headlineView)
    headlineRow.addView(adBadge)
    infoLayout.addView(headlineRow)

    // Body text
    val bodyView = TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 8.px() }
        setTextColor(0xAAFFFFFF.toInt())
        textSize = 11f
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    infoLayout.addView(bodyView)

    // Call-to-action button
    val ctaButton = Button(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            42.px()
        )
        background = GradientDrawable().apply {
            setColor(0xFFFF6F00.toInt())
            cornerRadius = 21.px().toFloat()
        }
        setTextColor(0xFFFFFFFF.toInt())
        textSize = 13f
        setTypeface(null, Typeface.BOLD)
        isAllCaps = false
    }
    infoLayout.addView(ctaButton)

    root.addView(infoLayout)
    nativeAdView.addView(root)

    // Wire all sub-views so Google can register clicks and impressions
    nativeAdView.mediaView       = mediaView
    nativeAdView.iconView        = iconView
    nativeAdView.headlineView    = headlineView
    nativeAdView.bodyView        = bodyView
    nativeAdView.callToActionView = ctaButton

    return nativeAdView
}

private fun populateNativeAdView(ad: NativeAd, adView: NativeAdView) {
    (adView.headlineView    as? TextView)?.text = ad.headline
    (adView.bodyView        as? TextView)?.text = ad.body
    (adView.callToActionView as? Button)?.text  = ad.callToAction ?: "Learn More"

    ad.icon?.drawable?.let { (adView.iconView as? ImageView)?.setImageDrawable(it) }

    adView.mediaView?.let { mv ->
        ad.mediaContent?.let { mc -> mv.mediaContent = mc }
    }
    adView.setNativeAd(ad)
}
