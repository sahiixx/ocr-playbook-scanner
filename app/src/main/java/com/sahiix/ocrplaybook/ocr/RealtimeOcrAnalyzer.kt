package com.sahiix.ocrplaybook.ocr

import android.annotation.SuppressLint
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.sahiix.ocrplaybook.util.BitmapUtils
import com.sahiix.ocrplaybook.util.Constants.REALTIME_TARGET_WIDTH
import com.sahiix.ocrplaybook.util.Constants.REALTIME_THROTTLE_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Real-time CameraX analyzer: runs on-device OCR on preview frames and emits
 * the latest [OcrResult] as a StateFlow for the overlay UI.
 *
 * Memory rules:
 * - Throttled (default 1.2 s) so we never queue overlapping recognitions.
 * - YUV_420_8888 -> RGB bitmap is downscaled BEFORE recognition.
 * - ImageProxy is ALWAYS closed, even on failure (else CameraX stalls).
 */
class RealtimeOcrAnalyzer(
    private val engine: OcrEngine,
    private val bitmapUtils: BitmapUtils,
    private val throttleMs: Long = REALTIME_THROTTLE_MS
) : ImageAnalysis.Analyzer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastRun = 0L
    @Volatile private var busy = false

    private val _latest = MutableStateFlow<OcrResult?>(null)
    val latest: StateFlow<OcrResult?> = _latest.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeOptInUsageError", "NewApi")
    override fun analyze(image: ImageProxy) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (busy || now - lastRun < throttleMs) { image.close(); return }
        busy = true
        lastRun = now
        _isWorking.value = true
        try {
            val bmp = image.toBitmap() // API 29+ helper on ImageProxy (YUV->RGB)
            val rotation = image.imageInfo.rotationDegrees
            image.close() // release camera buffer ASAP; work continues on the copy
            scope.launch {
                try {
                    val small = bitmapUtils.scaleForRealtime(bmp, REALTIME_TARGET_WIDTH)
                    try {
                        _latest.value = engine.recognize(small, rotation)
                    } finally {
                        if (small !== bmp && !small.isRecycled) small.recycle()
                    }
                } catch (_: Exception) {
                } finally {
                    if (!bmp.isRecycled) bmp.recycle()
                    busy = false
                    _isWorking.value = false
                }
            }
        } catch (_: Exception) {
            try { image.close() } catch (_: Exception) { }
            busy = false
            _isWorking.value = false
        }
    }

    fun stop() { /* scope cancelled with screen; no-op for safety */ }
}
