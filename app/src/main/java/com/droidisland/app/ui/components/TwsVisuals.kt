package com.droidisland.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidisland.app.model.BluetoothHeadphoneDevice
import com.droidisland.app.model.HeadphoneFormFactor

/**
 * Отрисовка фото/визуала Fast Pair для TWS-наушников (отдельно левый/правый)
 */
@Composable
fun FastPairEarbudGraphic(
    deviceName: String,
    isLeft: Boolean,
    size: Dp = 26.dp
) {
    val lower = deviceName.lowercase()
    val tintColor = when {
        lower.contains("pixel") -> Color(0xFFBCE3C5) // Mint / Sage
        lower.contains("galaxy") || lower.contains("buds 3") -> Color(0xFFA6ABB3) // Silver
        lower.contains("airpods") -> Color(0xFFF5F5F7) // Pure White
        else -> Color(0xFFE2E4E9)
    }

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        if (w <= 1f || h <= 1f) return@Canvas

        val baseRadius = (w * 0.40f).coerceAtLeast(1f)
        drawCircle(
            color = Color(0xFF1E2024),
            radius = baseRadius,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        val capRadius = (w * 0.32f).coerceAtLeast(1f)
        val capCenter = if (isLeft) Offset(w * 0.46f, h * 0.48f) else Offset(w * 0.54f, h * 0.48f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tintColor, tintColor.copy(alpha = 0.85f)),
                center = capCenter,
                radius = capRadius
            ),
            radius = capRadius,
            center = capCenter
        )

        drawCircle(
            color = Color(0xFF111111),
            radius = (w * 0.05f).coerceAtLeast(1f),
            center = Offset(capCenter.x + (if (isLeft) -w * 0.14f else w * 0.14f), capCenter.y)
        )
    }
}

/**
 * Отрисовка фото/визуала Fast Pair для футляра TWS
 */
@Composable
fun FastPairCaseGraphic(
    deviceName: String,
    isCharging: Boolean,
    size: Dp = 40.dp
) {
    val lower = deviceName.lowercase()
    val caseColor = if (lower.contains("galaxy") && lower.contains("graphite")) {
        Color(0xFF2B2D30)
    } else {
        Color(0xFFF7F7F8)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(caseColor)
            .border(1.dp, Color(0x33000000), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = size * 0.78f, height = 1.dp)
                .background(Color(0x28000000))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isCharging) Color(0xFF30D158) else Color(0xFF0A84FF))
        )
    }
}

/**
 * Отрисовка общего фото Fast Pair для полноразмерных наушников (Over-Ear)
 */
@Composable
fun FastPairOverEarGraphic(
    deviceName: String,
    size: Dp = 32.dp
) {
    val lower = deviceName.lowercase()
    val bodyColor = when {
        lower.contains("silver") || lower.contains("white") -> Color(0xFFE2E4E9)
        lower.contains("max") -> Color(0xFFD3D6DB)
        else -> Color(0xFF2E3035) // Deep Charcoal / Matte Black (Sony, Bose)
    }

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        if (w <= 1f || h <= 1f) return@Canvas

        // Оголовье (дужка наушников)
        drawArc(
            color = bodyColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.16f, h * 0.10f),
            size = Size(w * 0.68f, h * 0.60f),
            style = Stroke(width = w * 0.10f)
        )

        // Левая чаша (Ear cup)
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(w * 0.08f, h * 0.40f),
            size = Size(w * 0.22f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.11f, w * 0.11f)
        )

        // Правая чаша (Ear cup)
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(w * 0.70f, h * 0.40f),
            size = Size(w * 0.22f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.11f, w * 0.11f)
        )
    }
}
