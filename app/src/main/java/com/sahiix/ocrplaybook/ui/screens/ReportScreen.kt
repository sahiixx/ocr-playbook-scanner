package com.sahiix.ocrplaybook.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sahiix.ocrplaybook.ui.components.SectionHeader
import com.sahiix.ocrplaybook.viewmodel.ReportViewModel
import java.io.File

@Composable
fun ReportScreen(scanId: Long, vm: ReportViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val s by vm.state.collectAsState()
    LaunchedEffect(scanId) { vm.load(scanId) }
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (s.working && s.playbook == null) {
            CircularProgressIndicator()
            Text("Building playbook\u2026")
            return@Column
        }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error); return@Column }
        val scan = s.scan ?: return@Column
        val pb = s.playbook ?: return@Column
        Text(scan.title.ifBlank { "Untitled scan" }, style = MaterialTheme.typography.headlineSmall)
        scan.imagePath?.let { path ->
            AsyncImage(
                model = File(path), contentDescription = "scanned image",
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Words", style = MaterialTheme.typography.labelMedium)
                    Text("${scan.wordCount}", style = MaterialTheme.typography.titleMedium)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Time", style = MaterialTheme.typography.labelMedium)
                    Text("${scan.processingMs} ms", style = MaterialTheme.typography.titleMedium)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Actions", style = MaterialTheme.typography.labelMedium)
                    Text("${pb.actionItems.size}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        SectionHeader("Executive summary")
        Text(pb.summary)
        SectionHeader("Key points")
        if (pb.keyPoints.isEmpty()) Text("_None detected._")
        else pb.keyPoints.forEach { Text("\u2022  $it", modifier = Modifier.padding(bottom = 4.dp)) }
        SectionHeader("Action items")
        if (pb.actionItems.isEmpty()) Text("_None detected._")
        else pb.actionItems.forEachIndexed { i, a -> Text("${i + 1}. $a", modifier = Modifier.padding(bottom = 4.dp)) }
        SectionHeader("Dates and contacts")
        (pb.dates + pb.emails + pb.phones).ifEmpty { listOf("_None detected._") }
            .forEach { Text("\u2022  $it", modifier = Modifier.padding(bottom = 2.dp)) }
        SectionHeader("Keywords")
        Text(pb.keywords.joinToString("  ") { "${it.first} x${it.second}" }.ifBlank { "_None._" })
        if (pb.lowConfidence.isNotEmpty()) {
            SectionHeader("Needs review (low confidence)")
            pb.lowConfidence.forEach { Text("\u2022  $it", modifier = Modifier.padding(bottom = 2.dp)) }
        }
        SectionHeader("Full transcript")
        Card(Modifier.fillMaxWidth()) {
            Text(scan.fullText.ifBlank { "(empty)" }, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { vm.exportPdf() }, modifier = Modifier.weight(1f)) { Text("Export PDF") }
            OutlinedButton(onClick = { vm.exportMarkdown() }, modifier = Modifier.weight(1f)) { Text("Export MD") }
        }
        s.pdfFile?.let { f ->
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent.createChooser(vm.shareIntent(f, "application/pdf"), "Share PDF")
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Share PDF: ${f.name}") }
        }
        s.mdFile?.let { f ->
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent.createChooser(vm.shareIntent(f, "text/markdown"), "Share Markdown")
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Share Markdown: ${f.name}") }
        }
    }
}
