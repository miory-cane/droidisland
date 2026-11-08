package com.droidisland.app.ui.components

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.RadialGradient
import android.graphics.LinearGradient
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.droidisland.app.model.AncMode
import com.droidisland.app.model.BluetoothHeadphoneDevice
import com.droidisland.app.model.IslandState
import com.droidisland.app.model.MusicPalette
import com.droidisland.app.model.MusicTrack
import com.droidisland.app.model.SoundProfileType
import com.droidisland.app.service.IslandController
import com.droidisland.app.ui.theme.AccentCyan
import com.droidisland.app.ui.theme.TextPrimary
import com.droidisland.app.ui.theme.TextSecondary
import com.droidisland.app.util.HapticHelper
import com.droidisland.app.util.PaletteExtractor

// =========================================================================
// КЛЮЧЕВЫЕ ПАРАМЕТРЫ И ГРАФИЧЕСКИЕ КОНСТАНТЫ DYNAMIC ISLAND
// =========================================================================
private const val GLOW_BLUR_RADIUS_DP = 8.0f           // Базовый радиус размытия внешнего свечения
private const val GLOW_PADDING_DP = 16.0f             // Отступ bounding box для исключения клиппинга (blurRadius * 2)
private const val COLLAPSED_STROKE_WIDTH_DP = 1.5f     // Толщина контурного кольца в уменьшенном виде
private const val COLLAPSED_GLOW_SPREAD_DP = 4.0f      // Дополнительная толщина мягкого внешнего ореола свечения
private const val CONIC_ROTATION_DURATION_MS = 6000    // Длительность полного оборота 360° кольцевого SweepGradient
private const val AMBIENT_GLOW_DURATION_MS = 7500      // Длительность цикла плавного движения нижней подсветки
private const val EXPANDED_CORNER_RADIUS_DP = 28.0f    // Радиус скругления углов в увеличенном виде
private const val CAMERA_MASK_TOP_RATIO = 0.40f        // Доля верхней зоны со 100% OLED Black (маскировка камеры)
private const val CAMERA_MASK_FADE_RATIO = 0.78f       // Доля высоты, где маска плавно переходит в прозрачность
private const val SPRING_DAMPING = Spring.DampingRatioMediumBouncy
private const val SPRING_STIFFNESS = Spring.StiffnessMediumLow

// Пастельная палитра для системных статусов
val PastelLilac = Color(0xFFD7B8FF)   // Не беспокоить
val PastelPeach = Color(0xFFFFCEB4)   // Вибрация
val PastelMint = Color(0xFFB4F0D5)    // Звук
val PastelSky = Color(0xFFB8D7FF)     // Bluetooth и наушники

@Composable
fun DynamicIslandView(
    state: IslandState,
    cameraXOffset: Int,
    cameraYOffset: Int,
    cameraSize: Int,
    expandedWidth: Int,
    expandedHeight: Int,
    expandedOpacity: Float = 0.85f,
    heightMultiplier: Float = 1.0f,
    isCalibrationGuideActive: Boolean = false,
    onIslandClick: () -> Unit = {},
    onDismissClick: () -> Unit = {},
    onAncChanged: (AncMode) -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPrevClick: () -> Unit = {},
    onOpenMusicApp: () -> Unit = {},
    onTrackUpdated: (MusicTrack) -> Unit = {}
) {
    val view = LocalView.current
    val isExpanded = state is IslandState.TwsExpanded ||
            state is IslandState.OverEarExpanded ||
            state is IslandState.MusicExpanded ||
            state is IslandState.ChargingExpanded

    // Настраиваемая геометрия шторки: ширина до 600 dp, адаптивная высота
    val currentExpandedWidthDp = expandedWidth.coerceIn(260, 600).dp
    val currentExpandedHeightDp = (expandedHeight.coerceIn(100, 220) * heightMultiplier).coerceIn(90f, 240f).dp

    // В уменьшенном виде: cornerRadius = height / 2f (идеальная круглая капсула без срезанных углов)
    val (targetWidthDp, targetHeightDp, targetRadiusDp) = when (state) {
        is IslandState.Idle -> Triple(cameraSize.dp, cameraSize.dp, (cameraSize / 2f).dp)
        is IslandState.Charging -> Triple(220.dp, 42.dp, 21.dp)
        is IslandState.ChargingExpanded -> Triple(currentExpandedWidthDp, currentExpandedHeightDp, EXPANDED_CORNER_RADIUS_DP.dp)
        is IslandState.SoundProfile -> Triple((cameraSize + 70).dp, 38.dp, 19.dp)
        is IslandState.TwsCompact -> Triple(245.dp, 42.dp, 21.dp)
        is IslandState.TwsExpanded -> Triple(currentExpandedWidthDp, currentExpandedHeightDp, EXPANDED_CORNER_RADIUS_DP.dp)
        is IslandState.OverEarCompact -> Triple(220.dp, 42.dp, 21.dp)
        is IslandState.OverEarExpanded -> Triple(currentExpandedWidthDp, currentExpandedHeightDp, EXPANDED_CORNER_RADIUS_DP.dp)
        is IslandState.MusicCompact -> Triple((cameraSize + 76).dp, 40.dp, 20.dp)
        is IslandState.MusicExpanded -> Triple(currentExpandedWidthDp, currentExpandedHeightDp, EXPANDED_CORNER_RADIUS_DP.dp)
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidthDp,
        animationSpec = spring(dampingRatio = SPRING_DAMPING, stiffness = SPRING_STIFFNESS),
        label = "islandWidth"
    )

    val animatedHeight by animateDpAsState(
        targetValue = targetHeightDp,
        animationSpec = spring(dampingRatio = SPRING_DAMPING, stiffness = SPRING_STIFFNESS),
        label = "islandHeight"
    )

    val animatedRadius by animateDpAsState(
        targetValue = targetRadiusDp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "islandRadius"
    )

    // Плавная синхронизация переходов (Spring)
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = SPRING_DAMPING, stiffness = SPRING_STIFFNESS),
        label = "islandExpandProgress"
    )

    // Извлечение и нормализация 3 ключевых цветов (Color1, Color2, Color3)
    val rawColors = when (state) {
        is IslandState.MusicCompact -> Triple(
            state.track.palette.color1,
            state.track.palette.color2,
            state.track.palette.color3
        )
        is IslandState.MusicExpanded -> Triple(
            state.track.palette.color1,
            state.track.palette.color2,
            state.track.palette.color3
        )
        is IslandState.Charging, is IslandState.ChargingExpanded -> Triple(
            AccentCyan,
            Color(0xFF00E5FF),
            Color(0xFF00B0FF)
        )
        is IslandState.TwsCompact, is IslandState.TwsExpanded -> Triple(
            PastelSky,
            Color(0xFF82B1FF),
            Color(0xFFB388FF)
        )
        is IslandState.OverEarCompact, is IslandState.OverEarExpanded -> Triple(
            PastelSky,
            Color(0xFF80D8FF),
            Color(0xFF82B1FF)
        )
        is IslandState.SoundProfile -> {
            val profColor = when (state.profile) {
                SoundProfileType.DND -> PastelLilac
                SoundProfileType.VIBRATE -> PastelPeach
                SoundProfileType.NORMAL -> PastelMint
            }
            Triple(profColor, profColor.copy(alpha = 0.7f), profColor.copy(alpha = 0.4f))
        }
        is IslandState.Idle -> Triple(
            Color.Transparent,
            Color.Transparent,
            Color.Transparent
        )
    }

    val targetC1 = remember(rawColors.first) { PaletteExtractor.normalizeColor(rawColors.first) }
    val targetC2 = remember(rawColors.second) { PaletteExtractor.normalizeColor(rawColors.second) }
    val targetC3 = remember(rawColors.third) { PaletteExtractor.normalizeColor(rawColors.third) }

    // Плавная цветовая интерполяция при смене трека (lerp)
    val animatedC1 by animateColorAsState(
        targetValue = targetC1,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "islandColor1"
    )
    val animatedC2 by animateColorAsState(
        targetValue = targetC2,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "islandColor2"
    )
    val animatedC3 by animateColorAsState(
        targetValue = targetC3,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "islandColor3"
    )

    // ТРЕБОВАНИЕ 2: Точная привязка к верхней точке калибровки камеры (Top-Anchor Alignment)
    // expandedIsland.topY == collapsedIsland.topY == cameraCalibration.topY
    // Островок жестко зафиксирован по верхней границе и при раскрытии растет ИСКЛЮЧИТЕЛЬНО вниз и вширь!
    val cameraCalibrationTopY = cameraYOffset.coerceAtLeast(0).dp
    val glowPadding = GLOW_PADDING_DP.dp
    val outerBoxTopY = cameraCalibrationTopY - glowPadding

    val coroutineScope = rememberCoroutineScope()
    val swipeOffsetX = remember { Animatable(0f) }
    val swipeOffsetY = remember { Animatable(0f) }

    // Прозрачности двух режимов подсветки:
    // В уменьшенном виде: кольцевой SweepGradient затухает 1 -> 0 при расширении
    // В увеличенном виде: нижняя подсветка проявляется 0 -> 1 при расширении
    val contourAlpha = ((1f - expandProgress) * (if (state is IslandState.Idle && !isCalibrationGuideActive) 0f else 1f)).coerceIn(0f, 1f)
    val bottomGlowAlpha = (expandProgress * (if (state is IslandState.Idle) 0f else 1f)).coerceIn(0f, 1f)

    // ТРЕБОВАНИЕ 1: Родительская обертка с увеличенным bounding box (на blurRadius * 2 со всех сторон)
    // чтобы внешнее свечение и кольцевой градиент НИКОГДА не обрезались по краям!
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = cameraXOffset.dp, y = outerBoxTopY),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(swipeOffsetX.value.roundToInt(), swipeOffsetY.value.roundToInt()) }
                .size(
                    width = animatedWidth + (glowPadding * 2),
                    height = animatedHeight + (glowPadding * 2)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            // РЕЖИМ А: Внешнее кольцевое 3-цветное переливание вокруг капсулы (SweepGradient 360°)
            // Рендерится на расширенном Canvas без обрезания границ
            CollapsedContourGlowCanvas(
                color1 = animatedC1,
                color2 = animatedC2,
                color3 = animatedC3,
                contourAlpha = contourAlpha,
                capsuleWidth = animatedWidth,
                capsuleHeight = animatedHeight,
                cornerRadius = animatedRadius,
                glowPadding = glowPadding,
                isCalibrationActive = isCalibrationGuideActive && state is IslandState.Idle,
                modifier = Modifier.fillMaxSize()
            )

            // ФИЗИЧЕСКАЯ ЧЕРНАЯ КАПСУЛА И ИНТЕРАКТИВНЫЙ ХИТБОКС
            // Точный размер (animatedWidth x animatedHeight), верхний край математически равен cameraCalibrationTopY!
            Box(
                modifier = Modifier
                    .padding(top = glowPadding)
                    .size(
                        width = animatedWidth.coerceAtLeast(cameraSize.dp),
                        height = animatedHeight.coerceAtLeast(cameraSize.dp)
                    )
                    .clip(RoundedCornerShape(animatedRadius.coerceAtLeast(0.dp)))
                    // ТРЕБОВАНИЕ 3: Чистый OLED Black (#000000, alpha 1.0) в свернутом виде и вокруг камеры сверху,
                    // плавно раскрывающийся в 85% прозрачность снизу в развернутом виде
                    .background(
                        if (isExpanded) {
                            Brush.verticalGradient(
                                0.00f to Color(0xFF000000),
                                CAMERA_MASK_TOP_RATIO to Color(0xFF000000),
                                CAMERA_MASK_FADE_RATIO to Color(0xFF000000).copy(alpha = lerp(1.0f, expandedOpacity, 0.5f)),
                                1.00f to Color(0xFF000000).copy(alpha = expandedOpacity)
                            )
                        } else {
                            SolidColor(Color(0xFF000000))
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            HapticHelper.performClick(view)
                            onIslandClick()
                        }
                    )
                    // ТРЕБОВАНИЕ 4: Жест свайпа для сворачивания сохранен на самом увеличенном островке
                    .pointerInput(isExpanded) {
                        if (isExpanded) {
                            var cumulativeDragY = 0f
                            detectVerticalDragGestures(
                                onDragStart = {
                                    cumulativeDragY = 0f
                                },
                                onDragEnd = {
                                    val currentOffset = swipeOffsetY.value
                                    coroutineScope.launch {
                                        if (cumulativeDragY <= -24f || currentOffset <= -20f || cumulativeDragY >= 50f) {
                                            HapticHelper.performClick(view)
                                            onDismissClick()
                                        }
                                        swipeOffsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        swipeOffsetY.animateTo(0f)
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    cumulativeDragY += dragAmount
                                    coroutineScope.launch {
                                        val newOffset = (swipeOffsetY.value + dragAmount * 0.75f).coerceIn(-65f, 40f)
                                        swipeOffsetY.snapTo(newOffset)
                                        if (newOffset <= -38f || cumulativeDragY <= -42f || cumulativeDragY >= 65f) {
                                            HapticHelper.performClick(view)
                                            onDismissClick()
                                            swipeOffsetY.animateTo(0f)
                                        }
                                    }
                                }
                            )
                        } else if (state !is IslandState.Idle) {
                            var cumulativeDragX = 0f
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    cumulativeDragX = 0f
                                },
                                onDragEnd = {
                                    val currentOffset = swipeOffsetX.value
                                    coroutineScope.launch {
                                        if (abs(currentOffset) > 40f || abs(cumulativeDragX) > 45f) {
                                            HapticHelper.performLightTick(view)
                                            onDismissClick()
                                        }
                                        swipeOffsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        swipeOffsetX.animateTo(0f)
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    cumulativeDragX += dragAmount
                                    coroutineScope.launch {
                                        val newOffset = (swipeOffsetX.value + dragAmount * 0.7f).coerceIn(-120f, 120f)
                                        swipeOffsetX.snapTo(newOffset)
                                        if (abs(newOffset) > 75f) {
                                            HapticHelper.performLightTick(view)
                                            onDismissClick()
                                            swipeOffsetX.animateTo(0f)
                                        }
                                    }
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // РЕЖИМ Б: Нижняя живая подсветка увеличенного островка (Bottom Ambient Glow)
                // С гарантированной 100% OLED Black зоной сверху (маскировка камеры)
                // и обрезкой по внутреннему скругленному контуру капсулы
                ExpandedBottomAmbientGlow(
                    color1 = animatedC1,
                    color2 = animatedC2,
                    color3 = animatedC3,
                    glowAlpha = bottomGlowAlpha,
                    cornerRadius = animatedRadius,
                    modifier = Modifier.fillMaxSize()
                )

                // Содержимое каждого модуля (плеера, зарядки, наушников и калибровки)
                when (state) {
                    is IslandState.Idle -> {
                        if (isCalibrationGuideActive) {
                            CalibrationGuideTarget(cameraSize = cameraSize)
                        }
                    }

                    is IslandState.Charging -> {
                        ChargingContent(
                            state = state,
                            cameraSize = cameraSize,
                            onIslandClick = onIslandClick
                        )
                    }

                    is IslandState.ChargingExpanded -> {
                        ChargingExpandedContent(
                            state = state,
                            cameraSize = cameraSize,
                            onClose = {
                                HapticHelper.performLightTick(view)
                                onDismissClick()
                            }
                        )
                    }

                    is IslandState.SoundProfile -> {
                        SoundProfileContent(
                            state = state,
                            cameraSize = cameraSize,
                            onIslandClick = onIslandClick
                        )
                    }

                    is IslandState.TwsCompact -> {
                        TwsCompactContent(
                            device = state.device,
                            cameraSize = cameraSize,
                            onIslandClick = onIslandClick
                        )
                    }

                    is IslandState.TwsExpanded -> {
                        TwsExpandedContent(
                            device = state.device,
                            cameraSize = cameraSize,
                            ancMode = state.ancMode,
                            onClose = {
                                HapticHelper.performLightTick(view)
                                onDismissClick()
                            },
                            onAncSelect = { mode ->
                                HapticHelper.performClick(view)
                                onAncChanged(mode)
                            }
                        )
                    }

                    is IslandState.OverEarCompact -> {
                        OverEarCompactContent(
                            device = state.device,
                            cameraSize = cameraSize,
                            onIslandClick = onIslandClick
                        )
                    }

                    is IslandState.OverEarExpanded -> {
                        OverEarExpandedContent(
                            device = state.device,
                            cameraSize = cameraSize,
                            ancMode = state.ancMode,
                            onClose = {
                                HapticHelper.performLightTick(view)
                                onDismissClick()
                            },
                            onAncSelect = { mode ->
                                HapticHelper.performClick(view)
                                onAncChanged(mode)
                            }
                        )
                    }

                    is IslandState.MusicCompact -> {
                        MusicCompactContent(
                            track = state.track,
                            cameraSize = cameraSize,
                            onIslandClick = onIslandClick
                        )
                    }

                    is IslandState.MusicExpanded -> {
                        MusicExpandedContent(
                            track = state.track,
                            cameraSize = cameraSize,
                            onClose = {
                                HapticHelper.performLightTick(view)
                                onDismissClick()
                            },
                            onPlayPause = onPlayPauseClick,
                            onNext = onNextClick,
                            onPrev = onPrevClick,
                            onOpenApp = onOpenMusicApp,
                            onTrackChange = onTrackUpdated
                        )
                    }
                }
            }
        }
    }
}

/**
 * РЕЖИМ А: Бесшовный угловой градиент (SweepGradient) из 3-х цветов обложки вокруг черной капсулы.
 * Вращается непрерывно на 360 градусов с нулевым созданием объектов в onDraw.
 */
@Composable
private fun CollapsedContourGlowCanvas(
    color1: Color,
    color2: Color,
    color3: Color,
    contourAlpha: Float,
    capsuleWidth: Dp,
    capsuleHeight: Dp,
    cornerRadius: Dp,
    glowPadding: Dp,
    isCalibrationActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (contourAlpha <= 0.001f) return

    val infiniteTransition = rememberInfiniteTransition(label = "conicRing")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CONIC_ROTATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "conicRotationAngle"
    )

    // Кэшированные графические объекты для исключения аллокаций в frame loop
    val matrix = remember { Matrix() }
    val glowPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
    }
    val contourPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
    }
    val capsuleRect = remember { RectF() }

    Canvas(modifier = modifier) {
        val padPx = glowPadding.toPx()
        val wPx = capsuleWidth.toPx()
        val hPx = capsuleHeight.toPx()
        val rPx = cornerRadius.toPx()

        if (wPx <= 1f || hPx <= 1f) return@Canvas

        val cx = padPx + wPx / 2f
        val cy = padPx + hPx / 2f

        capsuleRect.set(padPx, padPx, padPx + wPx, padPx + hPx)

        // Бесшовный массив цветов: [Color1, Color2, Color3, Color1]
        val colors = intArrayOf(
            color1.toArgb(),
            color2.toArgb(),
            color3.toArgb(),
            color1.toArgb()
        )
        val positions = floatArrayOf(0f, 0.3333f, 0.6667f, 1.0f)
        val sweepShader = SweepGradient(cx, cy, colors, positions)

        matrix.reset()
        matrix.setRotate(rotationAngle, cx, cy)
        sweepShader.setLocalMatrix(matrix)

        val nativeCanvas = drawContext.canvas.nativeCanvas

        val strokeWidthPx = (if (isCalibrationActive) 2.0f else COLLAPSED_STROKE_WIDTH_DP).dp.toPx()
        val glowSpreadPx = COLLAPSED_GLOW_SPREAD_DP.dp.toPx()

        // 1. Мягкий внешний ореол свечения (без клиппинга, так как bounding box расширен на padPx)
        glowPaint.shader = sweepShader
        glowPaint.strokeWidth = strokeWidthPx + glowSpreadPx
        glowPaint.alpha = (contourAlpha * 0.40f * 255).roundToInt().coerceIn(0, 255)
        nativeCanvas.drawRoundRect(capsuleRect, rPx, rPx, glowPaint)

        // 2. Четкий контурный ободок
        contourPaint.shader = sweepShader
        contourPaint.strokeWidth = strokeWidthPx
        contourPaint.alpha = (contourAlpha * 0.95f * 255).roundToInt().coerceIn(0, 255)
        nativeCanvas.drawRoundRect(capsuleRect, rPx, rPx, contourPaint)
    }
}

/**
 * РЕЖИМ Б: Нижняя живая динамическая подсветка увеличенного островка (Bottom Ambient Glow).
 * 3 органических плавающих пятна из цветов обложки внизу островка,
 * с гарантированной зоной 100% OLED Black (#000000) сверху для полной маскировки камеры.
 */
@Composable
private fun ExpandedBottomAmbientGlow(
    color1: Color,
    color2: Color,
    color3: Color,
    glowAlpha: Float,
    cornerRadius: Dp,
    modifier: Modifier = Modifier
) {
    if (glowAlpha <= 0.001f) return

    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AMBIENT_GLOW_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientPhase"
    )

    // Кэшированные матрицы и Paint для исключения аллокаций в frame loop
    val matrix1 = remember { Matrix() }
    val matrix2 = remember { Matrix() }
    val matrix3 = remember { Matrix() }

    val blobPaint1 = remember { Paint().apply { isAntiAlias = true } }
    val blobPaint2 = remember { Paint().apply { isAntiAlias = true } }
    val blobPaint3 = remember { Paint().apply { isAntiAlias = true } }
    val maskPaint = remember { Paint().apply { isAntiAlias = true } }
    val drawRectF = remember { RectF() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@Canvas

        val rPx = cornerRadius.toPx()
        drawRectF.set(0f, 0f, w, h)

        // Органические траектории 3-х цветовых пятен в нижней половине островка (sin/cos от времени)
        val x1 = w * (0.28f + 0.18f * sin(phase))
        val y1 = h * (0.80f + 0.12f * cos(phase * 0.85f))
        val r1 = (w * 0.48f).coerceAtLeast(1f)

        val x2 = w * (0.72f + 0.18f * sin(phase + 2.0943951f))
        val y2 = h * (0.78f + 0.14f * cos(phase * 0.95f + 1.2f))
        val r2 = (w * 0.50f).coerceAtLeast(1f)

        val x3 = w * (0.50f + 0.22f * cos(phase + 4.1887902f))
        val y3 = h * (0.86f + 0.10f * sin(phase * 1.05f + 2.5f))
        val r3 = (w * 0.44f).coerceAtLeast(1f)

        // Радиальные градиенты для 3 пятен
        val radial1 = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(color1.toArgb(), color1.copy(alpha = 0.50f).toArgb(), android.graphics.Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        val radial2 = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(color2.toArgb(), color2.copy(alpha = 0.50f).toArgb(), android.graphics.Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        val radial3 = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(color3.toArgb(), color3.copy(alpha = 0.50f).toArgb(), android.graphics.Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        matrix1.reset()
        matrix1.setScale(r1, r1)
        matrix1.postTranslate(x1, y1)
        radial1.setLocalMatrix(matrix1)

        matrix2.reset()
        matrix2.setScale(r2, r2)
        matrix2.postTranslate(x2, y2)
        radial2.setLocalMatrix(matrix2)

        matrix3.reset()
        matrix3.setScale(r3, r3)
        matrix3.postTranslate(x3, y3)
        radial3.setLocalMatrix(matrix3)

        val alphaInt = (glowAlpha * 0.60f * 255).roundToInt().coerceIn(0, 255)

        blobPaint1.shader = radial1
        blobPaint1.alpha = alphaInt

        blobPaint2.shader = radial2
        blobPaint2.alpha = alphaInt

        blobPaint3.shader = radial3
        blobPaint3.alpha = alphaInt

        val nativeCanvas = drawContext.canvas.nativeCanvas

        // 1. Отрисовка 3-х динамических пятен свечения
        nativeCanvas.drawRoundRect(drawRectF, rPx, rPx, blobPaint1)
        nativeCanvas.drawRoundRect(drawRectF, rPx, rPx, blobPaint2)
        nativeCanvas.drawRoundRect(drawRectF, rPx, rPx, blobPaint3)

        // 2. Вертикальная маска: строго 100% OLED Black (#000000) в верхней зоне (35-45% высоты)
        // Гарантирует абсолютную маскировку камеры без бликов и засветок
        val topMaskShader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                android.graphics.Color.BLACK,
                android.graphics.Color.BLACK,
                android.graphics.Color.TRANSPARENT
            ),
            floatArrayOf(0f, CAMERA_MASK_TOP_RATIO, CAMERA_MASK_FADE_RATIO),
            Shader.TileMode.CLAMP
        )
        maskPaint.shader = topMaskShader
        maskPaint.alpha = (glowAlpha * 255).roundToInt().coerceIn(0, 255)
        nativeCanvas.drawRoundRect(drawRectF, rPx, rPx, maskPaint)
    }
}

/**
 * Неоновая оптическая мишень калибровки с прозрачным центром для центрирования точно поверх объектива
 */
@Composable
private fun CalibrationGuideTarget(cameraSize: Int) {
    Canvas(
        modifier = Modifier
            .size(cameraSize.dp)
            .padding(1.dp)
    ) {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@Canvas
        val c = Offset(w / 2f, h / 2f)
        val r = ((w / 2f) - 1.5.dp.toPx()).coerceAtLeast(1f)

        // Внешнее кольцо точного диаметра островка
        drawCircle(
            color = AccentCyan,
            radius = r,
            center = c,
            style = Stroke(width = 2.dp.toPx())
        )

        // Внутреннее тонкое кольцо зоны линзы
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = r * 0.70f,
            center = c,
            style = Stroke(width = 1.dp.toPx())
        )

        // Перекрестие прицела (горизонтальное и вертикальное)
        drawLine(
            color = AccentCyan.copy(alpha = 0.85f),
            start = Offset(c.x, c.y - r * 0.6f),
            end = Offset(c.x, c.y + r * 0.6f),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = AccentCyan.copy(alpha = 0.85f),
            start = Offset(c.x - r * 0.6f, c.y),
            end = Offset(c.x + r * 0.6f, c.y),
            strokeWidth = 1.5.dp.toPx()
        )

        // Центральная микроточка
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = c
        )
    }
}

/**
 * Премиальная обложка альбома:
 * Если есть Bitmap от играющего плеера — отрисовывает настоящую обложку.
 * Если Bitmap отсутствует — отрисовывает стилизованный виниловый арт с 3 цветами палитры.
 * В углу отображается аккуратный цветной бейдж источника (Spotify / Яндекс / YouTube / VK / Apple).
 */
@Composable
private fun AlbumCoverArtView(
    track: MusicTrack,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = track.albumArtBitmap,
            animationSpec = tween(durationMillis = 280),
            label = "coverCrossfade"
        ) { bitmap ->
            val safeImageBitmap = remember(bitmap) {
                if (bitmap != null && !bitmap.isRecycled) {
                    try {
                        val safe = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                            bitmap.config == android.graphics.Bitmap.Config.HARDWARE
                        ) {
                            bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                        } else {
                            bitmap
                        }
                        safe?.asImageBitmap()
                    } catch (e: Throwable) {
                        null
                    }
                } else null
            }

            if (safeImageBitmap != null) {
                Image(
                    bitmap = safeImageBitmap,
                    contentDescription = "Обложка трека",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                RenderVinylCover(track = track)
            }
        }

        // Тонкая рамка обложки
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
        )

        // Микро-бейдж источника в правом нижнем углу
        val badgeColor = when {
            track.appName.contains("spotify", ignoreCase = true) -> Color(0xFF1DB954)
            track.appName.contains("yandex", ignoreCase = true) || track.appName.contains("яндекс", ignoreCase = true) -> Color(0xFFFFCC00)
            track.appName.contains("youtube", ignoreCase = true) -> Color(0xFFFF0000)
            track.appName.contains("vk", ignoreCase = true) -> Color(0xFF0077FF)
            track.appName.contains("apple", ignoreCase = true) -> Color(0xFFFA243C)
            else -> track.albumAccentColor
        }
        Box(
            modifier = Modifier
                .size(9.dp)
                .align(Alignment.BottomEnd)
                .padding(1.dp)
                .clip(CircleShape)
                .background(badgeColor)
                .border(0.5.dp, Color.Black, CircleShape)
        )
    }
}

@Composable
private fun RenderVinylCover(track: MusicTrack) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f || w.isNaN() || h.isNaN()) return@Canvas

        try {
            val c1 = if (track.palette.color1 != Color.Unspecified) track.palette.color1 else Color(0xFFE50914)
            val c2 = if (track.palette.color2 != Color.Unspecified) track.palette.color2 else Color(0xFF6200EA)
            val c3 = if (track.palette.color3 != Color.Unspecified) track.palette.color3 else Color(0xFF00E5FF)

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(c1, c2, c3)
                )
            )

            val center = Offset(w * 0.5f, h * 0.5f)
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = (w * 0.38f).coerceAtLeast(1f),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = (w * 0.24f).coerceAtLeast(1f),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = (w * 0.12f).coerceAtLeast(1f),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = center
            )
        } catch (_: Throwable) {}
    }
}

/**
 * Музыка: Компактный режим в стиле Google Pixel
 * Слева: басовый эквалайзер Pixel Audio.
 * Центр: гарантированный зазор под глазок камеры (без наплыва текста).
 * Справа: мини-обложка альбома с динамически расширяющейся аурой.
 */
@Composable
private fun MusicCompactContent(
    track: MusicTrack,
    cameraSize: Int,
    onIslandClick: () -> Unit = {}
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    HapticHelper.performClick(view)
                    onIslandClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BouncingBassVisualizer(
            isPlaying = track.isPlaying,
            tintColor = track.albumAccentColor,
            maxHeight = 16.dp,
            barWidth = 3.dp,
            spacing = 2.dp
        )

        Spacer(modifier = Modifier.width((cameraSize + 6).dp))

        // Статичная чистая мини-обложка альбома без пульсации
        AlbumCoverArtView(
            track = track,
            size = 26.dp
        )
    }
}

/**
 * Музыка: Развернутый вид — Аутентичный медиаплеер Google Pixel
 * ТРЕБОВАНИЕ 4: Горизонтальная полоска-индикатор (grabber bar) полностью удалена,
 * лишний нижний/верхний margin убран, контент идеально сбалансирован.
 */
@Composable
private fun MusicExpandedContent(
    track: MusicTrack,
    cameraSize: Int,
    onClose: () -> Unit,
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrev: () -> Unit = {},
    onOpenApp: () -> Unit = {},
    onTrackChange: (MusicTrack) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isPlaying = track.isPlaying

    var isUserScrubbing by remember { mutableStateOf(false) }
    var localPositionSec by remember(track.title, track.artist) {
        mutableIntStateOf(track.currentPositionSec)
    }

    LaunchedEffect(track.currentPositionSec) {
        if (!isUserScrubbing) {
            val delta = abs(track.currentPositionSec - localPositionSec)
            if (delta >= 2) {
                localPositionSec = track.currentPositionSec
            }
        }
    }

    LaunchedEffect(isPlaying, track.durationSec) {
        if (isPlaying && track.durationSec > 0) {
            while (true) {
                delay(1000L)
                if (!isUserScrubbing) {
                    val nextSec = localPositionSec + 1
                    if (nextSec <= track.durationSec) {
                        localPositionSec = nextSec
                    }
                }
            }
        }
    }

    val displayProgress = if (track.durationSec > 0) {
        (localPositionSec.toFloat() / track.durationSec).coerceIn(0f, 1f)
    } else {
        0f
    }

    val topContentPadding = (cameraSize + 6).dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .padding(top = topContentPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ВЕРХНИЙ РЯД: Настоящая обложка, Название трека, Автор/Источник и кнопка закрытия
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            HapticHelper.performClick(view)
                            onOpenApp()
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumCoverArtView(track = track, size = 36.dp)

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = track.title,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} • ${track.appName}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = {
                    HapticHelper.performLightTick(view)
                    onClose()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Свернуть",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ЦЕНТР: Фирменный синусоидный волновой слайдер Pixel Squiggly Wave
        Column(modifier = Modifier.fillMaxWidth()) {
            PixelSquigglyWaveSlider(
                progress = displayProgress,
                isPlaying = isPlaying,
                waveColor = track.albumAccentColor,
                waveHeight = 6.dp,
                onSeekChange = { newProg ->
                    isUserScrubbing = true
                    val targetSec = (newProg * track.durationSec).toInt().coerceIn(0, track.durationSec)
                    localPositionSec = targetSec
                },
                onSeekComplete = { finalProg ->
                    isUserScrubbing = false
                    val targetSec = (finalProg * track.durationSec).toInt().coerceIn(0, track.durationSec)
                    localPositionSec = targetSec
                    IslandController.seekMediaTo(context, finalProg)
                }
            )

            var showRemainingTime by remember { mutableStateOf(false) }
            val currentSec = localPositionSec.coerceIn(0, track.durationSec)
            val elapsed = String.format("%d:%02d", currentSec / 60, currentSec % 60)
            val remainingSec = (track.durationSec - currentSec).coerceAtLeast(0)
            val rightTime = if (showRemainingTime) {
                String.format("-%d:%02d", remainingSec / 60, remainingSec % 60)
            } else {
                String.format("%d:%02d", track.durationSec / 60, track.durationSec % 60)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = elapsed, fontSize = 9.5.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Text(
                    text = rightTime,
                    fontSize = 9.5.sp,
                    color = if (showRemainingTime) track.albumAccentColor else TextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            HapticHelper.performLightTick(view)
                            showRemainingTime = !showRemainingTime
                        }
                    )
                )
            }
        }

        // НИЖНИЙ РЯД: Центрированные клавиши Prev - Play/Pause - Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    HapticHelper.performClick(view)
                    localPositionSec = 0
                    if (IslandController.activeMediaController != null) {
                        IslandController.skipToPreviousMedia(context)
                    } else {
                        val prevTrack = getPreviousDemoTrack(track)
                        onTrackChange(prevTrack)
                    }
                    onPrev()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Предыдущий",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(track.albumAccentColor)
                    .clickable {
                        HapticHelper.performClick(view)
                        val newPlay = !isPlaying
                        onTrackChange(track.copy(isPlaying = newPlay, currentPositionSec = localPositionSec))
                        IslandController.playPauseMedia(context)
                        onPlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            IconButton(
                onClick = {
                    HapticHelper.performClick(view)
                    localPositionSec = 0
                    if (IslandController.activeMediaController != null) {
                        IslandController.skipToNextMedia(context)
                    } else {
                        val nextTrack = getNextDemoTrack(track)
                        onTrackChange(nextTrack)
                    }
                    onNext()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Следующий",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun getNextDemoTrack(curr: MusicTrack): MusicTrack {
    return when (curr.title) {
        "Starboy" -> MusicTrack(
            title = "Blinding Lights",
            artist = "The Weeknd",
            appName = "Яндекс Музыка",
            albumAccentColor = Color(0xFFFF9100),
            palette = MusicPalette(Color(0xFFFF9100), Color(0xFFFF1493), Color(0xFF2979FF)),
            durationSec = 200,
            currentPositionSec = 0,
            isPlaying = curr.isPlaying
        )
        "Blinding Lights" -> MusicTrack(
            title = "Save Your Tears",
            artist = "The Weeknd",
            appName = "YouTube Music",
            albumAccentColor = Color(0xFF00B0FF),
            palette = MusicPalette(Color(0xFF00B0FF), Color(0xFF7C4DFF), Color(0xFFFF4081)),
            durationSec = 215,
            currentPositionSec = 0,
            isPlaying = curr.isPlaying
        )
        "Save Your Tears" -> MusicTrack(
            title = "Levitating",
            artist = "Dua Lipa",
            appName = "Spotify",
            albumAccentColor = Color(0xFFFF007F),
            palette = MusicPalette(Color(0xFFFF007F), Color(0xFF7928CA), Color(0xFF00F5FF)),
            durationSec = 203,
            currentPositionSec = 0,
            isPlaying = curr.isPlaying
        )
        "Levitating" -> MusicTrack(
            title = "Minor",
            artist = "Miyagi & Andy Panda",
            appName = "VK Музыка",
            albumAccentColor = Color(0xFFFFB300),
            palette = MusicPalette(Color(0xFFFFB300), Color(0xFFD84315), Color(0xFF00897B)),
            durationSec = 175,
            currentPositionSec = 0,
            isPlaying = curr.isPlaying
        )
        else -> MusicTrack(
            title = "Starboy",
            artist = "The Weeknd",
            appName = "Spotify",
            albumAccentColor = Color(0xFFE50914),
            palette = MusicPalette(Color(0xFFE50914), Color(0xFF6200EA), Color(0xFF00E5FF)),
            durationSec = 230,
            currentPositionSec = 0,
            isPlaying = curr.isPlaying
        )
    }
}

private fun getPreviousDemoTrack(curr: MusicTrack): MusicTrack {
    return when (curr.title) {
        "Blinding Lights" -> MusicTrack(
            title = "Starboy",
            artist = "The Weeknd",
            appName = "Spotify",
            albumAccentColor = Color(0xFFE50914),
            palette = MusicPalette(Color(0xFFE50914), Color(0xFF6200EA), Color(0xFF00E5FF)),
            durationSec = 230,
            currentPositionSec = 0,
            isPlaying = curr.isPlaying
        )
        "Save Your Tears" -> MusicTrack(
            title = "Blinding Lights",
            artist = "The Weeknd",
            appName = "Яндекс Музыка",
            albumAccentColor = Color(0xFFFF9100),
            palette = MusicPalette(Color(0xFFFF9100), Color(0xFFFF1493), Color(0xFF2979FF)),
            durationSec = 200,
            currentPositionSec = 0,
            isPlaying = curr.isPlaying
        )
        "Levitating" -> MusicTrack(
            title = "Save Your Tears",
            artist = "The Weeknd",
            appName = "YouTube Music",
            albumAccentColor = Color(0xFF00B0FF),
            palette = MusicPalette(Color(0xFF00B0FF), Color(0xFF7C4DFF), Color(0xFFFF4081)),
            durationSec = 215,
            currentPositionSec = 0,
            isPlaying = curr.isPlaying
        )
        "Minor" -> MusicTrack(
            title = "Levitating",
            artist = "Dua Lipa",
            appName = "Spotify",
            albumAccentColor = Color(0xFFFF007F),
            palette = MusicPalette(Color(0xFFFF007F), Color(0xFF7928CA), Color(0xFF00F5FF)),
            durationSec = 203,
            currentPositionSec = 0,
            isPlaying = curr.isPlaying
        )
        else -> MusicTrack(
            title = "Minor",
            artist = "Miyagi & Andy Panda",
            appName = "VK Музыка",
            albumAccentColor = Color(0xFFFFB300),
            palette = MusicPalette(Color(0xFFFFB300), Color(0xFFD84315), Color(0xFF00897B)),
            durationSec = 175,
            currentPositionSec = 0,
            isPlaying = curr.isPlaying
        )
    }
}

@Composable
private fun ChargingContent(
    state: IslandState.Charging,
    cameraSize: Int,
    onIslandClick: () -> Unit = {}
) {
    val view = LocalView.current
    val infiniteTransition = rememberInfiniteTransition(label = "chargingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    HapticHelper.performClick(view)
                    onIslandClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Зарядка",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (state.realWatts != null) "${state.realWatts}W" else state.sourceName,
                    color = AccentCyan,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width((cameraSize + 10).dp))

        Text(
            text = "${state.percentage}%",
            color = AccentCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChargingExpandedContent(
    state: IslandState.ChargingExpanded,
    cameraSize: Int,
    onClose: () -> Unit
) {
    val topPadding = (cameraSize + 6).dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .padding(top = topPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Батарея и питание",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.sourceName,
                        color = AccentCyan,
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.percentage}%",
                    color = AccentCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BatteryPixelChip(
                label = "Напряжение",
                value = String.format("%.2f В", state.voltageMv / 1000f),
                modifier = Modifier.weight(1f)
            )
            BatteryPixelChip(
                label = "Температура",
                value = String.format("%.1f °C", state.temperatureC),
                modifier = Modifier.weight(1f)
            )
            BatteryPixelChip(
                label = if (state.realWatts != null) "Мощность" else "Ток",
                value = if (state.realWatts != null) "${state.realWatts} Вт"
                else if (state.currentMa != null) "${state.currentMa} мА"
                else "Штатный",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BatteryPixelChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF16181F))
            .border(0.5.dp, Color(0x1FFFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = TextSecondary, fontSize = 9.5.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SoundProfileContent(
    state: IslandState.SoundProfile,
    cameraSize: Int,
    onIslandClick: () -> Unit = {}
) {
    val view = LocalView.current
    val (icon, tint) = when (state.profile) {
        SoundProfileType.DND -> Pair(Icons.Default.DoNotDisturbOn, PastelLilac)
        SoundProfileType.VIBRATE -> Pair(Icons.Default.Vibration, PastelPeach)
        SoundProfileType.NORMAL -> Pair(Icons.Default.NotificationsActive, PastelMint)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    HapticHelper.performClick(view)
                    onIslandClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )

        Spacer(modifier = Modifier.width((cameraSize + 6).dp))

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = 0.55f),
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun TwsCompactContent(
    device: BluetoothHeadphoneDevice,
    cameraSize: Int,
    onIslandClick: () -> Unit = {}
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    HapticHelper.performClick(view)
                    onIslandClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FastPairEarbudGraphic(deviceName = device.name, isLeft = true, size = 20.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${device.leftBattery ?: 85}%",
                color = PastelSky,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width((cameraSize + 10).dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${device.rightBattery ?: 90}%",
                color = PastelSky,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            FastPairEarbudGraphic(deviceName = device.name, isLeft = false, size = 20.dp)
        }
    }
}

@Composable
private fun TwsExpandedContent(
    device: BluetoothHeadphoneDevice,
    cameraSize: Int,
    ancMode: AncMode,
    onClose: () -> Unit,
    onAncSelect: (AncMode) -> Unit
) {
    val topPadding = (cameraSize + 6).dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .padding(top = topPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Google Fast Pair • TWS",
                    color = PastelSky,
                    fontSize = 10.sp
                )
            }

            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF14151B))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FastPairEarbudGraphic(deviceName = device.name, isLeft = true, size = 26.dp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = "L ${device.leftBattery ?: 85}%", color = PastelSky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FastPairCaseGraphic(deviceName = device.name, isCharging = device.isCaseCharging, size = 30.dp)
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Кейс ${device.caseBattery ?: 100}%", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (device.isCaseCharging) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(11.dp))
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FastPairEarbudGraphic(deviceName = device.name, isLeft = false, size = 26.dp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = "R ${device.rightBattery ?: 90}%", color = PastelSky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF16181F))
                .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AncMode.entries.forEach { mode ->
                val isSelected = ancMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onAncSelect(mode) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.title,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun OverEarCompactContent(
    device: BluetoothHeadphoneDevice,
    cameraSize: Int,
    onIslandClick: () -> Unit = {}
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    HapticHelper.performClick(view)
                    onIslandClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FastPairOverEarGraphic(deviceName = device.name, size = 20.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = device.name,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width((cameraSize + 10).dp))

        Text(
            text = "${device.overallBattery ?: 92}%",
            color = PastelSky,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OverEarExpandedContent(
    device: BluetoothHeadphoneDevice,
    cameraSize: Int,
    ancMode: AncMode,
    onClose: () -> Unit,
    onAncSelect: (AncMode) -> Unit
) {
    val topPadding = (cameraSize + 6).dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .padding(top = topPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Google Fast Pair • Полноразмерные наушники",
                    color = PastelSky,
                    fontSize = 10.sp
                )
            }

            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF14151B))
                .padding(vertical = 10.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FastPairOverEarGraphic(deviceName = device.name, size = 32.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Общий заряд аккумулятора", color = TextSecondary, fontSize = 10.sp)
                    Text(text = "${device.overallBattery ?: 92}%", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF16181F))
                .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AncMode.entries.forEach { mode ->
                val isSelected = ancMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onAncSelect(mode) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.title,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
