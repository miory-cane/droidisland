package com.droidisland.app.service

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.droidisland.app.data.IslandConfig
import com.droidisland.app.data.IslandPreferences
import com.droidisland.app.model.IslandState
import com.droidisland.app.ui.components.DynamicIslandView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Служба специальных возможностей (Accessibility Service).
 * Использует WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY (уровень окна 33),
 * что позволяет островку отрисовываться на самом верхнем слое:
 * ПОВЕРХ шторки уведомлений (уровень 25), системного статус-бара и экрана блокировки.
 * Это гарантирует, что клики не перехватываются статус-баром, а островок виден всегда.
 */
class IslandAccessibilityService : AccessibilityService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val appViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = appViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        var isRunning: Boolean = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        IslandController.isOverlayPaused = false
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        initAccessibilityOverlay()

        // Уведомляем стандартный оверлей скрыть свой вид, чтобы не было дублирования
        IslandOverlayService.onAccessibilityActiveChanged(true)

        // Отслеживаем изменения состояния островка для динамического включения кликабельности
        serviceScope.launch {
            IslandController.state.collectLatest { state ->
                val shouldBeTouchable = state !is IslandState.Idle
                updateTouchFlags(shouldBeTouchable)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Системные события
    }

    override fun onInterrupt() {
        cleanupOverlay()
        isRunning = false
        IslandOverlayService.onAccessibilityActiveChanged(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupOverlay()
        isRunning = false
        IslandOverlayService.onAccessibilityActiveChanged(false)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            overlayView?.visibility = View.GONE
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            overlayView?.visibility = if (IslandController.isOverlayPaused) View.GONE else View.VISIBLE
        }
    }

    private fun initAccessibilityOverlay() {
        val preferences = IslandPreferences(this)

        val composeView = ComposeView(this).apply {
            clipChildren = false
            clipToPadding = false
            setViewTreeLifecycleOwner(this@IslandAccessibilityService)
            setViewTreeViewModelStoreOwner(this@IslandAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@IslandAccessibilityService)

            setContent {
                val state by IslandController.state.collectAsState()
                val config by preferences.configFlow.collectAsState(initial = IslandConfig())

                DynamicIslandView(
                    state = state,
                    cameraXOffset = config.cameraXOffset,
                    cameraYOffset = config.cameraYOffset,
                    cameraSize = config.cameraSize,
                    expandedWidth = config.expandedWidth,
                    expandedHeight = config.expandedHeight,
                    expandedOpacity = config.expandedIslandOpacity,
                    heightMultiplier = config.expandedHeightMultiplier,
                    isCalibrationGuideActive = config.isCalibrationGuideActive,
                    onIslandClick = {
                        IslandController.isMusicDismissedByUser = false
                        IslandController.handleIslandClick()
                    },
                    onDismissClick = {
                        IslandController.handleDismissClick()
                    },
                    onAncChanged = { newAnc ->
                        IslandController.handleAncChange(newAnc)
                    },
                    onPlayPauseClick = {
                        IslandController.dispatchMediaKeyEvent(this@IslandAccessibilityService, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        val updated = IslandController.currentTrack.copy(isPlaying = !IslandController.currentTrack.isPlaying)
                        IslandController.updateTrack(updated)
                    },
                    onNextClick = {
                        IslandController.dispatchMediaKeyEvent(this@IslandAccessibilityService, KeyEvent.KEYCODE_MEDIA_NEXT)
                    },
                    onPrevClick = {
                        IslandController.dispatchMediaKeyEvent(this@IslandAccessibilityService, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    },
                    onOpenMusicApp = {
                        IslandController.openActiveMusicApp(this@IslandAccessibilityService)
                    },
                    onTrackUpdated = { newTrack ->
                        IslandController.updateTrack(newTrack)
                    }
                )
            }
        }

        overlayView = composeView

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0

            // Отрисовка поверх выреза камеры и системных элементов
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager.addView(composeView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTouchFlags(isTouchable: Boolean) {
        val runnable = Runnable {
            overlayView?.let { view ->
                val lp = view.layoutParams as? WindowManager.LayoutParams ?: return@let
                val isCurrentlyTouchable = (lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) == 0

                if (isTouchable != isCurrentlyTouchable) {
                    val newLp = WindowManager.LayoutParams().apply {
                        copyFrom(lp)
                        if (isTouchable) {
                            flags = (flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()) or
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        } else {
                            flags = (flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) and
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            @Suppress("DEPRECATION")
                            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                    }
                    try {
                        windowManager.updateViewLayout(view, newLp)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            Handler(Looper.getMainLooper()).post(runnable)
        }
    }

    private fun cleanupOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
    }
}
