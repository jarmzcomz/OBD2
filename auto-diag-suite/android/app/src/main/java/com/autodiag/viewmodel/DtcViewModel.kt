package com.autodiag.viewmodel

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.data.BleElm327Repository
import com.autodiag.data.BluetoothElm327Repository
import com.autodiag.data.ObdRepository
import com.autodiag.data.WebSocketRepository
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

    private var repository: ObdRepository? = null

    private val _dtcs = MutableStateFlow<List<DtcCode>>(emptyList())
    val dtcs: StateFlow<List<DtcCode>> = _dtcs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun initialize(mode: String, address: String) {
        if (repository != null) return
        val repo = createRepository(mode, address) ?: return
        repository = repo

        _isLoading.value = true
        repo.dtcs
            .onEach { codes ->
                _dtcs.value = codes.map { code ->
                    val dtc = DtcCode(
                        code = code,
                        description = dtcDescriptions[code] ?: "Unknown fault code",
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

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.connect() }
                .onFailure {
                    _error.value = it.message
                    _isLoading.value = false
                }
        }
    }

    private fun createRepository(mode: String, address: String): ObdRepository? {
        return when (mode) {
            "bluetooth", "ble" -> {
                val app = getApplication<Application>()
                val manager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = manager.adapter ?: run {
                    _error.value = "Bluetooth not available"
                    return null
                }
                val device = try {
                    adapter.getRemoteDevice(address)
                } catch (e: IllegalArgumentException) {
                    _error.value = "Invalid Bluetooth address"
                    return null
                }
                if (mode == "ble") BleElm327Repository(device, app)
                else BluetoothElm327Repository(device, adapter)
            }
            else -> WebSocketRepository(host = address)
        }
    }

    fun clearDtcs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) { repository?.clearDtcs() }
                _dtcs.value = emptyList()
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
