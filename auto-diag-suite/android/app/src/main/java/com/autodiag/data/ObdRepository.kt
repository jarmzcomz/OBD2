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
     * Base del terminal de ingeniero y de las consultas avanzadas (VIN, freeze
     * frame, modos 07/0A, etc.). Implementación por defecto vacía para repos que
     * no lo soporten (p.ej. WebSocket puro).
     */
    suspend fun sendRaw(command: String): String = ""

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
