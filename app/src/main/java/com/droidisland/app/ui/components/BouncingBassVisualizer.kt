package com.droidisland.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Подпрыгивающая волна-эквалайзер (басовый визуализатор) в стиле Google Pixel Audio.
 * 4 выразительных столбика с индивидуальными частотами и мягкими закруглениями.
 */
@Composable
fun BouncingBassVisualizer(
    isPlaying: Boolean,
    tintColor: Color = Color(0xFF30D158),
    maxHeight: Dp = 18.dp,
    barWidth: Dp = 3.dp,
    spacing: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "pixelBassVisualizer")

    // 4 столбика эквалайзера с реалистичными гармониками Pixel
    val bar1 by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(410, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(290, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3 by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val bar4 by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.80f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    val heights = if (isPlaying) listOf(bar1, bar2, bar3, bar4) else listOf(0.2f, 0.2f, 0.2f, 0.2f)

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { fraction ->
            val animatedH = maxHeight * fraction
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(animatedH.coerceAtLeast(3.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                tintColor.copy(alpha = 0.8f),
                                tintColor
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
