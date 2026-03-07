package com.offline.wallcorepro.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.repository.PreferenceManager
import com.offline.wallcorepro.domain.usecase.SyncWallpapersUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that syncs fresh wallpapers in the background.
 * Runs on periodic schedule and on app fork via enqueueUniquePeriodicWork.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncWallpapersUseCase: SyncWallpapersUseCase,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("SyncWorker: Starting background sync for niche ${AppConfig.NICHE_TYPE}")
            val result = syncWallpapersUseCase(force = false)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                Timber.d("SyncWorker: Synced $count wallpapers")
                Result.success()
            } else {
                Timber.w("SyncWorker: Sync failed - ${result.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Exception during sync")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "wallcore_sync_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                AppConfig.SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun scheduleImmediately(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
