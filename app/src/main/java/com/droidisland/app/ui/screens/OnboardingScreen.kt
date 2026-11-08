package com.droidisland.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.droidisland.app.R
import com.droidisland.app.service.BluetoothHeadphoneManager
import com.droidisland.app.ui.components.BouncingBassVisualizer
import com.droidisland.app.ui.components.PastelSky
import com.droidisland.app.ui.theme.AccentCyan
import com.droidisland.app.ui.theme.AccentGreen
import com.droidisland.app.ui.theme.AccentLavender
import com.droidisland.app.ui.theme.AccentPeach
import com.droidisland.app.ui.theme.AccentYellow
import com.droidisland.app.ui.theme.CardBackground
import com.droidisland.app.ui.theme.CardBackgroundHigh
import com.droidisland.app.ui.theme.CardBorder
import com.droidisland.app.ui.theme.DarkBackground
import com.droidisland.app.ui.theme.PrimaryBlue
import com.droidisland.app.ui.theme.TextPrimary
import com.droidisland.app.ui.theme.TextSecondary
import com.droidisland.app.util.HapticHelper
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val description: String,
    val badgeText: String,
    val badgeColor: Color = AccentCyan,
    val isPermissionSlide: Boolean = false,
    val pageType: OnboardingPageType
)

enum class OnboardingPageType {
    WELCOME,
    EXPANSION,
    PERMISSIONS,
    CALIBRATION
}

/**
 * Экран приветствия в аутентичном стиле Google Pixel Setup Wizard (Material You / Expressive).
 */
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val pages = listOf(
        OnboardingPageData(
            badgeText = "GOOGLE PIXEL ЭСТЕТИКА",
            badgeColor = PrimaryBlue,
            title = "Добро пожаловать в DroidIsland",
            subtitle = "Живой вырез камеры",
            description = "Превратите глазок вашей фронтальной камеры в стильный, плавный и полезный интерактивный элемент смартфона.",
            pageType = OnboardingPageType.WELCOME
        ),
        OnboardingPageData(
            badgeText = "ИНТЕЛЛЕКТУАЛЬНЫЙ ОСТРОВОК",
            badgeColor = Color(0xFFE50914),
            title = "Островок оживает при событиях",
            subtitle = "Плеер, зарядка и наушники",
            description = "В покое островок незаметно спрятан в объективе. Он плавно расширяется с физикой Pixel при прослушивании музыки, зарядке или подключении наушников.",
            pageType = OnboardingPageType.EXPANSION
        ),
        OnboardingPageData(
            badgeText = "БЕЗОПАСНЫЕ ДОПУСКИ",
            badgeColor = AccentGreen,
            title = "Системные разрешения",
            subtitle = "Работа поверх всех приложений",
            description = "Чтобы островок отображался над всеми экранами и корректно отслеживал события, активируйте эти допуски:",
            isPermissionSlide = true,
            pageType = OnboardingPageType.PERMISSIONS
        ),
        OnboardingPageData(
            badgeText = "ПИКСЕЛЬНАЯ ТОЧНОСТЬ",
            badgeColor = AccentYellow,
            title = "Ювелирная калибровка",
            subtitle = "Подгонка под вырез вашего смартфона",
            description = "В следующем шаге вы сможете откалибровать положение островка точно под глазок фронтальной камеры.",
            pageType = OnboardingPageType.CALIBRATION
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Кнопка «Пропустить» в правом верхнем углу
        AnimatedVisibility(
            visible = pagerState.currentPage < pages.size - 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 46.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E2127))
                    .clickable {
                        HapticHelper.performClick(view)
                        onFinishOnboarding()
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Пропустить",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Пейджер слайдов
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val page = pages[pageIndex]
                if (page.isPermissionSlide) {
                    PixelOnboardingPermissionsSlide(page = page, context = context)
                } else {
                    PixelOnboardingPageContent(data = page)
                }
            }

            // Нижняя панель управления: Индикаторы страниц и главная кнопка Pixel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Анимированный индикатор-пилюля Google Pixel
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
                            label = "indicatorWidth"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF)
                                )
                        )
                    }
                }

                // Главная кнопка действия в форме фирменной Pixel-пилюли
                Button(
                    onClick = {
                        HapticHelper.performClick(view)
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinishOnboarding()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    )
                ) {
                    val isLastPage = pagerState.currentPage == pages.size - 1
                    Text(
                        text = if (isLastPage) "Начать использование" else "Продолжить",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Контент информационных страниц приветствия с выразительными визуальными карточками
 */
@Composable
private fun PixelOnboardingPageContent(data: OnboardingPageData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Выразительная графическая карточка слайда
        when (data.pageType) {
            OnboardingPageType.WELCOME -> {
                PixelWelcomeGraphic()
            }
            OnboardingPageType.EXPANSION -> {
                PixelExpansionGraphic()
            }
            OnboardingPageType.CALIBRATION -> {
                PixelCalibrationGraphic()
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(34.dp))

        // Бейдж категории
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(data.badgeColor.copy(alpha = 0.15f))
                .border(0.5.dp, data.badgeColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = data.badgeText,
                color = data.badgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = data.title,
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = data.subtitle,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = data.description,
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

/**
 * Графика приветствия: Логотип приложения в обрамлении Google-цветов
 */
@Composable
private fun PixelWelcomeGraphic() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(170.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        PrimaryBlue.copy(alpha = 0.30f),
                        AccentGreen.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(108.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(CardBackgroundHigh)
                .border(1.dp, CardBorder, RoundedCornerShape(32.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Логотип DroidIsland",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
        }
    }
}

/**
 * Графика расширения камеры: Миниатюра смартфона с динамическим островком
 */
@Composable
private fun PixelExpansionGraphic() {
    Box(
        modifier = Modifier
            .size(width = 240.dp, height = 140.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(CardBackgroundHigh)
            .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
            .padding(14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Имитация островка на экране
            Box(
                modifier = Modifier
                    .size(width = 175.dp, height = 38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color(0xFF0A0B0E))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(19.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BouncingBassVisualizer(isPlaying = true, tintColor = Color(0xFFE50914), maxHeight = 14.dp)
                    // Камера
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF14151B))
                            .border(1.dp, Color(0x44FFFFFF), CircleShape)
                    )
                    // Обложка
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFE50914)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                Text("Плавное расширение островка в такт басам", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

/**
 * Графика калибровки: Интерактивный калибровочный прицел
 */
@Composable
private fun PixelCalibrationGraphic() {
    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(CardBackgroundHigh)
            .border(1.dp, CardBorder, RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F1116))
                .border(1.5.dp, AccentCyan.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14161C))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            )
            Icon(
                imageVector = Icons.Default.CropFree,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(46.dp)
            )
        }
    }
}

/**
 * Слайд разрешений в стиле Google Pixel Settings
 */
@Composable
fun PixelOnboardingPermissionsSlide(
    page: OnboardingPageData,
    context: Context
) {
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasBatteryOpt by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }
    var hasNearbyDevices by remember { mutableStateOf(BluetoothHeadphoneManager.hasBluetoothPermission(context)) }
    var hasAccessibility by remember {
        mutableStateOf(com.droidisland.app.service.IslandOverlayService.isAccessibilityEnabled(context))
    }

    val nearbyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasNearbyDevices = results.values.any { it } || BluetoothHeadphoneManager.hasBluetoothPermission(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = Settings.canDrawOverlays(context)
                hasBatteryOpt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                hasNearbyDevices = BluetoothHeadphoneManager.hasBluetoothPermission(context)
                hasAccessibility = com.droidisland.app.service.IslandOverlayService.isAccessibilityEnabled(context)
                if (hasOverlay && !com.droidisland.app.service.IslandAccessibilityService.isRunning) {
                    com.droidisland.app.service.IslandOverlayService.start(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AccentGreen.copy(alpha = 0.15f))
                .border(0.5.dp, AccentGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = page.badgeText,
                color = AccentGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.title,
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = page.subtitle,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Пункт 1: Оверлей
        PixelPermissionTile(
            title = "Отображение поверх окон",
            subtitle = "Отрисовка динамического островка",
            icon = Icons.Default.Security,
            isGranted = hasOverlay,
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Пункт 2: Фоновая работа
        PixelPermissionTile(
            title = "Работа в фоновом режиме",
            subtitle = "Защита от усыпления системой",
            icon = Icons.Default.BatteryChargingFull,
            isGranted = hasBatteryOpt,
            onClick = {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Пункт 3: Bluetooth Fast Pair
        PixelPermissionTile(
            title = "Устройства поблизости",
            subtitle = "Google Fast Pair для наушников",
            icon = Icons.Default.DevicesOther,
            isGranted = hasNearbyDevices,
            onClick = {
                if (hasNearbyDevices) return@PixelPermissionTile
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    nearbyPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.BLUETOOTH_SCAN
                        )
                    )
                } else {
                    nearbyPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_ADMIN
                        )
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Пункт 4: Поверх шторки
        PixelPermissionTile(
            title = "Приоритет над шторкой",
            subtitle = "Островок виден даже при опускании шторки",
            icon = Icons.Default.Layers,
            isGranted = hasAccessibility,
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun PixelPermissionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isGranted) AccentGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(400),
        label = "tileBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isGranted) AccentGreen.copy(alpha = 0.5f) else Color(0x22FFFFFF),
        animationSpec = tween(400),
        label = "tileBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, animatedBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = animatedBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isGranted) AccentGreen.copy(alpha = 0.20f) else Color(0xFF22252C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                        contentDescription = null,
                        tint = if (isGranted) AccentGreen else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isGranted) "Разрешено • Готово к работе" else subtitle,
                        color = if (isGranted) AccentGreen else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isGranted) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка включения/статуса в форме пилюли
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isGranted) AccentGreen.copy(alpha = 0.20f)
                        else MaterialTheme.colorScheme.primary
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Активно",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Включить",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
