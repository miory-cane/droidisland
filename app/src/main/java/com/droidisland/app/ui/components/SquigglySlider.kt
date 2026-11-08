package com.droidisland.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidisland.app.util.HapticHelper
import kotlin.math.PI
import kotlin.math.sin

/**
 * Фирменный ползунок прогресса с волнообразной синусоидой (Squiggly Waveform) в аутентичном стиле Google Pixel.
 *
 * Особенности и исправления:
 * 1. Исключено дерганье ползунка назад-вперед: разделены `onSeekChange` (для плавного UI-скруббинга)
 *    и `onSeekComplete` (для отправки единственной IPC-команды перемотки при отпускании).
 * 2. Тап подтверждается только при поднятии пальца (`onTap`), исключая ошибочные скачки при начале drag-жеста.
 * 3. Волна в самом начале (x = 0) плавно нарастает от нуля (исправлен срез/обрыв в начале).
 * 4. Перед скруббером волна мягко сходится в центр ползунка.
 * 5. Плавное затухание амплитуды в прямую линию при паузе.
 */
@Composable
fun PixelSquigglyWaveSlider(
    progress: Float, // 0.0f .. 1.0f
    isPlaying: Boolean,
    waveColor: Color,
    unplayedColor: Color = Color(0x33FFFFFF),
    waveHeight: Dp = 6.dp,
    strokeWidth: Dp = 3.2.dp,
    onSeekChange: (Float) -> Unit = {},
    onSeekComplete: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress.coerceIn(0f, 1f)) }
    var lastHapticThreshold by remember { mutableFloatStateOf(progress) }

    LaunchedEffect(progress) {
        if (!isDragging) {
            dragProgress = progress.coerceIn(0f, 1f)
        }
    }

    val currentDisplayProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    // Бегущая фаза волны: плавная синусоида со скоростью ~2400 мс
    val infiniteTransition = rememberInfiniteTransition(label = "squigglyPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnimation"
    )

    // Амплитуда: плавно гаснет в прямую линию при паузе
    val targetAmplitude = if (isPlaying) 1.0f else 0.0f
    val animatedAmplitudeMultiplier by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "amplitudeSmooth"
    )

    // Анимация масштабирования скруббера при касании/перетаскивании
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.35f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f),
        label = "thumbScale"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val w = size.width.toFloat()
                        if (w > 0f) {
                            HapticHelper.performClick(view)
                            val newProgress = (offset.x / w).coerceIn(0f, 1f)
                            if (!newProgress.isNaN()) {
                                dragProgress = newProgress
                                onSeekChange(newProgress)
                                onSeekComplete(newProgress)
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        if (w > 0f) {
                            isDragging = true
                            HapticHelper.performLightTick(view)
                            val newProgress = (offset.x / w).coerceIn(0f, 1f)
                            if (!newProgress.isNaN()) {
                                dragProgress = newProgress
                                lastHapticThreshold = newProgress
                                onSeekChange(newProgress)
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        HapticHelper.performClick(view)
                        if (!dragProgress.isNaN()) {
                            onSeekComplete(dragProgress)
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val w = size.width.toFloat()
                        if (w > 0f) {
                            val newProgress = (change.position.x / w).coerceIn(0f, 1f)
                            if (!newProgress.isNaN()) {
                                dragProgress = newProgress
                                // Тактильный щелчок каждые 5% сдвига
                                if (kotlin.math.abs(newProgress - lastHapticThreshold) > 0.05f) {
                                    HapticHelper.performLightTick(view)
                                    lastHapticThreshold = newProgress
                                }
                                onSeekChange(newProgress)
                            }
                        }
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        if (width <= 1f || height <= 1f || width.isNaN() || height.isNaN()) return@Canvas

        try {
            val safeWaveColor = if (waveColor != Color.Unspecified) waveColor else Color(0xFFE50914)
            val centerY = height / 2f
            val progressX = (width * currentDisplayProgress).coerceIn(0f, width)

            val strokePx = strokeWidth.toPx()
            val maxWaveAmpPx = (waveHeight.toPx() / 2f) * animatedAmplitudeMultiplier
            val wavelengthPx = 42.dp.toPx()
            val frequency = if (wavelengthPx > 0f) (2 * PI / wavelengthPx).toFloat() else 0.1f

            // 1. Несыгранная часть (подложка): элегантная прямая дорожка со скруглением
            if (progressX < width) {
                drawLine(
                    color = unplayedColor,
                    start = Offset(progressX, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }

            // 2. Сыгранная часть: волнообразная синусоида с плавным началом без среза
            if (progressX > 0f) {
                val squigglyPath = Path()
                val rampDistance = 24.dp.toPx().coerceAtMost(progressX * 0.45f).coerceAtLeast(1f)
                var x = 0f
                val step = 1.5f

                squigglyPath.moveTo(0f, centerY)

                while (x <= progressX) {
                    // Плавное нарастание амплитуды от 0 в начале (x = 0)
                    val startRamp = (x / rampDistance).coerceIn(0f, 1f)
                    val smoothStart = sin(startRamp * (PI.toFloat() / 2f)) // Ровно 0.0 в начале x=0!

                    // Плавное затухание амплитуды перед ползунком
                    val endRamp = ((progressX - x) / rampDistance).coerceIn(0f, 1f)
                    val smoothEnd = sin(endRamp * (PI.toFloat() / 2f))

                    val envelope = smoothStart * smoothEnd
                    val y = centerY + sin(x * frequency - phase) * (maxWaveAmpPx * envelope)
                    squigglyPath.lineTo(x, y)
                    x += step
                }

                // Завершаем линию ровно в центре положения скруббера
                squigglyPath.lineTo(progressX, centerY)

                drawPath(
                    path = squigglyPath,
                    color = safeWaveColor,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            // 3. Выразительный Scrubber Thumb в стиле Pixel (светящаяся точка с неоновым ореолом)
            val baseThumbRadius = (5.2.dp.toPx() * thumbScale).coerceAtLeast(1f)

            // Мягкий неоновый ореол вокруг скруббера
            drawCircle(
                color = safeWaveColor.copy(alpha = if (isDragging) 0.50f else 0.25f),
                radius = (baseThumbRadius * 2.2f).coerceAtLeast(1f),
                center = Offset(progressX, centerY)
            )

            // Внешнее белое кольцо скруббера
            drawCircle(
                color = Color.White,
                radius = baseThumbRadius,
                center = Offset(progressX, centerY)
            )

            // Внутреннее цветовое ядро скруббера
            drawCircle(
                color = safeWaveColor,
                radius = baseThumbRadius * 0.55f,
                center = Offset(progressX, centerY)
            )
        } catch (_: Throwable) {}
    }
}

/**
 * Перегрузка для обратной совместимости с существующим кодом
 */
@Composable
fun PixelSquigglyWaveSlider(
    progress: Float,
    isPlaying: Boolean,
    waveColor: Color,
    unplayedColor: Color = Color(0x33FFFFFF),
    waveHeight: Dp = 6.dp,
    strokeWidth: Dp = 3.2.dp,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    PixelSquigglyWaveSlider(
        progress = progress,
        isPlaying = isPlaying,
        waveColor = waveColor,
        unplayedColor = unplayedColor,
        waveHeight = waveHeight,
        strokeWidth = strokeWidth,
        onSeekChange = onSeek,
        onSeekComplete = onSeek,
        modifier = modifier
    )
}
