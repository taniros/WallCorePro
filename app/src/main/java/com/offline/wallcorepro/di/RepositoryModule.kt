package com.offline.wallcorepro.di

import com.offline.wallcorepro.data.local.WallCoreDatabase
import com.offline.wallcorepro.data.network.WallpaperApiService
import com.offline.wallcorepro.data.network.AiService
import com.offline.wallcorepro.data.repository.PreferenceManager
import com.offline.wallcorepro.data.repository.WallpaperRepositoryImpl
import com.offline.wallcorepro.domain.repository.WallpaperRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideWallpaperRepository(
        database: WallCoreDatabase,
        apiService: WallpaperApiService,
        preferenceManager: PreferenceManager,
        aiService: AiService
    ): WallpaperRepository = WallpaperRepositoryImpl(
        database,
        apiService,
        preferenceManager,
        aiService
    )
}
