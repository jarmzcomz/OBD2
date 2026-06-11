package com.autodiag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.data.ObdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class DashboardViewModel(private val repo: ObdRepository) : ViewModel() {

    private val _pids = MutableStateFlow<Map<String, Double>>(emptyMap())
    val pids: StateFlow<Map<String, Double>> = _pids

    private val _dtcs = MutableStateFlow<List<String>>(emptyList())
    val dtcs: StateFlow<List<String>> = _dtcs

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            repo.liveData
                .catch { e -> _error.value = e.message }
                .collect { _pids.value = it }
        }
    }

    fun loadDtcs() {
        viewModelScope.launch {
            repo.dtcs
                .catch { e -> _error.value = e.message }
                .collect { _dtcs.value = it }
        }
    }

    fun clearDtcs() {
        viewModelScope.launch {
            runCatching { repo.clearDtcs() }
                .onSuccess { _dtcs.value = emptyList() }
                .onFailure { _error.value = it.message }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.disconnect()
    }
}
