package com.offline.wallcorepro.ui.detail

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.app.WallpaperManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.WishQuotePool
import com.offline.wallcorepro.data.network.AiService
import com.offline.wallcorepro.data.repository.PreferenceManager
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.domain.model.WallpaperTarget
import com.offline.wallcorepro.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import javax.inject.Inject

data class DetailUiState(
    val wallpaper: Wallpaper? = null,
    val isLoading: Boolean = false,
    val isFavorite: Boolean = false,
    val isSettingWallpaper: Boolean = false,
    val isDownloading: Boolean = false,
    val wallpaperSetSuccess: Boolean = false,
    val error: String? = null,
    val interstitialTrigger: Boolean = false,
    val rewardedTrigger: Boolean = false,
    // New: triggers download-with-quote after rewarded ad completes
    val downloadWithQuoteTrigger: Boolean = false,
    val downloadSuccess: Boolean = false,
    // Share state
    val isSharing: Boolean = false,
    val shareUri: Uri? = null,
    val shareText: String = "",
    val whatsAppDirect: Boolean = false,  // true = route directly to WhatsApp
    // Dynamic Greeting State
    val isGreetingEnabled: Boolean = false,
    val greetingText: String = "",
    val isAiGenerating: Boolean = false,
    // Increments on every AI tap — drives mood rotation + Gemini variety seed
    val generateCount: Int = 0,
    // The mood/style used for the last AI generation (shown in FAB tooltip)
    val lastMoodUsed: String = "",
    // Current visual style for the quote overlay (cycles through OverlayStyle entries)
    val overlayStyleIndex: Int = 0,
    // Wish streak count shown as a badge
    val wishStreak: Int = 0,
    // User's own custom text to stamp on the wallpaper (empty = use auto quote)
    val customText: String = "",
    // Whether the custom text editor dialog is open
    val showCustomTextDialog: Boolean = false,
    // When true share/download sends the wallpaper WITHOUT any text
    val shareClean: Boolean = false,
    // True while the "Rephrase" AI call is in progress
    val isRephrasing: Boolean = false,
    // One-shot: show "Rephrased!" snackbar when rephrase succeeds
    val rephraseSuccess: Boolean = false,
    // Social proof share count for this wallpaper
    val shareCountLabel: String = ""
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWallpaperByIdUseCase: GetWallpaperByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val incrementDownloadUseCase: IncrementDownloadUseCase,
    private val preferenceManager: PreferenceManager,
    private val aiService: AiService,
    private val appResumeNotifier: com.offline.wallcorepro.util.AppResumeNotifier
) : ViewModel() {

    private val wallpaperId: String = checkNotNull(savedStateHandle["wallpaperId"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val preferFullScreen: StateFlow<Boolean> = preferenceManager.detailFullScreenByDefault
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadWallpaper()
        observeAppResume()
    }

    private fun observeAppResume() {
        viewModelScope.launch {
            appResumeNotifier.appResumed.collect {
                if (_uiState.value.customText.isBlank()) loadWallpaper(preferNewQuote = true)
            }
        }
    }

    private fun loadWallpaper(preferNewQuote: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val wallpaper     = getWallpaperByIdUseCase(wallpaperId)
            val selectedCats = preferenceManager.selectedQuoteCategories.first()
            val defaultQuote  = if (preferNewQuote)
                WishQuotePool.getNextQuote(selectedCats, wallpaper?.category ?: "")
            else
                WishQuotePool.getStableQuote(wallpaperId, wallpaper?.category ?: "", selectedCats)
            var styleIdx      = preferenceManager.overlayStyleIndex.first()
            val streak        = preferenceManager.wishStreak.first()
            val shareLabel    = if (AppConfig.FEATURE_SOCIAL_PROOF)
                "${AppConfig.getSimulatedShareCount(wallpaperId)} shares this week" else ""
            // Smart overlay: when user hasn't customized (default 0), suggest by wallpaper category
            if (styleIdx == 0 && wallpaper != null) {
                val suggested = AppConfig.getSuggestedOverlayStyle(wallpaper.category)
                styleIdx = AppConfig.OverlayStyle.entries.indexOf(suggested).coerceAtLeast(0)
            }
            _uiState.update {
                it.copy(
                    wallpaper       = wallpaper,
                    isFavorite      = wallpaper?.isFavorite ?: false,
                    isLoading       = false,
                    greetingText    = defaultQuote,
                    overlayStyleIndex = styleIdx,
                    wishStreak      = streak,
                    shareCountLabel = shareLabel
                )
            }
        }
    }

    fun toggleFavorite() {
        val wallpaper = _uiState.value.wallpaper ?: return
        viewModelScope.launch {
            val newFavorite = !_uiState.value.isFavorite
            toggleFavoriteUseCase(wallpaper.id, newFavorite)
            _uiState.update { it.copy(isFavorite = newFavorite) }
        }
    }

    fun setWallpaper(context: Context, target: WallpaperTarget) {
        val wallpaper = _uiState.value.wallpaper ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSettingWallpaper = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    setWallpaperInternal(context, wallpaper.imageUrl, target)
                }
                if (result) {
                    preferenceManager.incrementInterstitialCount()
                    _uiState.update {
                        it.copy(isSettingWallpaper = false, wallpaperSetSuccess = true, interstitialTrigger = true)
                    }
                } else {
                    _uiState.update { it.copy(isSettingWallpaper = false, error = "Failed to set wallpaper") }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error setting wallpaper")
                _uiState.update { it.copy(isSettingWallpaper = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    private fun setWallpaperInternal(context: Context, url: String, target: WallpaperTarget): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeStream(URL(url).openStream()) ?: return false
            val wm = WallpaperManager.getInstance(context)
            when (target) {
                WallpaperTarget.HOME -> wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                WallpaperTarget.LOCK -> wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                WallpaperTarget.BOTH -> {
                    wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "setWallpaperInternal failed"); false
        }
    }

    // ─── Download with Quote Overlay ─────────────────────────────────────────

    fun onDownload() {
        _uiState.update { it.copy(rewardedTrigger = true) }
    }

    fun completeDownload() {
        val wallpaper = _uiState.value.wallpaper ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            incrementDownloadUseCase(wallpaper.id)
            // Signal the UI (DetailScreen LaunchedEffect) to call downloadWithQuoteOverlay(context)
            _uiState.update { it.copy(isDownloading = false, rewardedTrigger = false, downloadWithQuoteTrigger = true) }
        }
    }

    /**
     * Downloads the wallpaper, draws the current quote/wish on it, and saves
     * the resulting image to the device's Pictures folder.
     * Called from DetailScreen after the rewarded-ad flow completes.
     */
    fun downloadWithQuoteOverlay(context: Context) {
        val wallpaper = _uiState.value.wallpaper ?: return
        val quote = activeQuote(wallpaper)

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadWithQuoteTrigger = false) }
            try {
                val userName = preferenceManager.userName.first()
                val signature = if (userName.isNotBlank()) "— $userName —" else "— ${AppConfig.APP_NAME} —"
                val success = withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeStream(URL(wallpaper.imageUrl).openStream())
                        ?: return@withContext false
                    val stamped = applyOverlay(bitmap, quote, signature)
                    val filename = "${AppConfig.APP_NAME_SHORT}_${wallpaper.id}"
                    saveBitmapToPictures(context, stamped, filename)
                }
                _uiState.update { it.copy(isDownloading = false, downloadSuccess = success) }
                if (success) {
                    preferenceManager.recordShareAndUpdateStreak()
                    val newStreak = preferenceManager.wishStreak.first()
                    _uiState.update { it.copy(wishStreak = newStreak) }
                }
            } catch (e: Exception) {
                Timber.e(e, "downloadWithQuoteOverlay failed")
                _uiState.update { it.copy(isDownloading = false, error = "Download failed. Try again.") }
            }
        }
    }

    fun clearDownloadSuccess() {
        _uiState.update { it.copy(downloadSuccess = false) }
    }

    // ─── Share with Quote Overlay ─────────────────────────────────────────────

    /**
     * Downloads the wallpaper, stamps the active quote onto it, saves it to
     * cache, and exposes a FileProvider URI + share text for the UI to launch
     * the system share sheet.
     */
    fun prepareAndShare(context: Context) {
        val wallpaper = _uiState.value.wallpaper ?: return
        val quote = activeQuote(wallpaper)

        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true) }
            try {
                val userName = preferenceManager.userName.first()
                val signature = if (userName.isNotBlank()) "— $userName —" else "— ${AppConfig.APP_NAME} —"
                val uri = withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeStream(URL(wallpaper.imageUrl).openStream())
                        ?: throw IllegalStateException("Failed to decode wallpaper image")
                    val stamped    = applyOverlay(bitmap, quote, signature)
                    val cacheFile  = File(context.cacheDir, "share_wallpaper.jpg")
                    FileOutputStream(cacheFile).use { out ->
                        stamped.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                }
                val greeting = AppConfig.TimeOfDay.current().displayName
                val text = "\"$quote\"\n\n✨ $greeting from ${AppConfig.APP_NAME}\n" +
                           "📲 Download: ${AppConfig.PLAY_STORE_URL}"

                _uiState.update { it.copy(isSharing = false, shareUri = uri, shareText = text, whatsAppDirect = false) }
                preferenceManager.recordShareAndUpdateStreak()
                val newStreak = preferenceManager.wishStreak.first()
                _uiState.update { it.copy(wishStreak = newStreak) }

            } catch (e: Exception) {
                Timber.e(e, "prepareAndShare failed")
                _uiState.update { it.copy(isSharing = false, error = "Could not prepare share. Try again.") }
            }
        }
    }

    fun clearShareSignal() {
        _uiState.update { it.copy(shareUri = null, shareText = "", whatsAppDirect = false) }
    }

    /**
     * Shares the wallpaper directly to WhatsApp (skips the generic share sheet).
     * Falls back to the generic sheet if WhatsApp is not installed.
     */
    fun shareToWhatsApp(context: Context) {
        val wallpaper = _uiState.value.wallpaper ?: return
        val quote     = activeQuote(wallpaper)
        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true) }
            try {
                val userName = preferenceManager.userName.first()
                val signature = if (userName.isNotBlank()) "— $userName —" else "— ${AppConfig.APP_NAME} —"
                val bitmap   = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeStream(URL(wallpaper.imageUrl).openStream())
                        ?: throw IllegalStateException("Failed to decode wallpaper image")
                }
                val stamped  = applyOverlay(bitmap, quote, signature)
                val file     = java.io.File(context.cacheDir, "wishmagic_wa_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { stamped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
                val uri      = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val greeting = AppConfig.TimeOfDay.current().displayName
                val text     = "\"$quote\"\n\n✨ $greeting from ${AppConfig.APP_NAME}\n📲 ${AppConfig.PLAY_STORE_URL}"

                _uiState.update { it.copy(isSharing = false, shareUri = uri, shareText = text, whatsAppDirect = true) }
                preferenceManager.recordShareAndUpdateStreak()
                val newStreak = preferenceManager.wishStreak.first()
                _uiState.update { it.copy(wishStreak = newStreak) }
            } catch (e: Exception) {
                Timber.e(e, "shareToWhatsApp failed")
                _uiState.update { it.copy(isSharing = false) }
                prepareAndShare(context)  // fallback to generic share
            }
        }
    }

    /** Cycles the quote overlay visual style and persists the choice. */
    fun cycleOverlayStyle() {
        val styles    = AppConfig.OverlayStyle.entries
        val nextIndex = (_uiState.value.overlayStyleIndex + 1) % styles.size
        _uiState.update { it.copy(overlayStyleIndex = nextIndex) }
        viewModelScope.launch { preferenceManager.setOverlayStyleIndex(nextIndex) }
    }

    // ─── Dynamic Greeting ─────────────────────────────────────────────────────

    fun toggleGreeting() {
        _uiState.update { it.copy(isGreetingEnabled = !it.isGreetingEnabled) }
    }

    fun generateAiGreeting() {
        val wallpaper = _uiState.value.wallpaper ?: return
        val niche     = when (AppConfig.TimeOfDay.current()) {
            AppConfig.TimeOfDay.MORNING   -> "Morning"
            AppConfig.TimeOfDay.AFTERNOON -> "Afternoon"
            AppConfig.TimeOfDay.EVENING   -> "Evening"
            AppConfig.TimeOfDay.NIGHT     -> "Night"
        }
        // Each tap bumps the count → different mood + different Gemini seed → guaranteed variety
        val newCount  = _uiState.value.generateCount + 1
        val mood      = AI_MOODS[newCount % AI_MOODS.size]

        viewModelScope.launch {
            _uiState.update { it.copy(isAiGenerating = true, generateCount = newCount, lastMoodUsed = mood) }
            val userName     = preferenceManager.userName.first()
            val selectedCats = preferenceManager.selectedQuoteCategories.first()
            val tone         = preferenceManager.selectedTone.first()

            aiService.generateWish(
                niche                = niche,
                mood                 = mood,
                userName             = userName,
                selectedCategoryKeys = selectedCats,
                variationSeed        = newCount,
                tone                 = tone
            ).onSuccess { text ->
                _uiState.update { it.copy(isAiGenerating = false, greetingText = text, isGreetingEnabled = true, showCustomTextDialog = true) }
            }.onFailure {
                // Offline / quota hit — serve a fresh local quote so user never gets nothing
                val fresh = WishQuotePool.getNextQuote(selectedCats, wallpaper.category)
                _uiState.update { it.copy(isAiGenerating = false, greetingText = fresh, isGreetingEnabled = true, showCustomTextDialog = true) }
            }
        }
    }

    /**
     * Rephrases the currently visible wish text using the user's selected tone.
     * If no wish is visible yet, falls back to generating a new one.
     */
    fun rephraseCurrentWish() {
        val currentText = _uiState.value.customText.ifBlank { _uiState.value.greetingText }
        if (currentText.isBlank()) { generateAiGreeting(); return }
        viewModelScope.launch {
            _uiState.update { it.copy(isRephrasing = true) }
            val tone   = preferenceManager.selectedTone.first()
            aiService.rephraseWish(original = currentText, tone = tone)
                .onSuccess { rephrased ->
                    val actuallyRephrased = rephrased != currentText
                    _uiState.update {
                        it.copy(
                            isRephrasing = false,
                            greetingText = rephrased,
                            customText = "",
                            isGreetingEnabled = true,
                            rephraseSuccess = actuallyRephrased
                        )
                    }
                }.onFailure {
                    val selectedCats = preferenceManager.selectedQuoteCategories.first()
                    val category     = _uiState.value.wallpaper?.category ?: ""
                    val fresh        = WishQuotePool.getNextQuote(selectedCats, category)
                    _uiState.update {
                        it.copy(
                            isRephrasing   = false,
                            greetingText   = fresh,
                            customText     = "",
                            isGreetingEnabled = true,
                            rephraseSuccess = true
                        )
                    }
                }
        }
    }

    companion object {
        // Cycles through every tap — each position = a genuinely different style
        private val AI_MOODS = listOf(
            "Inspirational", "Romantic",  "Heartfelt",  "Spiritual",
            "Funny",         "For Mom",   "For Friends","Motivation",
            "Cute",          "Formal",    "For Husband","Birthday"
        )
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(wallpaperSetSuccess = false, interstitialTrigger = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissRephraseSuccess() {
        _uiState.update { it.copy(rephraseSuccess = false) }
    }

    fun onInterstitialShown() = viewModelScope.launch {
        preferenceManager.resetInterstitialCount()
        _uiState.update { it.copy(interstitialTrigger = false) }
    }

    fun onRewardedShown(success: Boolean) {
        if (success) completeDownload()
        else _uiState.update { it.copy(rewardedTrigger = false, error = "Watch the full ad to download!") }
    }

    // ─── Custom text functions ────────────────────────────────────────────────

    fun toggleCustomTextDialog() {
        _uiState.update { it.copy(showCustomTextDialog = !it.showCustomTextDialog) }
    }

    fun applyCustomText(text: String) {
        _uiState.update {
            it.copy(
                customText          = text,
                greetingText        = text.ifBlank { it.greetingText },
                isGreetingEnabled   = true,
                showCustomTextDialog = false
            )
        }
    }

    fun clearCustomText() {
        val wallpaper = _uiState.value.wallpaper ?: return
        val autoQuote = WishQuotePool.getStableQuote(wallpaper.id, wallpaper.category)
        _uiState.update { it.copy(customText = "", greetingText = autoQuote, showCustomTextDialog = false) }
    }

    // ─── Clean share toggle ───────────────────────────────────────────────────

    fun toggleShareClean() {
        _uiState.update { it.copy(shareClean = !it.shareClean) }
    }

    // ─── Quote helper ─────────────────────────────────────────────────────────
    // Priority: customText → greetingText → stable pool quote

    private fun activeQuote(wallpaper: Wallpaper): String {
        val custom = _uiState.value.customText
        if (custom.isNotBlank()) return custom
        return _uiState.value.greetingText.ifEmpty {
            WishQuotePool.getStableQuote(wallpaper.id, wallpaper.category)
        }
    }

    // ─── Bitmap: draw quote overlay ───────────────────────────────────────────

    /**
     * Applies quote overlay and a brand watermark footer.
     * The watermark is present on ALL shares (clean or not) — it's the app's automatic
     * self-promotion engine. Every wallpaper shared to WhatsApp/Instagram carries the brand.
     */
    private suspend fun applyOverlay(source: Bitmap, quote: String, signature: String): Bitmap {
        val withQuote = if (_uiState.value.shareClean || quote.isBlank())
            source.copy(Bitmap.Config.ARGB_8888, false)
        else drawQuoteOnBitmap(source, quote, signature)
        return if (AppConfig.WATERMARK_ENABLED) addBrandWatermark(withQuote) else withQuote
    }

    /**
     * Burns a subtle branded footer onto the bitmap.
     * "Shared from WishMagic · Download FREE on Play Store"
     *
     * Design: tiny semi-transparent dark strip at the very bottom — noticeable when
     * seen in a WhatsApp chat but not distracting when set as a wallpaper.
     */
    private fun addBrandWatermark(bitmap: Bitmap): Bitmap {
        val result     = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas     = android.graphics.Canvas(result)
        val wmSize     = (result.width * 0.026f).coerceIn(20f, 34f)
        val barHeight  = wmSize * 2f

        // Semi-transparent black bar
        val bgPaint = Paint().apply {
            color = 0xCC000000.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            0f, result.height - barHeight,
            result.width.toFloat(), result.height.toFloat(),
            bgPaint
        )

        // Watermark text
        val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = 0xCCFFFFFF.toInt()
            textSize  = wmSize
            typeface  = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            AppConfig.WATERMARK_TEXT,
            result.width / 2f,
            result.height - barHeight + wmSize * 1.35f,
            txtPaint
        )
        return result
    }

    /**
     * Renders the quote onto the bitmap using the currently selected [OverlayStyle].
     * Returns a NEW mutable bitmap — the original is not modified.
     */
    private fun drawQuoteOnBitmap(source: Bitmap, quote: String, signature: String): Bitmap {
        val styleIndex = _uiState.value.overlayStyleIndex
        val style      = AppConfig.OverlayStyle.entries.getOrElse(styleIndex) { AppConfig.OverlayStyle.GLOW }
        return when (style) {
            AppConfig.OverlayStyle.GLOW    -> drawGlowStyle(source, quote, signature)
            AppConfig.OverlayStyle.MINIMAL -> drawMinimalStyle(source, quote, signature)
            AppConfig.OverlayStyle.WARM    -> drawWarmStyle(source, quote, signature)
            AppConfig.OverlayStyle.NEON   -> drawNeonStyle(source, quote, signature)
        }
    }

    // ── GLOW style: white italic on dark glassmorphism card (default) ──────────
    private fun drawGlowStyle(source: Bitmap, quote: String, signature: String): Bitmap {
        val out    = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(out)
        val w = out.width.toFloat(); val h = out.height.toFloat()
        val textSize  = (w * 0.038f).coerceIn(28f, 52f)
        val hPad = w * 0.07f; val vPad = h * 0.028f; val maxTxtW = w * 0.76f
        val cornerRad = w * 0.035f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE; this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(160, 0, 0, 0))
        }
        val lines = wrapTextToLines(quote, textPaint, maxTxtW); val lineH = textPaint.fontSpacing
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(200, 255, 200, 100); this.textSize = textSize * 0.58f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
        }
        val badgeH = badgePaint.fontSpacing
        val contentH = vPad + lines.size * lineH + vPad * 0.5f + badgeH + vPad
        val boxW = maxTxtW + hPad * 2f; val boxCenterY = h * 0.52f
        val boxTop = boxCenterY - contentH / 2f; val boxBottom = boxTop + contentH
        val boxLeft = (w - boxW) / 2f; val boxRight = w - boxLeft
        val rect = RectF(boxLeft, boxTop, boxRight, boxBottom)
        canvas.drawRoundRect(rect, cornerRad, cornerRad, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(175, 0, 0, 0) })
        canvas.drawRoundRect(rect, cornerRad, cornerRad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(70, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 1.5f
        })
        var y = boxTop + vPad + lineH * 0.85f
        for (line in lines) { canvas.drawText(line, w / 2f, y, textPaint); y += lineH }
        canvas.drawText(signature, w / 2f, boxBottom - vPad * 0.6f, badgePaint)
        return out
    }

    // ── MINIMAL style: text only, powerful shadow, no box ─────────────────────
    private fun drawMinimalStyle(source: Bitmap, quote: String, signature: String): Bitmap {
        val out    = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(out)
        val w = out.width.toFloat(); val h = out.height.toFloat()
        val textSize = (w * 0.042f).coerceIn(30f, 56f)
        val maxTxtW  = w * 0.82f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE; this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 4f, android.graphics.Color.argb(220, 0, 0, 0))
        }
        val lines = wrapTextToLines(quote, textPaint, maxTxtW); val lineH = textPaint.fontSpacing
        val totalH = lines.size * lineH
        var y = h * 0.50f - totalH / 2f + lineH * 0.85f
        for (line in lines) { canvas.drawText(line, w / 2f, y, textPaint); y += lineH }
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(160, 255, 255, 255); this.textSize = textSize * 0.50f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(200, 0, 0, 0))
        }
        canvas.drawText(signature, w / 2f, y + lineH * 0.3f, watermarkPaint)
        return out
    }

    // ── WARM style: gold/amber text on deep-dark gradient band ────────────────
    private fun drawWarmStyle(source: Bitmap, quote: String, signature: String): Bitmap {
        val out    = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(out)
        val w = out.width.toFloat(); val h = out.height.toFloat()
        val textSize = (w * 0.038f).coerceIn(28f, 52f)
        val hPad = w * 0.06f; val vPad = h * 0.03f; val maxTxtW = w * 0.80f
        val cornerRad = w * 0.04f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FFD700"); this.textSize = textSize
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(6f, 0f, 3f, android.graphics.Color.argb(200, 100, 50, 0))
        }
        val lines = wrapTextToLines(quote, textPaint, maxTxtW); val lineH = textPaint.fontSpacing
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(180, 255, 180, 60); this.textSize = textSize * 0.55f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
        }
        val badgeH = badgePaint.fontSpacing
        val contentH = vPad + lines.size * lineH + vPad * 0.5f + badgeH + vPad
        val boxW = maxTxtW + hPad * 2f; val boxCenterY = h * 0.52f
        val boxTop = boxCenterY - contentH / 2f; val boxBottom = boxTop + contentH
        val boxLeft = (w - boxW) / 2f; val boxRight = w - boxLeft
        val rect = RectF(boxLeft, boxTop, boxRight, boxBottom)
        // Deep warm-dark gradient background
        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                boxLeft, boxTop, boxRight, boxBottom,
                android.graphics.Color.argb(210, 60, 20, 0),
                android.graphics.Color.argb(200, 20, 5, 0),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRad, cornerRad, gradPaint)
        canvas.drawRoundRect(rect, cornerRad, cornerRad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(100, 255, 180, 50); style = Paint.Style.STROKE; strokeWidth = 2f
        })
        var y = boxTop + vPad + lineH * 0.85f
        for (line in lines) { canvas.drawText(line, w / 2f, y, textPaint); y += lineH }
        canvas.drawText(signature, w / 2f, boxBottom - vPad * 0.6f, badgePaint)
        return out
    }

    // ── NEON style: vibrant violet on semi-transparent dark card ──────────────
    private fun drawNeonStyle(source: Bitmap, quote: String, signature: String): Bitmap {
        val out    = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(out)
        val w = out.width.toFloat(); val h = out.height.toFloat()
        val textSize = (w * 0.038f).coerceIn(28f, 52f)
        val hPad = w * 0.07f; val vPad = h * 0.028f; val maxTxtW = w * 0.78f
        val cornerRad = w * 0.035f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#E040FB"); this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 0f, 0f, android.graphics.Color.argb(200, 180, 0, 255))
        }
        val lines = wrapTextToLines(quote, textPaint, maxTxtW); val lineH = textPaint.fontSpacing
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(200, 100, 220, 255); this.textSize = textSize * 0.55f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
            setShadowLayer(6f, 0f, 0f, android.graphics.Color.argb(180, 0, 200, 255))
        }
        val badgeH = badgePaint.fontSpacing
        val contentH = vPad + lines.size * lineH + vPad * 0.5f + badgeH + vPad
        val boxW = maxTxtW + hPad * 2f; val boxCenterY = h * 0.52f
        val boxTop = boxCenterY - contentH / 2f; val boxBottom = boxTop + contentH
        val boxLeft = (w - boxW) / 2f; val boxRight = w - boxLeft
        val rect = RectF(boxLeft, boxTop, boxRight, boxBottom)
        canvas.drawRoundRect(rect, cornerRad, cornerRad, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(190, 5, 0, 20) })
        canvas.drawRoundRect(rect, cornerRad, cornerRad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(150, 180, 0, 255); style = Paint.Style.STROKE; strokeWidth = 2f
        })
        var y = boxTop + vPad + lineH * 0.85f
        for (line in lines) { canvas.drawText(line, w / 2f, y, textPaint); y += lineH }
        canvas.drawText(signature, w / 2f, boxBottom - vPad * 0.6f, badgePaint)
        return out
    }

    private fun wrapTextToLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var cur   = StringBuilder()
        for (word in words) {
            val test = if (cur.isEmpty()) word else "$cur $word"
            if (paint.measureText(test) <= maxWidth) {
                cur = StringBuilder(test)
            } else {
                if (cur.isNotEmpty()) lines.add(cur.toString())
                cur = StringBuilder(word)
            }
        }
        if (cur.isNotEmpty()) lines.add(cur.toString())
        return lines.ifEmpty { listOf(text) }
    }

    // ─── Save bitmap to Pictures ──────────────────────────────────────────────

    private fun saveBitmapToPictures(context: Context, bitmap: Bitmap, filename: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+: MediaStore — no storage permission needed
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE,    "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${AppConfig.APP_NAME}")
                    put(MediaStore.Images.Media.IS_PENDING,   1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                    ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                cv.clear()
                cv.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, cv, null, null)
                true
            } else {
                // API 26–28: write to external storage directly
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    AppConfig.APP_NAME
                )
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "$filename.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "saveBitmapToPictures failed")
            false
        }
    }
}
