package com.offline.wallcorepro.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import androidx.work.NetworkType
import com.offline.wallcorepro.MainActivity
import com.offline.wallcorepro.R
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.WishQuotePool
import com.offline.wallcorepro.data.repository.PreferenceManager
import com.offline.wallcorepro.domain.usecase.GetRandomWallpaperUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Sends Good Morning (7 AM), Good Afternoon (2 PM), Good Evening (7 PM), and Good Night (9 PM)
 * notifications with beautiful niche-specific quotes. Respects the user's per-notification toggles.
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getRandomWallpaper: GetRandomWallpaperUseCase,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!AppConfig.FEATURE_DAILY_NOTIFICATION) return Result.success()

        val timeOfDayKey = inputData.getString(KEY_TIME_OF_DAY) ?: "morning"
        val timeOfDay = AppConfig.TimeOfDay.entries.firstOrNull { it.key == timeOfDayKey }
            ?: AppConfig.TimeOfDay.MORNING

        // Respect the user's per-notification toggle from Settings
        val isEnabled = when (timeOfDay) {
            AppConfig.TimeOfDay.MORNING   -> preferenceManager.isMorningNotifEnabled.first()
            AppConfig.TimeOfDay.AFTERNOON -> preferenceManager.isAfternoonNotifEnabled.first()
            AppConfig.TimeOfDay.EVENING   -> preferenceManager.isEveningNotifEnabled.first()
            AppConfig.TimeOfDay.NIGHT     -> preferenceManager.isNightNotifEnabled.first()
        }

        if (!isEnabled) {
            Timber.d("NotificationWorker: ${timeOfDay.displayName} notification is disabled by user — skipping")
            return Result.success()
        }

        return try {
            val quote    = WishQuotePool.getNotificationQuote(timeOfDay)
            val userName = preferenceManager.userName.first()
            val nameClause = if (userName.isNotBlank()) ", $userName" else ""
            val greeting = "${timeOfDay.emoji} ${timeOfDay.displayName}$nameClause!"

            val (notifId, _) = when (timeOfDay) {
                AppConfig.TimeOfDay.MORNING   -> AppConfig.MORNING_NOTIFICATION_ID to "Morning"
                AppConfig.TimeOfDay.AFTERNOON -> AppConfig.AFTERNOON_NOTIFICATION_ID to "Afternoon"
                AppConfig.TimeOfDay.EVENING   -> AppConfig.EVENING_NOTIFICATION_ID to "Evening"
                AppConfig.TimeOfDay.NIGHT     -> AppConfig.NIGHT_NOTIFICATION_ID to "Night"
            }

            showNotification(id = notifId, title = greeting, body = quote)
            Timber.d("NotificationWorker: ${timeOfDay.displayName} notification sent")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "NotificationWorker failed")
            Result.failure()
        }
    }

    private fun showNotification(id: Int, title: String, body: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, AppConfig.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(id, notification)
    }

    companion object {
        const val WORK_NAME_MORNING   = "wallcore_morning_notification"
        const val WORK_NAME_AFTERNOON = "wallcore_afternoon_notification"
        const val WORK_NAME_EVENING   = "wallcore_evening_notification"
        const val WORK_NAME_NIGHT     = "wallcore_night_notification"
        const val KEY_TIME_OF_DAY     = "time_of_day"

        /** Schedule all 4 notification workers: Morning (7 AM), Afternoon (2 PM), Evening (7 PM), Night (9 PM). */
        fun schedule(context: Context) {
            if (!AppConfig.FEATURE_DAILY_NOTIFICATION) return
            scheduleMorning(context)
            scheduleAfternoon(context)
            scheduleEvening(context)
            scheduleNight(context)
        }

        fun scheduleMorning(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                .setInputData(workDataOf(KEY_TIME_OF_DAY to "morning"))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .setInitialDelay(calculateDelayToHour(AppConfig.MORNING_NOTIFICATION_HOUR), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_MORNING, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun scheduleAfternoon(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                .setInputData(workDataOf(KEY_TIME_OF_DAY to "afternoon"))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .setInitialDelay(calculateDelayToHour(AppConfig.AFTERNOON_NOTIFICATION_HOUR), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_AFTERNOON, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun scheduleEvening(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                .setInputData(workDataOf(KEY_TIME_OF_DAY to "evening"))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .setInitialDelay(calculateDelayToHour(AppConfig.EVENING_NOTIFICATION_HOUR), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_EVENING, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun scheduleNight(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                .setInputData(workDataOf(KEY_TIME_OF_DAY to "night"))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .setInitialDelay(calculateDelayToHour(AppConfig.NIGHT_NOTIFICATION_HOUR), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_NIGHT, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun cancelMorning(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_MORNING)
        }

        fun cancelAfternoon(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_AFTERNOON)
        }

        fun cancelEvening(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_EVENING)
        }

        fun cancelNight(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_NIGHT)
        }

        private fun calculateDelayToHour(hour: Int): Long {
            val now    = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return (target.timeInMillis - now.timeInMillis).coerceAtLeast(1000L)
        }
    }
}
