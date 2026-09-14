package com.sahiix.ocrplaybook.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahiix.ocrplaybook.data.repo.ScanRepository
import com.sahiix.ocrplaybook.ocr.MlKitOcrEngine
import com.sahiix.ocrplaybook.ocr.OcrResult
import com.sahiix.ocrplaybook.ocr.RealtimeOcrAnalyzer
import com.sahiix.ocrplaybook.util.BitmapUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Working : ScanUiState
    data class Done(val scanId: Long, val result: OcrResult) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: ScanRepository,
    val engine: MlKitOcrEngine,
    val bitmapUtils: BitmapUtils
) : ViewModel() {
    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    /** Analyzer is created per camera session (needs the engine). */
    fun newAnalyzer(): RealtimeOcrAnalyzer = RealtimeOcrAnalyzer(engine, bitmapUtils)

    fun scanUri(uri: Uri) {
        _state.value = ScanUiState.Working
        viewModelScope.launch {
            try {
                val (id, res) = repository.scanUri(uri)
                _state.value = if (res.fullText.isBlank())
                    ScanUiState.Error("No text found — try better light or move closer.")
                else ScanUiState.Done(id, res)
            } catch (e: Exception) {
                _state.value = ScanUiState.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun scanBitmap(bmp: Bitmap) {
        _state.value = ScanUiState.Working
        viewModelScope.launch {
            try {
                val (id, res) = repository.scanBitmap(bmp)
                _state.value = if (res.fullText.isBlank())
                    ScanUiState.Error("No text found — try better light or move closer.")
                else ScanUiState.Done(id, res)
            } catch (e: Exception) {
                _state.value = ScanUiState.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun reset() { _state.value = ScanUiState.Idle }
}
