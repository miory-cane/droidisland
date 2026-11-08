package com.droidisland.app.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaController
import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import com.droidisland.app.model.AncMode
import com.droidisland.app.model.IslandState
import com.droidisland.app.model.MusicPalette
import com.droidisland.app.model.MusicTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Единый контроллер состояния и команд динамического островка DroidIsland.
 * Координирует работу как службы специальных возможностей (IslandAccessibilityService,
 * работающей поверх шторки уведомлений и статус-бара с TYPE_ACCESSIBILITY_OVERLAY),
 * так и стандартного фонового оверлея (IslandOverlayService) и слушателя медиа-сессий.
 */
object IslandController {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow<IslandState>(IslandState.Idle)
    val state: StateFlow<IslandState> = _state.asStateFlow()

    var currentTrack: MusicTrack = MusicTrack(
        title = "Starboy",
        artist = "The Weeknd",
        appName = "Spotify",
        albumAccentColor = Color(0xFFE50914),
        palette = MusicPalette(
            color1 = Color(0xFFE50914),
            color2 = Color(0xFF6200EA),
            color3 = Color(0xFF00E5FF)
        ),
        durationSec = 230,
        currentPositionSec = 84,
        isPlaying = true
    )

    var activeMediaController: MediaController? = null
    var isMusicDismissedByUser: Boolean = false
    var lastActiveMusicPackage: String? = null
    var isOverlayPaused: Boolean = false
        set(value) {
            field = value
            onOverlayPausedChanged?.invoke(value)
        }

    var onOverlayPausedChanged: ((Boolean) -> Unit)? = null

    private var autoDismissJob: Job? = null

    var lastUserTriggeredStateTime: Long = 0L

    fun showState(newState: IslandState, autoDismissMs: Long? = null) {
        autoDismissJob?.cancel()
        lastUserTriggeredStateTime = System.currentTimeMillis()
        _state.value = newState
        if (newState is IslandState.MusicCompact || newState is IslandState.MusicExpanded) {
            isMusicDismissedByUser = false
        }
        if (autoDismissMs != null && autoDismissMs > 0) {
            scheduleAutoDismiss(autoDismissMs)
        }
    }

    fun scheduleAutoDismiss(millis: Long) {
        autoDismissJob?.cancel()
        autoDismissJob = scope.launch {
            delay(millis)
            val curr = _state.value
            if (curr is IslandState.TwsCompact ||
                curr is IslandState.OverEarCompact ||
                curr is IslandState.Charging ||
                curr is IslandState.SoundProfile
            ) {
                _state.value = IslandState.Idle
            }
        }
    }

    fun handleIslandClick() {
        autoDismissJob?.cancel()
        when (val curr = _state.value) {
            is IslandState.Charging -> {
                _state.value = IslandState.ChargingExpanded(
                    percentage = curr.percentage,
                    isCharging = curr.isCharging,
                    sourceName = curr.sourceName,
                    voltageMv = curr.voltageMv,
                    temperatureC = curr.temperatureC,
                    realWatts = curr.realWatts,
                    currentMa = curr.currentMa
                )
            }
            is IslandState.ChargingExpanded -> {
                _state.value = IslandState.Charging(
                    percentage = curr.percentage,
                    isCharging = curr.isCharging,
                    sourceName = curr.sourceName,
                    voltageMv = curr.voltageMv,
                    temperatureC = curr.temperatureC,
                    realWatts = curr.realWatts,
                    currentMa = curr.currentMa
                )
                scheduleAutoDismiss(3000)
            }
            is IslandState.TwsCompact -> {
                _state.value = IslandState.TwsExpanded(device = curr.device, ancMode = AncMode.ANC)
            }
            is IslandState.TwsExpanded -> {
                _state.value = IslandState.TwsCompact(device = curr.device)
                scheduleAutoDismiss(3000)
            }
            is IslandState.OverEarCompact -> {
                _state.value = IslandState.OverEarExpanded(device = curr.device, ancMode = AncMode.ANC)
            }
            is IslandState.OverEarExpanded -> {
                _state.value = IslandState.OverEarCompact(device = curr.device)
                scheduleAutoDismiss(3000)
            }
            is IslandState.MusicCompact -> {
                _state.value = IslandState.MusicExpanded(track = curr.track)
            }
            is IslandState.MusicExpanded -> {
                _state.value = IslandState.MusicCompact(track = curr.track)
            }
            is IslandState.Idle -> {
                _state.value = IslandState.MusicCompact(track = currentTrack)
            }
            else -> {}
        }
    }

    fun handleDismissClick() {
        autoDismissJob?.cancel()
        if (_state.value is IslandState.MusicExpanded) {
            _state.value = IslandState.MusicCompact(track = currentTrack)
        } else {
            isMusicDismissedByUser = true
            _state.value = IslandState.Idle
        }
    }

    fun handleAncChange(newAnc: AncMode) {
        when (val curr = _state.value) {
            is IslandState.TwsExpanded -> {
                _state.value = curr.copy(ancMode = newAnc)
            }
            is IslandState.OverEarExpanded -> {
                _state.value = curr.copy(ancMode = newAnc)
            }
            else -> {}
        }
    }

    fun updateTrack(newTrack: MusicTrack) {
        currentTrack = newTrack
        val curr = _state.value
        if (curr is IslandState.MusicExpanded) {
            _state.value = IslandState.MusicExpanded(track = newTrack)
        } else if (curr is IslandState.MusicCompact) {
            _state.value = IslandState.MusicCompact(track = newTrack)
        }
    }

    fun playPauseMedia(context: Context) {
        val controller = activeMediaController
        if (controller != null) {
            try {
                val isPlaying = currentTrack.isPlaying
                if (isPlaying) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
                updateTrack(currentTrack.copy(isPlaying = !isPlaying))
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        updateTrack(currentTrack.copy(isPlaying = !currentTrack.isPlaying))
    }

    fun skipToNextMedia(context: Context) {
        val controller = activeMediaController
        if (controller != null) {
            try {
                controller.transportControls.skipToNext()
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun skipToPreviousMedia(context: Context) {
        val controller = activeMediaController
        if (controller != null) {
            try {
                controller.transportControls.skipToPrevious()
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun seekMediaTo(context: Context, progress: Float) {
        val newSec = (progress * currentTrack.durationSec).toInt().coerceIn(0, currentTrack.durationSec)
        updateTrack(currentTrack.copy(currentPositionSec = newSec))
        val controller = activeMediaController
        if (controller != null) {
            try {
                val targetMs = (progress * currentTrack.durationSec * 1000L).toLong()
                controller.transportControls.seekTo(targetMs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dispatchMediaKeyEvent(context: Context, keyCode: Int) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            am.dispatchMediaKeyEvent(eventDown)
            am.dispatchMediaKeyEvent(eventUp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openActiveMusicApp(context: Context) {
        try {
            val pkg = lastActiveMusicPackage ?: "com.spotify.music"
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
