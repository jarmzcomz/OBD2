package com.autodiag.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.AutoDiagApplication
import com.autodiag.model.DtcCode
import com.autodiag.model.dtcDescriptions
import com.autodiag.model.inferSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DtcViewModel(application: Application) : AndroidViewModel(application) {

    private val _dtcs = MutableStateFlow<List<DtcCode>>(emptyList())
    val dtcs: StateFlow<List<DtcCode>> = _dtcs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        val repo = getApplication<AutoDiagApplication>().activeRepository
        if (repo == null) {
            _error.value = "No hay conexión activa."
            return
        }

        _isLoading.value = true
        repo.dtcs
            .onEach { codes ->
                _dtcs.value = codes.map { code ->
                    val dtc = DtcCode(
                        code = code,
                        description = dtcDescriptions[code] ?: "Código de falla desconocido"
                    )
                    dtc.copy(severity = dtc.inferSeverity())
                }
                _isLoading.value = false
            }
            .catch { e ->
                _error.value = e.message
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    fun clearDtcs() {
        val repo = getApplication<AutoDiagApplication>().activeRepository ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) { repo.clearDtcs() }
                _dtcs.value = emptyList()
            } catch (e: Exception) {
                _error.value = "Error al borrar DTCs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
