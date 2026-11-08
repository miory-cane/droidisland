package com.droidisland.app.model

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

enum class SoundProfileType(val title: String) {
    NORMAL("Звук включен"),
    VIBRATE("Режим вибрации"),
    DND("Не беспокоить")
}

enum class AncMode(val title: String) {
    ANC("Шумоподавление"),
    TRANSPARENCY("Прозрачность"),
    OFF("Выключено")
}

data class MusicPalette(
    val color1: Color = Color(0xFFE50914), // Доминантный цвет
    val color2: Color = Color(0xFF6200EA), // Вторичный перелив
    val color3: Color = Color(0xFF00E5FF)  // Третичный контрастный акцент
)

data class MusicTrack(
    val title: String = "Starboy",
    val artist: String = "The Weeknd",
    val appName: String = "Spotify",
    val albumAccentColor: Color = Color(0xFFE50914),
    val palette: MusicPalette = MusicPalette(
        color1 = Color(0xFFE50914),
        color2 = Color(0xFF6200EA),
        color3 = Color(0xFF00E5FF)
    ),
    val durationSec: Int = 230,
    val currentPositionSec: Int = 84,
    val isPlaying: Boolean = true,
    val albumArtBitmap: Bitmap? = null,
    val isLiked: Boolean = false,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val outputDevice: String = "Pixel Buds Pro"
)

sealed interface IslandState {
    // В покое: скрыт ровно в объективе камеры
    data object Idle : IslandState

    // Модуль питания (Зарядка: Компактный вид)
    data class Charging(
        val percentage: Int = 82,
        val isCharging: Boolean = true,
        val sourceName: String = "Сетевое ЗУ",
        val voltageMv: Int = 4150,
        val temperatureC: Float = 28.5f,
        val realWatts: Float? = null,
        val currentMa: Int? = null
    ) : IslandState

    // Модуль питания (Зарядка: Развернутое подробное меню всей информации АКБ)
    data class ChargingExpanded(
        val percentage: Int = 82,
        val isCharging: Boolean = true,
        val sourceName: String = "Сетевое ЗУ",
        val voltageMv: Int = 4150,
        val temperatureC: Float = 28.5f,
        val realWatts: Float? = null,
        val currentMa: Int? = null
    ) : IslandState

    // Модуль звука
    data class SoundProfile(
        val profile: SoundProfileType = SoundProfileType.DND
    ) : IslandState

    // TWS Наушники (раздельные фото левого, правого наушника и кейса)
    data class TwsCompact(
        val device: BluetoothHeadphoneDevice
    ) : IslandState

    data class TwsExpanded(
        val device: BluetoothHeadphoneDevice,
        val ancMode: AncMode = AncMode.ANC
    ) : IslandState

    // Полноразмерные наушники Over-Ear (одно общее фото)
    data class OverEarCompact(
        val device: BluetoothHeadphoneDevice
    ) : IslandState

    data class OverEarExpanded(
        val device: BluetoothHeadphoneDevice,
        val ancMode: AncMode = AncMode.ANC
    ) : IslandState

    // Музыкальный плеер: Компактный режим (басовая волна слева + обложка справа)
    data class MusicCompact(
        val track: MusicTrack = MusicTrack()
    ) : IslandState

    // Музыкальный плеер: Развернутая шторка (Pixel Squiggly Wave, 3-цветный перелив, центрированное управление)
    data class MusicExpanded(
        val track: MusicTrack = MusicTrack()
    ) : IslandState
}
