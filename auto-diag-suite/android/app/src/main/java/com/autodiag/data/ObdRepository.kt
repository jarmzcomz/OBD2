package com.autodiag.data

import kotlinx.coroutines.flow.Flow

/**
 * Abstracción de transporte OBD-II. Implementaciones: BLE, Bluetooth Clásico,
 * WebSocket (WiFi al backend) y Demo.
 */
interface ObdRepository {
    val liveData: Flow<Map<String, Double>>
    val dtcs: Flow<List<String>>
    suspend fun clearDtcs()
    fun connect()
    fun disconnect()

    /**
     * Envía un comando crudo (AT u OBD) y devuelve la respuesta del adaptador.
     */
    suspend fun sendRaw(command: String): String = ""

    /**
     * Marca el inicio de una sesión manual (VIN, UDS, sensor...). Mientras haya
     * una sesión activa, el liveData pausa para no corromper el contexto del ELM.
     * SIEMPRE llamar [endSession] en un bloque finally.
     */
    fun beginSession() {}

    /** Marca el fin de una sesión manual y reanuda el liveData. */
    fun endSession() {}

    /** Capacidades opcionales que la UI usa para mostrar/ocultar paneles. */
    val capabilities: ObdCapabilities get() = ObdCapabilities()
}

/**
 * Indica qué funciones soporta el transporte activo, para que la UI muestre solo
 * lo aplicable (p.ej. el panel avanzado UDS/CAN/VAG solo en modo WiFi/backend).
 */
data class ObdCapabilities(
    val rawTerminal: Boolean = false,
    val canMonitor: Boolean = false,
    val backendAdvanced: Boolean = false
)
