package com.offline.wallcorepro.di

import com.offline.wallcorepro.data.local.WallCoreDatabase
import com.offline.wallcorepro.data.network.PexelsApiService
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
        pexelsApiService: PexelsApiService,
        pixabayApiService: com.offline.wallcorepro.data.network.PixabayApiService,
        preferenceManager: PreferenceManager,
        aiService: AiService
    ): WallpaperRepository {
        return WallpaperRepositoryImpl(
            database, 
            apiService, 
            pexelsApiService, 
            pixabayApiService, 
            preferenceManager,
            aiService
        )
    }
}
