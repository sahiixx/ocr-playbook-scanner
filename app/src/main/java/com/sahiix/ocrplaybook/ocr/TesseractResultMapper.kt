package com.sahiix.ocrplaybook.ocr

/**
 * Maps raw Tesseract (tess-two) output into the framework-agnostic OCR model.
 *
 * Pure JVM: no android.* imports, so this mapping logic unit-tests directly
 * on the host JVM (CI runs it without a device). tess-two exposes only the
 * recognized line text — no per-block confidence and no geometry — so blocks
 * are built from non-blank lines with confidence -1f and a null boundingBox.
 *
 * The optional [lines] parameter lets callers that happen to hold line-level
 * metadata pre-segment the blocks; [rawText] is always preserved verbatim as
 * OcrResult.fullText.
 */
object TesseractResultMapper {

    /** Engine identifier stamped on every result produced by this mapper. */
    const val ENGINE_NAME = "tesseract-local"

    /**
     * Converts raw Tesseract output into an OcrResult. When [lines] is empty,
     * blocks are derived from the non-blank lines of [rawText]; otherwise the
     * pairs in [lines] (line text, caller-side line metadata) override block
     * generation while fullText stays [rawText] untouched.
     */
    fun toResult(
        rawText: String,
        lines: List<Pair<String, Int>> = emptyList(),
        processingMs: Long = 0L,
        rotationDegrees: Int = 0
    ): OcrResult {
        val blocks = if (lines.isEmpty())
            rawText.lines().filter { it.isNotBlank() }.map { OcrBlock(it, -1f, null) }
        else
            lines.map { p -> OcrBlock(p.first, -1f, null) }
        return OcrResult(
            fullText = rawText,
            blocks = blocks,
            rotationDegrees = rotationDegrees,
            processingMs = processingMs,
            engine = ENGINE_NAME
        )
    }

    /** Counts whitespace-separated words in [rawText]; blank chunks are skipped. */
    fun countWords(rawText: String): Int =
        rawText.split("\\s+".toRegex()).count { it.isNotBlank() }
}