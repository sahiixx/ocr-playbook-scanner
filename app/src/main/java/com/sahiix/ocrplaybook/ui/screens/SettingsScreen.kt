package com.sahiix.ocrplaybook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahiix.ocrplaybook.ui.components.SectionHeader

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        SectionHeader("Privacy")
        Text(
            "All OCR runs 100% on-device with ML Kit. " +
                "Images stay in the app's private storage. " +
                "No account, no analytics, no network calls.",
            style = MaterialTheme.typography.bodyMedium
        )
        SectionHeader("Memory")
        MemoryRow("Low-RAM mode", "Auto-caps image edge at 1280px on low-RAM devices.", true)
        MemoryRow("RGB_565 decode", "Halves bitmap RAM vs ARGB_8888; text needs no alpha.", true)
        MemoryRow("Throttled realtime", "Max one OCR pass per 1.2s; frames recycled.", true)
        SectionHeader("About")
        Text("OCR Playbook Scanner v1.0.0 \u2014 on-device OCR + playbook reports.")
    }
}

@Composable
private fun MemoryRow(title: String, desc: String, on: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = on, onCheckedChange = {})
    }
}
