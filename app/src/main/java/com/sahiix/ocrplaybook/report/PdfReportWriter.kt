package com.sahiix.ocrplaybook.report

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.sahiix.ocrplaybook.util.Constants.REPORTS_DIR
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a [PlaybookBuilder.Playbook] to a paginated PDF using only the
 * framework `android.graphics.pdf.PdfDocument` — no third-party PDF dep,
 * tiny APK impact, no native libs.
 */
@Singleton
class PdfReportWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun writePdf(playbook: PlaybookBuilder.Playbook, scanTitle: String): File =
        withContext(Dispatchers.IO) {
            val doc = PdfDocument()
            val pageW = 595 // A4 @72dpi
            val pageH = 842
            val margin = 48f
            var pageNum = 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
            var canvas = page.canvas
            var y = margin

            val titlePaint = Paint().apply { textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = 0xFF0B1220.toInt() }
            val hPaint = Paint().apply { textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = 0xFF1E3A8A.toInt() }
            val bodyPaint = Paint().apply { textSize = 10.5f; color = 0xFF111827.toInt() }
            val monoPaint = Paint().apply { textSize = 9f; typeface = Typeface.MONOSPACE; color = 0xFF374151.toInt() }
            val footPaint = Paint().apply { textSize = 8.5f; color = 0xFF6B7280.toInt() }

            fun newPage() {
                canvas.drawText("— $pageNum —", pageW / 2f - 12, pageH - 24f, footPaint)
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
                canvas = page.canvas
                y = margin
            }
            fun need(h: Float) { if (y + h > pageH - margin) newPage() }
            fun drawWrapped(text: String, paint: Paint, indent: Float = 0f, lineH: Float = 15f) {
                val maxW = pageW - margin * 2 - indent
                var line = StringBuilder()
                fun flush() { if (line.isNotEmpty()) { need(lineH); canvas.drawText(line.toString(), margin + indent, y, paint); y += lineH; line = StringBuilder() } }
                for (word in text.split(" ")) {
                    val trial = if (line.isEmpty()) word else "$line $word"
                    if (paint.measureText(trial) > maxW) flush()
                    if (line.isNotEmpty()) line.append(' ')
                    // Hard-split pathological long tokens (URLs).
                    var w = word
                    while (paint.measureText(w) > maxW) {
                        var cut = w.length - 1
                        while (cut > 1 && paint.measureText(w.substring(0, cut)) > maxW) cut--
                        line.append(w.substring(0, cut)); flush(); w = w.substring(cut)
                    }
                    line.append(w)
                }
                flush()
            }

            // ---- Title ----
            drawWrapped("Playbook — ${scanTitle.ifBlank { "Untitled scan" }}", titlePaint, lineH = 26f)
            y += 4
            val p = playbook
            drawWrapped("Summary: ${p.summary}", bodyPaint); y += 6
            fun section(h: String, items: List<String>, numbered: Boolean = false) {
                if (items.isEmpty()) return
                y += 4; need(22f)
                canvas.drawText(h, margin, y, hPaint); y += 18f
                items.forEachIndexed { i, s ->
                    drawWrapped(if (numbered) "${i + 1}. $s" else "•  $s", bodyPaint)
                }
            }
            section("Key points", p.keyPoints)
            section("Action items", p.actionItems, numbered = true)
            section("Dates & deadlines", p.dates)
            section("Contacts", p.emails.map { "Email: $it" } + p.phones.map { "Phone: $it" })
            if (p.keywords.isNotEmpty()) {
                y += 4; need(22f); canvas.drawText("Keywords", margin, y, hPaint); y += 18f
                drawWrapped(p.keywords.joinToString("  ") { "${it.first} ×${it.second}" }, monoPaint)
            }
            if (p.lowConfidence.isNotEmpty()) section("Needs review (low confidence)", p.lowConfidence)
            // Transcript
            y += 4; need(22f); canvas.drawText("Full transcript", margin, y, hPaint); y += 18f
            val transcript = p.markdown.substringAfter("```").substringBefore("```").trim()
            transcript.lines().forEach { drawWrapped(it.ifBlank { " " }, monoPaint, lineH = 13.5f) }
            y += 10; drawWrapped("Generated on-device by OCR Playbook Scanner. Verify critical details against the original.", footPaint)
            canvas.drawText("— $pageNum —", pageW / 2f - 12, pageH - 24f, footPaint)
            doc.finishPage(page)

            val dir = File(context.filesDir, REPORTS_DIR).apply { mkdirs() }
            val safe = scanTitle.ifBlank { "playbook" }.replace(Regex("[^A-Za-z0-9-_]+"), "_").take(40)
            val out = File(dir, "playbook_${safe}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(out).use { doc.writeTo(it) }
            doc.close()
            out
        }
}
