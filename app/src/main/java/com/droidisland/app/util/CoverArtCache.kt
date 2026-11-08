package com.droidisland.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.droidisland.app.model.MusicPalette

/**
 * Высокопроизводительный потокобезопасный кэш обложек треков и вычисленных палитр.
 * Предотвращает мерцание, повторную загрузку и случайное исчезновение обложек
 * при системных обновлениях статуса воспроизведения (пауза, буферизация, перемотка).
 */
object CoverArtCache {

    private const val MAX_BITMAP_CACHE_SIZE = 32
    private const val MAX_PALETTE_CACHE_SIZE = 64

    private val bitmapCache = object : LruCache<String, Bitmap>(MAX_BITMAP_CACHE_SIZE) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    private val paletteCache = object : LruCache<String, MusicPalette>(MAX_PALETTE_CACHE_SIZE) {
        override fun sizeOf(key: String, value: MusicPalette): Int = 1
    }

    fun makeKey(title: String, artist: String): String {
        val cleanTitle = title.trim().lowercase()
        val cleanArtist = artist.trim().lowercase()
        return if (cleanTitle.isEmpty() && cleanArtist.isEmpty()) "unknown_track" else "$cleanTitle - $cleanArtist"
    }

    fun getBitmap(key: String): Bitmap? {
        synchronized(bitmapCache) {
            return bitmapCache.get(key)
        }
    }

    fun putBitmap(key: String, bitmap: Bitmap) {
        synchronized(bitmapCache) {
            bitmapCache.put(key, bitmap)
        }
    }

    fun getPalette(key: String): MusicPalette? {
        synchronized(paletteCache) {
            return paletteCache.get(key)
        }
    }

    fun putPalette(key: String, palette: MusicPalette) {
        synchronized(paletteCache) {
            paletteCache.put(key, palette)
        }
    }

    fun clear() {
        synchronized(bitmapCache) {
            bitmapCache.evictAll()
        }
        synchronized(paletteCache) {
            paletteCache.evictAll()
        }
    }

    fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap
            }
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
