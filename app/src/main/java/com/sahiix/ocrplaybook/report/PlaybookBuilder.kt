package com.sahiix.ocrplaybook.report

import com.sahiix.ocrplaybook.data.db.OcrBlockEntity
import com.sahiix.ocrplaybook.data.db.ScanEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns raw OCR text into a structured "playbook" report.
 * Pure Kotlin — zero Android deps, fully unit-testable on the JVM.
 */
object PlaybookBuilder {

    data class Playbook(
        val title: String,
        val markdown: String,
        val summary: String,
        val keyPoints: List<String>,
        val actionItems: List<String>,
        val dates: List<String>,
        val emails: List<String>,
        val phones: List<String>,
        val keywords: List<Pair<String, Int>>,
        val lowConfidence: List<String>
    )

    private val EMAIL = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val PHONE = Regex("(?:\\+?\\d[\\d\\s().-]{6,}\\d)")
    private val DATE = Regex(
        "(?i)\\b(\\d{1,2}[/-.]\\d{1,2}[/-.]\\d{2,4}|\\d{4}-\\d{2}-\\d{2})\\b"
    )
    private val MONTH_DATE = Regex(
        "(?i)\\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s+\\d{1,2}(?:st|nd|rd|th)?(?:,?\\s+\\d{4})?\\b"
    )
    private val ACTION_VERBS = Regex(
        "(?i)^\\s*(?:[-*\u2022\\[\\]x\\s]*)(action|todo|follow[-\\s]?up|call|email|send|schedule|book|pay|submit|review|prepare|arrange|confirm|remind|deadline|due|must|should|need to|please)\\b[\\s:.-]*"
    )
    private val NUMBERED = Regex("^\\s*\\d+[.)]\\s+\\S+")
    private val STOPWORDS = setOf(
        "the", "and", "for", "with", "this", "that", "from",
        "your", "our", "their", "will", "shall", "should"
    )

    fun build(scan: ScanEntity, blocks: List<OcrBlockEntity>): Playbook {
        val text = scan.fullText
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val emails = EMAIL.findAll(text).map { it.value }.distinct().toList()
        val phones = PHONE.findAll(text).map { it.value.trim() }
            .filter { it.filter(Char::isDigit).length in 7..15 }.distinct().toList()
        val dates = (DATE.findAll(text) + MONTH_DATE.findAll(text))
            .map { it.value }.distinct().toList()
        val actionItems = lines
            .filter { ACTION_VERBS.containsMatchIn(it) || NUMBERED.containsMatchIn(it) }
            .map { it.replace(Regex("^[-*\u2022\\[\\]x\\s]+"), "").trim() }
            .filter { it.length > 3 }.distinct().take(30)
        val keyPoints = lines
            .filter { it.length >= 25 && it !in actionItems }
            .sortedByDescending { it.length }.distinct().take(10)
        val summary = when {
            lines.isEmpty() -> "No text was recognized in this scan."
            lines.size <= 3 -> lines.joinToString(" ")
            else -> lines.take(3).joinToString(" ") +
                "  \u2026(+${lines.size - 3} more lines \u2014 see transcript)."
        }
        val keywords = text.lowercase(Locale.US)
            .split(Regex("[^a-z0-9']+")).filter { it.length >= 4 && it !in STOPWORDS }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(15)
            .map { it.key to it.value }
        val lowConfidence = blocks.filter { it.confidence in 0f..0.6f }
            .take(20).map { it.text.take(120) }
        val dateStr =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(scan.createdAt))
        val confStr = if (scan.meanConfidence < 0) "n/a (engine)"
            else "%.2f".format(scan.meanConfidence)
        val md = buildString {
            appendLine("# Playbook \u2014 ${scan.title.ifBlank { "Untitled scan" }}")
            appendLine()
            appendLine("| Field | Value |")
            appendLine("|---|---|")
            appendLine("| Scanned | $dateStr |")
            appendLine("| Engine | ${scan.engine} (${scan.processingMs} ms) |")
            appendLine("| Words / Lines | ${scan.wordCount} / ${scan.lineCount} |")
            appendLine("| Mean confidence | $confStr |")
            appendLine()
            appendLine("## Executive summary")
            appendLine(summary)
            appendLine()
            appendLine("## Key points")
            if (keyPoints.isEmpty()) appendLine("_None detected._")
            else keyPoints.forEach { appendLine("- $it") }
            appendLine()
            appendLine("## Action items")
            if (actionItems.isEmpty()) appendLine("_None detected._")
            else actionItems.forEachIndexed { i, a -> appendLine("${i + 1}. $a") }
            appendLine()
            appendLine("## Dates and deadlines")
            if (dates.isEmpty()) appendLine("_None detected._")
            else dates.forEach { appendLine("- $it") }
            appendLine()
            appendLine("## Contacts")
            if (emails.isEmpty() && phones.isEmpty()) appendLine("_None detected._")
            else {
                emails.forEach { appendLine("- Email: `$it`") }
                phones.forEach { appendLine("- Phone: `$it`") }
            }
            appendLine()
            appendLine("## Keywords")
            if (keywords.isEmpty()) appendLine("_None._")
            else keywords.forEach { (k, c) -> appendLine("- `$k` x$c") }
            if (lowConfidence.isNotEmpty()) {
                appendLine()
                appendLine("## Needs review (low confidence)")
                lowConfidence.forEach { appendLine("- _$it\_") }
            }
            appendLine()
            appendLine("## Full transcript")
            appendLine("```")
            appendLine(text.ifBlank { "(empty)" })
            appendLine("```")
            appendLine()
            appendLine("_Generated on-device. Verify critical details._")
        }
        return Playbook(
            scan.title, md, summary, keyPoints, actionItems,
            dates, emails, phones, keywords, lowConfidence
        )
    }
}
