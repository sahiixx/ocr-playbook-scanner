package com.sahiix.ocrplaybook.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sahiix.ocrplaybook.util.BitmapUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device OCR engine (ML Kit Text Recognition v2, Latin) — the app's
 * default OcrEngine. 100% offline — no key, no network, no billing.
 * Heavy work stays on Dispatchers.Default; callers are free to call from Main.
 */
@Singleton
class MlKitOcrEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bitmapUtils: BitmapUtils
) : OcrEngine {
    override val name: String = "mlkit-on-device"

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(src: Bitmap, rotationDegrees: Int = 0): OcrResult =
        withContext(Dispatchers.Default) {
            // Downscale copy; original bitmap untouched. Recycle the copy after.
            val scaled = bitmapUtils.scaleForRealtime(src, bitmapUtils.suggestedMaxEdge())
            val t0 = android.os.SystemClock.elapsedRealtime()
            try {
                val image = InputImage.fromBitmap(scaled, rotationDegrees)
                val visionText = recognizer.process(image).await()
                val blocks = visionText.textBlocks.map { b ->
                    OcrBlock(
                        text = b.text,
                        confidence = -1f, // ML Kit Text v2 exposes no block confidence
                        boundingBox = b.boundingBox,
                        languages = b.recognizedLanguage?.let { listOf(it) } ?: emptyList()
                    )
                }
                OcrResult(
                    fullText = visionText.text,
                    blocks = blocks,
                    rotationDegrees = rotationDegrees,
                    processingMs = android.os.SystemClock.elapsedRealtime() - t0
                )
            } finally {
                if (scaled !== src && !scaled.isRecycled) scaled.recycle()
            }
        }

    suspend fun recognizeUri(uri: Uri): OcrResult = withContext(Dispatchers.Default) {
        // Prefer streaming path (no full-size alloc): decode sampled, then recognize.
        val bmp = bitmapUtils.decodeSampled(uri, bitmapUtils.suggestedMaxEdge())
            ?: return@withContext OcrResult("", emptyList())
        try {
            recognize(bmp)
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }

    suspend fun recognizeFile(path: String): OcrResult = withContext(Dispatchers.Default) {
        val bmp = bitmapUtils.decodeSampled(path, bitmapUtils.suggestedMaxEdge())
            ?: return@withContext OcrResult("", emptyList())
        try {
            recognize(bmp)
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }

    override fun close() { try { recognizer.close() } catch (_: Exception) { } }
}
