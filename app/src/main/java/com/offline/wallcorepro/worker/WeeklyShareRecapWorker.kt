package com.offline.wallcorepro.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.offline.wallcorepro.MainActivity
import com.offline.wallcorepro.R
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.repository.PreferenceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Weekly notification: "You shared X wishes this week! 🌟"
 * Drives re-engagement and reminds users of the app's value.
 */
@HiltWorker
class WeeklyShareRecapWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!AppConfig.FEATURE_WEEKLY_SHARE_RECAP) return Result.success()

        val shares = preferenceManager.getSharesThisWeek()
        if (shares <= 0) {
            Timber.d("WeeklyShareRecap: no shares this week — skip")
            return Result.success()
        }

        return try {
            val message = when (shares) {
                1 -> "You shared 1 wish this week! Keep spreading love 💕"
                else -> "You shared $shares wishes this week! You're amazing 🌟"
            }
            showNotification(message)
            preferenceManager.resetWeeklyShareCount()
            Timber.d("WeeklyShareRecap: sent ($shares shares)")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "WeeklyShareRecap failed")
            Result.failure()
        }
    }

    private fun showNotification(body: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, AppConfig.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("✨ Your Weekly Recap")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val WORK_NAME = "wallcore_weekly_share_recap"
        private const val NOTIFICATION_ID = 2003

        fun schedule(context: Context) {
            if (!AppConfig.FEATURE_WEEKLY_SHARE_RECAP) return
            val request = PeriodicWorkRequestBuilder<WeeklyShareRecapWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
