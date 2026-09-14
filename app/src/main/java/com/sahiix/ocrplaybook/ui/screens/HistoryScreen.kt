package com.sahiix.ocrplaybook.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahiix.ocrplaybook.ui.components.EmptyHint
import com.sahiix.ocrplaybook.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onOpen: (Long) -> Unit,
    vm: HistoryViewModel = hiltViewModel()
) {
    var q by remember { mutableStateOf("") }
    val scans by vm.scans.collectAsState(initial = emptyList())
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = q, onValueChange = { q = it; vm.setQuery(it) },
            label = { Text("Search scans") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        if (scans.isEmpty()) {
            EmptyHint("No scans yet. Scan a page to build your first playbook.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scans, key = { it.id }) { s ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(s.id) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    s.title.ifBlank { "Untitled scan" },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "${fmt.format(Date(s.createdAt))} \u00b7 " +
                                        "${s.wordCount} words \u00b7 ${s.processingMs} ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    s.fullText.take(120),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                            IconButton(onClick = { vm.toggleFavorite(s) }) {
                                Icon(
                                    if (s.favorite) Icons.Default.Favorite
                                    else Icons.Default.FavoriteBorder,
                                    contentDescription = "favorite"
                                )
                            }
                            IconButton(onClick = { vm.delete(s.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
