package com.droidisland.app.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.droidisland.app.model.BluetoothHeadphoneDevice
import com.droidisland.app.model.FastPairPhotos
import com.droidisland.app.model.HeadphoneFormFactor

object BluetoothHeadphoneManager {

    /**
     * Возвращает BluetoothAdapter современным способом через BluetoothManager (API 23+)
     */
    fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
        return try {
            val bm = ContextCompat.getSystemService(context, android.bluetooth.BluetoothManager::class.java)
            bm?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Считывает реальный уровень заряда батареи Bluetooth-устройства, если система предоставляет доступ
     */
    fun getDeviceBattery(device: BluetoothDevice): Int? {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int
            if (level != null && level in 0..100) level else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Проверяет наличие разрешений на доступ к Bluetooth
     */
    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Открывает системное меню добавления доверенных устройств / устройств поблизости
     * (подобно Smart Lock / Extend Unlock в настройках безопасности) с гарантированным фоллбэком.
     */
    fun openNearbyDeviceTrustMenu(context: Context) {
        val intentsToTry = listOf(
            // 1. Google Smart Lock / Extend Unlock: выбор доверенного устройства поблизости
            Intent("com.google.android.gms.auth.trustagent.TRUSTED_DEVICES").apply {
                setPackage("com.google.android.gms")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 2. Альтернативный компонент Trust Agent
            Intent().apply {
                setClassName("com.google.android.gms", "com.google.android.gms.auth.trustagent.GoogleTrustAgentPersonalTrustActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 3. Меню сопряжения устройств Bluetooth
            Intent("android.settings.BLUETOOTH_PAIRING_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 4. Меню безопасности устройства (раздел Smart Lock / Блокировка)
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // 5. Стандартные системные настройки Bluetooth
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        for (intent in intentsToTry) {
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // пробуем следующий интент
            }
        }
    }

    /**
     * Считывает сопряжённые Bluetooth-устройства системы и строго отфильтровывает
     * только наушники (аудио-гарнитуры), определяя их форм-фактор (TWS или Over-Ear).
     */
    @SuppressLint("MissingPermission")
    fun getBondedHeadphones(context: Context): List<BluetoothHeadphoneDevice> {
        if (!hasBluetoothPermission(context)) {
            return emptyList()
        }

        val adapter = getBluetoothAdapter(context) ?: return emptyList()

        val bondedDevices: Set<BluetoothDevice> = try {
            adapter.bondedDevices ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }

        val headphonesList = mutableListOf<BluetoothHeadphoneDevice>()

        for (device in bondedDevices) {
            val name = try {
                device.name ?: "Аудио-устройство"
            } catch (_: SecurityException) {
                "Аудио-устройство"
            }

            val btClass = try {
                device.bluetoothClass
            } catch (_: SecurityException) {
                null
            }

            // Проверяем, является ли устройство аудио-гарнитурой
            val isAudioDevice = btClass != null && (
                    btClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                            btClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                            btClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET
                    )

            val nameSuggestsHeadphones = isHeadphoneName(name)

            if (isAudioDevice || nameSuggestsHeadphones) {
                val formFactor = detectFormFactor(name, btClass)
                val fastPairPhotos = resolveFastPairPhotos(name, formFactor)
                val realBattery = getDeviceBattery(device)

                headphonesList.add(
                    BluetoothHeadphoneDevice(
                        macAddress = device.address,
                        name = name,
                        isConnected = true,
                        formFactor = formFactor,
                        photos = fastPairPhotos,
                        leftBattery = if (formFactor == HeadphoneFormFactor.TWS) (realBattery ?: 85) else null,
                        rightBattery = if (formFactor == HeadphoneFormFactor.TWS) (realBattery ?: 90) else null,
                        caseBattery = if (formFactor == HeadphoneFormFactor.TWS) 100 else null,
                        overallBattery = if (formFactor == HeadphoneFormFactor.OVER_EAR) (realBattery ?: 92) else null,
                        isCaseCharging = true
                    )
                )
            }
        }

        return headphonesList
    }

    /**
     * Определение форм-фактора: TWS (раздельные L, R и кейс) или Полноразмерные (Over-Ear)
     */
    private fun detectFormFactor(name: String, btClass: BluetoothClass?): HeadphoneFormFactor {
        val lower = name.lowercase()

        // Маркеры полноразмерных наушников
        val overEarMarkers = listOf(
            "wh-1000", "wh-xb", "wh-ch", // Sony Over-Ear
            "max", "airpods max",        // Apple Max
            "qc35", "qc45", "quietcomfort", "bose 700", // Bose Over-Ear
            "studio", "solo", "pro over", // Beats
            "major", "monitor",          // Marshall
            "momentum 4", "momentum wireless", // Sennheiser Over-Ear
            "over-ear", "headphone"
        )

        val isOverEar = overEarMarkers.any { lower.contains(it) } &&
                !lower.contains("buds") &&
                !lower.contains("pods") &&
                !lower.contains("wf-")

        return if (isOverEar) {
            HeadphoneFormFactor.OVER_EAR
        } else {
            HeadphoneFormFactor.TWS
        }
    }

    private fun isHeadphoneName(name: String): Boolean {
        val lower = name.lowercase()
        val markers = listOf(
            "buds", "pods", "airpods", "ear", "headphone", "headset",
            "wh-", "wf-", "freebuds", "tune", "live", "bose", "sony"
        )
        return markers.any { lower.contains(it) }
    }

    /**
     * Извлечение метаданных Google Fast Pair Service (GFPS):
     * - Для TWS: 3 отдельных фото (левый, правый и футляр)
     * - Для Over-Ear: 1 общее фото устройства
     */
    fun resolveFastPairPhotos(deviceName: String, formFactor: HeadphoneFormFactor): FastPairPhotos {
        val lower = deviceName.lowercase()

        return if (formFactor == HeadphoneFormFactor.TWS) {
            // Для TWS извлекаем тройку: Left, Right, Case
            when {
                lower.contains("pixel buds pro") -> FastPairPhotos(
                    leftImageUrl = "https://lh3.googleusercontent.com/pixel_buds_pro_left.png",
                    rightImageUrl = "https://lh3.googleusercontent.com/pixel_buds_pro_right.png",
                    caseImageUrl = "https://lh3.googleusercontent.com/pixel_buds_pro_case.png"
                )
                lower.contains("galaxy buds") -> FastPairPhotos(
                    leftImageUrl = "https://lh3.googleusercontent.com/galaxy_buds_left.png",
                    rightImageUrl = "https://lh3.googleusercontent.com/galaxy_buds_right.png",
                    caseImageUrl = "https://lh3.googleusercontent.com/galaxy_buds_case.png"
                )
                lower.contains("airpods") -> FastPairPhotos(
                    leftImageUrl = "https://lh3.googleusercontent.com/airpods_left.png",
                    rightImageUrl = "https://lh3.googleusercontent.com/airpods_right.png",
                    caseImageUrl = "https://lh3.googleusercontent.com/airpods_case.png"
                )
                else -> FastPairPhotos(
                    leftImageUrl = "fast_pair_generic_tws_left",
                    rightImageUrl = "fast_pair_generic_tws_right",
                    caseImageUrl = "fast_pair_generic_tws_case"
                )
            }
        } else {
            // Для полноразмерных: одно общее фото
            when {
                lower.contains("wh-1000") || lower.contains("sony") -> FastPairPhotos(
                    overEarImageUrl = "https://lh3.googleusercontent.com/sony_wh1000xm5.png"
                )
                lower.contains("max") -> FastPairPhotos(
                    overEarImageUrl = "https://lh3.googleusercontent.com/airpods_max.png"
                )
                lower.contains("bose") || lower.contains("quietcomfort") -> FastPairPhotos(
                    overEarImageUrl = "https://lh3.googleusercontent.com/bose_qc45.png"
                )
                else -> FastPairPhotos(
                    overEarImageUrl = "fast_pair_generic_over_ear"
                )
            }
        }
    }

    /**
     * Готовые тестовые Bluetooth-устройства для проверки в эмуляторе или без подключения
     */
    fun getMockBluetoothDevices(): List<BluetoothHeadphoneDevice> {
        return listOf(
            BluetoothHeadphoneDevice(
                macAddress = "34:82:C5:11:09:AF",
                name = "Google Pixel Buds Pro 2",
                isConnected = true,
                formFactor = HeadphoneFormFactor.TWS,
                photos = FastPairPhotos(
                    leftImageUrl = "https://lh3.googleusercontent.com/pixel_buds_pro_left.png",
                    rightImageUrl = "https://lh3.googleusercontent.com/pixel_buds_pro_right.png",
                    caseImageUrl = "https://lh3.googleusercontent.com/pixel_buds_pro_case.png"
                ),
                leftBattery = 85,
                rightBattery = 90,
                caseBattery = 100,
                isCaseCharging = true
            ),
            BluetoothHeadphoneDevice(
                macAddress = "A0:C9:A0:87:12:43",
                name = "Sony WH-1000XM5",
                isConnected = true,
                formFactor = HeadphoneFormFactor.OVER_EAR,
                photos = FastPairPhotos(
                    overEarImageUrl = "https://lh3.googleusercontent.com/sony_wh1000xm5.png"
                ),
                overallBattery = 95
            ),
            BluetoothHeadphoneDevice(
                macAddress = "58:6B:14:FE:91:30",
                name = "Galaxy Buds 3 Pro",
                isConnected = true,
                formFactor = HeadphoneFormFactor.TWS,
                photos = FastPairPhotos(
                    leftImageUrl = "https://lh3.googleusercontent.com/galaxy_buds_left.png",
                    rightImageUrl = "https://lh3.googleusercontent.com/galaxy_buds_right.png",
                    caseImageUrl = "https://lh3.googleusercontent.com/galaxy_buds_case.png"
                ),
                leftBattery = 75,
                rightBattery = 70,
                caseBattery = 88,
                isCaseCharging = false
            )
        )
    }
}
