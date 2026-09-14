package com.sahiix.ocrplaybook.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahiix.ocrplaybook.ocr.RealtimeOcrAnalyzer
import com.sahiix.ocrplaybook.viewmodel.ScannerViewModel
import java.util.concurrent.Executors

@Composable
fun CameraPreviewBox(
    realtime: Boolean,
    onAnalyzer: (RealtimeOcrAnalyzer?) -> Unit,
    vm: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    DisposableEffect(realtime) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val executor = Executors.newSingleThreadExecutor()
        var analyzer: RealtimeOcrAnalyzer? = null
        val listener = Runnable {
            try {
                val provider = providerFuture.get()
                provider.unbindAll()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                if (realtime) {
                    analyzer = vm.newAnalyzer().also(onAnalyzer)
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also { it.setAnalyzer(executor, analyzer!!) }
                    provider.bindToLifecycle(lifecycle, selector, preview, analysis)
                } else {
                    onAnalyzer(null)
                    provider.bindToLifecycle(lifecycle, selector, preview)
                }
            } catch (_: Exception) { }
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            try { if (providerFuture.isDone) providerFuture.get().unbindAll() }
            catch (_: Exception) { }
            executor.shutdown()
        }
    }
    Box(Modifier.fillMaxWidth().height(320.dp)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}
