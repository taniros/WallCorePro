package com.offline.wallcorepro.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.offline.wallcorepro.config.AppConfig
import timber.log.Timber

/**
 * Re-schedules daily notifications after device reboot.
 * WorkManager periodic tasks survive reboots on modern Android, but this
 * explicit receiver provides a reliable safety net for all API levels.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON") return

        if (!AppConfig.FEATURE_DAILY_NOTIFICATION) return

        Timber.d("BootReceiver: device rebooted — rescheduling daily notifications")
        NotificationWorker.schedule(context)
    }
}
