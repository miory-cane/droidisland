package com.droidisland.app.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.droidisland.app.model.IslandState
import com.droidisland.app.model.MusicTrack
import com.droidisland.app.util.CoverArtCache
import com.droidisland.app.util.PaletteExtractor

/**
 * Системная служба прослушивания медиа-сессий и уведомлений.
 * Считывает название трека, исполнителя, источник, обложку альбома с надежным кэшированием
 * (исключает пропадание обложки) и точный расчет прогресса воспроизведения в реальном времени.
 */
class IslandNotificationListener : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeMediaController: MediaController? = null

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        onControllersChanged(controllers)
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            mainHandler.post { updateMediaInfo() }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            mainHandler.post { updateMediaInfo() }
        }
    }

    companion object {
        var isConnected: Boolean = false
            private set

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledListeners.contains(context.packageName)
        }

        fun openNotificationAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        try {
            val component = ComponentName(this, IslandNotificationListener::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            val controllers = mediaSessionManager?.getActiveSessions(component)
            onControllersChanged(controllers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        activeMediaController?.unregisterCallback(mediaCallback)
        activeMediaController = null
        IslandController.activeMediaController = null
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        extractArtworkFromNotification(sbn)
        checkMediaSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        checkMediaSessions()
    }

    private fun extractArtworkFromNotification(sbn: StatusBarNotification) {
        try {
            val notif = sbn.notification ?: return
            val extras = notif.extras
            val largeIcon = notif.getLargeIcon()
            val bmp = CoverArtCache.drawableToBitmap(largeIcon?.loadDrawable(this))
                ?: (extras?.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap)
                ?: (extras?.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap)

            if (bmp != null) {
                val notifTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
                val notifArtist = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

                if (!notifTitle.isNullOrBlank()) {
                    val notifKey = CoverArtCache.makeKey(notifTitle, notifArtist ?: "")
                    CoverArtCache.putBitmap(notifKey, bmp)
                    PaletteExtractor.extractPalette(bmp, notifTitle, notifArtist ?: "", sbn.packageName)
                }

                val controller = activeMediaController
                if (controller != null && controller.packageName == sbn.packageName) {
                    val metadata = controller.metadata
                    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                        ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)

                    val isMatch = notifTitle.isNullOrBlank() || title.isNullOrBlank() ||
                            notifTitle.equals(title, ignoreCase = true) ||
                            title.contains(notifTitle, ignoreCase = true) ||
                            notifTitle.contains(title, ignoreCase = true)

                    if (isMatch && !title.isNullOrBlank()) {
                        val key = CoverArtCache.makeKey(title, artist ?: "")
                        CoverArtCache.putBitmap(key, bmp)
                        PaletteExtractor.extractPalette(bmp, title, artist ?: "", sbn.packageName)

                        // Немедленно применяем точные цвета и обложку для текущего трека
                        mainHandler.post {
                            updateMediaInfo()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkMediaSessions() {
        try {
            val component = ComponentName(this, IslandNotificationListener::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(component)
            onControllersChanged(controllers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onControllersChanged(controllers: List<MediaController>?) {
        val playingController = controllers?.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers?.firstOrNull()

        if (playingController != activeMediaController) {
            activeMediaController?.unregisterCallback(mediaCallback)
            activeMediaController = playingController
            IslandController.activeMediaController = playingController
            playingController?.registerCallback(mediaCallback, mainHandler)
        }

        updateMediaInfo()
    }

    private fun updateMediaInfo() {
        val controller = activeMediaController ?: return
        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Сейчас играет"

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: "Аудиоплеер"

        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val durationSec = if (durationMs > 0) (durationMs / 1000).toInt() else 240

        // ТОЧНЫЙ РАСЧЕТ ПОЗИЦИИ В РЕАЛЬНОМ ВРЕМЕНИ (исключает откат назад при событиях)
        val rawPositionMs = playbackState?.position ?: 0L
        val updateTime = playbackState?.lastPositionUpdateTime ?: 0L
        val speed = playbackState?.playbackSpeed ?: 1.0f
        val currentRealtime = SystemClock.elapsedRealtime()

        val effectivePositionMs = if (isPlaying && updateTime > 0L && speed > 0f) {
            val timeDiff = (currentRealtime - updateTime).coerceAtLeast(0L)
            rawPositionMs + (timeDiff * speed).toLong()
        } else {
            rawPositionMs
        }
        val positionSec = (effectivePositionMs / 1000L).toInt().coerceIn(0, durationSec)

        val pkgName = controller.packageName
        IslandController.lastActiveMusicPackage = pkgName

        val appName = when {
            pkgName.contains("spotify", ignoreCase = true) -> "Spotify"
            pkgName.contains("yandex", ignoreCase = true) -> "Яндекс Музыка"
            pkgName.contains("youtube", ignoreCase = true) -> "YouTube Music"
            pkgName.contains("vk", ignoreCase = true) -> "VK Музыка"
            pkgName.contains("apple", ignoreCase = true) -> "Apple Music"
            else -> {
                try {
                    val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    "Музыка"
                }
            }
        }

        // БЕЗОПАСНОЕ ИЗВЛЕЧЕНИЕ И КЭШИРОВАНИЕ ОБЛОЖКИ (НИКОГДА НЕ ОБНУЛЯЕТСЯ ПРИ СМЕНЕ СТЕЙТА!)
        val trackKey = CoverArtCache.makeKey(title, artist)
        var albumArtBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        if (albumArtBitmap == null) {
            albumArtBitmap = CoverArtCache.getBitmap(trackKey)
        }

        if (albumArtBitmap == null) {
            val curr = IslandController.currentTrack
            if (curr.title.equals(title, ignoreCase = true) &&
                curr.artist.equals(artist, ignoreCase = true) &&
                curr.albumArtBitmap != null
            ) {
                albumArtBitmap = curr.albumArtBitmap
            }
        }

        if (albumArtBitmap == null) {
            albumArtBitmap = findArtworkFromActiveNotifications(pkgName, title)
        }

        if (albumArtBitmap != null) {
            CoverArtCache.putBitmap(trackKey, albumArtBitmap)
        }

        // Извлекаем 3 цвета палитры с мгновенным кэшированием
        val palette = PaletteExtractor.extractPalette(albumArtBitmap, title, artist, appName)

        // Проверяем, изменились ли значимые данные трека
        val curr = IslandController.currentTrack
        val currState = IslandController.state.value
        val isAlreadyShowing = currState is IslandState.MusicCompact || currState is IslandState.MusicExpanded

        // Если сменился трек или снова включили воспроизведение — сбрасываем флаг скрытия пользователем
        if (curr.title != title || curr.artist != artist || (!curr.isPlaying && isPlaying)) {
            IslandController.isMusicDismissedByUser = false
        }

        val isSameTrack = curr.title == title && curr.artist == artist
        val isSamePlayState = curr.isPlaying == isPlaying
        val isSameCover = (curr.albumArtBitmap === albumArtBitmap)
        val isSamePalette = (curr.palette == palette)
        val posDelta = kotlin.math.abs(curr.currentPositionSec - positionSec)

        // Пропускаем повторное обновление ТОЛЬКО если островок УЖЕ отображается
        // (если островок в Idle или сменились палитра/обложка, мы ОБЯЗАНЫ его обновить!)
        if (isAlreadyShowing && isSameTrack && isSamePlayState && isSameCover && isSamePalette && posDelta < 2 && durationSec == curr.durationSec) {
            return
        }

        val track = MusicTrack(
            title = title,
            artist = artist,
            appName = appName,
            albumAccentColor = palette.color1,
            palette = palette,
            durationSec = durationSec,
            currentPositionSec = positionSec,
            isPlaying = isPlaying,
            albumArtBitmap = albumArtBitmap
        )

        IslandController.updateTrack(track)

        if (isPlaying && !IslandController.isMusicDismissedByUser) {
            if (!isAlreadyShowing) {
                IslandController.showState(IslandState.MusicCompact(track))
            }
        } else if (!isPlaying && (playbackState?.state == PlaybackState.STATE_STOPPED || playbackState?.state == PlaybackState.STATE_NONE)) {
            if (currState is IslandState.MusicCompact) {
                IslandController.showState(IslandState.Idle)
            }
        }
    }

    private fun findArtworkFromActiveNotifications(pkgName: String, expectedTitle: String): Bitmap? {
        try {
            val notifications = activeNotifications ?: return null
            val pkgNotifications = notifications.filter { it.packageName == pkgName }
            if (pkgNotifications.isEmpty()) return null

            // Ищем уведомление, строго соответствующее ТЕКУЩЕМУ треку (исключаем обложку прошлого трека!)
            val match = if (expectedTitle.isNotBlank() && expectedTitle != "Сейчас играет") {
                pkgNotifications.firstOrNull { sbn ->
                    val notifTitle = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
                    notifTitle != null && (
                        notifTitle.equals(expectedTitle, ignoreCase = true) ||
                        notifTitle.contains(expectedTitle, ignoreCase = true) ||
                        expectedTitle.contains(notifTitle, ignoreCase = true)
                    )
                }
            } else {
                pkgNotifications.firstOrNull()
            } ?: return null

            val notif = match.notification ?: return null
            val extras = notif.extras

            val largeIcon = notif.getLargeIcon()
            val bmpFromIcon = CoverArtCache.drawableToBitmap(largeIcon?.loadDrawable(this))
            if (bmpFromIcon != null) return bmpFromIcon

            val bmpExtra = (extras?.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap)
                ?: (extras?.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap)
            if (bmpExtra != null) return bmpExtra
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
