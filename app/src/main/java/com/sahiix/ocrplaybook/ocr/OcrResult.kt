package com.sahiix.ocrplaybook.ocr

import android.graphics.Rect

/**
 * Framework-agnostic OCR model. The ML Kit layer maps into this so the UI,
 * database and report builder never depend on GMS classes directly
 * (easier to unit-test on the JVM).
 */
data class OcrBlock(
    val text: String,
    val confidence: Float, // 0..1, -1 when engine gives none
    val boundingBox: Rect?,
    val languages: List<String> = emptyList()
)

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock>,
    val rotationDegrees: Int = 0,
    val processingMs: Long = 0L,
    val engine: String = "mlkit-on-device"
) {
    val wordCount: Int get() =
        fullText.split("\\s+".toRegex()).count { it.isNotBlank() }
    val lineCount: Int get() =
        fullText.lines().count { it.isNotBlank() }
    val meanConfidence: Float get() {
        val scored = blocks.filter { it.confidence >= 0f }
        if (scored.isEmpty()) return -1f
        return scored.sumOf { it.confidence.toDouble() }.toFloat() / scored.size
    }
    val lowConfidenceBlocks: Int get() =
        blocks.count { it.confidence in 0f..0.6f }
}
