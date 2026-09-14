package com.sahiix.ocrplaybook.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.sahiix.ocrplaybook.data.db.OcrBlockEntity
import com.sahiix.ocrplaybook.data.db.ScanDao
import com.sahiix.ocrplaybook.data.db.ScanEntity
import com.sahiix.ocrplaybook.ocr.OcrEngine
import com.sahiix.ocrplaybook.ocr.OcrResult
import com.sahiix.ocrplaybook.util.BitmapUtils
import com.sahiix.ocrplaybook.util.Constants.IMAGES_DIR
import com.sahiix.ocrplaybook.util.Constants.SCAN_JPEG_QUALITY
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for scan persistence.
 * Copies the source image into internal storage (downscaled JPEG) then
 * writes the scan + blocks to Room in one transaction.
 */
@Singleton
class ScanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ScanDao,
    private val engine: OcrEngine,
    private val bitmapUtils: BitmapUtils
) {
    fun observeAll(): Flow<List<ScanEntity>> = dao.observeAll()
    fun observeFavorites(): Flow<List<ScanEntity>> = dao.observeFavorites()
    fun search(q: String): Flow<List<ScanEntity>> = dao.search(q)
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun blocksFor(id: Long) = dao.blocksFor(id)
    suspend fun toggleFavorite(s: ScanEntity) = dao.updateScan(s.copy(favorite = !s.favorite))
    suspend fun delete(id: Long) {
        val s = dao.getById(id)
        dao.deleteScan(id)
        s?.imagePath?.let { try { File(it).delete() } catch (_: Exception) { } }
    }

    suspend fun updateTags(id: Long, tags: String) {
        dao.getById(id)?.let { dao.updateScan(it.copy(tags = tags)) }
    }

    /** OCR a content:// Uri, persist image + text, return new row id. */
    suspend fun scanUri(uri: Uri): Pair<Long, OcrResult> = withContext(Dispatchers.IO) {
        val bmp = bitmapUtils.decodeSampled(uri, bitmapUtils.suggestedMaxEdge())
            ?: throw IllegalArgumentException("Could not decode image")
        try { scanBitmap(bmp) } finally { if (!bmp.isRecycled) bmp.recycle() }
    }

    /** OCR an in-memory bitmap (camera capture), persist, return id. */
    suspend fun scanBitmap(src: Bitmap): Pair<Long, OcrResult> = withContext(Dispatchers.IO) {
        val result = engine.recognize(src)
        val savedPath = persistBitmap(src)
        val title = result.fullText.lineSequence()
            .map { it.trim() }.firstOrNull { it.isNotEmpty() }
            ?.take(60) ?: "Untitled scan"
        val id = dao.insertScanWithBlocks(
            ScanEntity(
                imagePath = savedPath, fullText = result.fullText,
                wordCount = result.wordCount, lineCount = result.lineCount,
                meanConfidence = result.meanConfidence,
                processingMs = result.processingMs, engine = result.engine, title = title
            ),
            result.blocks.mapIndexed { i, b ->
                OcrBlockEntity(
                    position = i, text = b.text, confidence = b.confidence,
                    left = b.boundingBox?.left ?: 0, top = b.boundingBox?.top ?: 0,
                    right = b.boundingBox?.right ?: 0, bottom = b.boundingBox?.bottom ?: 0,
                    languages = b.languages.joinToString(",")
                )
            }
        )
        id to result
    }

    private fun persistBitmap(src: Bitmap): String {
        val dir = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }
        val out = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { fos ->
            src.compress(Bitmap.CompressFormat.JPEG, SCAN_JPEG_QUALITY, fos)
            fos.flush()
        }
        return out.absolutePath
    }
}
