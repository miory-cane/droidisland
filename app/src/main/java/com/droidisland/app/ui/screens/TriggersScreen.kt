package com.droidisland.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidisland.app.data.IslandConfig
import com.droidisland.app.data.IslandPreferences
import com.droidisland.app.ui.components.BouncingBassVisualizer
import com.droidisland.app.ui.components.PastelLilac
import com.droidisland.app.ui.components.PastelMint
import com.droidisland.app.ui.components.PastelPeach
import com.droidisland.app.ui.components.PastelSky
import com.droidisland.app.ui.components.PixelSquigglyWaveSlider
import com.droidisland.app.ui.theme.AccentCyan
import com.droidisland.app.ui.theme.CardBackground
import com.droidisland.app.ui.theme.CardBorder
import com.droidisland.app.ui.theme.DarkBackground
import com.droidisland.app.ui.theme.PrimaryBlue
import com.droidisland.app.ui.theme.TextPrimary
import com.droidisland.app.ui.theme.TextSecondary
import com.droidisland.app.util.HapticHelper
import kotlinx.coroutines.launch

@Composable
fun TriggersScreen(
    config: IslandConfig,
    preferences: IslandPreferences
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var chargingEnabled by remember { mutableStateOf(config.chargingEnabled) }
    var chargingTimeout by remember { mutableFloatStateOf(config.chargingTimeoutSec) }

    var soundEnabled by remember { mutableStateOf(config.soundProfileEnabled) }
    var soundTimeout by remember { mutableFloatStateOf(config.soundProfileTimeoutSec) }

    var musicEnabled by remember { mutableStateOf(config.musicEnabled) }
    var twsEnabled by remember { mutableStateOf(config.twsEnabled) }

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
            // Заголовок экрана в стиле Google Pixel Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Триггеры событий",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Управляйте автоматическим появлением островка при системных событиях смартфона.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 1. МУЗЫКАЛЬНЫЙ ТРИГГЕР (Pixel Squiggly Waveform & Equalizer)
            PixelTriggerCard(
                icon = Icons.Default.MusicNote,
                iconTint = Color(0xFFE50914),
                title = "Медиаплеер Google Pixel",
                subtitle = "Бегущая волна синусоиды, басы и перелив обложки",
                isEnabled = musicEnabled,
                onCheckedChange = {
                    HapticHelper.performClick(view)
                    musicEnabled = it
                    scope.launch { preferences.setMusicEnabled(it) }
                }
            ) {
                // Живой интерактивный бейдж волны Pixel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF14161C))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Плеер активен в фоне", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            BouncingBassVisualizer(isPlaying = true, tintColor = Color(0xFFE50914), maxHeight = 14.dp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        var demoProgress by remember { mutableFloatStateOf(0.42f) }
                        PixelSquigglyWaveSlider(
                            progress = demoProgress,
                            isPlaying = true,
                            waveColor = Color(0xFFE50914),
                            onSeek = { demoProgress = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TriggerFeaturePill(label = "Синусоида Pixel", modifier = Modifier.weight(1f))
                    TriggerFeaturePill(label = "Басовая волна", modifier = Modifier.weight(1f))
                    TriggerFeaturePill(label = "Аура обложки", modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. ЗАРЯДКА И ПИТАНИЕ (Pixel Battery Hub)
            PixelTriggerCard(
                icon = Icons.Default.Bolt,
                iconTint = AccentCyan,
                title = "Быстрая зарядка",
                subtitle = "Индикация мощности (W), вольтажа и температуры",
                isEnabled = chargingEnabled,
                onCheckedChange = {
                    HapticHelper.performClick(view)
                    chargingEnabled = it
                    scope.launch { preferences.setChargingEnabled(it) }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Длительность компактного показа:", fontSize = 13.sp, color = TextSecondary)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = String.format("%.1f с", chargingTimeout),
                            fontSize = 12.sp,
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = chargingTimeout,
                    onValueChange = {
                        chargingTimeout = it
                        scope.launch { preferences.setChargingTimeout(it) }
                    },
                    valueRange = 1.5f..6.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TriggerFeaturePill(label = "Кабель USB-PD", modifier = Modifier.weight(1f))
                    TriggerFeaturePill(label = "Беспроводная Qi", modifier = Modifier.weight(1f))
                    TriggerFeaturePill(label = "Карточка АКБ", modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. BLUETOOTH НАУШНИКИ & FAST PAIR
            PixelTriggerCard(
                icon = Icons.Default.Headphones,
                iconTint = PastelSky,
                title = "Google Fast Pair • Наушники",
                subtitle = "Индикаторы заряда TWS наушников, кейса и режимов ANC",
                isEnabled = twsEnabled,
                onCheckedChange = {
                    HapticHelper.performClick(view)
                    twsEnabled = it
                    scope.launch { preferences.setTwsEnabled(it) }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TriggerFeaturePill(label = "Pixel Buds Pro", modifier = Modifier.weight(1f))
                    TriggerFeaturePill(label = "Раздельный заряд", modifier = Modifier.weight(1f))
                    TriggerFeaturePill(label = "Шумоподавление", modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. ЗВУКОВЫЕ ПРОФИЛИ (Pixel Volume / Ringer Popups)
            PixelTriggerCard(
                icon = Icons.Default.NotificationsActive,
                iconTint = PastelLilac,
                title = "Профили громкости и звонка",
                subtitle = "Всплывающие капсулы DND, вибрации и обычного режима",
                isEnabled = soundEnabled,
                onCheckedChange = {
                    HapticHelper.performClick(view)
                    soundEnabled = it
                    scope.launch { preferences.setSoundProfileEnabled(it) }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Длительность индикатора:", fontSize = 13.sp, color = TextSecondary)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PastelLilac.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = String.format("%.1f с", soundTimeout),
                            fontSize = 12.sp,
                            color = PastelLilac,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = soundTimeout,
                    onValueChange = {
                        soundTimeout = it
                        scope.launch { preferences.setSoundProfileTimeout(it) }
                    },
                    valueRange = 1.0f..3.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = PastelLilac,
                        activeTrackColor = PastelLilac,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )
            }
        }
    }
}

/**
 * Карточка триггера в стилистике Google Pixel Settings Card
 */
@Composable
private fun PixelTriggerCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun TriggerFeaturePill(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14161C))
            .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}
