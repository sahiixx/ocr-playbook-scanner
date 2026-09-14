package com.sahiix.ocrplaybook.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sahiix.ocrplaybook.R
import com.sahiix.ocrplaybook.ocr.RealtimeOcrAnalyzer
import com.sahiix.ocrplaybook.viewmodel.ScanUiState
import com.sahiix.ocrplaybook.viewmodel.ScannerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onScanDone: (Long) -> Unit,
    vm: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    var realtime by remember { mutableStateOf(true) }
    var analyzer by remember { mutableStateOf<RealtimeOcrAnalyzer?>(null) }
    var liveText by remember { mutableStateOf<com.sahiix.ocrplaybook.ocr.OcrResult?>(null) }
    var workingLive by remember { mutableStateOf(false) }
    LaunchedEffect(analyzer) {
        analyzer?.let { a ->
            launch {
                a.latest.collect { liveText = it }
            }
            launch {
                a.isWorking.collect { workingLive = it }
            }
        } ?: run {
            liveText = null
            workingLive = false
        }
    }
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris: List<Uri> -> uris.firstOrNull()?.let { vm.scanUri(it) } }
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { vm.scanUri(it) } }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp -> bmp?.let { vm.scanBitmap(it) } }

    val activity = context as? android.app.Activity
    val sharedUri: Uri? = remember(activity?.intent) {
        val i = activity?.intent
        if (i?.action == Intent.ACTION_SEND) {
            @Suppress("DEPRECATION")
            i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        } else null
    }
    LaunchedEffect(sharedUri) {
        if (sharedUri != null && state is ScanUiState.Idle) vm.scanUri(sharedUri)
    }
    LaunchedEffect(state) {
        if (state is ScanUiState.Done) {
            onScanDone((state as ScanUiState.Done).scanId)
            vm.reset()
        }
    }
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.titleMedium)
        if (!cameraPermission.status.isGranted) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Camera access is needed for live scanning. Gallery import works without it.")
                    Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                        Text("Grant camera permission")
                    }
                }
            }
        } else {
            CameraPreviewBox(realtime = realtime, onAnalyzer = { analyzer = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Live OCR overlay", Modifier.weight(1f))
                Switch(checked = realtime, onCheckedChange = { realtime = it })
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (workingLive) "Reading\u2026"
                            else liveText?.fullText?.lineSequence()
                                ?.firstOrNull { it.isNotBlank() }?.take(60)
                                ?: "Point at text",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (workingLive) CircularProgressIndicator(Modifier.padding(start = 8.dp))
                    }
                    val preview = liveText?.fullText?.take(280)
                    if (!preview.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            preview + if ((liveText?.fullText?.length ?: 0) > 280) "\u2026" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ExtendedFloatingActionButton(
                onClick = {
                    if (cameraPermission.status.isGranted) takePictureLauncher.launch(null)
                    else cameraPermission.launchPermissionRequest()
                },
                icon = { Icon(Icons.Default.CameraAlt, null) },
                text = { Text(stringResource(R.string.action_scan)) },
                modifier = Modifier.weight(1f)
            )
            ExtendedFloatingActionButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                icon = { Icon(Icons.Default.PhotoLibrary, null) },
                text = { Text(stringResource(R.string.action_gallery)) },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedButton(
            onClick = { fileLauncher.launch(arrayOf("image/*")) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Pick a file (image/*)") }
        when (val s = state) {
            ScanUiState.Working -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Text("  Recognizing text on-device\u2026")
            }
            is ScanUiState.Error -> Card {
                Text(s.message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }
    }
}
