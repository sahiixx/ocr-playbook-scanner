package com.sahiix.ocrplaybook

import com.sahiix.ocrplaybook.ocr.OcrBlock
import com.sahiix.ocrplaybook.ocr.OcrResult
import org.junit.Assert.*
import org.junit.Test

class OcrResultTest {
    @Test fun statsComputed() {
        val r = OcrResult(
            fullText = "hello world\nsecond line",
            blocks = listOf(
                OcrBlock("hello world", 0.9f, null),
                OcrBlock("second line", 0.3f, null)
            )
        )
        assertEquals(4, r.wordCount)
        assertEquals(2, r.lineCount)
        assertEquals(0.6f, r.meanConfidence, 0.001f)
        assertEquals(1, r.lowConfidenceBlocks)
    }

    @Test fun noScoresMeansUnknown() {
        val r = OcrResult("hi", listOf(OcrBlock("hi", -1f, null)))
        assertEquals(-1f, r.meanConfidence, 0.001f)
    }
}
