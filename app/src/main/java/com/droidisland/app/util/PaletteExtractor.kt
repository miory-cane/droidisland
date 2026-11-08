package com.droidisland.app.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.droidisland.app.model.MusicPalette
import kotlin.math.abs

object PaletteExtractor {

    /**
     * Нормализует яркость и насыщенность цвета для неонового свечения и шейдеров:
     * - Исключает слишком темные или грязные оттенки.
     * - Для монохромных изображений сохраняет чистый платиновый / серебристый тон.
     */
    fun normalizeColor(color: Color): Color {
        if (color == Color.Unspecified || color == Color.Transparent) {
            return Color(0xFF00E5FF)
        }
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        val h = hsl[0]
        var s = hsl[1]
        var l = hsl[2]

        if (s < 0.12f) {
            // Монохромные / Ч/Б обложки: благородный платиновый оттенок
            l = l.coerceIn(0.55f, 0.88f)
            return Color(ColorUtils.HSLToColor(floatArrayOf(215f, 0.05f, l)))
        }

        // Цветные обложки: сочная насыщенность и люминесцентная яркость
        s = s.coerceIn(0.60f, 0.95f)
        l = l.coerceIn(0.42f, 0.65f)
        return Color(ColorUtils.HSLToColor(floatArrayOf(h, s, l)))
    }

    /**
     * Извлекает ТОЧНУЮ палитру из 3 цветов непосредственно из обложки трека:
     * 1. Доминантный цвет (первичный)
     * 2. Гармоничный вспомогательный цвет (вторичный)
     * 3. Контрастный акцент (третичный)
     *
     * Поддерживает любые типы обложек:
     * - Черно-белые / монохромные обложки получают благородную платиново-серебристую,
     *   титановую и обсидиановую гамму БЕЗ посторонних желтых, красных и фиолетовых оттенков.
     * - Цветные обложки получают точные цвета, присутствующие на изображении.
     */
    fun extractPalette(
        bitmap: Bitmap?,
        title: String,
        artist: String,
        appName: String
    ): MusicPalette {
        val cacheKey = CoverArtCache.makeKey(title, artist)

        // Если передан реальный Bitmap — ВСЕГДА извлекаем цвета напрямую из его пикселей!
        if (bitmap != null) {
            try {
                val extracted = extractFromBitmap(bitmap)
                if (extracted != null) {
                    CoverArtCache.putPalette(cacheKey, extracted)
                    return extracted
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Если битмап пока отсутствует, проверяем кэш
        val cached = CoverArtCache.getPalette(cacheKey)
        if (cached != null) {
            return cached
        }

        return getKnownOrAlgorithmicPalette(title, artist, appName)
    }

    private fun extractFromBitmap(bitmap: Bitmap): MusicPalette? {
        if (bitmap.isRecycled) return null
        val sampleSize = 20
        val safeBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            bitmap.config == Bitmap.Config.HARDWARE
        ) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        } else {
            bitmap
        }
        val scaled = Bitmap.createScaledBitmap(safeBitmap, sampleSize, sampleSize, false)
        val hsl = FloatArray(3)

        data class PixelSample(val color: Int, val hue: Float, val sat: Float, val light: Float)

        val pixels = mutableListOf<PixelSample>()
        var totalSat = 0f
        var maxSat = 0f

        for (x in 0 until sampleSize) {
            for (y in 0 until sampleSize) {
                val pixel = scaled.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 120) continue

                ColorUtils.colorToHSL(pixel, hsl)
                val h = hsl[0]
                val s = hsl[1]
                val l = hsl[2]

                pixels.add(PixelSample(pixel, h, s, l))
                totalSat += s
                if (s > maxSat) maxSat = s
            }
        }

        if (pixels.isEmpty()) return null

        val avgSat = totalSat / pixels.size

        // 1. АБСОЛЮТНО ТОЧНАЯ ОБРАБОТКА ЧЕРНО-БЕЛЫХ И МОНОХРОМНЫХ ОБЛОЖЕК:
        // Если обложка черно-белая (средняя насыщенность < 0.12 или максимальная < 0.20)
        // — формируем строгую монохромную палитру из реальных градаций яркости обложки
        // (никаких желтых, красных или фиолетовых сервисных цветов!).
        if (maxSat < 0.20f || avgSat < 0.12f) {
            val sortedByLight = pixels.sortedBy { it.light }

            // 1) Светлый платиновый/белый акцент (для волны, текста и свечения)
            val bright = sortedByLight.lastOrNull { it.light in 0.70f..0.98f }
                ?: sortedByLight.last()
            val c1 = Color(ColorUtils.HSLToColor(floatArrayOf(0f, 0f, bright.light.coerceIn(0.85f, 0.98f))))

            // 2) Средний холодный титаново-серый полутон (середина градиента)
            val mid = sortedByLight.firstOrNull { it.light in 0.45f..0.65f }
                ?: sortedByLight[sortedByLight.size / 2]
            val c2 = Color(ColorUtils.HSLToColor(floatArrayOf(215f, 0.05f, mid.light.coerceIn(0.46f, 0.60f))))

            // 3) Глубокий темно-графитовый/обсидиановый контраст
            val dark = sortedByLight.firstOrNull { it.light in 0.18f..0.35f }
                ?: sortedByLight.first()
            val c3 = Color(ColorUtils.HSLToColor(floatArrayOf(220f, 0.06f, dark.light.coerceIn(0.20f, 0.32f))))

            return MusicPalette(color1 = c1, color2 = c2, color3 = c3)
        }

        // 2. ЦВЕТНЫЕ ОБЛОЖКИ: Извлекаем именно те цвета, которые ЕСТЬ на картинке
        val saturatedPixels = pixels.filter { it.sat >= 0.14f && it.light in 0.12f..0.88f }
        if (saturatedPixels.isEmpty()) {
            return MusicPalette(
                color1 = Color(0xFFF2F4F8),
                color2 = Color(0xFF7E828E),
                color3 = Color(0xFF2A2B32)
            )
        }

        data class ScoredCandidate(val color: Int, val hue: Float, val sat: Float, val light: Float, val score: Float)

        val candidates = saturatedPixels.map {
            // Оценка выразительности: предпочтение чистым и гармоничным тонам средней яркости
            val score = it.sat * 2.2f + (1.0f - abs(it.light - 0.50f)) * 1.4f
            ScoredCandidate(it.color, it.hue, it.sat, it.light, score)
        }

        // 1) Доминантный цвет — самый насыщенный и заметный в обложке
        val primary = candidates.maxByOrNull { it.score } ?: candidates.first()
        val c1 = Color(primary.color)

        // 2) Вторичный цвет — ищем пиксель с другим оттенком (дистанция по цветовому кругу > 25 градусов)
        val secondary = candidates.filter {
            val diff = abs(it.hue - primary.hue)
            val hueDistance = if (diff > 180f) 360f - diff else diff
            hueDistance in 25f..160f
        }.maxByOrNull { it.score }

        val c2 = if (secondary != null) {
            Color(secondary.color)
        } else {
            // Если обложка одноцветная (например, вся синяя), берем более глубокий оттенок ТОГО ЖЕ цвета
            val newLight = if (primary.light > 0.50f) (primary.light - 0.22f).coerceAtLeast(0.26f)
                           else (primary.light + 0.22f).coerceAtMost(0.76f)
            Color(ColorUtils.HSLToColor(floatArrayOf(primary.hue, primary.sat, newLight)))
        }

        // 3) Третичный цвет — еще один оттенок из обложки
        val tertiary = candidates.filter {
            val diff1 = abs(it.hue - primary.hue)
            val dist1 = if (diff1 > 180f) 360f - diff1 else diff1
            val diff2 = secondary?.let { s ->
                val d = abs(it.hue - s.hue)
                if (d > 180f) 360f - d else d
            } ?: 999f
            dist1 > 32f && diff2 > 22f
        }.maxByOrNull { it.score }

        val c3 = if (tertiary != null) {
            Color(tertiary.color)
        } else {
            // Тональный акцент в гармоничной температуре обложки
            val targetHue = (primary.hue + 20f) % 360f
            val targetLight = (primary.light * 0.72f).coerceIn(0.22f, 0.44f)
            Color(ColorUtils.HSLToColor(floatArrayOf(targetHue, primary.sat.coerceAtLeast(0.55f), targetLight)))
        }

        return MusicPalette(color1 = c1, color2 = c2, color3 = c3)
    }

    fun getKnownOrAlgorithmicPalette(title: String, artist: String, appName: String): MusicPalette {
        val lowerTitle = title.lowercase()
        val lowerArtist = artist.lowercase()

        // Специальные палитры для популярных треков (только когда обложки нет вообще)
        if (lowerTitle.contains("starboy") || lowerArtist.contains("weeknd")) {
            return MusicPalette(
                color1 = Color(0xFFE50914), // Алый Crimson
                color2 = Color(0xFF6200EA), // Глубокий Индиго-Фиолет
                color3 = Color(0xFF00E5FF)  // Неоновый Циан
            )
        }
        if (lowerTitle.contains("blinding lights")) {
            return MusicPalette(
                color1 = Color(0xFFFF9100), // Закатный Неон
                color2 = Color(0xFFFF1493), // Маджента
                color3 = Color(0xFF2979FF)  // Электрик Блю
            )
        }
        if (lowerTitle.contains("save your tears")) {
            return MusicPalette(
                color1 = Color(0xFF00B0FF), // Лазурный
                color2 = Color(0xFF7C4DFF), // Электрик Фиолет
                color3 = Color(0xFFFF4081)  // Неоновый Коралл
            )
        }
        if (lowerTitle.contains("levitating") || lowerArtist.contains("dua lipa")) {
            return MusicPalette(
                color1 = Color(0xFFFF007F), // Кибер-Розовый
                color2 = Color(0xFF7928CA), // Неоновый Пурпур
                color3 = Color(0xFF00F5FF)  // Бирюза
            )
        }
        if (lowerTitle.contains("minor") || lowerArtist.contains("miyagi")) {
            return MusicPalette(
                color1 = Color(0xFFFFB300), // Теплый Янтарь
                color2 = Color(0xFFD84315), // Терракота
                color3 = Color(0xFF00897B)  // Изумрудный Опал
            )
        }

        // Фирменные палитры стриминговых сервисов (запасной вариант, если нет обложки)
        val lowerApp = appName.lowercase()
        if (lowerApp.contains("spotify")) {
            return MusicPalette(
                color1 = Color(0xFF1DB954), // Spotify Green
                color2 = Color(0xFF10897B), // Глубокий Океанический
                color3 = Color(0xFF14FFEC)  // Светящийся Мятный
            )
        }
        if (lowerApp.contains("yandex") || lowerApp.contains("яндекс")) {
            return MusicPalette(
                color1 = Color(0xFFFFCC00), // Яндекс Желтый
                color2 = Color(0xFFFF5722), // Неоновый Оранж
                color3 = Color(0xFF8E24AA)  // Пурпурный
            )
        }
        if (lowerApp.contains("youtube") || lowerApp.contains("yt")) {
            return MusicPalette(
                color1 = Color(0xFFFF0000), // YouTube Red
                color2 = Color(0xFF880E4F), // Рубиновый
                color3 = Color(0xFFFF5252)  // Коралловый
            )
        }
        if (lowerApp.contains("vk") || lowerApp.contains("вконтакте")) {
            return MusicPalette(
                color1 = Color(0xFF0077FF), // VK Blue
                color2 = Color(0xFF7B1FA2), // Фиолетовый
                color3 = Color(0xFF00E5FF)  // Неоновый Циан
            )
        }
        if (lowerApp.contains("apple")) {
            return MusicPalette(
                color1 = Color(0xFFFA243C), // Apple Music Red
                color2 = Color(0xFFFF5E3A), // Персиковый
                color3 = Color(0xFFD2006B)  // Фуксия
            )
        }

        // Алгоритмическая палитра на основе хэша названия
        val hash = abs("$title $artist".hashCode())
        val hue1 = (hash % 360).toFloat()
        val hue2 = (hue1 + 45f) % 360f
        val hue3 = (hue1 + 160f) % 360f

        return MusicPalette(
            color1 = Color(ColorUtils.HSLToColor(floatArrayOf(hue1, 0.85f, 0.52f))),
            color2 = Color(ColorUtils.HSLToColor(floatArrayOf(hue2, 0.80f, 0.50f))),
            color3 = Color(ColorUtils.HSLToColor(floatArrayOf(hue3, 0.85f, 0.58f)))
        )
    }
}
