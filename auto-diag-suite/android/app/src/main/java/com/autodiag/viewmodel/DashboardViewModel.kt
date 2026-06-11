package com.autodiag.viewmodel

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.data.BluetoothElm327Repository
import com.autodiag.data.ObdRepository
import com.autodiag.data.WebSocketRepository
import com.autodiag.model.PidData
import com.autodiag.model.pidDataFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: ObdRepository? = null

    private val _pidData = MutableStateFlow<Map<String, PidData>>(emptyMap())
    val pidData: StateFlow<Map<String, PidData>> = _pidData.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun initialize(mode: String, address: String) {
        if (repository != null) return
        val repo = createRepository(mode, address) ?: return
        repository = repo

        repo.liveData
            .onEach { readings ->
                _isConnected.value = true
                _pidData.value = readings.mapValues { (name, value) -> pidDataFrom(name, value) }
            }
            .catch { e ->
                _isConnected.value = false
                _error.value = e.message
            }
            .launchIn(viewModelScope)

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.connect() }
                .onSuccess { _isConnected.value = true }
                .onFailure {
                    _isConnected.value = false
                    _error.value = it.message
                }
        }
    }

    private fun createRepository(mode: String, address: String): ObdRepository? {
        return when (mode) {
            "bluetooth" -> {
                val manager = getApplication<Application>()
                    .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
                BluetoothElm327Repository(device, adapter)
            }
            else -> WebSocketRepository(host = address)
        }
    }

    fun disconnect() {
        repository?.disconnect()
        _isConnected.value = false
    }

    override fun onCleared() {
        super.onCleared()
        repository?.disconnect()
    }
}
