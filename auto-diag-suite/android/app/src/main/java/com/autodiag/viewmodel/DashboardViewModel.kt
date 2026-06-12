package com.autodiag.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.AutoDiagApplication
import com.autodiag.model.PidData
import com.autodiag.model.pidDataFrom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _pidData = MutableStateFlow<Map<String, PidData>>(emptyMap())
    val pidData: StateFlow<Map<String, PidData>> = _pidData.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Historial en memoria para las gráficas en vivo: por PID, los últimos
    // MAX_HISTORY valores. Suficiente para una traza temporal sin Room.
    private val _history = MutableStateFlow<Map<String, List<Float>>>(emptyMap())
    val history: StateFlow<Map<String, List<Float>>> = _history.asStateFlow()

    private val historyBuffers = mutableMapOf<String, ArrayDeque<Float>>()

    private var initialized = false

    companion object { const val MAX_HISTORY = 60 }

    fun initialize() {
        if (initialized) return
        initialized = true

        val repo = getApplication<AutoDiagApplication>().activeRepository
        if (repo == null) {
            _error.value = "No hay conexión activa. Vuelve y conecta el adaptador."
            return
        }

        _isConnected.value = true

        repo.liveData
            .onEach { readings ->
                _pidData.value = readings.mapValues { (name, value) -> pidDataFrom(name, value) }
                // Acumular historial para gráficas.
                readings.forEach { (name, value) ->
                    val buf = historyBuffers.getOrPut(name) { ArrayDeque() }
                    buf.addLast(value.toFloat())
                    while (buf.size > MAX_HISTORY) buf.removeFirst()
                }
                _history.value = historyBuffers.mapValues { it.value.toList() }
            }
            .catch { e ->
                _isConnected.value = false
                _error.value = e.message
            }
            .launchIn(viewModelScope)
    }

    fun disconnect() {
        getApplication<AutoDiagApplication>().clearRepository()
        _isConnected.value = false
    }

    override fun onCleared() {
        super.onCleared()
        // No desconectar aquí: DtcScreen puede estar usando el mismo repo.
    }
}
