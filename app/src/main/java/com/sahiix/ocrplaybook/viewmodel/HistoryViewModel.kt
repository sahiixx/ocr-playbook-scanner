package com.sahiix.ocrplaybook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahiix.ocrplaybook.data.db.ScanEntity
import com.sahiix.ocrplaybook.data.repo.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {
    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val scans: Flow<List<ScanEntity>> = query.flatMapLatest { q ->
        if (q.isBlank()) repository.observeAll() else repository.search(q)
    }

    fun setQuery(q: String) { query.value = q }
    fun toggleFavorite(s: ScanEntity) = viewModelScope.launch { repository.toggleFavorite(s) }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}
