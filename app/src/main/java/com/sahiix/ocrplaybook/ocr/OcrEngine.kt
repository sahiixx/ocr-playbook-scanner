package com.sahiix.ocrplaybook.ocr

import android.graphics.Bitmap

/**
 * Pluggable on-device OCR engine.
 *
 * UI screens, the scan repository and the playbook report builder depend on
 * this interface (and on OcrResult) — never on a concrete engine — so engines
 * can be swapped or A/B-tested without touching UI/DB/report code. Each
 * engine reports a stable [name] that is persisted verbatim on every scan row
 * (see OcrResult.engine) so history and reports can tell which engine
 * produced the text.
 */
interface OcrEngine {

    /** Stable engine identifier persisted on OcrResult and scan rows. */
    val name: String

    /**
     * Recognizes the text in [bitmap], optionally rotated by [rotationDegrees].
     *
     * Implementations must never mutate [bitmap] and must be safe to call
     * repeatedly (the realtime analyzer throttles at 1 pass / 1.2 s). Return
     * the empty OcrResult (kept engine-tagged) when the engine is unavailable
     * or finds no text.
     */
    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0): OcrResult

    /** Releases native/engine resources. Must be idempotent. */
    fun close()
}