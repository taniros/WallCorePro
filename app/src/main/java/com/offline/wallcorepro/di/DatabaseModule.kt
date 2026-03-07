package com.offline.wallcorepro.di

import android.content.Context
import androidx.room.Room
import com.offline.wallcorepro.data.local.WallCoreDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WallCoreDatabase {
        return Room.databaseBuilder(
            context,
            WallCoreDatabase::class.java,
            WallCoreDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideWallpaperDao(database: WallCoreDatabase) = database.wallpaperDao()

    @Provides
    fun provideCategoryDao(database: WallCoreDatabase) = database.categoryDao()

    @Provides
    fun provideRemoteKeysDao(database: WallCoreDatabase) = database.remoteKeysDao()
}
