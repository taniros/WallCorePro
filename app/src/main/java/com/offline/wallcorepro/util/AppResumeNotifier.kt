package com.offline.wallcorepro.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits when the app returns from background (user reopens the app) and when
 * the app goes to background.  Used to trigger fresh wallpapers on every reopen
 * and to schedule a DB cache clear so the next session always shows new content.
 */
@Singleton
class AppResumeNotifier @Inject constructor() : DefaultLifecycleObserver {

    private val _appResumed      = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val appResumed: SharedFlow<Unit> = _appResumed

    private val _appBackgrounded = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val appBackgrounded: SharedFlow<Unit> = _appBackgrounded

    private var hasBeenBackgrounded = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (hasBeenBackgrounded) {
            hasBeenBackgrounded = false
            _appResumed.tryEmit(Unit)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        hasBeenBackgrounded = true
        _appBackgrounded.tryEmit(Unit)
    }
}
