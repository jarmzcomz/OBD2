package com.autodiag.viewmodel

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.data.BluetoothElm327
import com.autodiag.data.ObdRepository
import com.autodiag.data.WebSocketClient
import com.autodiag.model.DtcCode
import com.autodiag.model.dtcDescriptions
import com.autodiag.model.inferSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DtcViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: ObdRepository? = null

    private val _dtcs = MutableStateFlow<List<DtcCode>>(emptyList())
    val dtcs: StateFlow<List<DtcCode>> = _dtcs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun initialize(mode: String, address: String) {
        val repo = when (mode) {
            "bluetooth" -> {
                val bluetoothManager = getApplication<Application>()
                    .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter ?: run {
                    _error.value = "Bluetooth not available"
                    return
                }
                BluetoothElm327(address, adapter)
            }
            else -> WebSocketClient(host = address)
        }
        repository = repo

        viewModelScope.launch {
            repo.connect()
        }

        repo.dtcs
            .onEach { codes ->
                _dtcs.value = codes.map { code ->
                    DtcCode(
                        code = code,
                        description = dtcDescriptions[code] ?: "Unknown fault code",
                    ).let { it.copy(severity = it.inferSeverity()) }
                }
            }
            .catch { e -> _error.value = e.message }
            .launchIn(viewModelScope)

        // Trigger DTC fetch for Bluetooth
        if (mode == "bluetooth" && repo is BluetoothElm327) {
            viewModelScope.launch {
                _isLoading.value = true
                (repo as BluetoothElm327).fetchDtcs()
                _isLoading.value = false
            }
        }
    }

    fun clearDtcs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository?.clearDtcs()
            } catch (e: Exception) {
                _error.value = "Failed to clear DTCs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository?.disconnect()
    }
}
