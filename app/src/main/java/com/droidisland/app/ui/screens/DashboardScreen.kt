package com.droidisland.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import com.droidisland.app.service.IslandAccessibilityService
import com.droidisland.app.service.IslandOverlayService
import com.droidisland.app.ui.components.DynamicIslandView
import com.droidisland.app.ui.components.PastelSky
import com.droidisland.app.ui.theme.AccentCyan
import com.droidisland.app.ui.theme.AccentGreen
import com.droidisland.app.ui.theme.AccentLavender
import com.droidisland.app.ui.theme.CardBackground
import com.droidisland.app.ui.theme.CardBackgroundHigh
import com.droidisland.app.ui.theme.CardBorder
import com.droidisland.app.ui.theme.CardBorderSubtle
import com.droidisland.app.ui.theme.DarkBackground
import com.droidisland.app.ui.theme.PrimaryBlue
import com.droidisland.app.ui.theme.TextPrimary
import com.droidisland.app.ui.theme.TextSecondary
import com.droidisland.app.util.HapticHelper

/**
 * Главный экран DroidIsland в дизайне Google Pixel (Material You / Expressive).
 */
@Composable
fun DashboardScreen(
    config: IslandConfig,
    preferences: IslandPreferences,
    onNavigateToTests: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current

    var isServiceRunning by remember { mutableStateOf(IslandOverlayService.isRunning) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasAccessibilityPermission by remember { mutableStateOf(IslandOverlayService.isAccessibilityEnabled(context)) }

    // Интерактивный предпросмотр островка на дашборде
    var previewState by remember {
        mutableStateOf<IslandState>(
            IslandState.MusicExpanded(
                track = MusicTrack(
                    title = "Starboy",
                    artist = "The Weeknd",
                    appName = "Spotify",
                    albumAccentColor = Color(0xFFE50914),
                    isPlaying = true
                )
            )
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasAccessibilityPermission = IslandOverlayService.isAccessibilityEnabled(context)
                if (hasOverlayPermission && !IslandOverlayService.isRunning && !IslandAccessibilityService.isRunning) {
                    IslandOverlayService.start(context)
                }
                isServiceRunning = IslandOverlayService.isRunning || IslandAccessibilityService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasAccessibilityPermission = IslandOverlayService.isAccessibilityEnabled(context)
        isServiceRunning = IslandOverlayService.isRunning || IslandAccessibilityService.isRunning
        if (hasOverlayPermission && !IslandOverlayService.isRunning && !IslandAccessibilityService.isRunning) {
            IslandOverlayService.start(context)
            isServiceRunning = true
        }
    }

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
            // 1. Google Pixel "At a Glance" карточка приветствия
            PixelAtAGlanceHeader(isServiceRunning = isServiceRunning && hasOverlayPermission)

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Интерактивная витрина островка (Live Island Preview)
            PixelLiveIslandCard(
                currentState = previewState,
                onStateSelected = {
                    HapticHelper.performClick(view)
                    previewState = it
                },
                config = config,
                onTrackChange = { previewState = IslandState.MusicExpanded(it) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Карточка мастер-переключателя службы в стиле Pixel Quick Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isServiceRunning && hasOverlayPermission) AccentCyan.copy(alpha = 0.20f)
                                    else Color(0x22FF453A)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (isServiceRunning && hasOverlayPermission) AccentCyan else Color(0xFFFF5252),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Островок поверх всего",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isServiceRunning && hasOverlayPermission) "Система активна и отслеживает события"
                                else "Служба приостановлена",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isServiceRunning && hasOverlayPermission,
                        onCheckedChange = { enable ->
                            HapticHelper.performClick(view)
                            if (!hasOverlayPermission) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                isServiceRunning = enable
                                if (enable) {
                                    IslandOverlayService.start(context)
                                } else {
                                    IslandOverlayService.stop(context)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3.5. Карточка наивысшего приоритета: Отображение поверх шторки уведомлений (Спец. возможности)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasAccessibilityPermission) MaterialTheme.colorScheme.surfaceContainerHigh
                    else Color(0xFF1E2129)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (hasAccessibilityPermission) AccentCyan.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (hasAccessibilityPermission) AccentCyan.copy(alpha = 0.20f)
                                        else Color(0x26FF9F0A)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasAccessibilityPermission) Icons.Default.CheckCircle else Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (hasAccessibilityPermission) AccentCyan else Color(0xFFFF9F0A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Поверх шторки уведомлений",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (hasAccessibilityPermission) "Активен наивысший приоритет (уровень 33)"
                                    else "Отображение над шторкой и статус-баром без блокировки нажатий",
                                    fontSize = 12.sp,
                                    color = if (hasAccessibilityPermission) AccentCyan else TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            HapticHelper.performClick(view)
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasAccessibilityPermission) Color(0xFF262C38) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (hasAccessibilityPermission) Icons.Default.Check else Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (hasAccessibilityPermission) AccentCyan else Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasAccessibilityPermission) "Настроено в спец. возможностях" else "Включить поверх шторки",
                            color = if (hasAccessibilityPermission) AccentCyan else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Сетка функциональных возможностей Pixel (Quick Tiles)
            PixelFeaturesOverviewCard()

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Переход к полной лаборатории тестов
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Лаборатория тестов Pixel", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                            Text("Проверка анимаций плеера, быстрой зарядки и Fast Pair", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            HapticHelper.performClick(view)
                            onNavigateToTests()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Открыть лабораторию тестов", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Фирменный заголовок "At a Glance" в стиле Google Pixel
 */
@Composable
private fun PixelAtAGlanceHeader(isServiceRunning: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Google Pixel Experience",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Статусный бейдж
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isServiceRunning) AccentGreen.copy(alpha = 0.15f)
                        else Color(0x228E8E93)
                    )
                    .border(
                        0.5.dp,
                        if (isServiceRunning) AccentGreen.copy(alpha = 0.4f) else Color(0x338E8E93),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isServiceRunning) AccentGreen else Color(0xFF8E8E93))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isServiceRunning) "Активен" else "Пауза",
                    color = if (isServiceRunning) AccentGreen else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "DroidIsland",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Живой интерактивный вырез камеры с волновым медиаплеером Google Pixel.",
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 19.sp
        )
    }
}

/**
 * Интерактивная витрина островка (Live Island Preview) прямо на главном экране
 */
@Composable
private fun PixelLiveIslandCard(
    currentState: IslandState,
    onStateSelected: (IslandState) -> Unit,
    config: IslandConfig,
    onTrackChange: (MusicTrack) -> Unit
) {
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Живой предпросмотр островка",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Интерактивный",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Сам динамический островок во всей красе
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0A0B0E))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                DynamicIslandView(
                    state = currentState,
                    cameraXOffset = config.cameraXOffset,
                    cameraYOffset = config.cameraYOffset,
                    cameraSize = config.cameraSize,
                    expandedWidth = config.expandedWidth,
                    expandedHeight = config.expandedHeight,
                    expandedOpacity = config.expandedIslandOpacity,
                    heightMultiplier = config.expandedHeightMultiplier,
                    isCalibrationGuideActive = config.isCalibrationGuideActive,
                    onIslandClick = {
                        // Раскрытие/сворачивание при клике
                        val nextState = when (currentState) {
                            is IslandState.MusicCompact -> IslandState.MusicExpanded(currentState.track)
                            is IslandState.MusicExpanded -> IslandState.MusicCompact(currentState.track)
                            is IslandState.Charging -> IslandState.ChargingExpanded(percentage = 85, realWatts = 30f)
                            is IslandState.ChargingExpanded -> IslandState.Charging(percentage = 85, realWatts = 30f)
                            else -> IslandState.Idle
                        }
                        onStateSelected(nextState)
                    },
                    onDismissClick = {
                        onStateSelected(IslandState.Idle)
                    },
                    onPlayPauseClick = {
                        if (currentState is IslandState.MusicExpanded) {
                            val updated = currentState.track.copy(isPlaying = !currentState.track.isPlaying)
                            onTrackChange(updated)
                        }
                    },
                    onTrackUpdated = onTrackChange
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Чипы переключения состояний предпросмотра
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PixelStateChip(
                    label = "🎵 Плеер",
                    isSelected = currentState is IslandState.MusicExpanded || currentState is IslandState.MusicCompact,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onStateSelected(
                            IslandState.MusicExpanded(
                                track = MusicTrack(
                                    title = "Starboy",
                                    artist = "The Weeknd",
                                    albumAccentColor = Color(0xFFE50914),
                                    isPlaying = true
                                )
                            )
                        )
                    }
                )

                PixelStateChip(
                    label = "⚡ Зарядка",
                    isSelected = currentState is IslandState.Charging || currentState is IslandState.ChargingExpanded,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onStateSelected(
                            IslandState.ChargingExpanded(
                                percentage = 85,
                                sourceName = "Turbo Power",
                                voltageMv = 4280,
                                temperatureC = 30.5f,
                                realWatts = 30.0f
                            )
                        )
                    }
                )

                PixelStateChip(
                    label = "💤 В покое",
                    isSelected = currentState is IslandState.Idle,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onStateSelected(IslandState.Idle)
                    }
                )
            }
        }
    }
}

@Composable
private fun PixelStateChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color(0xFF14161B)
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0x1FFFFFFF),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary
        )
    }
}

/**
 * Карточка обзора ключевых возможностей DroidIsland
 */
@Composable
private fun PixelFeaturesOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Модули Material You",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Островок плавно разворачивается только при наступлении событий:",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PixelModuleTile(
                    icon = Icons.Default.MusicNote,
                    title = "Плеер Pixel",
                    subtitle = "Волна и басы",
                    tint = Color(0xFFE50914),
                    modifier = Modifier.weight(1f)
                )

                PixelModuleTile(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Быстрая зарядка",
                    subtitle = "Ватты и вольтаж",
                    tint = AccentCyan,
                    modifier = Modifier.weight(1f)
                )

                PixelModuleTile(
                    icon = Icons.Default.Headphones,
                    title = "Fast Pair",
                    subtitle = "TWS и кейс",
                    tint = PastelSky,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PixelModuleTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF14161B))
            .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextSecondary, fontSize = 10.sp)
        }
    }
}
