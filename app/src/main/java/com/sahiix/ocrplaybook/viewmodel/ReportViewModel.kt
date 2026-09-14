package com.sahiix.ocrplaybook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahiix.ocrplaybook.data.db.OcrBlockEntity
import com.sahiix.ocrplaybook.data.db.ScanEntity
import com.sahiix.ocrplaybook.data.repo.ScanRepository
import com.sahiix.ocrplaybook.report.PlaybookBuilder
import com.sahiix.ocrplaybook.report.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ReportUiState(
    val scan: ScanEntity? = null,
    val blocks: List<OcrBlockEntity> = emptyList(),
    val playbook: PlaybookBuilder.Playbook? = null,
    val pdfFile: File? = null,
    val mdFile: File? = null,
    val working: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: ScanRepository,
    private val exporter: ReportExporter
) : ViewModel() {
    private val _state = MutableStateFlow(ReportUiState(working = true))
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    fun load(scanId: Long) {
        viewModelScope.launch {
            try {
                val scan = repository.getById(scanId)
                    ?: throw IllegalArgumentException("Scan not found")
                val blocks = repository.blocksFor(scanId)
                _state.value = ReportUiState(
                    scan = scan, blocks = blocks,
                    playbook = PlaybookBuilder.build(scan, blocks)
                )
            } catch (e: Exception) {
                _state.value = ReportUiState(error = e.message)
            }
        }
    }

    fun exportPdf() {
        val (scan, pb) = _state.value.scan to _state.value.playbook ?: return
        if (scan == null || pb == null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true)
            try {
                val f = exporter.exportPdf(pb, scan.title)
                _state.value = _state.value.copy(working = false, pdfFile = f)
            } catch (e: Exception) {
                _state.value = _state.value.copy(working = false, error = e.message)
            }
        }
    }

    fun exportMarkdown() {
        val (scan, pb) = _state.value.scan to _state.value.playbook ?: return
        if (scan == null || pb == null) return
        viewModelScope.launch {
            try {
                val f = exporter.exportMarkdown(pb, scan.title)
                _state.value = _state.value.copy(mdFile = f)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun shareIntent(file: File, mime: String) = exporter.shareIntent(file, mime)
}
