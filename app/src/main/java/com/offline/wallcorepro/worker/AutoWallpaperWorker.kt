package com.offline.wallcorepro.worker

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.repository.PreferenceManager
import com.offline.wallcorepro.domain.model.WallpaperTarget
import com.offline.wallcorepro.domain.usecase.GetRandomWallpaperUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that automatically changes wallpaper at the configured interval.
 * Target (Home/Lock/Both) is read from DataStore preferences.
 */
@HiltWorker
class AutoWallpaperWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getRandomWallpaperUseCase: GetRandomWallpaperUseCase,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val target = preferenceManager.autoWallpaperTarget.first()
            val wallpaper = getRandomWallpaperUseCase(AppConfig.NICHE_TYPE)
                ?: return Result.retry()

            val inputStream = URL(wallpaper.imageUrl).openStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)

            when (target) {
                "HOME" -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                "LOCK" -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                "BOTH" -> {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
            }

            Timber.d("AutoWallpaperWorker: Set wallpaper ${wallpaper.id} to $target")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "AutoWallpaperWorker failed")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "wallcore_auto_wallpaper_work"

        fun schedule(context: Context, intervalHours: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoWallpaperWorker>(
                intervalHours,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
