package com.offline.wallcorepro.data.local.dao

import androidx.room.*
import com.offline.wallcorepro.data.local.entity.WallpaperRemoteKeys

@Dao
interface RemoteKeysDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKeys: List<WallpaperRemoteKeys>)

    @Query("SELECT * FROM wallpaper_remote_keys WHERE wallpaperId = :wallpaperId")
    suspend fun remoteKeysById(wallpaperId: String): WallpaperRemoteKeys?

    @Query("DELETE FROM wallpaper_remote_keys")
    suspend fun clearRemoteKeys()

    @Query("SELECT * FROM wallpaper_remote_keys ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastCreationTime(): WallpaperRemoteKeys?
}
