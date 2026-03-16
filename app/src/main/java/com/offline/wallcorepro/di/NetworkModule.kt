package com.offline.wallcorepro.di

import android.content.Context
import com.offline.wallcorepro.BuildConfig
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.network.AiApiService
import com.offline.wallcorepro.data.network.WallpaperApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.Dispatcher
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
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        // HEADERS not BODY — logging the full response body of 24 parallel warm-up
        // requests adds significant overhead and noise in logcat during fast scrolling.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.HEADERS
            else
                HttpLoggingInterceptor.Level.NONE
        }
        // OkHttp default is maxRequestsPerHost=5.  The warm-up fires up to 10 parallel
        // requests to the same host — without this fix they all queue behind 5 slots.
        val dispatcher = Dispatcher().apply {
            maxRequests        = 64
            maxRequestsPerHost = 20   // headroom for Coil + API requests combined
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            // 10 idle connections kept alive for 5 min.  Each idle socket holds ~64 KB
            // of kernel buffers; 10 = ~640 KB vs 20 = ~1.3 MB.  10 still covers the
            // Phase-2 parallel blast (10 seeds) and simultaneous Coil fetches.
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            // 10 MB on-disk cache for API JSON responses.  Speeds up resumes when the
            // server is still warm: cache returns in <1 ms instead of a round-trip.
            .cache(Cache(
                directory = context.cacheDir.resolve("okhttp_api_cache"),
                maxSize   = 10L * 1024 * 1024
            ))
            .dispatcher(dispatcher)
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
