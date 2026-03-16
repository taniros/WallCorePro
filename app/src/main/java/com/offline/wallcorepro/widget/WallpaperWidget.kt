package com.offline.wallcorepro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.offline.wallcorepro.MainActivity
import com.offline.wallcorepro.R
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.WishQuotePool
import com.offline.wallcorepro.domain.usecase.GetRandomWallpaperUseCase
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.URL

/**
 * Home screen widget — shows the current wallpaper thumbnail with a
 * time-of-day greeting and a wish quote. Tapping opens the app.
 *
 * Update triggers:
 *  • On device boot (via BootReceiver)
 *  • After AutoWallpaperWorker sets a new wallpaper
 *  • Whenever the user adds the widget to their launcher
 */
class WallpaperWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            // Show placeholder immediately so the widget is never blank
            appWidgetManager.updateAppWidget(id, buildPlaceholder(context))
            // Then load the real wallpaper image in the background
            CoroutineScope(Dispatchers.IO).launch {
                loadAndUpdate(context, appWidgetManager, id)
            }
        }
    }

    companion object {

        /** Refresh every widget instance currently on the home screen. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, WallpaperWidget::class.java)
            )
            if (ids.isEmpty()) return
            WallpaperWidget().onUpdate(context, manager, ids)
        }

        private suspend fun loadAndUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java
                )
                val wallpaper = entryPoint.getRandomWallpaperUseCase()(AppConfig.NICHE_TYPE)

                val timeOfDay = AppConfig.TimeOfDay.current()
                val greeting  = "${timeOfDay.emoji} ${timeOfDay.displayName}!"
                val quote     = WishQuotePool.getNotificationQuote(timeOfDay)

                val views = buildViews(context, greeting, quote)

                if (wallpaper != null) {
                    val imageUrl = wallpaper.thumbnailUrl.ifBlank { wallpaper.imageUrl }
                    if (imageUrl.isNotBlank()) {
                        try {
                            val bitmap = BitmapFactory.decodeStream(
                                URL(imageUrl).openStream()
                            )
                            if (bitmap != null) {
                                views.setImageViewBitmap(R.id.widget_bg_image, bitmap)
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "WallpaperWidget: image download failed, showing text-only")
                        }
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Timber.e(e, "WallpaperWidget: update failed for id=$appWidgetId")
            }
        }

        private fun buildPlaceholder(context: Context): RemoteViews {
            val timeOfDay = AppConfig.TimeOfDay.current()
            return buildViews(
                context,
                greeting = "${timeOfDay.emoji} ${timeOfDay.displayName}!",
                quote    = "Loading your wallpaper…"
            )
        }

        private fun buildViews(
            context: Context,
            greeting: String,
            quote: String
        ): RemoteViews {
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val tapPending = PendingIntent.getActivity(
                context, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return RemoteViews(context.packageName, R.layout.widget_wallpaper).apply {
                setTextViewText(R.id.widget_greeting, greeting)
                setTextViewText(R.id.widget_quote, quote)
                setOnClickPendingIntent(R.id.widget_root, tapPending)
            }
        }
    }
}

/** Hilt entry point — lets the non-Hilt AppWidgetProvider access injected use cases. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getRandomWallpaperUseCase(): GetRandomWallpaperUseCase
}
