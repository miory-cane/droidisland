package com.droidisland.app.model

enum class HeadphoneFormFactor(val title: String) {
    TWS("Внутриканальные TWS (L + R + Кейс)"),
    OVER_EAR("Полноразмерные (Over-Ear)")
}

data class FastPairPhotos(
    // Для TWS: 3 отдельных фото
    val leftImageUrl: String? = null,
    val rightImageUrl: String? = null,
    val caseImageUrl: String? = null,

    // Для полноразмерных наушников: 1 общее фото
    val overEarImageUrl: String? = null
)

data class BluetoothHeadphoneDevice(
    val macAddress: String,
    val name: String,
    val isConnected: Boolean = true,
    val formFactor: HeadphoneFormFactor = HeadphoneFormFactor.TWS,
    val photos: FastPairPhotos,
    val leftBattery: Int? = null,
    val rightBattery: Int? = null,
    val caseBattery: Int? = null,
    val overallBattery: Int? = null,
    val isCaseCharging: Boolean = false
)
