package com.autodiag.viewmodel

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.AutoDiagApplication
import com.autodiag.data.BleElm327Repository
import com.autodiag.data.BluetoothElm327Repository
import com.autodiag.data.DemoRepository
import com.autodiag.data.ObdRepository
import com.autodiag.data.WebSocketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed class ConnectState {
    object Idle : ConnectState()
    object Connecting : ConnectState()
    object Connected : ConnectState()
    data class Error(val message: String) : ConnectState()
}

class ConnectViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ConnectState>(ConnectState.Idle)
    val state: StateFlow<ConnectState> = _state.asStateFlow()

    fun connect(mode: String, address: String) {
        if (_state.value is ConnectState.Connecting) return
        _state.value = ConnectState.Connecting

        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<AutoDiagApplication>()
            val repo = createRepository(mode, address)
            if (repo == null) {
                _state.value = ConnectState.Error("No se pudo crear la conexión")
                return@launch
            }

            runCatching {
                // Demo conecta al instante; el resto puede colgarse si el
                // hardware no responde, así que limitamos a 15s.
                val ok = withTimeoutOrNull(15_000) { repo.connect(); true }
                if (ok != true) throw java.io.IOException("Tiempo de conexión agotado")
            }
                .onSuccess {
                    app.setRepository(repo, labelFor(mode, address))
                    _state.value = ConnectState.Connected
                }
                .onFailure { e ->
                    runCatching { repo.disconnect() }
                    _state.value = ConnectState.Error(e.message ?: "Error al conectar")
                }
        }
    }

    fun resetState() {
        _state.value = ConnectState.Idle
    }

    private fun labelFor(mode: String, address: String): String = when (mode) {
        "demo" -> "Modo Demo (datos simulados)"
        "ble" -> "Bluetooth LE · $address"
        "bluetooth" -> "Bluetooth Clásico · $address"
        else -> "WiFi · $address"
    }

    private fun createRepository(mode: String, address: String): ObdRepository? {
        val app = getApplication<Application>()
        return when (mode) {
            "demo" -> DemoRepository()
            "bluetooth", "ble" -> {
                val manager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = manager.adapter ?: return null
                val device = try {
                    adapter.getRemoteDevice(address)
                } catch (e: IllegalArgumentException) {
                    return null
                }
                if (mode == "ble") BleElm327Repository(device, app)
                else BluetoothElm327Repository(device, adapter)
            }
            else -> WebSocketRepository(address)
        }
    }
}
