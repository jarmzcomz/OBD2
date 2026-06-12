package com.autodiag.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.AutoDiagApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CodingStep(val request: String, val response: String, val ok: Boolean)

/**
 * Menú de MODIFICACIONES del vehículo. A diferencia del panel avanzado (que solo
 * existe en modo WiFi/backend), este funciona DIRECTO sobre el adaptador activo
 * (Bluetooth clásico o BLE) usando comandos crudos AT + UDS (ISO 14229).
 *
 * Permite cambiar configuraciones reales de la ECU: sesión diagnóstica, acceso
 * de seguridad, escritura por identificador (WriteDataByIdentifier 0x2E), rutinas
 * (RoutineControl 0x31) y control de actuadores (InputOutputControl 0x2F).
 *
 * Toda escritura es delicada: la UI exige confirmación explícita antes de ejecutar.
 */
class CodingViewModel(application: Application) : AndroidViewModel(application) {

    private val _log = MutableStateFlow<List<CodingStep>>(emptyList())
    val log: StateFlow<List<CodingStep>> = _log.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Disponible siempre que el transporte soporte comandos crudos (BT/BLE/backend). */
    val available: Boolean
        get() = getApplication<AutoDiagApplication>().activeRepository?.capabilities?.rawTerminal == true

    private fun repo() = getApplication<AutoDiagApplication>().activeRepository

    fun clearLog() { _log.value = emptyList() }

    /** Fija el header CAN destino de la ECU (p.ej. 7E0 para motor). */
    fun setEcuHeader(header: String) = sequence("Seleccionar ECU $header", listOf("AT SH $header"))

    /** Abre sesión diagnóstica extendida (necesaria para la mayoría de escrituras). */
    fun openExtendedSession() = sequence("Abrir sesión extendida", listOf("1003"))

    /** Abre sesión de programación (requerida para flasheo/codificación profunda). */
    fun openProgrammingSession() = sequence("Abrir sesión de programación", listOf("1002"))

    /** Solicita semilla de seguridad (nivel 1). El cálculo de la llave es por marca. */
    fun requestSeed() = sequence("Solicitar semilla (SecurityAccess 27 01)", listOf("2701"))

    /** Envía la llave calculada (nivel 2). */
    fun sendKey(keyHex: String) =
        sequence("Enviar llave (SecurityAccess 27 02)", listOf("2702 ${clean(keyHex)}"))

    /** Escribe un valor por identificador de dato (WriteDataByIdentifier 0x2E). */
    fun writeDataByIdentifier(did: String, valueHex: String) =
        sequence("Escribir DID ${clean(did)}", listOf("2E ${clean(did)} ${clean(valueHex)}"))

    /** Ejecuta una rutina (RoutineControl 0x31). sub: 01=arrancar 02=parar 03=resultado. */
    fun routineControl(sub: String, routineId: String, params: String = "") =
        sequence("Rutina ${clean(routineId)} (sub ${clean(sub)})",
            listOf("31 ${clean(sub)} ${clean(routineId)} ${clean(params)}".trim()))

    /** Control de actuador / I-O (InputOutputControl 0x2F). */
    fun ioControl(did: String, controlParam: String, state: String = "") =
        sequence("Actuador ${clean(did)}",
            listOf("2F ${clean(did)} ${clean(controlParam)} ${clean(state)}".trim()))

    /** Reset de la ECU (ECUReset 0x11). 01=hard 02=keyOff/on 03=soft. */
    fun ecuReset(sub: String = "01") = sequence("Reiniciar ECU (11 ${clean(sub)})", listOf("11 ${clean(sub)}"))

    /** Borra adaptaciones / aprendizajes (rutina común 0xFF00). */
    fun resetAdaptations() = sequence("Reset de adaptaciones",
        listOf("1003", "31 01 FF00"))

    /** Comando crudo libre escrito por el usuario. */
    fun sendRawWrite(command: String) = sequence("Comando manual", listOf(command))

    /**
     * Ejecuta una secuencia de comandos en orden, guardando cada paso. Si un paso
     * devuelve respuesta negativa (7F) marca el paso como fallido pero continúa,
     * para que el usuario vea exactamente dónde se rechazó la operación.
     */
    private fun sequence(label: String, commands: List<String>) {
        val r = repo() ?: run {
            push("(sin conexión)", "No hay adaptador activo.", false); return
        }
        if (_busy.value) return
        _busy.value = true
        push("— $label —", "", true)
        viewModelScope.launch {
            for (raw in commands) {
                val cmd = raw.trim()
                if (cmd.isEmpty()) continue
                val resp = runCatching { r.sendRaw(cmd) }.getOrElse { "ERROR: ${it.message}" }
                push(cmd, resp.ifBlank { "(sin respuesta)" }, !isNegative(resp))
            }
            _busy.value = false
        }
    }

    private fun push(req: String, resp: String, ok: Boolean) {
        _log.value = _log.value + CodingStep(req, resp, ok)
    }

    /** Respuesta negativa UDS empieza con 7F, o errores típicos del ELM327. */
    private fun isNegative(resp: String): Boolean {
        val u = resp.uppercase()
        return u.contains("7F") || u.contains("ERROR") || u.contains("NO DATA") ||
            u.contains("UNABLE") || u.contains("?")
    }

    private fun clean(s: String) = s.trim().replace(Regex("\\s+"), " ")
}
