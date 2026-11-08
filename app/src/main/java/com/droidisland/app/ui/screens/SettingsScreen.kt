package com.droidisland.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import com.droidisland.app.service.IslandNotificationListener
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidisland.app.data.IslandConfig
import com.droidisland.app.data.IslandPreferences
import com.droidisland.app.model.BluetoothHeadphoneDevice
import com.droidisland.app.model.HeadphoneFormFactor
import com.droidisland.app.model.IslandState
import com.droidisland.app.model.MusicTrack
import com.droidisland.app.service.BluetoothHeadphoneManager
import com.droidisland.app.service.IslandOverlayService
import com.droidisland.app.ui.components.CalibrationSlider
import com.droidisland.app.ui.components.DynamicIslandView
import com.droidisland.app.ui.components.FastPairCaseGraphic
import com.droidisland.app.ui.components.FastPairEarbudGraphic
import com.droidisland.app.ui.components.FastPairOverEarGraphic
import com.droidisland.app.ui.components.PastelSky
import com.droidisland.app.ui.theme.AccentCyan
import com.droidisland.app.ui.theme.CardBackground
import com.droidisland.app.ui.theme.CardBorder
import com.droidisland.app.ui.theme.DarkBackground
import com.droidisland.app.ui.theme.PrimaryBlue
import com.droidisland.app.ui.theme.TextPrimary
import com.droidisland.app.ui.theme.TextSecondary
import com.droidisland.app.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    config: IslandConfig,
    preferences: IslandPreferences
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var twsEnabled by remember { mutableStateOf(config.twsEnabled) }
    var detectedHeadphones by remember { mutableStateOf<List<BluetoothHeadphoneDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BluetoothHeadphoneDevice?>(null) }
    var isCalibrationGuideActive by remember { mutableStateOf(config.isCalibrationGuideActive) }

    fun refreshBluetoothDevices() {
        val realDevices = BluetoothHeadphoneManager.getBondedHeadphones(context)
        detectedHeadphones = if (realDevices.isNotEmpty()) {
            realDevices
        } else {
            BluetoothHeadphoneManager.getMockBluetoothDevices()
        }
        if (selectedDevice == null || detectedHeadphones.none { it.macAddress == selectedDevice?.macAddress }) {
            selectedDevice = detectedHeadphones.firstOrNull()
        }
    }

    LaunchedEffect(Unit) {
        refreshBluetoothDevices()
    }

    var hasAccessibility by remember { mutableStateOf(IslandOverlayService.isAccessibilityEnabled(context)) }
    var hasNotificationAccess by remember { mutableStateOf(IslandNotificationListener.isNotificationAccessGranted(context)) }

    // При калибровке в настройках приостанавливаем системный оверлей,
    // чтобы на экране был строго ОДИН настраиваемый кружок без дублирования старого
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibility = IslandOverlayService.isAccessibilityEnabled(context)
                hasNotificationAccess = IslandNotificationListener.isNotificationAccessGranted(context)
                IslandOverlayService.isOverlayPaused = false
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                IslandOverlayService.isOverlayPaused = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        IslandOverlayService.isOverlayPaused = false
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            IslandOverlayService.isOverlayPaused = false
        }
    }

    // Калибровочные стейты
    var xOffset by remember { mutableFloatStateOf(config.cameraXOffset.toFloat()) }
    var yOffset by remember { mutableFloatStateOf(config.cameraYOffset.toFloat()) }
    var cameraSize by remember { mutableFloatStateOf(config.cameraSize.toFloat()) }
    var expandedWidth by remember { mutableFloatStateOf(config.expandedWidth.coerceIn(280, 600).toFloat()) }
    var expandedHeight by remember { mutableFloatStateOf(config.expandedHeight.toFloat()) }

    var expandedOpacity by remember { mutableFloatStateOf(config.expandedIslandOpacity) }
    var expandedScale by remember { mutableFloatStateOf(config.expandedHeightMultiplier) }

    LaunchedEffect(config.cameraXOffset, config.cameraYOffset, config.cameraSize, config.expandedWidth, config.expandedHeight) {
        xOffset = config.cameraXOffset.toFloat()
        yOffset = config.cameraYOffset.toFloat()
        cameraSize = config.cameraSize.toFloat()
        expandedWidth = config.expandedWidth.coerceIn(280, 600).toFloat()
        expandedHeight = config.expandedHeight.toFloat()
        expandedOpacity = config.expandedIslandOpacity
        expandedScale = config.expandedHeightMultiplier
        isCalibrationGuideActive = config.isCalibrationGuideActive
    }

    // Статусы подтверждения для кнопок
    var showCalibrationSaved by remember { mutableStateOf(false) }
    var showSheetSaved by remember { mutableStateOf(false) }
    var previewState by remember { mutableStateOf<IslandState>(IslandState.Idle) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 70.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Настройки системы",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Калибровка объектива камеры, наушники Fast Pair, анимация и системные допуски.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ==========================================
            // РАЗДЕЛ 1: КАЛИБРОВКА ВЫРЕЗА КАМЕРЫ (Pixel Camera Studio)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AccentCyan.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CropFree, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Калибровка выреза камеры", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text("Пиксельная подгонка точно под объектив", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Пресеты для моделей смартфонов:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip(
                            label = "Pixel 8/9",
                            isSelected = (xOffset == 0f && cameraSize <= 32f),
                            modifier = Modifier.weight(1f)
                        ) {
                            HapticHelper.performClick(view)
                            xOffset = 0f
                            yOffset = 18f
                            cameraSize = 30f
                            scope.launch { preferences.saveCalibration(0, 18, 30, expandedWidth.roundToInt(), expandedHeight.roundToInt()) }
                        }
                        PresetChip(
                            label = "Pixel 6/7",
                            isSelected = (xOffset == 0f && cameraSize > 32f && cameraSize < 35f),
                            modifier = Modifier.weight(1f)
                        ) {
                            HapticHelper.performClick(view)
                            xOffset = 0f
                            yOffset = 16f
                            cameraSize = 33f
                            scope.launch { preferences.saveCalibration(0, 16, 33, expandedWidth.roundToInt(), expandedHeight.roundToInt()) }
                        }
                        PresetChip(
                            label = "В углу",
                            isSelected = (xOffset < -40f),
                            modifier = Modifier.weight(1f)
                        ) {
                            HapticHelper.performClick(view)
                            xOffset = -125f
                            yOffset = 14f
                            cameraSize = 32f
                            scope.launch { preferences.saveCalibration(-125, 14, 32, expandedWidth.roundToInt(), expandedHeight.roundToInt()) }
                        }
                        PresetChip(
                            label = "Пилюля",
                            isSelected = (cameraSize >= 35f),
                            modifier = Modifier.weight(1f)
                        ) {
                            HapticHelper.performClick(view)
                            xOffset = 0f
                            yOffset = 16f
                            cameraSize = 36f
                            scope.launch { preferences.saveCalibration(0, 16, 36, expandedWidth.roundToInt(), expandedHeight.roundToInt()) }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF14161C))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Неоновый оптический прицел",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Прозрачный центр: объектив виден сквозь экран",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isCalibrationGuideActive,
                            onCheckedChange = {
                                HapticHelper.performLightTick(view)
                                isCalibrationGuideActive = it
                                scope.launch {
                                    preferences.setCalibrationGuideActive(it)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentCyan
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // МИКРО-ДЖОЙСТИК ЮВЕЛИРНОЙ ПОДГОНКИ ПОД ФИЗИЧЕСКУЮ ЛИНЗУ (ШАГ 1 DP)
                    CalibrationDPad(
                        xOffset = xOffset,
                        yOffset = yOffset,
                        cameraSize = cameraSize,
                        onNudgeX = {
                            xOffset = it
                            scope.launch { preferences.saveCalibration(it.roundToInt(), yOffset.roundToInt(), cameraSize.roundToInt(), expandedWidth.roundToInt(), expandedHeight.roundToInt()) }
                        },
                        onNudgeY = {
                            yOffset = it
                            scope.launch { preferences.saveCalibration(xOffset.roundToInt(), it.roundToInt(), cameraSize.roundToInt(), expandedWidth.roundToInt(), expandedHeight.roundToInt()) }
                        },
                        onNudgeSize = {
                            cameraSize = it
                            scope.launch { preferences.saveCalibration(xOffset.roundToInt(), yOffset.roundToInt(), it.roundToInt(), expandedWidth.roundToInt(), expandedHeight.roundToInt()) }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    CalibrationSlider(
                        title = "Смещение по Y (вверх / вниз)",
                        value = yOffset,
                        range = 0f..100f,
                        unit = "dp",
                        onValueChange = {
                            yOffset = it
                            scope.launch { preferences.saveCalibration(xOffset.roundToInt(), it.roundToInt(), cameraSize.roundToInt(), expandedWidth.roundToInt(), expandedHeight.roundToInt()) }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CalibrationSlider(
                        title = "Смещение по X (влево / вправо)",
                        value = xOffset,
                        range = -150f..150f,
                        unit = "dp",
                        onValueChange = {
                            xOffset = it
                            scope.launch { preferences.saveCalibration(it.roundToInt(), yOffset.roundToInt(), cameraSize.roundToInt(), expandedWidth.roundToInt(), expandedHeight.roundToInt()) }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CalibrationSlider(
                        title = "Диаметр камеры (в покое)",
                        value = cameraSize,
                        range = 20f..56f,
                        unit = "dp",
                        onValueChange = {
                            cameraSize = it
                            scope.launch { preferences.saveCalibration(xOffset.roundToInt(), yOffset.roundToInt(), it.roundToInt(), expandedWidth.roundToInt(), expandedHeight.roundToInt()) }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            HapticHelper.performSuccess(view)
                            scope.launch {
                                preferences.saveCalibration(
                                    xOffset = xOffset.roundToInt(),
                                    yOffset = yOffset.roundToInt(),
                                    cameraSize = cameraSize.roundToInt(),
                                    expandedWidth = expandedWidth.roundToInt(),
                                    expandedHeight = expandedHeight.roundToInt()
                                )
                                showCalibrationSaved = true
                                delay(2200)
                                showCalibrationSaved = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (showCalibrationSaved) AccentCyan else AccentCyan)
                    ) {
                        if (showCalibrationSaved) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Калибровка сохранена!", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Text("Сохранить калибровку камеры", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // РАЗДЕЛ 2: НАУШНИКИ И FAST PAIR
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PastelSky.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Headphones, contentDescription = null, tint = PastelSky, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Наушники Bluetooth", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Автоопределение по Fast Pair", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Switch(
                            checked = twsEnabled,
                            onCheckedChange = {
                                twsEnabled = it
                                scope.launch { preferences.setTwsEnabled(it) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (twsEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Добавить новое устройство", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Сопряжённые наушники:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { refreshBluetoothDevices() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            detectedHeadphones.forEach { device ->
                                val isSelected = selectedDevice?.macAddress == device.macAddress
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFF14161C))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedDevice = device }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = device.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(text = device.formFactor.title, color = PastelSky, fontSize = 11.sp)
                                        }
                                    }

                                    if (device.formFactor == HeadphoneFormFactor.TWS) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FastPairEarbudGraphic(deviceName = device.name, isLeft = true, size = 18.dp)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            FastPairEarbudGraphic(deviceName = device.name, isLeft = false, size = 18.dp)
                                        }
                                    } else {
                                        FastPairOverEarGraphic(deviceName = device.name, size = 22.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // РАЗДЕЛ 3: ПОВЕДЕНИЕ ШТОРКИ ПРИ НАЖАТИИ
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Геометрия и поведение шторки", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text("Ширина, высота и жест сворачивания", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Быстрые пресеты размера шторки:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip(
                            label = "Компакт\n340×125",
                            isSelected = (expandedWidth == 340f && expandedHeight == 125f),
                            modifier = Modifier.weight(1f)
                        ) {
                            HapticHelper.performClick(view)
                            expandedWidth = 340f
                            expandedHeight = 125f
                            scope.launch { preferences.setExpandedDimensions(340, 125) }
                        }
                        PresetChip(
                            label = "Стандарт\n366×136",
                            isSelected = (expandedWidth == 366f && expandedHeight == 136f),
                            modifier = Modifier.weight(1f)
                        ) {
                            HapticHelper.performClick(view)
                            expandedWidth = 366f
                            expandedHeight = 136f
                            scope.launch { preferences.setExpandedDimensions(366, 136) }
                        }
                        PresetChip(
                            label = "Широкая\n440×136",
                            isSelected = (expandedWidth == 440f && expandedHeight == 136f),
                            modifier = Modifier.weight(1f)
                        ) {
                            HapticHelper.performClick(view)
                            expandedWidth = 440f
                            expandedHeight = 136f
                            scope.launch { preferences.setExpandedDimensions(440, 136) }
                        }
                        PresetChip(
                            label = "Макси\n600×140",
                            isSelected = (expandedWidth == 600f && expandedHeight == 140f),
                            modifier = Modifier.weight(1f)
                        ) {
                            HapticHelper.performClick(view)
                            expandedWidth = 600f
                            expandedHeight = 140f
                            scope.launch { preferences.setExpandedDimensions(600, 140) }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // НАСТРОЙКА ШИРИНЫ ШТОРКИ (до 600 dp)
                    CalibrationSlider(
                        title = "Ширина шторки (по горизонтали)",
                        value = expandedWidth,
                        range = 280f..600f,
                        unit = "dp",
                        onValueChange = {
                            expandedWidth = it
                            scope.launch {
                                preferences.setExpandedDimensions(
                                    width = it.roundToInt(),
                                    height = expandedHeight.roundToInt()
                                )
                            }
                        }
                    )
                    Text(
                        text = "Значения от 400 до 600 dp раскрывают шторку на максимальную ширину дисплея (во весь экран)",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // НАСТРОЙКА УЗКОСТИ / ВЫСОТЫ ШТОРКИ
                    CalibrationSlider(
                        title = "Высота / узкость шторки (сверху вниз)",
                        value = expandedHeight,
                        range = 110f..165f,
                        unit = "dp",
                        onValueChange = {
                            expandedHeight = it
                            scope.launch {
                                preferences.setExpandedDimensions(
                                    width = expandedWidth.roundToInt(),
                                    height = it.roundToInt()
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ИНФОРМАЦИОННАЯ КАРТОЧКА О ЖЕСТЕ СВАЙПА ВВЕРХ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF14161C))
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Жест сворачивания: смахните вверх", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Чтобы свернуть широкий плеер — просто потяните шторку вверх. Смахивания вправо и влево отключены, чтобы не мешать управлению.", color = TextSecondary, fontSize = 10.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (previewState !is IslandState.Idle) "Шторка развёрнута на экране" else "Шторка свернута в глазок",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        TextButton(
                            onClick = {
                                previewState = if (previewState is IslandState.Idle) {
                                    IslandState.MusicExpanded(
                                        track = MusicTrack(
                                            title = "Starboy",
                                            artist = "The Weeknd",
                                            appName = "Spotify",
                                            albumAccentColor = Color(0xFFE50914),
                                            isPlaying = true
                                        )
                                    )
                                } else {
                                    IslandState.Idle
                                }
                            }
                        ) {
                            Text(
                                text = if (previewState !is IslandState.Idle) "Скрыть предпросмотр" else "Предпросмотр шторки",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            HapticHelper.performSuccess(view)
                            scope.launch {
                                preferences.setExpandedDimensions(
                                    width = expandedWidth.roundToInt(),
                                    height = expandedHeight.roundToInt()
                                )
                                preferences.setExpandedAppearance(
                                    opacity = expandedOpacity,
                                    scale = expandedScale
                                )
                                showSheetSaved = true
                                delay(2200)
                                showSheetSaved = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (showSheetSaved) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сохранено!", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Text("Сохранить параметры шторки", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // РАЗДЕЛ 4: СИСТЕМНЫЕ РАЗРЕШЕНИЯ И ФОН
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x26FF9F0A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Работа в фоне и оверлей", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text("Необходимые системные допуски", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D2027))
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Отображение поверх всех приложений", color = TextPrimary, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D2027))
                    ) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Работа в фоне (без оптимизации АКБ)", color = TextPrimary, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasAccessibility) AccentCyan.copy(alpha = 0.15f) else Color(0xFF1D2027)
                        )
                    ) {
                        Icon(
                            imageVector = if (hasAccessibility) Icons.Default.Check else Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (hasAccessibility) AccentCyan else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasAccessibility) "Поверх шторки: Активно (Спец. возможности)" else "Отображение поверх шторки (Спец. возможности)",
                            color = if (hasAccessibility) AccentCyan else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (hasAccessibility) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            IslandNotificationListener.openNotificationAccessSettings(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasNotificationAccess) AccentCyan.copy(alpha = 0.15f) else Color(0xFF1D2027)
                        )
                    ) {
                        Icon(
                            imageVector = if (hasNotificationAccess) Icons.Default.Check else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (hasNotificationAccess) AccentCyan else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasNotificationAccess) "Доступ к медиа-сессиям: Активен (Обложки и треки)" else "Доступ к медиа-сессиям (Spotify, Яндекс Музыка)",
                            color = if (hasNotificationAccess) AccentCyan else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (hasNotificationAccess) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Превью динамического островка (по нажатию раскрывается или сворачивается)
        DynamicIslandView(
            state = previewState,
            cameraXOffset = xOffset.roundToInt(),
            cameraYOffset = yOffset.roundToInt(),
            cameraSize = cameraSize.roundToInt(),
            expandedWidth = expandedWidth.roundToInt(),
            expandedHeight = expandedHeight.roundToInt(),
            expandedOpacity = expandedOpacity,
            heightMultiplier = expandedScale,
            isCalibrationGuideActive = isCalibrationGuideActive,
            onIslandClick = {
                previewState = if (previewState is IslandState.Idle) {
                    IslandState.MusicExpanded(
                        track = MusicTrack(
                            title = "Starboy",
                            artist = "The Weeknd",
                            albumAccentColor = Color(0xFFE50914),
                            isPlaying = true
                        )
                    )
                } else {
                    IslandState.Idle
                }
            },
            onDismissClick = {
                previewState = IslandState.Idle
            },
            onTrackUpdated = { newTrack ->
                if (previewState is IslandState.MusicExpanded) {
                    previewState = IslandState.MusicExpanded(track = newTrack)
                }
            }
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) AccentCyan.copy(alpha = 0.20f) else Color(0xFF14161C))
            .border(
                width = 1.dp,
                color = if (isSelected) AccentCyan else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) AccentCyan else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CalibrationDPad(
    xOffset: Float,
    yOffset: Float,
    cameraSize: Float,
    onNudgeX: (Float) -> Unit,
    onNudgeY: (Float) -> Unit,
    onNudgeSize: (Float) -> Unit
) {
    val view = LocalView.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF101217))
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ювелирный D-Pad (шаг строго 1 dp)",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = AccentCyan
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "X: ${xOffset.toInt()} dp  •  Y: ${yOffset.toInt()} dp  •  Диаметр: ${cameraSize.toInt()} dp",
            fontSize = 11.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Стрелка Вверх [ ▲ ]
        IconButton(
            onClick = {
                HapticHelper.performLightTick(view)
                onNudgeY((yOffset - 1f).coerceAtLeast(0f))
            },
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D2028))
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Вверх", tint = Color.White)
        }

        // Ряд: Влево [ ◄ ], Индикатор по центру, Вправо [ ► ]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            IconButton(
                onClick = {
                    HapticHelper.performLightTick(view)
                    onNudgeX((xOffset - 1f).coerceAtLeast(-150f))
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1D2028))
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Влево", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.15f))
                    .border(1.dp, AccentCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CropFree, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = {
                    HapticHelper.performLightTick(view)
                    onNudgeX((xOffset + 1f).coerceAtMost(150f))
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1D2028))
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Вправо", tint = Color.White)
            }
        }

        // Стрелка Вниз [ ▼ ]
        IconButton(
            onClick = {
                HapticHelper.performLightTick(view)
                onNudgeY((yOffset + 1f).coerceAtMost(100f))
            },
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D2028))
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Вниз", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Быстрые кнопки диаметра
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    HapticHelper.performLightTick(view)
                    onNudgeSize((cameraSize - 1f).coerceAtLeast(18f))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D2028)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("- 1 dp размер", fontSize = 11.5.sp, color = TextPrimary)
            }

            Button(
                onClick = {
                    HapticHelper.performLightTick(view)
                    onNudgeSize((cameraSize + 1f).coerceAtMost(60f))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D2028)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("+ 1 dp размер", fontSize = 11.5.sp, color = TextPrimary)
            }
        }
    }
}
