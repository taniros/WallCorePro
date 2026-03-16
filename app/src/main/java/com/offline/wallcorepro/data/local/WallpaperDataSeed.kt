package com.offline.wallcorepro.data.local

import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.dao.WallpaperDao
import com.offline.wallcorepro.data.local.entity.WallpaperEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the local database with 250 high-quality wallpapers on fresh install.
 *
 * WHY: Render free-tier backend takes 20-60 s to wake from sleep.  Without seeds
 * the user sees a blank / shimmer screen for up to a minute on every cold start.
 * With seeds they see 250 beautiful images INSTANTLY (pure DB read, zero network).
 *
 * SOURCES:
 *   • 10 curated Pexels nature photos  (known IDs, no people)
 *   • 240 Picsum Photos (stable Unsplash CDN, IDs 10-249, no auth required)
 *
 * ORDERING TRICK: all seed items use cachedAt = SEED_CACHED_AT (≈ epoch + 1s).
 * The feed is sorted ORDER BY cachedAt DESC, so freshly-fetched backend items
 * (cachedAt = System.currentTimeMillis()) always float to the TOP automatically.
 * Seed items provide a rich visible base while the backend warms up, then quietly
 * sink to the bottom as real content accumulates.
 */
@Singleton
class WallpaperDataSeed @Inject constructor(
    private val wallpaperDao: WallpaperDao
) {
    companion object {
        // Use (CACHE_MAX_AGE_DAYS - 1) days ago so seeds:
        //  (a) survive the age-based cache cleanup (older threshold = CACHE_MAX_AGE_DAYS days)
        //  (b) are replaced by warm-up content on the pendingCacheClear pass (cachedAt < sessionStartMs)
        // Feed ordering uses ORDER BY shuffleKey (random), not cachedAt, so position is unaffected.
        // Computed at access time so every seed run uses the current clock.
        private val seedCachedAt: Long
            get() = System.currentTimeMillis() - ((AppConfig.CACHE_MAX_AGE_DAYS - 1) * 24 * 3_600 * 1_000)
    }

    suspend fun seedIfNeeded(niche: String) {
        val count = wallpaperDao.getWallpaperCount(niche)
        // Run if DB is brand-new OR if only the old 8-item seed is present (count < 50).
        if (count < 50) {
            val ts = seedCachedAt
            val seeds = buildSeeds(niche, ts)
            wallpaperDao.insertWallpapers(seeds)
            Timber.d("WallpaperDataSeed: seeded ${seeds.size} wallpapers for $niche (was $count)")
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun pexels(
        id: String, pexelsId: Long, title: String,
        category: String, color: String, niche: String, ts: Long, trending: Boolean = false
    ) = WallpaperEntity(
        id            = id,
        title         = title,
        imageUrl      = "https://images.pexels.com/photos/$pexelsId/pexels-photo-$pexelsId.jpeg?auto=compress&cs=tinysrgb&w=1080",
        thumbnailUrl  = "https://images.pexels.com/photos/$pexelsId/pexels-photo-$pexelsId.jpeg?auto=compress&cs=tinysrgb&w=400",
        category      = category,
        niche         = niche,
        dominantColor = color,
        isTrending    = trending,
        isPremium     = false,
        createdAt     = ts,
        downloadsCount = if (trending) 500 else 100,
        cachedAt      = ts
    )

    private fun picsum(
        picsumId: Int, title: String,
        category: String, color: String, niche: String, ts: Long, trending: Boolean = false
    ) = WallpaperEntity(
        id            = "gm_p$picsumId",
        title         = title,
        imageUrl      = "https://picsum.photos/id/$picsumId/1080/1920",
        thumbnailUrl  = "https://picsum.photos/id/$picsumId/400/600",
        category      = category,
        niche         = niche,
        dominantColor = color,
        isTrending    = trending,
        isPremium     = false,
        createdAt     = ts,
        downloadsCount = if (trending) 500 else 100,
        cachedAt      = ts
    )

    // ── data ──────────────────────────────────────────────────────────────────

    private fun buildSeeds(niche: String, ts: Long): List<WallpaperEntity> = buildList {

        // ── 10 curated Pexels nature photos (no people, verified stable IDs) ──
        add(pexels("gm_s01", 1032650, "Golden Sunrise Over Mountains",  "Morning Greetings",  "#FF8F00", niche, ts, trending = true))
        add(pexels("gm_s02",   15286, "Morning Mist in the Forest",     "Daily Blessings",    "#4CAF50", niche, ts))
        add(pexels("gm_s03",   56866, "Red Roses at Dawn",              "Romantic Greetings", "#C62828", niche, ts, trending = true))
        add(pexels("gm_s04", 1624496, "Starry Night Over the Lake",     "Night Wishes",       "#001845", niche, ts, trending = true))
        add(pexels("gm_s05",  956999, "Moonlit Night Sky",              "Nightly Love",       "#240046", niche, ts))
        add(pexels("gm_s06", 1252890, "Sweet Dreams Moon and Stars",    "Sweet Night Dreams", "#1A237E", niche, ts, trending = true))
        add(pexels("gm_s07",  462162, "Mountain Peaks at Sunrise",      "Morning Greetings",  "#FF6F00", niche, ts))
        add(pexels("gm_s08",  248797, "Lavender Field at Golden Hour",  "Afternoon Wishes",   "#9C27B0", niche, ts, trending = true))
        add(pexels("gm_s09",  325257, "Misty Alpine Valley",            "Spiritual Morning",  "#1565C0", niche, ts))
        add(pexels("gm_s10",  380768, "Autumn Forest Path",             "Evening Greetings",  "#E65100", niche, ts))

        // ── 240 Picsum Photos (IDs 10-249) ───────────────────────────────────
        val categories = listOf(
            "Morning Greetings"  to "#FF8F00",
            "Afternoon Wishes"   to "#FF6F00",
            "Evening Greetings"  to "#E65100",
            "Night Wishes"       to "#311B92",
            "Morning Love"       to "#E91E63",
            "Afternoon Love"     to "#FF4081",
            "Evening Love"       to "#9C27B0",
            "Nightly Love"       to "#4A148C",
            "Family Wishes"      to "#F57F17",
            "Friends Greetings"  to "#00BCD4",
            "Daily Blessings"    to "#FFC107",
            "Spiritual Morning"  to "#1565C0",
            "Sweet Night Dreams" to "#1A237E",
            "Romantic Greetings" to "#C62828"
        )

        val titles = listOf(
            "Golden Sunrise", "Radiant Dawn", "Morning Glow", "First Light",
            "Sunrise Sky", "Golden Hour", "Morning Mist", "Amber Horizon",
            "Dawn Break", "Peaceful Morning", "Sunrise Over Hills", "Soft Dawn",
            "Mountain Sunrise", "Forest Morning", "Misty Sunrise", "Rose Sunrise",
            "Colorful Dawn", "Twilight Glow", "Dusk Beauty", "Starry Heaven",
            "Moonlit Sky", "Sunset Colors", "Evening Light", "Purple Dusk",
            "Orange Sunset", "Evening Peace", "Galaxy View", "Cosmic Night",
            "Night Serenity", "Sunrise Garden", "Morning Radiance", "Warm Daybreak"
        )

        for (i in 0 until 240) {
            val picsumId  = i + 10
            val (cat, color) = categories[i % categories.size]
            val title     = "${titles[i % titles.size]} ${i + 1}"
            val trending  = (i % 24 == 0)
            add(picsum(picsumId, title, cat, color, niche, ts, trending))
        }
    }
}
