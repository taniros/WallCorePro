package com.offline.wallcorepro.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.repository.PreferenceManager
import com.offline.wallcorepro.worker.AutoWallpaperWorker
import com.offline.wallcorepro.worker.NotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean                  = true,
    val isAutoWallpaperEnabled: Boolean      = false,
    val autoWallpaperIntervalHours: Long     = 24L,
    val autoWallpaperTarget: String          = "HOME",
    val isMorningNotifEnabled: Boolean       = true,
    val isAfternoonNotifEnabled: Boolean     = true,
    val isEveningNotifEnabled: Boolean       = true,
    val isNightNotifEnabled: Boolean         = true,
    val userName: String                     = "",
    val selectedQuoteCategories: Set<String> = AppConfig.QuoteCategory.defaults,
    // New feature states
    val autoTheme: Boolean                   = false,
    val isFavoritesLocked: Boolean           = false,
    val smartReminderHours: Pair<Int,Int>?   = null, // suggested (morning, night) hours
    val isSmartReminderDismissed: Boolean    = true,
    val detailFullScreenByDefault: Boolean  = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    // ── All properties declared FIRST so init can safely reference them ────────

    private data class ExtraState(
        val autoTheme: Boolean              = false,
        val isFavoritesLocked: Boolean      = false,
        val smartReminderHours: Pair<Int,Int>? = null,
        val isSmartReminderDismissed: Boolean  = true,
        val detailFullScreenByDefault: Boolean = false
    )
    private val _extraState = MutableStateFlow(ExtraState())

    // Base 10-flow combine (core settings including 4 notification toggles)
    private val _baseUiState: StateFlow<SettingsUiState> = combine(
        preferenceManager.isDarkMode,
        preferenceManager.isAutoWallpaperEnabled,
        preferenceManager.autoWallpaperIntervalHours,
        preferenceManager.autoWallpaperTarget,
        preferenceManager.isMorningNotifEnabled,
        preferenceManager.isAfternoonNotifEnabled,
        preferenceManager.isEveningNotifEnabled,
        preferenceManager.isNightNotifEnabled,
        preferenceManager.userName,
        preferenceManager.selectedQuoteCategories
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            isDarkMode                 = values[0] as Boolean,
            isAutoWallpaperEnabled     = values[1] as Boolean,
            autoWallpaperIntervalHours = values[2] as Long,
            autoWallpaperTarget        = values[3] as String,
            isMorningNotifEnabled      = values[4] as Boolean,
            isAfternoonNotifEnabled    = values[5] as Boolean,
            isEveningNotifEnabled      = values[6] as Boolean,
            isNightNotifEnabled        = values[7] as Boolean,
            userName                   = values[8] as String,
            selectedQuoteCategories    = @Suppress("UNCHECKED_CAST") (values[9] as Set<String>)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    private val _mergedUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _mergedUiState.asStateFlow()

    // ── init runs after all property declarations ──────────────────────────────
    init {
        // Merge base + extra into the single exposed StateFlow
        viewModelScope.launch {
            combine(_baseUiState, _extraState) { base, extra ->
                base.copy(
                    autoTheme                = extra.autoTheme,
                    isFavoritesLocked        = extra.isFavoritesLocked,
                    smartReminderHours       = extra.smartReminderHours,
                    isSmartReminderDismissed = extra.isSmartReminderDismissed,
                    detailFullScreenByDefault = extra.detailFullScreenByDefault
                )
            }.collect { merged -> _mergedUiState.value = merged }
        }
        viewModelScope.launch {
            preferenceManager.autoTheme.collect { v ->
                _extraState.update { it.copy(autoTheme = v) }
            }
        }
        viewModelScope.launch {
            preferenceManager.isFavoritesLocked.collect { v ->
                _extraState.update { it.copy(isFavoritesLocked = v) }
            }
        }
        viewModelScope.launch {
            combine(
                preferenceManager.appOpenHours,
                preferenceManager.isSmartReminderDismissed
            ) { hours, dismissed -> Pair(hours, dismissed) }
            .collect { (hours, dismissed) ->
                val suggestion = AppConfig.computeSmartReminder(hours)
                _extraState.update { it.copy(smartReminderHours = suggestion, isSmartReminderDismissed = dismissed) }
            }
        }
        viewModelScope.launch {
            preferenceManager.detailFullScreenByDefault.collect { v ->
                _extraState.update { it.copy(detailFullScreenByDefault = v) }
            }
        }
    }

    fun setDetailFullScreenByDefault(enabled: Boolean) = viewModelScope.launch {
        preferenceManager.setDetailFullScreenByDefault(enabled)
    }

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        preferenceManager.setDarkMode(enabled)
    }

    fun setAutoWallpaperEnabled(enabled: Boolean, context: Context) = viewModelScope.launch {
        preferenceManager.setAutoWallpaperEnabled(enabled)
        if (enabled) {
            val hours = uiState.value.autoWallpaperIntervalHours
            AutoWallpaperWorker.schedule(context, hours)
        } else {
            AutoWallpaperWorker.cancel(context)
        }
    }

    fun setAutoWallpaperInterval(hours: Long, context: Context) = viewModelScope.launch {
        preferenceManager.setAutoWallpaperInterval(hours)
        if (uiState.value.isAutoWallpaperEnabled) {
            AutoWallpaperWorker.schedule(context, hours)
        }
    }

    fun setAutoWallpaperTarget(target: String) = viewModelScope.launch {
        preferenceManager.setAutoWallpaperTarget(target)
    }

    /**
     * Persists the morning notification toggle AND schedules or cancels the worker
     * so the change takes effect immediately — not just on next app launch.
     */
    fun setMorningNotifEnabled(enabled: Boolean, context: Context) = viewModelScope.launch {
        preferenceManager.setMorningNotifEnabled(enabled)
        if (enabled) NotificationWorker.scheduleMorning(context)
        else         NotificationWorker.cancelMorning(context)
    }

    /**
     * Persists the afternoon notification toggle AND schedules or cancels the worker.
     */
    fun setAfternoonNotifEnabled(enabled: Boolean, context: Context) = viewModelScope.launch {
        preferenceManager.setAfternoonNotifEnabled(enabled)
        if (enabled) NotificationWorker.scheduleAfternoon(context)
        else         NotificationWorker.cancelAfternoon(context)
    }

    /**
     * Persists the evening notification toggle AND schedules or cancels the worker.
     */
    fun setEveningNotifEnabled(enabled: Boolean, context: Context) = viewModelScope.launch {
        preferenceManager.setEveningNotifEnabled(enabled)
        if (enabled) NotificationWorker.scheduleEvening(context)
        else         NotificationWorker.cancelEvening(context)
    }

    /**
     * Persists the night notification toggle AND schedules or cancels the worker.
     */
    fun setNightNotifEnabled(enabled: Boolean, context: Context) = viewModelScope.launch {
        preferenceManager.setNightNotifEnabled(enabled)
        if (enabled) NotificationWorker.scheduleNight(context)
        else         NotificationWorker.cancelNight(context)
    }

    fun setUserName(name: String) = viewModelScope.launch {
        preferenceManager.setUserName(name)
    }

    fun setAutoTheme(enabled: Boolean) = viewModelScope.launch {
        preferenceManager.setAutoTheme(enabled)
    }

    fun setFavoritesLocked(locked: Boolean) = viewModelScope.launch {
        preferenceManager.setFavoritesLocked(locked)
    }

    fun applySmartReminder(context: Context) = viewModelScope.launch {
        val hours = uiState.value.smartReminderHours ?: return@launch
        preferenceManager.setMorningNotifEnabled(true)
        preferenceManager.setNightNotifEnabled(true)
        NotificationWorker.scheduleMorning(context)
        NotificationWorker.scheduleNight(context)
        preferenceManager.dismissSmartReminder()
    }

    fun dismissSmartReminder() = viewModelScope.launch {
        preferenceManager.dismissSmartReminder()
    }

    fun toggleQuoteCategory(key: String) = viewModelScope.launch {
        val current = uiState.value.selectedQuoteCategories.toMutableSet()
        if (key in current) {
            // Always keep at least one category active
            if (current.size > 1) current.remove(key)
        } else {
            current.add(key)
        }
        preferenceManager.setSelectedQuoteCategories(current)
    }
}
