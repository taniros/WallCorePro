package com.offline.wallcorepro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.offline.wallcorepro.data.local.dao.CategoryDao
import com.offline.wallcorepro.data.local.dao.RemoteKeysDao
import com.offline.wallcorepro.data.local.dao.WallpaperDao
import com.offline.wallcorepro.data.local.entity.CategoryEntity
import com.offline.wallcorepro.data.local.entity.WallpaperEntity
import com.offline.wallcorepro.data.local.entity.WallpaperRemoteKeys

@Database(
    entities = [
        WallpaperEntity::class,
        CategoryEntity::class,
        WallpaperRemoteKeys::class
    ],
    version = 4,
    exportSchema = true
)
abstract class WallCoreDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun categoryDao(): CategoryDao
    abstract fun remoteKeysDao(): RemoteKeysDao

    companion object {
        const val DATABASE_NAME = "wallcore_database"
    }
}
