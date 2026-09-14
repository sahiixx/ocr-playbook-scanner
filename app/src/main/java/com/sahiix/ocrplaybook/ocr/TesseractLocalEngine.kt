package com.sahiix.ocrplaybook.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local OCR engine backed by Tesseract 3.05 via the tess-two JNI wrapper.
 *
 * True local open-source OCR — no cloud, no API key — but heavier than the
 * ML Kit engine: tess-two loads four native libraries into the app process
 * and additionally needs `eng.traineddata` on disk (see [ensureTrainedData]
 * for the two supported locations). All native calls are serialized on an
 * internal lock and run on Dispatchers.Default; callers are free to call
 * from Main.
 */
@Singleton
class TesseractLocalEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : OcrEngine {

    override val name: String = TesseractResultMapper.ENGINE_NAME

    private val lock = Object()

    /** TessBaseAPI handle; created lazily on the first usable init attempt. */
    private var api: TessBaseAPI? = null
    private var initialized = false

    private val trainingDataDir: File = File(context.filesDir, "tessdata")
    private val trainedData: File = File(trainingDataDir, "eng.traineddata")

    /**
     * Lazily (re)initializes Tesseract for English. A no-op once initialized
     * or while eng.traineddata is missing — recognize() then returns an empty
     * result instead of throwing. Idempotent and safe to call from any
     * thread; never suspends.
     */
    fun init() {
        synchronized (lock) {
            if (initialized) return
            if (!trainedData.exists()) return
            try {
                val tesseract = api ?: TessBaseAPI().also { api = it }
                // tess-two's init(datapath, language) expects the PARENT of the
                // tessdata/ directory (it appends "tessdata/" itself, then
                // validates <datapath>/tessdata/eng.traineddata). It returns
                // false on native failure and throws on a bad datapath.
                initialized = tesseract.init(context.filesDir.absolutePath, "eng")
            } catch (_: Exception) {
                initialized = false
            }
        }
    }

    /**
     * Returns the absolute path of eng.traineddata (Success), copying the
     * bundled asset assets/tessdata/eng.traineddata into <filesDir>/tessdata/
     * on first use when present. Fails when neither location has the file;
     * the Failure message explains both options.
     */
    suspend fun ensureTrainedData(): Result<String> = withContext(Dispatchers.IO) {
        ensureTrainedDataOnDisk()
    }

    /** Non-suspending body of [ensureTrainedData]; runs on the IO dispatcher. */
    private fun ensureTrainedDataOnDisk(): Result<String> {
        if (trainedData.exists()) return Success(trainedData.absolutePath)
        val bundled = File(context.assetsDir, "tessdata/eng.traineddata")
        if (bundled.exists() && bundled.isFile) {
            trainingDataDir.mkdirs()
            bundled.copyTo(trainedData, overwrite = true)
            return Success(trainedData.absolutePath)
        }
        return Failure(IllegalStateException(missingDataMessage()))
    }

    private fun missingDataMessage(): String =
        "Trained data missing — place eng.traineddata in ${trainingDataDir.absolutePath} " +
            "or assets/tessdata/eng.traineddata (download from tessdata_fast)"

    override suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0): OcrResult =
        withContext(Dispatchers.Default) {
            // Best effort: copy a bundled asset once so the first scan needs
            // no manual setup. init() re-checks the file under the lock.
            if (!initialized) ensureTrainedData()
            synchronized (lock) {
                init()
                val tesseract = api
                if (!initialized || tesseract == null)
                    return@withContext OcrResult("", emptyList(), engine = name)
                val t0 = android.os.SystemClock.elapsedRealtime()
                try {
                    tesseract.setImage(bitmap)
                    val text = tesseract.getUTF8Text() ?: ""
                    return@withContext TesseractResultMapper.toResult(
                        rawText = text,
                        processingMs = android.os.SystemClock.elapsedRealtime() - t0,
                        rotationDegrees = rotationDegrees
                    )
                } finally {
                    // clear() frees the previous frame's pixels so the next
                    // setImage() starts from a clean page buffer.
                    try { tesseract.clear() } catch (_: Exception) { }
                }
            }
        }

    /** Releases the native Tesseract handle. Idempotent. */
    override fun close() {
        synchronized (lock) {
            try { api?.end() } catch (_: Exception) { }
            api = null
            initialized = false
        }
    }
}