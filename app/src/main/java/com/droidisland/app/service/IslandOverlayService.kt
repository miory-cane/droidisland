package com.droidisland.app.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
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
import com.droidisland.app.MainActivity
import com.droidisland.app.R
import com.droidisland.app.data.IslandConfig
import com.droidisland.app.data.IslandPreferences
import com.droidisland.app.model.AncMode
import com.droidisland.app.model.BluetoothHeadphoneDevice
import com.droidisland.app.model.HeadphoneFormFactor
import com.droidisland.app.model.IslandState
import com.droidisland.app.model.MusicTrack
import com.droidisland.app.model.SoundProfileType
import com.droidisland.app.ui.components.DynamicIslandView
import com.droidisland.app.util.PaletteExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class IslandOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val appViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = appViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // Состояние системного островка, синхронизированное с IslandController
    private var liveIslandState: IslandState
        get() = IslandController.state.value
        set(value) {
            IslandController.showState(value)
        }

    // Системный перехватчик событий Bluetooth наушников и зарядки
    private var systemEventsReceiver: BroadcastReceiver? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "droid_island_overlay_channel"
        private const val NOTIFICATION_ID = 1001

        var isRunning: Boolean = false
            private set

        private var instance: IslandOverlayService? = null

        var isOverlayPaused: Boolean
            get() = IslandController.isOverlayPaused
            set(value) {
                IslandController.isOverlayPaused = value
                instance?.updateOverlayVisibility()
            }

        fun onAccessibilityActiveChanged(active: Boolean) {
            instance?.let { service ->
                service.serviceScope.launch(Dispatchers.Main) {
                    service.updateOverlayVisibility()
                }
            }
        }

        fun isAccessibilityEnabled(context: Context): Boolean {
            if (IslandAccessibilityService.isRunning) return true
            val expectedServiceName = "${context.packageName}/${IslandAccessibilityService::class.java.canonicalName}"
            val enabledServicesSetting = try {
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
            } catch (e: Exception) {
                null
            } ?: return false
            return enabledServicesSetting.split(':').any {
                it.equals(expectedServiceName, ignoreCase = true) ||
                        it.contains(IslandAccessibilityService::class.java.simpleName)
            }
        }

        fun start(context: Context) {
            try {
                if (!android.provider.Settings.canDrawOverlays(context)) return
                val intent = Intent(context, IslandOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, IslandOverlayService::class.java))
        }

        fun showState(context: Context, state: IslandState, autoDismissMs: Long? = null) {
            try {
                if (!isRunning && !IslandAccessibilityService.isRunning && android.provider.Settings.canDrawOverlays(context)) {
                    start(context)
                }
                IslandController.showState(state, autoDismissMs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        IslandController.isOverlayPaused = false
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        initOverlayView()
        updateOverlayVisibility()
        registerAutoEventsReceiver()
        registerMusicPlaybackListener()
        checkAlreadyConnectedHeadphones()
        checkInitialStatus()

        // Синхронизация кликабельности со стейтом контроллера
        serviceScope.launch {
            IslandController.state.collectLatest { state ->
                val shouldBeTouchable = state !is IslandState.Idle
                updateTouchFlags(shouldBeTouchable)
            }
        }
    }

    /**
     * Проверка начального состояния при первом/повторном запуске службы,
     * чтобы островок был сразу виден и активен поверх всех приложений
     */
    private fun checkInitialStatus() {
        serviceScope.launch {
            val preferences = IslandPreferences(this@IslandOverlayService)
            val config = preferences.configFlow.firstOrNull() ?: IslandConfig()

            // 1. Проверяем, подключено ли зарядное устройство прямо сейчас
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = try {
                registerReceiver(null, batteryFilter)
            } catch (_: Exception) { null }
            val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
            val isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                    plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

            if (isPlugged && config.chargingEnabled) {
                val realCharging = getRealBatteryInfo()
                withContext(Dispatchers.Main) {
                    liveIslandState = realCharging
                    scheduleAutoDismiss((config.chargingTimeoutSec * 1000).toLong())
                }
                return@launch
            }

            // 2. Проверяем, воспроизводится ли музыка прямо сейчас
            val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
            if (audioManager?.isMusicActive == true && config.musicEnabled) {
                withContext(Dispatchers.Main) {
                    liveIslandState = IslandState.MusicCompact(track = currentTrack)
                }
                return@launch
            }

            // 3. Если устройство не заряжается и музыка не играет, кратковременно показываем анимацию готовности островка
            withContext(Dispatchers.Main) {
                liveIslandState = IslandState.SoundProfile(SoundProfileType.NORMAL)
                scheduleAutoDismiss(2500)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
        }
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            checkMusicPlayback(audioManager)
        }
        return START_STICKY
    }

    private fun initOverlayView() {
        val preferences = IslandPreferences(this)

        val composeView = ComposeView(this).apply {
            clipChildren = false
            clipToPadding = false
            setViewTreeLifecycleOwner(this@IslandOverlayService)
            setViewTreeViewModelStoreOwner(this@IslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@IslandOverlayService)

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
                        IslandController.dispatchMediaKeyEvent(this@IslandOverlayService, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        val updated = IslandController.currentTrack.copy(isPlaying = !IslandController.currentTrack.isPlaying)
                        IslandController.updateTrack(updated)
                    },
                    onNextClick = {
                        IslandController.dispatchMediaKeyEvent(this@IslandOverlayService, KeyEvent.KEYCODE_MEDIA_NEXT)
                    },
                    onPrevClick = {
                        IslandController.dispatchMediaKeyEvent(this@IslandOverlayService, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    },
                    onOpenMusicApp = {
                        IslandController.openActiveMusicApp(this@IslandOverlayService)
                    },
                    onTrackUpdated = { newTrack ->
                        IslandController.updateTrack(newTrack)
                    }
                )
            }
        }

        overlayView = composeView
        if (IslandAccessibilityService.isRunning) {
            composeView.visibility = View.GONE
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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

            // Отрисовка максимально поверх выреза камеры и системных элементов на всех версиях Android
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

    /**
     * Автоматический перехват подключения наушников, кабеля питания и звуковых профилей
     */
    private fun registerAutoEventsReceiver() {
        systemEventsReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                val preferences = IslandPreferences(this@IslandOverlayService)

                when (action) {
                    // Переключение звуковых профилей системы (Звук / Вибрация / Не беспокоить)
                    AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                        serviceScope.launch {
                            val config = preferences.configFlow.firstOrNull() ?: IslandConfig()
                            if (config.soundProfileEnabled) {
                                val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
                                val profile = when (audioManager?.ringerMode) {
                                    AudioManager.RINGER_MODE_SILENT -> SoundProfileType.DND
                                    AudioManager.RINGER_MODE_VIBRATE -> SoundProfileType.VIBRATE
                                    else -> SoundProfileType.NORMAL
                                }
                                withContext(Dispatchers.Main) {
                                    liveIslandState = IslandState.SoundProfile(profile)
                                    scheduleAutoDismiss((config.soundProfileTimeoutSec * 1000).toLong())
                                }
                            }
                        }
                    }

                    // Наушники подключились (открыт кейс / сопряжение)
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                        serviceScope.launch {
                            val config = preferences.configFlow.firstOrNull() ?: IslandConfig()
                            if (config.twsEnabled) {
                                withContext(Dispatchers.Main) {
                                    device?.let { handleHeadphoneConnected(it) }
                                }
                            }
                        }
                    }

                    // Обновление реального заряда наушников
                    "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" -> {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val level = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                        if (device != null && level in 0..100) {
                            handleDeviceBatteryChanged(device, level)
                        }
                    }

                    // Наушники отключились (убраны в кейс)
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        liveIslandState = IslandState.Idle
                    }

                    // Подключение кабеля зарядки
                    Intent.ACTION_POWER_CONNECTED -> {
                        serviceScope.launch {
                            val config = preferences.configFlow.firstOrNull() ?: IslandConfig()
                            if (config.chargingEnabled) {
                                val realCharging = getRealBatteryInfo()
                                withContext(Dispatchers.Main) {
                                    liveIslandState = realCharging
                                    scheduleAutoDismiss((config.chargingTimeoutSec * 1000).toLong())
                                }
                            }
                        }
                    }

                    // Отключение зарядки
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        liveIslandState = IslandState.Idle
                    }

                    // Низкий заряд батареи
                    Intent.ACTION_BATTERY_LOW -> {
                        serviceScope.launch {
                            val config = preferences.configFlow.firstOrNull() ?: IslandConfig()
                            if (config.chargingEnabled) {
                                val realCharging = getRealBatteryInfo().copy(isCharging = false)
                                withContext(Dispatchers.Main) {
                                    liveIslandState = realCharging
                                    scheduleAutoDismiss(3500)
                                }
                            }
                        }
                    }

                    // Медиа-трансляции метаданных плееров (Spotify, Яндекс Музыка, VK и др.)
                    "com.android.music.metadatachanged",
                    "com.spotify.music.metadatachanged",
                    "com.yandex.music.metadatachanged" -> {
                        val trackName = intent.getStringExtra("track")
                            ?: intent.getStringExtra("title")
                            ?: "Сейчас играет"
                        val artistName = intent.getStringExtra("artist")
                            ?: intent.getStringExtra("album")
                            ?: "Аудиоплеер"
                        val app = when {
                            action.contains("spotify") -> {
                                lastActiveMusicPackage = "com.spotify.music"
                                "Spotify"
                            }
                            action.contains("yandex") -> {
                                lastActiveMusicPackage = "ru.yandex.music"
                                "Яндекс Музыка"
                            }
                            else -> "Музыка"
                        }
                        val palette = PaletteExtractor.extractPalette(null, trackName, artistName, app)

                        val trackKey = "$trackName - $artistName"
                        if (trackKey != lastTrackIdentifier) {
                            lastTrackIdentifier = trackKey
                            isMusicDismissedByUser = false // При переключении трека снова показываем
                        }

                        currentTrack = MusicTrack(
                            title = trackName,
                            artist = artistName,
                            appName = app,
                            albumAccentColor = palette.color1,
                            palette = palette,
                            isPlaying = true
                        )

                        // Если музыка играет в открытом перед глазами приложении, не перекрываем его
                        if (isAnyMusicAppInForeground()) {
                            if (liveIslandState is IslandState.MusicCompact || liveIslandState is IslandState.MusicExpanded) {
                                liveIslandState = IslandState.Idle
                            }
                        } else {
                            if (!isMusicDismissedByUser) {
                                liveIslandState = if (liveIslandState is IslandState.MusicExpanded) {
                                    IslandState.MusicExpanded(track = currentTrack)
                                } else {
                                    IslandState.MusicCompact(track = currentTrack)
                                }
                            }
                        }
                    }

                    "com.android.music.playstatechanged",
                    "com.spotify.music.playbackstatechanged",
                    "com.yandex.music.playbackstatechanged" -> {
                        val isPlaying = intent.getBooleanExtra("playing", false)
                            || intent.getBooleanExtra("playstate", false)
                        if (!isPlaying && liveIslandState is IslandState.MusicCompact) {
                            liveIslandState = IslandState.Idle
                        }
                    }

                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        if (liveIslandState is IslandState.MusicCompact) {
                            liveIslandState = IslandState.Idle
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction("com.android.music.metadatachanged")
            addAction("com.android.music.playstatechanged")
            addAction("com.spotify.music.metadatachanged")
            addAction("com.spotify.music.playbackstatechanged")
            addAction("com.yandex.music.metadatachanged")
            addAction("com.yandex.music.playbackstatechanged")
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(systemEventsReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(systemEventsReceiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Обработка автоматического подключения наушников
     */
    @SuppressLint("MissingPermission")
    private fun handleHeadphoneConnected(device: BluetoothDevice) {
        val name = try { device.name ?: "Наушники Bluetooth" } catch (e: SecurityException) { "Наушники Bluetooth" }

        // Проверяем, являются ли они наушниками
        val bondedHeadphones = BluetoothHeadphoneManager.getBondedHeadphones(this)
        val matched = bondedHeadphones.firstOrNull { it.macAddress == device.address }

        val headphoneDevice = matched ?: run {
            val formFactor = if (name.lowercase().contains("wh-") || name.lowercase().contains("max"))
                HeadphoneFormFactor.OVER_EAR else HeadphoneFormFactor.TWS
            val photos = BluetoothHeadphoneManager.resolveFastPairPhotos(name, formFactor)
            val realBattery = BluetoothHeadphoneManager.getDeviceBattery(device)
            BluetoothHeadphoneDevice(
                macAddress = device.address,
                name = name,
                formFactor = formFactor,
                photos = photos,
                leftBattery = if (formFactor == HeadphoneFormFactor.TWS) (realBattery ?: 85) else null,
                rightBattery = if (formFactor == HeadphoneFormFactor.TWS) (realBattery ?: 90) else null,
                caseBattery = if (formFactor == HeadphoneFormFactor.TWS) 100 else null,
                overallBattery = if (formFactor == HeadphoneFormFactor.OVER_EAR) (realBattery ?: 92) else null
            )
        }

        // Активируем соответствующий всплывающий островок
        liveIslandState = if (headphoneDevice.formFactor == HeadphoneFormFactor.TWS) {
            IslandState.TwsCompact(device = headphoneDevice)
        } else {
            IslandState.OverEarCompact(device = headphoneDevice)
        }

        // Автоматическое схлопывание обратно в камеру через 3.8 секунды
        scheduleAutoDismiss(3800)
    }

    /**
     * Обработка изменения уровня батареи подключенного Bluetooth-устройства
     */
    private fun handleDeviceBatteryChanged(device: BluetoothDevice, batteryLevel: Int) {
        when (val state = liveIslandState) {
            is IslandState.TwsCompact -> {
                if (state.device.macAddress == device.address) {
                    val updated = state.device.copy(leftBattery = batteryLevel, rightBattery = batteryLevel)
                    liveIslandState = IslandState.TwsCompact(device = updated)
                }
            }
            is IslandState.TwsExpanded -> {
                if (state.device.macAddress == device.address) {
                    val updated = state.device.copy(leftBattery = batteryLevel, rightBattery = batteryLevel)
                    liveIslandState = state.copy(device = updated)
                }
            }
            is IslandState.OverEarCompact -> {
                if (state.device.macAddress == device.address) {
                    val updated = state.device.copy(overallBattery = batteryLevel)
                    liveIslandState = IslandState.OverEarCompact(device = updated)
                }
            }
            is IslandState.OverEarExpanded -> {
                if (state.device.macAddress == device.address) {
                    val updated = state.device.copy(overallBattery = batteryLevel)
                    liveIslandState = state.copy(device = updated)
                }
            }
            else -> {}
        }
    }

    /**
     * Проверка, если наушники уже подключены прямо сейчас
     */
    @SuppressLint("MissingPermission")
    private fun checkAlreadyConnectedHeadphones() {
        if (!BluetoothHeadphoneManager.hasBluetoothPermission(this)) return
        try {
            val adapter = BluetoothHeadphoneManager.getBluetoothAdapter(this) ?: return
            adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    try {
                        if (profile == BluetoothProfile.A2DP) {
                            val a2dp = proxy as? BluetoothA2dp
                            val connected = a2dp?.connectedDevices ?: emptyList()
                            if (connected.isNotEmpty()) {
                                val firstDev = connected.first()
                                val name = try { firstDev.name ?: "Наушники" } catch (e: SecurityException) { "Наушники" }
                                val formFactor = if (name.lowercase().contains("wh-") || name.lowercase().contains("max"))
                                    HeadphoneFormFactor.OVER_EAR else HeadphoneFormFactor.TWS
                                val photos = BluetoothHeadphoneManager.resolveFastPairPhotos(name, formFactor)
                                val realBattery = BluetoothHeadphoneManager.getDeviceBattery(firstDev)

                                val dev = BluetoothHeadphoneDevice(
                                    macAddress = firstDev.address,
                                    name = name,
                                    formFactor = formFactor,
                                    photos = photos,
                                    leftBattery = if (formFactor == HeadphoneFormFactor.TWS) (realBattery ?: 85) else null,
                                    rightBattery = if (formFactor == HeadphoneFormFactor.TWS) (realBattery ?: 90) else null,
                                    caseBattery = if (formFactor == HeadphoneFormFactor.TWS) 100 else null,
                                    overallBattery = if (formFactor == HeadphoneFormFactor.OVER_EAR) (realBattery ?: 92) else null
                                )
                                liveIslandState = if (formFactor == HeadphoneFormFactor.TWS) {
                                    IslandState.TwsCompact(device = dev)
                                } else {
                                    IslandState.OverEarCompact(device = dev)
                                }
                                scheduleAutoDismiss(3200)
                            }
                            adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                        }
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.A2DP)
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Отправка реального медиа-события в системный аудиоплеер (Play/Pause, Next, Prev)
     */
    fun dispatchMediaKeyEvent(keyCode: Int) {
        IslandController.dispatchMediaKeyEvent(this, keyCode)
    }

    /**
     * Моментальный переход в активное приложение воспроизведения музыки
     */
    fun openActiveMusicApp() {
        IslandController.openActiveMusicApp(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // В ландшафтном режиме (игры, просмотр фильмов) скрываем островок, чтобы не мешать
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            overlayView?.visibility = View.GONE
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            updateOverlayVisibility()
        }
    }

    private var audioPlaybackCallback: AudioManager.AudioPlaybackCallback? = null

    private var lastActiveMusicPackage: String?
        get() = IslandController.lastActiveMusicPackage
        set(value) { IslandController.lastActiveMusicPackage = value }

    private var isMusicDismissedByUser: Boolean
        get() = IslandController.isMusicDismissedByUser
        set(value) { IslandController.isMusicDismissedByUser = value }

    private var lastTrackIdentifier = ""

    private val KNOWN_MEDIA_PACKAGES = setOf(
        "com.spotify.music",
        "com.spotify.lite",
        "ru.yandex.music",
        "com.google.android.apps.youtube.music",
        "com.google.android.youtube",
        "com.vkontakte.android",
        "com.vk.vkclient",
        "com.vk.music",
        "com.apple.android.music",
        "deezer.android.app",
        "com.aspiro.tidal",
        "com.soundcloud.android",
        "ru.sberbank.sberzvuk",
        "sberzvuk",
        "ru.mts.music",
        "com.maxmpz.audioplayer",
        "com.aimp.player",
        "in.krosbits.musicolet",
        "code.name.monkey.retromusic",
        "ru.stellio.player",
        "com.sec.android.app.music",
        "com.miui.player",
        "org.videolan.vlc"
    )

    private fun isKnownMusicPackage(pkg: String): Boolean {
        if (pkg == packageName) return false
        val lower = pkg.lowercase()

        lastActiveMusicPackage?.let {
            if (lower.startsWith(it.lowercase()) || lower == it.lowercase()) return true
        }

        if (KNOWN_MEDIA_PACKAGES.any { lower.startsWith(it) || lower == it }) return true

        if (lower.contains(".music") || lower.contains(".audioplayer") || lower.contains(".player.music")) {
            return true
        }

        return false
    }

    /**
     * Проверяет, открыто ли музыкальное/видео приложение на экране пользователя прямо сейчас.
     * Если приложение активно в фокусе (foreground), шторка/островок не должны отображаться.
     */
    private fun isAnyMusicAppInForeground(): Boolean {
        // 1. Проверка через UsageStatsManager (если предоставлено системное разрешение)
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm?.queryEvents(now - 3500L, now)
            if (events != null) {
                val event = UsageEvents.Event()
                var latestForegroundPkg: String? = null
                var latestTimestamp = 0L
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val type = event.eventType
                    if (type == UsageEvents.Event.ACTIVITY_RESUMED || type == 1) {
                        if (event.timeStamp > latestTimestamp) {
                            latestTimestamp = event.timeStamp
                            latestForegroundPkg = event.packageName
                        }
                    }
                }
                if (!latestForegroundPkg.isNullOrBlank() && latestForegroundPkg != packageName) {
                    if (isKnownMusicPackage(latestForegroundPkg)) {
                        return true
                    }
                }
            }
        } catch (_: Exception) {}

        return false
    }

    private var currentTrack: MusicTrack
        get() = IslandController.currentTrack
        set(value) { IslandController.updateTrack(value) }

    /**
     * Системный слушатель воспроизведения музыки (Android 8.0+)
     * Автоматически поднимает островок с волной, когда на смартфоне начинает играть музыка.
     */
    private fun registerMusicPlaybackListener() {
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return

        // 1. Сразу проверяем текущий статус воспроизведения музыки
        checkMusicPlayback(audioManager)

        // 2. Системный callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioPlaybackCallback = object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                    super.onPlaybackConfigChanged(configs)
                    checkMusicPlayback(audioManager, configs)
                }
            }
            try {
                audioManager.registerAudioPlaybackCallback(
                    audioPlaybackCallback!!,
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Фоновый опрос для моментального обнаружения звука даже когда приложение закрыто
        startMusicPolling(audioManager)
    }

    private fun checkMusicPlayback(
        audioManager: AudioManager,
        configs: MutableList<AudioPlaybackConfiguration>? = null
    ) {
        val preferences = IslandPreferences(this@IslandOverlayService)
        serviceScope.launch {
            val config = preferences.configFlow.firstOrNull() ?: IslandConfig()
            if (!config.musicEnabled) return@launch

            val isMediaPlaying = audioManager.isMusicActive || (configs?.any { cfg ->
                val usage = cfg.audioAttributes.usage
                usage == AudioAttributes.USAGE_MEDIA || usage == AudioAttributes.USAGE_GAME
            } ?: false)

            val isForeground = isAnyMusicAppInForeground()

            withContext(Dispatchers.Main) {
                if (isMediaPlaying) {
                    if (isForeground) {
                        // Музыка играет в открытом приложении -> скрываем шторку
                        if (liveIslandState is IslandState.MusicCompact || liveIslandState is IslandState.MusicExpanded) {
                            liveIslandState = IslandState.Idle
                        }
                    } else {
                        // Музыка играет в фоне -> показываем компактный островок
                        if (!isMusicDismissedByUser) {
                            if (liveIslandState !is IslandState.MusicCompact && liveIslandState !is IslandState.MusicExpanded) {
                                liveIslandState = IslandState.MusicCompact(track = currentTrack)
                            }
                        }
                    }
                } else {
                    val timeSinceTrigger = System.currentTimeMillis() - IslandController.lastUserTriggeredStateTime
                    if (timeSinceTrigger > 10_000L && !IslandNotificationListener.isConnected) {
                        if (liveIslandState is IslandState.MusicCompact) {
                            liveIslandState = IslandState.Idle
                        }
                        isMusicDismissedByUser = false
                    }
                }
            }
        }
    }

    private fun startMusicPolling(audioManager: AudioManager) {
        val preferences = IslandPreferences(this@IslandOverlayService)
        serviceScope.launch {
            while (isActive) {
                delay(1500)
                val config = preferences.configFlow.firstOrNull() ?: IslandConfig()
                if (config.musicEnabled) {
                    val isMusicPlaying = audioManager.isMusicActive
                    val isForeground = isAnyMusicAppInForeground()
                    withContext(Dispatchers.Main) {
                        if (isMusicPlaying) {
                            if (isForeground) {
                                // Плеер открыт на переднем плане
                                if (liveIslandState is IslandState.MusicCompact || liveIslandState is IslandState.MusicExpanded) {
                                    liveIslandState = IslandState.Idle
                                }
                            } else {
                                // Плеер играет в фоне
                                if (!isMusicDismissedByUser) {
                                    if (liveIslandState is IslandState.Idle) {
                                        liveIslandState = IslandState.MusicCompact(track = currentTrack)
                                    }
                                }
                            }
                        } else {
                            // Не сбрасываем в Idle, если это тест или если работает IslandNotificationListener
                            val timeSinceTrigger = System.currentTimeMillis() - IslandController.lastUserTriggeredStateTime
                            if (timeSinceTrigger > 10_000L && !IslandNotificationListener.isConnected) {
                                if (liveIslandState is IslandState.MusicCompact) {
                                    liveIslandState = IslandState.Idle
                                }
                                isMusicDismissedByUser = false
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Считывание настоящей информации о батарее и зарядке из системного стека
     */
    private fun getRealBatteryInfo(): IslandState.Charging {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = try {
            registerReceiver(null, ifilter)
        } catch (_: Exception) { null }

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 80
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val percentage = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 80

        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        val sourceName = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Сетевое ЗУ"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Беспроводная Qi"
            BatteryManager.BATTERY_PLUGGED_USB -> "Зарядка через USB"
            else -> "Быстрая зарядка"
        }

        val voltageMv = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4150) ?: 4150
        val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val temperatureC = tempRaw / 10.0f

        val bm = getSystemService(BATTERY_SERVICE) as? BatteryManager
        val currentUa = try {
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        } catch (_: Exception) { 0 }

        val currentMa = if (currentUa != 0) {
            val absVal = Math.abs(currentUa)
            if (absVal > 50000) absVal / 1000 else absVal
        } else null

        val realWatts = if (currentMa != null && currentMa > 0 && voltageMv > 0) {
            val w = (voltageMv / 1000f) * (currentMa / 1000f)
            if (w in 1.0f..150.0f) Math.round(w * 10f) / 10f else null
        } else null

        return IslandState.Charging(
            percentage = percentage,
            isCharging = true,
            sourceName = sourceName,
            voltageMv = voltageMv,
            temperatureC = temperatureC,
            realWatts = realWatts,
            currentMa = currentMa
        )
    }

    private fun scheduleAutoDismiss(millis: Long) {
        IslandController.scheduleAutoDismiss(millis)
    }

    private fun handleIslandClick() {
        IslandController.handleIslandClick()
    }

    private fun handleAncChange(newAnc: AncMode) {
        IslandController.handleAncChange(newAnc)
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_service_title))
            .setContentText(getString(R.string.overlay_service_description))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateOverlayVisibility() {
        if (IslandAccessibilityService.isRunning) {
            overlayView?.visibility = View.GONE
        } else {
            overlayView?.visibility = if (isOverlayPaused) View.GONE else View.VISIBLE
        }
    }

    private fun updateTouchFlags(shouldBeTouchable: Boolean = liveIslandState !is IslandState.Idle) {
        val runnable = Runnable {
            overlayView?.let { view ->
                val lp = view.layoutParams as? WindowManager.LayoutParams ?: return@let
                val isCurrentlyTouchable = (lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) == 0

                if (shouldBeTouchable != isCurrentlyTouchable) {
                    val newLp = WindowManager.LayoutParams().apply {
                        copyFrom(lp)
                        if (shouldBeTouchable) {
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

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false
        audioPlaybackCallback?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
                audioManager?.unregisterAudioPlaybackCallback(it)
            }
        }
        systemEventsReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
