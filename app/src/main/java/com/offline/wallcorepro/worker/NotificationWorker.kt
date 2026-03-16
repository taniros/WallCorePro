package com.offline.wallcorepro.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URL
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
            val quote      = WishQuotePool.getNotificationQuote(timeOfDay)
            val userName   = preferenceManager.userName.first()
            val nameClause = if (userName.isNotBlank()) ", $userName" else ""
            val greeting   = "${timeOfDay.emoji} ${timeOfDay.displayName}$nameClause!"

            val notifId = when (timeOfDay) {
                AppConfig.TimeOfDay.MORNING   -> AppConfig.MORNING_NOTIFICATION_ID
                AppConfig.TimeOfDay.AFTERNOON -> AppConfig.AFTERNOON_NOTIFICATION_ID
                AppConfig.TimeOfDay.EVENING   -> AppConfig.EVENING_NOTIFICATION_ID
                AppConfig.TimeOfDay.NIGHT     -> AppConfig.NIGHT_NOTIFICATION_ID
            }

            // Fetch a cached wallpaper to use as the notification hero image.
            // Falls back to text-only if nothing is cached yet (e.g. first launch).
            val wallpaper = try { getRandomWallpaper(AppConfig.NICHE_TYPE) } catch (_: Exception) { null }
            val thumbUrl  = wallpaper?.thumbnailUrl?.ifBlank { wallpaper.imageUrl } ?: ""
            val heroBitmap: Bitmap? = if (thumbUrl.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    try {
                        BitmapFactory.decodeStream(URL(thumbUrl).openStream())
                    } catch (e: Exception) {
                        Timber.w(e, "NotificationWorker: thumbnail download failed, using text-only")
                        null
                    }
                }
            } else null

            showNotification(
                id         = notifId,
                title      = greeting,
                body       = quote,
                heroBitmap = heroBitmap,
                wallpaperId = wallpaper?.id
            )
            Timber.d("NotificationWorker: ${timeOfDay.displayName} notification sent (hasBitmap=${heroBitmap != null})")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "NotificationWorker failed")
            Result.failure()
        }
    }

    private fun showNotification(
        id: Int,
        title: String,
        body: String,
        heroBitmap: Bitmap? = null,
        wallpaperId: String? = null
    ) {
        // Main tap: open app home (or deep-link to specific wallpaper)
        val mainUri   = if (!wallpaperId.isNullOrBlank())
            Uri.parse("wallcorepro://detail/$wallpaperId")
        else
            Uri.parse("wallcorepro://home")
        val mainIntent = Intent(Intent.ACTION_VIEW, mainUri, applicationContext, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val mainPending = PendingIntent.getActivity(
            applicationContext, id, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action button: "See More" — opens home screen
        val browseIntent = Intent(Intent.ACTION_VIEW, Uri.parse("wallcorepro://home"),
            applicationContext, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val browsePending = PendingIntent.getActivity(
            applicationContext, id + 1000, browseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, AppConfig.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(mainPending)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "See More 🌅",
                browsePending
            )

        if (heroBitmap != null) {
            // Rich notification: large wallpaper preview in expanded state,
            // thumbnail icon in collapsed state — higher tap rate than text-only.
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(heroBitmap)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(body)
            )
            builder.setLargeIcon(heroBitmap)
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(id, builder.build())
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
