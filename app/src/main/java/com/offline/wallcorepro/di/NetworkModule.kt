package com.offline.wallcorepro.di

import com.offline.wallcorepro.BuildConfig
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.network.AiApiService
import com.offline.wallcorepro.data.network.WallpaperApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideWallpaperApiService(retrofit: Retrofit): WallpaperApiService =
        retrofit.create(WallpaperApiService::class.java)

    @Provides
    @Singleton
    fun provideAiApiService(retrofit: Retrofit): AiApiService =
        retrofit.create(AiApiService::class.java)
}
