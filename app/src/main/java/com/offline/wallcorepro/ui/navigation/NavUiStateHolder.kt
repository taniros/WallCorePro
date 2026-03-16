package com.offline.wallcorepro.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared state holder for navigation-level UI decisions.
 * Tracks whether the main content has loaded to control ad visibility.
 */
@Singleton
class NavUiStateHolder @Inject constructor() {
    
    /** True when home screen has wallpapers loaded and ready to display */
    var homeContentLoaded by mutableStateOf(false)
        private set
    
    /** Called by HomeScreen when wallpapers are successfully loaded */
    fun markHomeContentLoaded() {
        homeContentLoaded = true
    }
    
    /** Called on app start / refresh to reset state */
    fun resetContentState() {
        homeContentLoaded = false
    }
}
