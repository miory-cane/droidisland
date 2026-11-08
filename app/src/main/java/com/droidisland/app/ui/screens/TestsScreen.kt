package com.droidisland.app.ui.screens

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidisland.app.data.IslandConfig
import com.droidisland.app.model.AncMode
import com.droidisland.app.model.HeadphoneFormFactor
import com.droidisland.app.model.IslandState
import com.droidisland.app.model.MusicPalette
import com.droidisland.app.model.MusicTrack
import com.droidisland.app.model.SoundProfileType
import com.droidisland.app.service.BluetoothHeadphoneManager
import com.droidisland.app.service.IslandOverlayService
import com.droidisland.app.ui.components.DynamicIslandView
import com.droidisland.app.ui.components.PastelLilac
import com.droidisland.app.ui.components.PastelMint
import com.droidisland.app.ui.components.PastelPeach
import com.droidisland.app.ui.components.PastelSky
import com.droidisland.app.ui.theme.AccentCyan
import com.droidisland.app.ui.theme.CardBackground
import com.droidisland.app.ui.theme.CardBorder
import com.droidisland.app.ui.theme.DarkBackground
import com.droidisland.app.ui.theme.PrimaryBlue
import com.droidisland.app.ui.theme.TextPrimary
import com.droidisland.app.ui.theme.TextSecondary
import com.droidisland.app.util.HapticHelper

@Composable
fun TestsScreen(
    config: IslandConfig
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    var islandState by remember { mutableStateOf<IslandState>(IslandState.Idle) }

    val mockDevices = remember { BluetoothHeadphoneManager.getMockBluetoothDevices() }
    val mockTwsDevice = mockDevices.first { it.formFactor == HeadphoneFormFactor.TWS }
    val mockOverEarDevice = mockDevices.first { it.formFactor == HeadphoneFormFactor.OVER_EAR }

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
            // Заголовок в стиле Pixel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Лаборатория тестов",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Мгновенная симуляция анимаций плеера Pixel, быстрой зарядки, наушников и звука.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. ТЕСТ МУЗЫКИ (Google Pixel Squiggly Wave, Басы, Лайк, Шторка)
            PixelTestCaseCard(
                icon = Icons.Default.MusicNote,
                iconTint = Color(0xFFE50914),
                title = "Медиаплеер Google Pixel",
                subtitle = "Бегущая волна синусоиды, басы слева и разворот по клику",
                actionButtonText = "🎵 Запустить тест плеера Pixel",
                actionButtonColor = Color(0xFFE50914),
                onAction = {
                    HapticHelper.performClick(view)
                    val state = IslandState.MusicCompact(
                        track = MusicTrack(
                            title = "Starboy",
                            artist = "The Weeknd",
                            appName = "Spotify",
                            albumAccentColor = Color(0xFFE50914),
                            palette = MusicPalette(Color(0xFFE50914), Color(0xFF6200EA), Color(0xFF00E5FF)),
                            isPlaying = true
                        )
                    )
                    islandState = state
                    IslandOverlayService.showState(context, state)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. ТЕСТ ЗАРЯДКИ (Pixel Battery Widget с ваттами и вольтажом)
            PixelTestCaseCard(
                icon = Icons.Default.Bolt,
                iconTint = AccentCyan,
                title = "Быстрая зарядка и АКБ",
                subtitle = "Индикация 30W Turbo, температуры и разворот в подробную карточку",
                actionButtonText = "⚡ Запустить тест зарядки (30W)",
                actionButtonColor = AccentCyan,
                isActionDarkText = true,
                onAction = {
                    HapticHelper.performClick(view)
                    val state = IslandState.Charging(
                        percentage = 85,
                        sourceName = "Turbo Power 30W",
                        voltageMv = 4280,
                        temperatureC = 30.2f,
                        realWatts = 30.0f,
                        currentMa = 5200
                    )
                    islandState = state
                    IslandOverlayService.showState(context, state, 6000)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. ТЕСТ НАУШНИКОВ (Google Fast Pair)
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
                                .background(PastelSky.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Headphones, contentDescription = null, tint = PastelSky, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Google Fast Pair • Наушники", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                            Text("Индикаторы L, R, кейса и ANC переключатель", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                HapticHelper.performClick(view)
                                val state = IslandState.TwsCompact(device = mockTwsDevice)
                                islandState = state
                                IslandOverlayService.showState(context, state, 6000)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PastelSky.copy(alpha = 0.20f))
                        ) {
                            Text("TWS (Pixel Buds)", color = PastelSky, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                HapticHelper.performClick(view)
                                val state = IslandState.OverEarCompact(device = mockOverEarDevice)
                                islandState = state
                                IslandOverlayService.showState(context, state, 6000)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PastelSky.copy(alpha = 0.20f))
                        ) {
                            Text("Полноразмерные", color = PastelSky, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. ТЕСТ ПРОФИЛЕЙ ЗВУКА
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
                                .background(PastelLilac.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PastelLilac, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Профили звука Pixel", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                            Text("Мягкие пастельные капсулы DND, вибро и звонка", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                HapticHelper.performClick(view)
                                val state = IslandState.SoundProfile(SoundProfileType.DND)
                                islandState = state
                                IslandOverlayService.showState(context, state, 4000)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PastelLilac.copy(alpha = 0.20f))
                        ) {
                            Text("DND", color = PastelLilac, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                HapticHelper.performClick(view)
                                val state = IslandState.SoundProfile(SoundProfileType.VIBRATE)
                                islandState = state
                                IslandOverlayService.showState(context, state, 4000)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PastelPeach.copy(alpha = 0.20f))
                        ) {
                            Text("Вибро", color = PastelPeach, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                HapticHelper.performClick(view)
                                val state = IslandState.SoundProfile(SoundProfileType.NORMAL)
                                islandState = state
                                IslandOverlayService.showState(context, state, 4000)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PastelMint.copy(alpha = 0.20f))
                        ) {
                            Text("Звук", color = PastelMint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. КНОПКА СВЕРНУТЬ ОСТРОВОК
            Button(
                onClick = {
                    HapticHelper.performLightTick(view)
                    islandState = IslandState.Idle
                    IslandOverlayService.showState(context, IslandState.Idle)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D2027))
            ) {
                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Свернуть островок обратно в камеру", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Превью динамического островка поверх тестов
        DynamicIslandView(
            state = islandState,
            cameraXOffset = config.cameraXOffset,
            cameraYOffset = config.cameraYOffset,
            cameraSize = config.cameraSize,
            expandedWidth = config.expandedWidth,
            expandedHeight = config.expandedHeight,
            expandedOpacity = config.expandedIslandOpacity,
            heightMultiplier = config.expandedHeightMultiplier,
            isCalibrationGuideActive = config.isCalibrationGuideActive,
            onIslandClick = {
                val nextState = when (islandState) {
                    is IslandState.TwsCompact -> {
                        val c = islandState as IslandState.TwsCompact
                        IslandState.TwsExpanded(device = c.device, ancMode = AncMode.ANC)
                    }
                    is IslandState.TwsExpanded -> {
                        val e = islandState as IslandState.TwsExpanded
                        IslandState.TwsCompact(device = e.device)
                    }
                    is IslandState.OverEarCompact -> {
                        val c = islandState as IslandState.OverEarCompact
                        IslandState.OverEarExpanded(device = c.device, ancMode = AncMode.ANC)
                    }
                    is IslandState.OverEarExpanded -> {
                        val e = islandState as IslandState.OverEarExpanded
                        IslandState.OverEarCompact(device = e.device)
                    }
                    is IslandState.MusicCompact -> {
                        val track = (islandState as IslandState.MusicCompact).track
                        IslandState.MusicExpanded(track = track)
                    }
                    is IslandState.MusicExpanded -> {
                        val track = (islandState as IslandState.MusicExpanded).track
                        IslandState.MusicCompact(track = track)
                    }
                    is IslandState.Charging -> {
                        val c = islandState as IslandState.Charging
                        IslandState.ChargingExpanded(
                            percentage = c.percentage,
                            sourceName = c.sourceName,
                            voltageMv = c.voltageMv,
                            temperatureC = c.temperatureC,
                            realWatts = c.realWatts,
                            currentMa = c.currentMa
                        )
                    }
                    is IslandState.ChargingExpanded -> {
                        val e = islandState as IslandState.ChargingExpanded
                        IslandState.Charging(
                            percentage = e.percentage,
                            sourceName = e.sourceName,
                            voltageMv = e.voltageMv,
                            temperatureC = e.temperatureC,
                            realWatts = e.realWatts,
                            currentMa = e.currentMa
                        )
                    }
                    else -> islandState
                }
                islandState = nextState
                IslandOverlayService.showState(context, nextState)
            },
            onDismissClick = {
                val nextState = if (islandState is IslandState.MusicExpanded) {
                    val track = (islandState as IslandState.MusicExpanded).track
                    IslandState.MusicCompact(track = track)
                } else {
                    IslandState.Idle
                }
                islandState = nextState
                IslandOverlayService.showState(context, nextState)
            },
            onTrackUpdated = { newTrack ->
                if (islandState is IslandState.MusicExpanded) {
                    val nextState = IslandState.MusicExpanded(track = newTrack)
                    islandState = nextState
                    IslandOverlayService.showState(context, nextState)
                }
            }
        )
    }
}

@Composable
private fun PixelTestCaseCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    actionButtonText: String,
    actionButtonColor: Color,
    isActionDarkText: Boolean = false,
    onAction: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconTint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = actionButtonColor)
            ) {
                Text(
                    text = actionButtonText,
                    color = if (isActionDarkText) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
