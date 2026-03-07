package com.offline.wallcorepro.config

import com.offline.wallcorepro.BuildConfig

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║        WallCore Engine – Good Morning & Good Night Edition      ║
 * ║   To create a NEW niche app, edit ONLY this file + package name ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
object AppConfig {

    // ─── App Identity ──────────────────────────────────────────────
    const val APP_NAME       = "Morning, Afternoon & Night Wishes"
    const val APP_NAME_SHORT = "WishMagic"
    const val NICHE_TYPE     = "GOOD_MORNING"

    // Auto-derived from build.gradle — never needs manual editing
    val PACKAGE_NAME: String get() = BuildConfig.APPLICATION_ID
    val VERSION_NAME: String get() = BuildConfig.VERSION_NAME

    // ─── Domain Override (REQUIRED if you don't own wallcorepro.com) ───
    // The package "com.offline.wallcorepro" auto-derives "wallcorepro.com" — you may not own it!
    // Option A: Set CUSTOM_DOMAIN to your real domain (e.g. "myapp.com")
    // Option B: Leave CUSTOM_DOMAIN empty and set the overrides below (no domain needed)
    const val CUSTOM_DOMAIN = ""

    // No domain? Use these — works with free Google Docs / Gmail
    const val CUSTOM_PRIVACY_POLICY_URL = ""  // e.g. "https://docs.google.com/document/d/YOUR_ID/pub"
    const val CUSTOM_TERMS_URL          = ""  // optional, or same as privacy
    const val CUSTOM_FEEDBACK_EMAIL     = ""  // e.g. "youremail@gmail.com"

    private val BASE_DOMAIN: String
        get() = CUSTOM_DOMAIN.ifBlank {
            val parts = PACKAGE_NAME.split(".")
            if (parts.size >= 2) "${parts.last()}.${parts.first()}" else PACKAGE_NAME
        }

    val PLAY_STORE_URL:     String get() = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"
    val PRIVACY_POLICY_URL: String get() = CUSTOM_PRIVACY_POLICY_URL.ifBlank { "https://$BASE_DOMAIN/privacy" }
    val TERMS_URL:          String get() = CUSTOM_TERMS_URL.ifBlank { "https://$BASE_DOMAIN/terms" }
    val FEEDBACK_EMAIL:     String get() = CUSTOM_FEEDBACK_EMAIL.ifBlank { "support@$BASE_DOMAIN" }

    // ─── Niche Pexels Search Queries ────────────────────────────────
    // Strict nature/flower/sky/landscape only.
    // Negative keywords appended to every query to prevent people images from appearing.
    private const val PEOPLE_EXCLUSION = "-woman -girl -man -person -people -portrait -face -couple -wedding -selfie -model"

    // Best pretty morning wallpapers — aesthetic terms for high-quality results
    val MORNING_QUERIES = listOf(
        "sunrise pink golden sky flowers nature beautiful $PEOPLE_EXCLUSION",
        "morning roses dew garden sunlight bokeh stunning $PEOPLE_EXCLUSION",
        "cherry blossom sunrise golden hour nature aesthetic $PEOPLE_EXCLUSION",
        "sunflower field bright morning sky nature pretty $PEOPLE_EXCLUSION",
        "misty morning forest sunbeam golden light dreamy $PEOPLE_EXCLUSION",
        "rose garden morning soft pink bokeh nature lovely $PEOPLE_EXCLUSION",
        "sunrise sky orange golden clouds nature gorgeous $PEOPLE_EXCLUSION",
        "spring flowers garden morning sunlight colorful vivid $PEOPLE_EXCLUSION",
        "lavender field sunrise purple golden nature peaceful $PEOPLE_EXCLUSION",
        "morning dewdrops roses warm light flowers romantic $PEOPLE_EXCLUSION",
        "dawn golden hour mountains clouds beautiful landscape $PEOPLE_EXCLUSION",
        "morning light through trees forest magical nature $PEOPLE_EXCLUSION",
        "sunrise over water reflection lake peaceful beautiful $PEOPLE_EXCLUSION",
        "morning mist valley mountains ethereal nature $PEOPLE_EXCLUSION",
        "flower petals morning sunlight macro beautiful bokeh $PEOPLE_EXCLUSION"
    )
    // Best pretty night wallpapers — aesthetic terms for high-quality results
    val NIGHT_QUERIES = listOf(
        "full moon night sky stars beautiful nature magical $PEOPLE_EXCLUSION",
        "roses candlelight night flowers bokeh dark romantic $PEOPLE_EXCLUSION",
        "milky way stars purple blue night sky nature stunning $PEOPLE_EXCLUSION",
        "moonlit ocean calm water reflection nature serene $PEOPLE_EXCLUSION",
        "night sky stars long exposure galaxy beautiful $PEOPLE_EXCLUSION",
        "red roses dark bokeh flowers night aesthetic $PEOPLE_EXCLUSION",
        "crescent moon clouds night dreamy nature lovely $PEOPLE_EXCLUSION",
        "night garden flowers moon glowing nature peaceful $PEOPLE_EXCLUSION",
        "starry night sky beautiful long exposure cosmic $PEOPLE_EXCLUSION",
        "purple galaxy stars cosmos beautiful nature gorgeous $PEOPLE_EXCLUSION",
        "northern lights aurora borealis night sky colorful $PEOPLE_EXCLUSION",
        "city lights night skyline reflection water beautiful $PEOPLE_EXCLUSION",
        "moon over mountains silhouette night dramatic $PEOPLE_EXCLUSION",
        "fireflies night forest magical glowing nature $PEOPLE_EXCLUSION",
        "night flowers moonlight petals soft bokeh romantic $PEOPLE_EXCLUSION"
    )

    // Morning greeting photos for Pixabay – best pretty nature/flower themed
    val MORNING_ILLUSTRATION_QUERIES = listOf(
        "good morning flowers roses nature beautiful",
        "good morning sunrise stunning flowers",
        "good morning sunshine roses garden pretty",
        "good morning wishes flowers bokeh aesthetic",
        "good morning blessings flowers sunlight lovely",
        "beautiful morning flowers nature sunrise gorgeous",
        "good morning butterflies flowers garden dreamy",
        "good morning garden flowers sunrise nature peaceful",
        "morning glory flowers dewdrops sunlight romantic",
        "good morning roses pink garden nature vibrant",
        "lovely morning flowers golden light nature",
        "good morning daisy sunflower field beautiful"
    )

    // Afternoon greeting photos – sunny, vibrant, productive
    val AFTERNOON_ILLUSTRATION_QUERIES = listOf(
        "good afternoon sunshine flowers nature beautiful",
        "afternoon sun golden light garden flowers",
        "good afternoon wishes flowers vibrant nature",
        "sunny afternoon flowers garden peaceful",
        "beautiful afternoon sky clouds nature",
        "good afternoon blessings flowers sunlight",
        "afternoon light golden hour nature flowers",
        "good afternoon roses garden warm light",
        "peaceful afternoon nature landscape beautiful",
        "good afternoon sunshine garden flowers lovely"
    )

    // Evening greeting photos – sunset, golden hour, twilight
    val EVENING_ILLUSTRATION_QUERIES = listOf(
        "good evening sunset sky beautiful nature",
        "evening golden hour flowers nature",
        "good evening wishes sunset roses",
        "beautiful evening sky clouds nature",
        "good evening blessings flowers twilight",
        "sunset evening flowers garden peaceful",
        "good evening roses sunset warm light",
        "twilight evening nature beautiful sky",
        "good evening sky golden hour nature",
        "peaceful evening sunset flowers nature"
    )

    // Night greeting photos for Pixabay – best pretty moon/stars/flowers
    val NIGHT_ILLUSTRATION_QUERIES = listOf(
        "good night moon stars nature beautiful",
        "good night flowers stunning roses",
        "sweet dreams moon flowers stars pretty",
        "good night wishes roses moonlight aesthetic",
        "good night stars sky flowers gorgeous",
        "good night sleep well flowers moon dreamy",
        "beautiful goodnight stars sky nature peaceful",
        "good night blessings moon flowers romantic",
        "sweet dreams night flowers roses lovely",
        "good night candle flowers bokeh magical",
        "lovely good night moonlit sky nature",
        "good night galaxy stars cosmos beautiful"
    )

    // Combined fallback list (all greeting illustrations)
    val ILLUSTRATION_QUERIES = MORNING_ILLUSTRATION_QUERIES + AFTERNOON_ILLUSTRATION_QUERIES + EVENING_ILLUSTRATION_QUERIES + NIGHT_ILLUSTRATION_QUERIES

    // Active query – NO FALLBACK, always morning or night
    fun getMorningQuery(): String = MORNING_QUERIES.random()
    fun getNightQuery(): String = NIGHT_QUERIES.random()

    /** Returns both a morning and night query for mixed-feed strategy — best pretty wallpapers from both. */
    fun getMorningAndNightQueries(): Pair<String, String> = Pair(getMorningQuery(), getNightQuery())

    /** Returns both morning and night illustration queries for Pixabay mixed feed. */
    fun getMorningAndNightIllustrationQueries(): Pair<String, String> =
        Pair(MORNING_ILLUSTRATION_QUERIES.random(), NIGHT_ILLUSTRATION_QUERIES.random())

    // Time-aware illustration query (for Pixabay greeting-style images)
    fun getNicheIllustrationQuery(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour in MORNING_START_HOUR until MORNING_END_HOUR   -> MORNING_ILLUSTRATION_QUERIES.random()
            hour in AFTERNOON_START_HOUR until AFTERNOON_END_HOUR -> AFTERNOON_ILLUSTRATION_QUERIES.random()
            hour in EVENING_START_HOUR until EVENING_END_HOUR   -> EVENING_ILLUSTRATION_QUERIES.random()
            else -> NIGHT_ILLUSTRATION_QUERIES.random()
        }
    }

    // ─── Time-Aware Behavior (4 periods: Morning, Afternoon, Evening, Night) ───
    const val MORNING_START_HOUR   = 4   // 4 AM
    const val MORNING_END_HOUR     = 12  // Until noon
    const val AFTERNOON_START_HOUR = 12  // Noon
    const val AFTERNOON_END_HOUR   = 17  // 5 PM
    const val EVENING_START_HOUR   = 17  // 5 PM
    const val EVENING_END_HOUR    = 21  // 9 PM
    const val NIGHT_START_HOUR     = 21  // 9 PM (or before morning)

    /** Current time-of-day: Morning (4–12), Afternoon (12–17), Evening (17–21), Night (21–4) */
    enum class TimeOfDay(val displayName: String, val emoji: String, val key: String) {
        MORNING   ("Good Morning",   "☀️", "morning"),
        AFTERNOON ("Good Afternoon", "🌤️", "afternoon"),
        EVENING   ("Good Evening",   "🌅", "evening"),
        NIGHT     ("Good Night",     "🌙", "night");

        companion object {
            fun current(): TimeOfDay {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                return when {
                    hour in MORNING_START_HOUR until MORNING_END_HOUR   -> MORNING
                    hour in AFTERNOON_START_HOUR until AFTERNOON_END_HOUR -> AFTERNOON
                    hour in EVENING_START_HOUR until EVENING_END_HOUR   -> EVENING
                    else -> NIGHT
                }
            }
        }
    }

    // Expanded Categories – strictly greeting & wish focused (4 time periods)
    val NICHE_CATEGORIES = listOf(
        "Morning Greetings",
        "Afternoon Wishes",
        "Evening Greetings",
        "Night Wishes",
        "Morning Love",
        "Afternoon Love",
        "Evening Love",
        "Nightly Love",
        "Family Wishes",
        "Friends Greetings",
        "Daily Blessings",
        "Spiritual Morning",
        "Sweet Night Dreams",
        "Romantic Greetings"
    )

    // Map categories → Nature/flower/sky queries only — NO people, couples or faces
    val CATEGORY_QUERY_MAP = mapOf(
        "Morning Greetings"   to "beautiful sunrise flowers morning garden nature bokeh",
        "Afternoon Wishes"    to "sunny afternoon flowers garden nature bright",
        "Evening Greetings"   to "sunset golden hour sky clouds nature beautiful",
        "Night Wishes"        to "beautiful moon night stars sky clouds nature",
        "Morning Love"        to "red roses morning dew garden flowers soft bokeh",
        "Afternoon Love"      to "red roses sunlight garden flowers warm bokeh",
        "Evening Love"        to "red roses sunset golden hour flowers romantic",
        "Nightly Love"        to "red roses candle night moon flowers bokeh dark",
        "Family Wishes"       to "sunflowers morning sunshine garden warm nature",
        "Friends Greetings"   to "colorful flowers morning garden butterflies nature",
        "Daily Blessings"     to "golden sunrise nature peaceful mountain light rays",
        "Spiritual Morning"   to "sunrise golden light rays nature peaceful sky",
        "Sweet Night Dreams"  to "dreamy moonlit clouds night stars soft nature",
        "Romantic Greetings"  to "red roses pink flowers morning dew garden bokeh"
    )

    // ─── Backend Configuration ──────────────────────────────────────
    // REQUIRED: After deploying (see DEPLOYMENT.md), set BASE_URL to your Render URL + "/v1/"
    // Example: "https://wallcorepro-api.onrender.com/v1/"
    const val BASE_URL             = "https://wallcorepro-api.onrender.com/v1/"
    const val API_KEY              = "YOUR_API_KEY_HERE"
    const val API_TIMEOUT_SECONDS  = 30L
    const val MAX_RETRIES          = 3

    // ─── Multi-Source Integration (License-Safe Strategy) ──────────────────
    // BACKEND_ONLY = true: App fetches ONLY from your backend. No Pexels/Pixabay in app.
    // Play Store cannot see image sources. Deploy backend from /backend folder first.
    const val USE_BACKEND_ONLY   = true   // RECOMMENDED: 100% Play-safe, no third-party APIs in app
    const val USE_PEXELS_API     = false  // DISABLED: Pexels forbids wallpaper apps
    const val USE_PIXABAY_API    = false  // OFF when USE_BACKEND_ONLY (backend fetches from Pixabay)
    
    const val PEXELS_API_KEY     = "HnnfqrCtFamQpgLqVUmQRKwI2fvG8JnUeEYerLBAr9voSNobj9o5DKld"
    const val PEXELS_BASE_URL    = "https://api.pexels.com/v1/"
    
    // Placeholder - User should replace with their own Pixabay Key
    const val PIXABAY_API_KEY    = "54842696-d4ae6b57d9c83c77f03263f7f"
    const val PIXABAY_BASE_URL   = "https://pixabay.com/api/"

    // ─── Gemini AI Integration ──────────────────────────────────────────
    const val GEMINI_API_KEY     = "AIzaSyDdkOtRmV7TbQ2gimZjNJoHzkvhdo33EaY" // Free tier available
    const val GEMINI_MODEL       = "gemini-2.0-flash"

    // ─── Cache & Sync ────────────────────────────────────────────────
    const val INITIAL_WALLPAPER_COUNT    = 80
    const val PAGE_SIZE                  = 40
    const val PREFETCH_DISTANCE          = 10
    const val CACHE_DISK_SIZE_MB         = 500L
    const val CACHE_MEMORY_SIZE_MB       = 100L
    const val SYNC_INTERVAL_HOURS        = 3L
    const val FRESHNESS_THRESHOLD_HOURS  = 1L

    // ─── Feature Flags ───────────────────────────────────────────────
    const val FEATURE_FAVORITES         = true
    const val FEATURE_SHARE             = true
    const val FEATURE_DOWNLOAD          = true
    const val FEATURE_PREMIUM           = false
    const val FEATURE_AUTO_WALLPAPER    = true
    const val FEATURE_DAILY_NOTIFICATION = true
    const val FEATURE_CATEGORIES        = true
    const val FEATURE_TRENDING          = true
    const val FEATURE_SEARCH            = true
    const val FEATURE_DARK_MODE_TOGGLE  = true
    const val FEATURE_TIME_GREETING     = true

    // ─── Auto Wallpaper ──────────────────────────────────────────────
    const val AUTO_WALLPAPER_DEFAULT_INTERVAL_HOURS = 24L
    val AUTO_WALLPAPER_INTERVALS = listOf(1L, 6L, 12L, 24L, 48L, 72L)

    // ─── Notifications ────────────────────────────────────────────────
    const val MORNING_NOTIFICATION_HOUR   = 7   // 7 AM
    const val AFTERNOON_NOTIFICATION_HOUR  = 14  // 2 PM
    const val EVENING_NOTIFICATION_HOUR    = 19  // 7 PM
    const val NIGHT_NOTIFICATION_HOUR      = 21  // 9 PM
    const val DAILY_NOTIFICATION_HOUR      = 7
    const val NOTIFICATION_CHANNEL_ID      = "morning_night_channel"
    const val NOTIFICATION_CHANNEL_NAME    = "Morning & Night Wishes"
    const val MORNING_NOTIFICATION_ID      = 2001
    const val AFTERNOON_NOTIFICATION_ID    = 2003
    const val EVENING_NOTIFICATION_ID      = 2004
    const val NIGHT_NOTIFICATION_ID        = 2002

    // ─── OneSignal Push Notifications ───────────────────────────────────
    // Replace with your real App ID from https://app.onesignal.com → App Settings → Keys & IDs
    const val FEATURE_ONESIGNAL  = true
    const val ONESIGNAL_APP_ID   = "YOUR-ONESIGNAL-APP-ID-HERE"

    // ─── Ads & Monetization Strategy ───────────────────────────────────────
    // CPM hierarchy: App Open ($5–20) > Rewarded ($3–8) > Interstitial ($2–6) > Native ($1–4) > Banner ($0.5–2)
    // Placements: App Open | Interstitial (scroll, wallpaper, AI) | Rewarded | Native | Banner
    // All toggles RC-controlled. Consider AdMob Mediation for higher fill.
    // ───────────────────────────────────────────────────────────────────────
    const val ADS_ENABLED              = true
    const val ADS_PRIMARY_NETWORK      = "ADMOB"

    // AdMob Sample IDs (Replace with real ones before publishing)
    const val ADMOB_APP_ID             = "ca-app-pub-3940256099942544~3347511713"
    const val ADMOB_BANNER_ID          = "ca-app-pub-3940256099942544/6300978111"
    const val ADMOB_NATIVE_ID          = "ca-app-pub-3940256099942544/2247696110"
    const val ADMOB_INTERSTITIAL_ID    = "ca-app-pub-3940256099942544/1033173712"
    const val ADMOB_REWARDED_ID        = "ca-app-pub-3940256099942544/5224354917"
    const val ADMOB_APP_OPEN_ID        = "ca-app-pub-3940256099942544/9257395921"

    const val NATIVE_AD_INTERVAL        = 7    // 1 native ad every 7 wallpaper cards
    const val INTERSTITIAL_APPLY_COUNT  = 3    // interstitial every 3rd action
    const val INTERSTITIAL_SCROLL_ITEMS = 25   // show interstitial every N wallpapers scrolled
    const val INTERSTITIAL_AI_GENERATE   = 2    // show interstitial after Nth AI wish generation
    const val BANNER_REFRESH_SECONDS    = 45
    const val APP_OPEN_AD_ENABLED       = true
    const val APP_OPEN_COOLDOWN_MINUTES = 2

    // ─── AdMob High-CPM Targeting ─────────────────────────────────────
    // Content URL signals to AdMob what your app is about → better contextual match → higher CPM.
    // Uses Play Store URL (works without a domain). Replace with your domain if you have one.
    val ADMOB_CONTENT_URL: String get() = PLAY_STORE_URL
    val ADMOB_NEIGHBORING_URLS: List<String>
        get() = listOf(PLAY_STORE_URL)

    // ─── Branding Colors ─────────────────────────────────────────────
    const val PRIMARY_COLOR       = "#FF6F00"
    const val PRIMARY_DARK_COLOR  = "#E65100"
    const val ACCENT_COLOR        = "#FFD54F"
    const val BACKGROUND_COLOR    = "#0D0A0E"
    const val SURFACE_COLOR       = "#1A1015"

    // ─── Remote Config Defaults (Enhanced for Ads) ──────────────────────────────────
    const val RC_ADS_ENABLED          = true
    const val RC_INTERSTITIAL_ENABLED = true
    const val RC_REWARDED_ENABLED     = true
    const val RC_NATIVE_AD_ENABLED    = true
    const val RC_BANNER_AD_ENABLED    = true
    const val RC_APP_OPEN_AD_ENABLED  = true
    const val RC_SHOW_NEW_BADGE       = true
    const val RC_FORCE_FRESH_ON_OPEN  = true
    const val RC_PREMIUM_ENABLED      = false

    // ─── Automatic Promotion Strategy ────────────────────────────────────────
    // Watermark: branded footer burned into every shared/downloaded wallpaper.
    // When users share to WhatsApp/Status, every contact sees it — free advertising.
    const val WATERMARK_ENABLED      = true
    // val (not const) because it interpolates APP_NAME_SHORT at runtime
    val WATERMARK_TEXT get()         = "Shared from $APP_NAME_SHORT · Download FREE on Play Store"

    // In-App Review: ask for a 5-star rating at the perfect moment.
    // More reviews → better Play Store ranking → more organic installs.
    const val REVIEW_MIN_SHARES      = 3    // ask after 3rd share
    const val REVIEW_MIN_DOWNLOADS   = 5    // ask after 5th download
    const val REVIEW_COOLDOWN_DAYS   = 30   // never ask again within 30 days

    // Branded Invite Card: visual image generated for "Share App" action.
    // A beautiful card converts far better than a plain-text link.
    const val INVITE_CARD_ENABLED    = true
    val INVITE_MESSAGE get()         = "✨ I'm using $APP_NAME_SHORT for beautiful daily wishes!\n" +
                                       "Download FREE → "

    // ─── Organic Install Promotion (Auto-promote without paid ads) ───────────
    const val FEATURE_POST_SHARE_INVITE   = true   // After share: "Invite friends?" snackbar
    const val FEATURE_EXIT_SHARE_PROMPT   = true   // Exit dialog: "Share App" option
    const val FEATURE_SOCIAL_PROOF_BANNER = true   // "Join 50,000+ users" social proof
    const val FEATURE_WEEKLY_SHARE_RECAP  = true   // Notification: "You shared X wishes this week!"

    // ─── Personalization ──────────────────────────────────────────────
    const val PERSONALIZATION_ENABLED     = true
    const val INTERACTION_BOOST_WEIGHT    = 1.5f
    const val TRENDING_RECENCY_DAYS       = 7

    // ─── Quote Categories ─────────────────────────────────────────────────
    // Each entry controls which quote pool is active for wallpaper overlays
    // and what the AI WishMagic screen can generate.
    // isEnabledByDefault = true → pre-selected when user first opens app.
    enum class QuoteCategory(
        val key: String,
        val displayName: String,
        val emoji: String,
        val isEnabledByDefault: Boolean = true
    ) {
        GOOD_MORNING   ("good_morning",   "Good Morning",    "☀️",  true),
        GOOD_AFTERNOON ("good_afternoon", "Good Afternoon",  "🌤️", true),
        GOOD_EVENING   ("good_evening",   "Good Evening",    "🌅",  true),
        GOOD_NIGHT     ("good_night",     "Good Night",      "🌙",  true),
        LOVE         ("love",          "Love & Romance",  "❤️",  true),
        FOR_MOM      ("for_mom",       "For Mom",         "💐",  true),
        FOR_DAD      ("for_dad",       "For Dad",         "👨",  false),
        FOR_HUSBAND  ("for_husband",   "For Husband",     "💍",  false),
        FOR_WIFE     ("for_wife",      "For Wife",        "🌹",  false),
        FOR_FRIENDS  ("for_friends",   "For Friends",     "🤝",  false),
        MOTIVATION   ("motivation",    "Motivation",      "💪",  false),
        BIRTHDAY     ("birthday",      "Birthday Wishes", "🎂",  false);

        companion object {
            val defaults: Set<String>
                get() = entries.filter { it.isEnabledByDefault }.map { it.key }.toSet()
            fun fromKey(key: String) = entries.firstOrNull { it.key == key }
        }
    }

    // ─── Cross-App Promotion ──────────────────────────────────────────────
    // Everything you need to promote your other apps is configurable here.
    // Set FEATURE_CROSS_PROMOTION = false to hide all promo placements.
    const val FEATURE_CROSS_PROMOTION = true
    // Show one promo card every N wallpaper slots in the home feed (0 = disabled in feed)
    const val PROMO_IN_FEED_INTERVAL  = 20

    data class PromoApp(
        val id: String,
        val name: String,
        val tagline: String,
        val iconEmoji: String,        // large emoji icon shown on the promo card
        val packageName: String,
        val playStoreUrl: String,
        val badgeText: String  = "FREE",  // chip label: "NEW", "FREE", "HOT" …
        val accentColor: Long  = 0xFFFF6F00, // card accent colour (0xAARRGGBB)
        val isEnabled: Boolean = true
    )

    // ── Add your other apps below ──────────────────────────────────────────
    // Example:
    // PromoApp(
    //     id           = "bible_daily",
    //     name         = "Bible Verses Daily",
    //     tagline      = "Daily scripture & wallpapers",
    //     iconEmoji    = "📖",
    //     packageName  = "com.yourcompany.bibleverses",
    //     playStoreUrl = "https://play.google.com/store/apps/details?id=com.yourcompany.bibleverses",
    //     badgeText    = "NEW",
    //     accentColor  = 0xFF1565C0
    // )
    val PROMO_APPS: List<PromoApp> = listOf(
        // ← Paste your PromoApp entries here
    )

    // ─── Special Occasions ────────────────────────────────────────────────────
    // Auto-detected from device date. Set FEATURE_OCCASION_DETECTION = false to disable.
    const val FEATURE_OCCASION_DETECTION = true

    data class AppOccasion(
        val key: String,
        val name: String,
        val emoji: String,
        val gradientStart: Long = 0xFFFF6F00,
        val gradientEnd: Long   = 0xFF8B2500,
        val searchQuery: String = ""          // optional Pexels query override for this day
    )

    fun getCurrentOccasion(): AppOccasion? {
        if (!FEATURE_OCCASION_DETECTION) return null
        val cal   = java.util.Calendar.getInstance()
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val day   = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val dow   = cal.get(java.util.Calendar.DAY_OF_WEEK)
        return when {
            month == 1  && day == 1  ->
                AppOccasion("new_year",    "Happy New Year! 🎊",        "🎊", 0xFFFF4081, 0xFF9C27B0, "new year fireworks night sky colorful")
            month == 2  && day == 14 ->
                AppOccasion("valentine",   "Happy Valentine's Day! 💝",  "💝", 0xFFE91E63, 0xFFAD1457, "red roses hearts valentine beautiful")
            month == 3  && day == 8  ->
                AppOccasion("womens_day",  "Happy Women's Day! 🌹",      "🌹", 0xFF9C27B0, 0xFFE91E63, "purple roses flowers beautiful nature")
            month == 4  && day == 22 ->
                AppOccasion("earth_day",   "Happy Earth Day! 🌍",        "🌍", 0xFF2E7D32, 0xFF1B5E20, "earth nature green forest beautiful")
            month == 5  && isNthWeekdayOfMonth(cal, java.util.Calendar.SUNDAY, 2) ->
                AppOccasion("mothers_day", "Happy Mother's Day! 💐",     "💐", 0xFFFF6F00, 0xFFE91E63, "flowers roses mother day garden")
            month == 6  && day == 1  ->
                AppOccasion("children_day","Happy Children's Day! 🎈",   "🎈", 0xFFFF9800, 0xFFFF5722, "colorful flowers sunshine children day")
            month == 6  && isNthWeekdayOfMonth(cal, java.util.Calendar.SUNDAY, 3) ->
                AppOccasion("fathers_day", "Happy Father's Day! 👨",     "👨", 0xFF1565C0, 0xFF0D47A1, "sunrise mountain golden light strength")
            month == 10 && day == 31 ->
                AppOccasion("halloween",   "Happy Halloween! 🎃",        "🎃", 0xFFBF360C, 0xFF212121, "halloween pumpkin night dark orange")
            month == 12 && day == 24 ->
                AppOccasion("christmas_eve","Merry Christmas Eve! 🎄",   "🎄", 0xFF2E7D32, 0xFFB71C1C, "christmas snow winter stars night")
            month == 12 && day == 25 ->
                AppOccasion("christmas",   "Merry Christmas! 🎄",        "🎄", 0xFF2E7D32, 0xFFB71C1C, "christmas snow winter trees beautiful")
            month == 12 && day == 31 ->
                AppOccasion("new_year_eve","Happy New Year's Eve! 🥂",   "🥂", 0xFFFFD600, 0xFFFF6D00, "new year eve fireworks night golden")
            dow == java.util.Calendar.FRIDAY ->
                AppOccasion("friday",      "Blessed Friday! 🌟",         "✨", 0xFF37474F, 0xFF1A237E, "friday evening sunset beautiful calm")
            else -> null
        }
    }

    private fun isNthWeekdayOfMonth(cal: java.util.Calendar, dayOfWeek: Int, n: Int): Boolean {
        val test = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR,         cal.get(java.util.Calendar.YEAR))
            set(java.util.Calendar.MONTH,        cal.get(java.util.Calendar.MONTH))
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        var count = 0
        while (test.get(java.util.Calendar.MONTH) == cal.get(java.util.Calendar.MONTH)) {
            if (test.get(java.util.Calendar.DAY_OF_WEEK) == dayOfWeek) {
                count++
                if (count == n)
                    return test.get(java.util.Calendar.DAY_OF_MONTH) == cal.get(java.util.Calendar.DAY_OF_MONTH)
            }
            test.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        return false
    }

    // ─── Emotional Tone System ────────────────────────────────────────────────
    // Global persistent tone that adapts AI wish wording style.
    // Different from per-generation "mood" — this is the user's preferred expression style.
    const val FEATURE_TONE_SELECTOR          = true
    const val FEATURE_AUTO_THEME             = true
    const val FEATURE_BIOMETRIC_FAVORITES    = true
    const val FEATURE_SOCIAL_PROOF           = true
    const val FEATURE_SMART_REMINDER         = true
    const val FEATURE_RECOMMENDATION_ENGINE  = true

    enum class EmotionalTone(
        val key: String,
        val displayName: String,
        val emoji: String,
        val accentColor: Long
    ) {
        SOFT      ("soft",      "Soft",      "🌸", 0xFFFF6F9C),
        DEEP      ("deep",      "Deep",      "🌊", 0xFF1565C0),
        ROMANTIC  ("romantic",  "Romantic",  "💕", 0xFFE91E63),
        ENERGETIC ("energetic", "Energetic", "⚡", 0xFFFF6F00),
        FUNNY     ("funny",     "Funny",     "😄", 0xFF8BC34A);

        companion object {
            val default: EmotionalTone = SOFT
            fun fromKey(key: String)   = entries.firstOrNull { it.key == key } ?: default
        }
    }

    // ─── Micro Social Proof ───────────────────────────────────────────────────
    // Deterministic simulated share counter — no external tracking required.
    fun getSimulatedShareCount(wallpaperId: String): String {
        val base     = kotlin.math.abs(wallpaperId.hashCode()) % 4200 + 600
        val weekSeed = (System.currentTimeMillis() / (7L * 24 * 3600 * 1000)) % 900
        val count    = (base + weekSeed).toInt()
        return if (count >= 1000) "${count / 1000},${"%03d".format(count % 1000)}" else "$count"
    }

    // ─── Smart Reminder: compute optimal notification hour from usage history ─
    fun computeSmartReminder(openHoursStr: String): Pair<Int, Int>? {
        val hours   = openHoursStr.split(",").mapNotNull { it.toIntOrNull() }
        if (hours.size < 5) return null
        val morning = hours.filter { it in 5..11 }.groupBy { it }.maxByOrNull { it.value.size }?.key
        val night   = hours.filter { it in 19..23 }.groupBy { it }.maxByOrNull { it.value.size }?.key
        return if (morning != null || night != null) Pair(morning ?: 7, night ?: 21) else null
    }

    // ─── Quote Overlay Styles ─────────────────────────────────────────────────
    // Users can cycle between these in the detail screen — unique to this app.
    enum class OverlayStyle(val displayName: String, val emoji: String) {
        GLOW    ("Glow",    "✨"),   // white italic on dark blur box  (default)
        MINIMAL ("Minimal", "🤍"),   // barely-there text, subtle shadow, no box
        WARM    ("Warm",    "🔥"),   // amber/gold on deep-dark background
        NEON    ("Neon",    "💜")    // vibrant violet/cyan on dark semi-transparent
    }

    // ─── Helper ───────────────────────────────────────────────────────
    fun getNicheQuery(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour in MORNING_START_HOUR until MORNING_END_HOUR -> MORNING_QUERIES.random()
            else -> NIGHT_QUERIES.random()
        }
    }

    fun isMorningTime(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour in MORNING_START_HOUR until MORNING_END_HOUR
    }

    /** Backward compatibility: true when hour is in afternoon (12–17). */
    fun isAfternoonTime(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour in AFTERNOON_START_HOUR until AFTERNOON_END_HOUR
    }

    /** Backward compatibility: true when hour is in evening (17–21). */
    fun isEveningTime(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour in EVENING_START_HOUR until EVENING_END_HOUR
    }

    fun isNightTime(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour >= NIGHT_START_HOUR || hour < MORNING_START_HOUR
    }

    /** Peak sharing times: 7–8 AM and 9–10 PM — when users most often send wishes */
    fun isPeakShareTime(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour in 7..8 || hour in 21..22
    }

    /** Suggests overlay style based on wallpaper category for best visual harmony */
    fun getSuggestedOverlayStyle(category: String): OverlayStyle = when {
        category.contains("Love", ignoreCase = true) ||
        category.contains("Romantic", ignoreCase = true) -> OverlayStyle.WARM
        category.contains("Night", ignoreCase = true) ||
        category.contains("Dream", ignoreCase = true) ||
        category.contains("Spiritual", ignoreCase = true) -> OverlayStyle.NEON
        category.contains("Morning", ignoreCase = true) ||
        category.contains("Sunrise", ignoreCase = true) -> OverlayStyle.WARM
        else -> OverlayStyle.GLOW
    }

    fun getGreeting(): String {
        val cal = java.util.Calendar.getInstance()
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val tod = TimeOfDay.current()
        return when {
            dow == java.util.Calendar.FRIDAY && tod == TimeOfDay.MORNING   -> "✨ Blessed Friday Morning!"
            dow == java.util.Calendar.FRIDAY && tod == TimeOfDay.AFTERNOON -> "✨ Blessed Friday Afternoon!"
            dow == java.util.Calendar.FRIDAY && tod == TimeOfDay.EVENING   -> "✨ Blessed Friday Evening!"
            dow == java.util.Calendar.FRIDAY                               -> "🌙 Blessed Friday Night!"
            dow == java.util.Calendar.SATURDAY && tod == TimeOfDay.MORNING -> "☀️ Happy Saturday!"
            dow == java.util.Calendar.SUNDAY   && tod == TimeOfDay.MORNING -> "🙏 Blessed Sunday Morning!"
            dow == java.util.Calendar.MONDAY   && tod == TimeOfDay.MORNING -> "💪 Rise & Shine, Monday!"
            else -> when (tod) {
                TimeOfDay.MORNING   -> "☀️ Good Morning!"
                TimeOfDay.AFTERNOON -> "🌤️ Good Afternoon!"
                TimeOfDay.EVENING   -> "🌅 Good Evening!"
                TimeOfDay.NIGHT     -> "🌙 Good Night!"
            }
        }
    }

    fun getGreetingSubtitle(): String {
        val cal = java.util.Calendar.getInstance()
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val tod = TimeOfDay.current()
        return when (dow) {
            java.util.Calendar.MONDAY -> when (tod) {
                TimeOfDay.MORNING   -> "New week, new blessings ahead 💪"
                TimeOfDay.AFTERNOON -> "Tuesday's potential is just around the corner ✨"
                TimeOfDay.EVENING   -> "You made Monday count — rest well 🌙"
                TimeOfDay.NIGHT     -> "Rest well — tomorrow is a fresh start 🌙"
            }
            java.util.Calendar.TUESDAY -> when (tod) {
                TimeOfDay.MORNING   -> "Tuesday's potential is all yours ✨"
                TimeOfDay.AFTERNOON -> "Keep the momentum going — you're doing great 🌟"
                TimeOfDay.EVENING   -> "You made Tuesday shine 🌙"
                TimeOfDay.NIGHT     -> "Sweet dreams — Wednesday awaits ✨"
            }
            java.util.Calendar.WEDNESDAY -> when (tod) {
                TimeOfDay.MORNING   -> "Midweek magic — keep going! 🌟"
                TimeOfDay.AFTERNOON -> "Halfway there — you're amazing 💫"
                TimeOfDay.EVENING   -> "Wednesday evening — almost Friday! 🌅"
                TimeOfDay.NIGHT     -> "Halfway through, dream beautifully 🌙"
            }
            java.util.Calendar.THURSDAY -> when (tod) {
                TimeOfDay.MORNING   -> "Almost Friday — make it count! ☀️"
                TimeOfDay.AFTERNOON -> "One more push — the weekend is near! 🌤️"
                TimeOfDay.EVENING   -> "Thursday done — tomorrow is your victory lap 🌙"
                TimeOfDay.NIGHT     -> "Thursday done, dream beautifully 🌙"
            }
            java.util.Calendar.FRIDAY -> when (tod) {
                TimeOfDay.MORNING   -> "Best day of the week — enjoy it! 🎉"
                TimeOfDay.AFTERNOON -> "Weekend vibes are here — savour the moment ✨"
                TimeOfDay.EVENING   -> "Weekend begins — you deserve this rest ✨"
                TimeOfDay.NIGHT     -> "Blessed Friday night — rest well 🌙"
            }
            java.util.Calendar.SATURDAY -> when (tod) {
                TimeOfDay.MORNING   -> "Your weekend, your joy 🌸"
                TimeOfDay.AFTERNOON -> "Saturday afternoon — make it memorable ☀️"
                TimeOfDay.EVENING   -> "Saturday evening magic — savour every moment ✨"
                TimeOfDay.NIGHT     -> "Saturday night magic — savour every moment ✨"
            }
            java.util.Calendar.SUNDAY -> when (tod) {
                TimeOfDay.MORNING   -> "A day of rest, love and gratitude 🙏"
                TimeOfDay.AFTERNOON -> "Peaceful Sunday — recharge and reflect 🌤️"
                TimeOfDay.EVENING   -> "New week starts tomorrow — rest in peace 🌙"
                TimeOfDay.NIGHT     -> "New week starts tomorrow — rest in peace 🌙"
            }
            else -> when (tod) {
                TimeOfDay.MORNING   -> "Beautiful morning greetings just for you ☀️"
                TimeOfDay.AFTERNOON -> "Sunny afternoon wishes to brighten your day 🌤️"
                TimeOfDay.EVENING   -> "Warm evening greetings for a peaceful night 🌅"
                TimeOfDay.NIGHT     -> "Heartfelt night wishes for sweet dreams 🌙"
            }
        }
    }

    // ─── Wishes & Greetings ONLY ─────────────────────────────────────────────────
    data class Quote(val text: String, val author: String)

    fun getQuoteOfDay(): Quote {
        val remote = com.offline.wallcorepro.util.RemoteConfigManager.quotesOfDay
        if (remote.isNotEmpty()) {
            val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
            return remote[dayOfYear % remote.size]
        }
        // Local fallback: use WishQuotePool's larger pool for better variety
        return com.offline.wallcorepro.data.local.WishQuotePool.getQuoteOfDay()
    }
}

