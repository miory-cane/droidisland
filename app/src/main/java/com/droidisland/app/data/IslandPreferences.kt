package com.droidisland.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "island_settings")

data class IslandConfig(
    val isOnboardingCompleted: Boolean = false,
    val cameraXOffset: Int = 0,         // Смещение от центра по горизонтали (в dp)
    val cameraYOffset: Int = 18,        // Отступ от верхнего края экрана (в dp)
    val cameraSize: Int = 32,           // Диаметр глазка камеры в покое (в dp)
    val expandedWidth: Int = 366,       // Базовая ширина развернутой шторки (в dp)
    val expandedHeight: Int = 136,      // Базовая высота развернутой шторки (в dp)
    val isCalibrationGuideActive: Boolean = false, // Режим калибровки с прозрачным центром

    // Триггеры
    val chargingEnabled: Boolean = true,
    val chargingTimeoutSec: Float = 3.0f,
    val soundProfileEnabled: Boolean = true,
    val soundProfileTimeoutSec: Float = 2.0f,

    // TWS наушники
    val twsEnabled: Boolean = true,
    val selectedTwsModelId: String = "pixel_buds_pro_2",
    val selectedTwsColorName: String = "Wintergreen",

    // Музыка и параметры развёрнутой шторки
    val musicEnabled: Boolean = true,
    val expandedIslandOpacity: Float = 0.85f,     // Прозрачность острова снизу (по умолчанию 85%)
    val expandedHeightMultiplier: Float = 1.0f   // Масштаб увеличения шторки при нажатии (0.85f .. 1.25f)
)

class IslandPreferences(private val context: Context) {

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_CAMERA_X_OFFSET = intPreferencesKey("camera_x_offset")
        val KEY_CAMERA_Y_OFFSET = intPreferencesKey("camera_y_offset")
        val KEY_CAMERA_SIZE = intPreferencesKey("camera_size")
        val KEY_EXPANDED_WIDTH = intPreferencesKey("expanded_width")
        val KEY_EXPANDED_HEIGHT = intPreferencesKey("expanded_height")
        val KEY_CALIBRATION_GUIDE_ACTIVE = booleanPreferencesKey("calibration_guide_active")

        val KEY_CHARGING_ENABLED = booleanPreferencesKey("charging_enabled")
        val KEY_CHARGING_TIMEOUT = floatPreferencesKey("charging_timeout")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_SOUND_TIMEOUT = floatPreferencesKey("sound_timeout")

        val KEY_TWS_ENABLED = booleanPreferencesKey("tws_enabled")
        val KEY_TWS_MODEL_ID = stringPreferencesKey("tws_model_id")
        val KEY_TWS_COLOR_NAME = stringPreferencesKey("tws_color_name")

        val KEY_MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val KEY_EXPANDED_OPACITY = floatPreferencesKey("expanded_opacity")
        val KEY_EXPANDED_SCALE = floatPreferencesKey("expanded_scale")
    }

    val configFlow: Flow<IslandConfig> = context.dataStore.data.map { preferences ->
        IslandConfig(
            isOnboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED] ?: false,
            cameraXOffset = preferences[KEY_CAMERA_X_OFFSET] ?: 0,
            cameraYOffset = preferences[KEY_CAMERA_Y_OFFSET] ?: 18,
            cameraSize = preferences[KEY_CAMERA_SIZE] ?: 32,
            expandedWidth = preferences[KEY_EXPANDED_WIDTH] ?: 366,
            expandedHeight = preferences[KEY_EXPANDED_HEIGHT] ?: 136,
            isCalibrationGuideActive = preferences[KEY_CALIBRATION_GUIDE_ACTIVE] ?: false,

            chargingEnabled = preferences[KEY_CHARGING_ENABLED] ?: true,
            chargingTimeoutSec = preferences[KEY_CHARGING_TIMEOUT] ?: 3.0f,
            soundProfileEnabled = preferences[KEY_SOUND_ENABLED] ?: true,
            soundProfileTimeoutSec = preferences[KEY_SOUND_TIMEOUT] ?: 2.0f,

            twsEnabled = preferences[KEY_TWS_ENABLED] ?: true,
            selectedTwsModelId = preferences[KEY_TWS_MODEL_ID] ?: "pixel_buds_pro_2",
            selectedTwsColorName = preferences[KEY_TWS_COLOR_NAME] ?: "Wintergreen",

            musicEnabled = preferences[KEY_MUSIC_ENABLED] ?: true,
            expandedIslandOpacity = preferences[KEY_EXPANDED_OPACITY] ?: 0.85f,
            expandedHeightMultiplier = preferences[KEY_EXPANDED_SCALE] ?: 1.0f
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun saveCalibration(
        xOffset: Int,
        yOffset: Int,
        cameraSize: Int,
        expandedWidth: Int = 366,
        expandedHeight: Int = 136
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CAMERA_X_OFFSET] = xOffset
            preferences[KEY_CAMERA_Y_OFFSET] = yOffset
            preferences[KEY_CAMERA_SIZE] = cameraSize
            preferences[KEY_EXPANDED_WIDTH] = expandedWidth
            preferences[KEY_EXPANDED_HEIGHT] = expandedHeight
        }
    }

    suspend fun setCalibrationGuideActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CALIBRATION_GUIDE_ACTIVE] = active
        }
    }

    suspend fun setChargingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CHARGING_ENABLED] = enabled }
    }

    suspend fun setChargingTimeout(seconds: Float) {
        context.dataStore.edit { it[KEY_CHARGING_TIMEOUT] = seconds }
    }

    suspend fun setSoundProfileEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setSoundProfileTimeout(seconds: Float) {
        context.dataStore.edit { it[KEY_SOUND_TIMEOUT] = seconds }
    }

    suspend fun setTwsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_TWS_ENABLED] = enabled }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MUSIC_ENABLED] = enabled }
    }

    suspend fun setExpandedAppearance(opacity: Float, scale: Float) {
        context.dataStore.edit {
            it[KEY_EXPANDED_OPACITY] = opacity
            it[KEY_EXPANDED_SCALE] = scale
        }
    }

    suspend fun setExpandedDimensions(width: Int, height: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EXPANDED_WIDTH] = width
            preferences[KEY_EXPANDED_HEIGHT] = height
        }
    }
}
