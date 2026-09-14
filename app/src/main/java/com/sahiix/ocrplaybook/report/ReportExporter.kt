package com.sahiix.ocrplaybook.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.sahiix.ocrplaybook.util.Constants.FILEPROVIDER_SUFFIX
import com.sahiix.ocrplaybook.util.Constants.REPORTS_DIR
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Persists markdown playbooks + builds share intents via FileProvider. */
@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfWriter: PdfReportWriter
) {
    suspend fun exportMarkdown(playbook: PlaybookBuilder.Playbook, scanTitle: String): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, REPORTS_DIR).apply { mkdirs() }
            val safe = scanTitle.ifBlank { "playbook" }.replace(Regex("[^A-Za-z0-9-_]+"), "_").take(40)
            File(dir, "playbook_${safe}_${System.currentTimeMillis()}.md").apply {
                writeText(playbook.markdown)
            }
        }

    suspend fun exportPdf(playbook: PlaybookBuilder.Playbook, scanTitle: String): File =
        pdfWriter.writePdf(playbook, scanTitle)

    fun shareIntent(file: File, mime: String): Intent {
        val uri = FileProvider.getUriForFile(
            context, context.packageName + FILEPROVIDER_SUFFIX, file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
