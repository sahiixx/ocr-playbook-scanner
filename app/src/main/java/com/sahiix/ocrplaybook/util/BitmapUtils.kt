package com.sahiix.ocrplaybook.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.sahiix.ocrplaybook.util.Constants.MAX_IMAGE_EDGE
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Memory-safe bitmap loading.
 *
 * Camera frames and gallery photos can be 12–48 MP; decoding them at full
 * resolution will OOM a low-RAM device. Every decode path in this app goes
 * through here:
 *  1. `inJustDecodeBounds` pass to learn dimensions without allocating pixels.
 *  2. Power-of-two `inSampleSize` so the long edge <= MAX_IMAGE_EDGE.
 *  3. `RGB_565` for OCR (halves RAM vs ARGB_8888; text doesn't need alpha).
 */
@Singleton
class BitmapUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun decodeSampled(uri: Uri, maxEdge: Int = MAX_IMAGE_EDGE): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val (w, h) = bounds.outWidth to bounds.outHeight
        if (w <= 0 || h <= 0) return null
        var sample = 1
        while (max(w, h) / sample > maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return open(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    fun decodeSampled(path: String, maxEdge: Int = MAX_IMAGE_EDGE): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val (w, h) = bounds.outWidth to bounds.outHeight
        if (w <= 0 || h <= 0) return null
        var sample = 1
        while (max(w, h) / sample > maxEdge) sample *= 2
        return BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        )
    }

    /** Scale a live frame down for the realtime analyzer; never mutates input. */
    fun scaleForRealtime(src: Bitmap, targetWidth: Int): Bitmap {
        if (src.width <= targetWidth) return src
        val ratio = targetWidth.toFloat() / src.width
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, targetWidth, h, true)
    }

    fun isLowMemory(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return am.isLowRamDevice || mi.lowMemory
    }

    fun suggestedMaxEdge(): Int {
        // Tighter cap on low-RAM devices.
        if (isLowMemory()) return min(MAX_IMAGE_EDGE, 1280)
        return MAX_IMAGE_EDGE
    }

    private fun open(uri: Uri): InputStream? =
        try { context.contentResolver.openInputStream(uri) } catch (_: Exception) { null }
}
