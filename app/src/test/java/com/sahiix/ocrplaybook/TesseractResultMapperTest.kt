package com.sahiix.ocrplaybook

import com.sahiix.ocrplaybook.ocr.TesseractResultMapper
import org.junit.Assert.*
import org.junit.Test

class TesseractResultMapperTest {

    @Test fun emptyTextProducesEmptyResult() {
        val r = TesseractResultMapper.toResult("")
        assertEquals(0, r.blocks.size)
        assertEquals("", r.fullText)
        assertEquals(0, TesseractResultMapper.countWords(""))
        assertEquals("tesseract-local", r.engine)
    }

    @Test fun multiLineTextBuildsOneBlockPerNonBlankLine() {
        val raw = "First line\n\nSecond line\nThird line"
        val r = TesseractResultMapper.toResult(raw, processingMs = 12, rotationDegrees = 90)
        assertEquals(raw, r.fullText)
        assertEquals(3, r.blocks.size)
        assertEquals(listOf("First line", "Second line", "Third line"), r.blocks.map { it.text })
        assertTrue(r.blocks.all { it.confidence == -1f })
        assertTrue(r.blocks.all { it.boundingBox == null })
        assertEquals(90, r.rotationDegrees)
        assertEquals(12L, r.processingMs)
        assertEquals(6, TesseractResultMapper.countWords(raw))
        // Mapper word count stays consistent with OcrResult's own statistic.
        assertEquals(r.wordCount, TesseractResultMapper.countWords(raw))
    }

    @Test fun linesParamOverridesBlockGeneration() {
        val raw = "alpha\nbeta"
        val r = TesseractResultMapper.toResult(
            rawText = raw,
            lines = listOf(Pair("merged single block", 0))
        )
        assertEquals(raw, r.fullText)
        assertEquals(1, r.blocks.size)
        assertEquals("merged single block", r.blocks[0].text)
        assertEquals(-1f, r.blocks[0].confidence, 0.0f)
        assertNull(r.blocks[0].boundingBox)
        assertEquals(2, r.wordCount) // statistic still derives from raw fullText
    }

    @Test fun countWordsHandlesWhitespaceEdgeCases() {
        assertEquals(0, TesseractResultMapper.countWords(""))
        assertEquals(0, TesseractResultMapper.countWords("   \t  \n  "))
        assertEquals(2, TesseractResultMapper.countWords("hello  world"))
        assertEquals(3, TesseractResultMapper.countWords("\tone  two\tthree"))
        assertEquals(2, TesseractResultMapper.countWords("  alpha \t\t beta  "))
    }
}