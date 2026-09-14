package com.sahiix.ocrplaybook

import com.sahiix.ocrplaybook.data.db.OcrBlockEntity
import com.sahiix.ocrplaybook.data.db.ScanEntity
import com.sahiix.ocrplaybook.report.PlaybookBuilder
import org.junit.Assert.*
import org.junit.Test

class PlaybookBuilderTest {

    private fun scan(text: String) = ScanEntity(
        id = 1, createdAt = 0L, fullText = text,
        wordCount = text.split("\\s+".toRegex()).size,
        lineCount = text.lines().size,
        meanConfidence = 0.9f, processingMs = 120,
        engine = "test", title = "Test"
    )

    @Test fun extractsContacts() {
        val pb = PlaybookBuilder.build(
            scan("Contact john@example.com or +971 50 123 4567 today"),
            emptyList()
        )
        assertTrue(pb.emails.contains("john@example.com"))
        assertTrue(pb.phones.any { it.contains("971") })
    }

    @Test fun extractsActionItems() {
        val text = "Meeting notes\nTODO: send the contract\n3. Book flights for Monday"
        val pb = PlaybookBuilder.build(scan(text), emptyList())
        assertTrue(pb.actionItems.any { it.contains("send the contract", ignoreCase = true) })
        assertTrue(pb.actionItems.any { it.contains("Book flights", ignoreCase = true) })
    }

    @Test fun extractsDates() {
        val pb = PlaybookBuilder.build(scan("Deadline 12/05/2026 and review on March 3, 2026"), emptyList())
        assertEquals(2, pb.dates.size)
    }

    @Test fun flagsLowConfidence() {
        val blocks = listOf(
            OcrBlockEntity(position = 0, text = "clear text", confidence = 0.95f),
            OcrBlockEntity(position = 1, text = "blurry text", confidence = 0.3f)
        )
        val pb = PlaybookBuilder.build(scan("clear text\nblurry text"), blocks)
        assertTrue(pb.lowConfidence.any { it.contains("blurry") })
    }

    @Test fun emptyTextProducesFallbackSummary() {
        val pb = PlaybookBuilder.build(scan(""), emptyList())
        assertTrue(pb.summary.contains("No text"))
        assertTrue(pb.markdown.contains("Full transcript"))
    }

    @Test fun keywordsRanked() {
        val pb = PlaybookBuilder.build(scan("contract contract contract invoice payment"), emptyList())
        assertEquals("contract", pb.keywords.first().first)
        assertEquals(3, pb.keywords.first().second)
    }
}
