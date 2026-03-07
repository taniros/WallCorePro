package com.offline.wallcorepro.data.local

import com.offline.wallcorepro.data.local.dao.WallpaperDao
import com.offline.wallcorepro.data.mapper.toEntity
import com.offline.wallcorepro.data.model.WallpaperDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperDataSeed @Inject constructor(
    private val wallpaperDao: WallpaperDao
) {
    suspend fun seedIfNeeded(niche: String) {
        val count = wallpaperDao.getWallpaperCount(niche)
        if (count == 0) {
            val seedDtos = when (niche.uppercase()) {
                "GOOD_MORNING" -> listOf(
                    WallpaperDto(
                        id = "seed_1",
                        title = "Golden Sunrise Over Mountains",
                        imageUrl = "https://images.pexels.com/photos/1032650/pexels-photo-1032650.jpeg",
                        thumbnailUrl = "https://images.pexels.com/photos/1032650/pexels-photo-1032650.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500",
                        photographer = "Johannes Plenio",
                        photographerUrl = "https://www.pexels.com/@jplenio",
                        category = "Morning Greetings",
                        niche = "GOOD_MORNING",
                        dominantColor = "#FF6F00",
                        isTrending = true
                    ),
                    WallpaperDto(
                        id = "seed_2",
                        title = "Morning Mist in the Forest",
                        imageUrl = "https://images.pexels.com/photos/15286/pexels-photo.jpg",
                        thumbnailUrl = "https://images.pexels.com/photos/15286/pexels-photo.jpg?auto=compress&cs=tinysrgb&dpr=1&w=500",
                        photographer = "Luis del Río",
                        photographerUrl = "https://www.pexels.com/@luis-del-rio-137597",
                        category = "Daily Blessings",
                        niche = "GOOD_MORNING",
                        dominantColor = "#4CAF50"
                    ),
                    WallpaperDto(
                        id = "seed_3",
                        title = "Romantic Roses at Dawn",
                        imageUrl = "https://images.pexels.com/photos/56866/garden-rose-red-pink-56866.jpeg",
                        thumbnailUrl = "https://images.pexels.com/photos/56866/garden-rose-red-pink-56866.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500",
                        photographer = "Pixabay",
                        photographerUrl = "https://www.pexels.com/@pixabay",
                        category = "Romantic Greetings",
                        niche = "GOOD_MORNING",
                        dominantColor = "#C62828",
                        isTrending = true
                    ),
                    WallpaperDto(
                        id = "seed_4",
                        title = "Morning Coffee & Flowers",
                        imageUrl = "https://images.pexels.com/photos/1118448/pexels-photo-1118448.jpeg",
                        thumbnailUrl = "https://images.pexels.com/photos/1118448/pexels-photo-1118448.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500",
                        photographer = "Valeria Boltneva",
                        photographerUrl = "https://www.pexels.com/@valeria-boltneva-245641",
                        category = "Friends Greetings",
                        niche = "GOOD_MORNING",
                        dominantColor = "#795548"
                    ),
                    WallpaperDto(
                        id = "seed_5",
                        title = "Starry Night Over Calm Lake",
                        imageUrl = "https://images.pexels.com/photos/1624496/pexels-photo-1624496.jpeg",
                        thumbnailUrl = "https://images.pexels.com/photos/1624496/pexels-photo-1624496.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500",
                        photographer = "Eberhard Grossgasteiger",
                        photographerUrl = "https://www.pexels.com/@eberhardgross",
                        category = "Night Wishes",
                        niche = "GOOD_MORNING",
                        dominantColor = "#001845",
                        isTrending = true
                    ),
                    WallpaperDto(
                        id = "seed_6",
                        title = "Moonlit Romantic Night",
                        imageUrl = "https://images.pexels.com/photos/956999/pexels-photo-956999.jpeg",
                        thumbnailUrl = "https://images.pexels.com/photos/956999/pexels-photo-956999.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500",
                        photographer = "Felix Mittermeier",
                        photographerUrl = "https://www.pexels.com/@felixmittermeier",
                        category = "Nightly Love",
                        niche = "GOOD_MORNING",
                        dominantColor = "#240046"
                    ),
                    WallpaperDto(
                        id = "seed_7",
                        title = "Sunrise Love Couple",
                        imageUrl = "https://images.pexels.com/photos/3184291/pexels-photo-3184291.jpeg",
                        thumbnailUrl = "https://images.pexels.com/photos/3184291/pexels-photo-3184291.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500",
                        photographer = "fauxels",
                        photographerUrl = "https://www.pexels.com/@fauxels",
                        category = "Morning Love",
                        niche = "GOOD_MORNING",
                        dominantColor = "#FF8F00",
                        isTrending = true
                    ),
                    WallpaperDto(
                        id = "seed_8",
                        title = "Sweet Dreams Moon & Stars",
                        imageUrl = "https://images.pexels.com/photos/1252890/pexels-photo-1252890.jpeg",
                        thumbnailUrl = "https://images.pexels.com/photos/1252890/pexels-photo-1252890.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500",
                        photographer = "Hristo Fidanov",
                        photographerUrl = "https://www.pexels.com/@hristo-fidanov-520662",
                        category = "Sweet Night Dreams",
                        niche = "GOOD_MORNING",
                        dominantColor = "#1A237E"
                    )
                )
                else -> emptyList()
            }
            if (seedDtos.isNotEmpty()) {
                val entities = seedDtos.map { it.toEntity(niche) }
                wallpaperDao.insertWallpapers(entities)
            }
        }
    }
}
