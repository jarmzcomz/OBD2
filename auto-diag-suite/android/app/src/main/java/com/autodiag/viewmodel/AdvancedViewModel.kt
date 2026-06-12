package com.autodiag.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.AutoDiagApplication
import com.autodiag.data.WebSocketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONArray

/**
 * Panel avanzado: solo activo en modo WiFi contra el backend FastAPI, que YA
 * implementa UDS (ISO 14229), sniffer CAN + decode DBC y codificación VAG con
 * el protocolo Zero-Brick. Las escrituras pasan SIEMPRE por el backend (que
 * exige backup + hash), nunca directo desde el celular.
 */
class AdvancedViewModel(application: Application) : AndroidViewModel(application) {

    private val _output = MutableStateFlow("Selecciona una operación del backend.")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val available: Boolean
        get() = getApplication<AutoDiagApplication>().activeRepository is WebSocketRepository

    private val repo: WebSocketRepository?
        get() = getApplication<AutoDiagApplication>().activeRepository as? WebSocketRepository

    fun loadCanFrames() = run("CAN frames") { it.httpGet("can/frames") }
    fun loadVagModules() = run("Módulos VAG") { it.httpGet("vag/modules") }
    fun loadHealth() = run("Estado backend") { it.httpGet("health") }

    /** Lectura UDS por DID (solo lectura, segura). Crea sesión y consulta. */
    fun udsRead(ecu: String, did: String) = run("UDS leer DID $did") { r ->
        val sessionResp = r.httpPost("session", "{}")
        val sessionId = runCatching { JSONObject(sessionResp).optString("session_id") }.getOrNull()
        if (sessionId.isNullOrBlank()) {
            "No se pudo crear sesión:\n$sessionResp"
        } else {
            r.httpPost("session/$sessionId/uds/read", """{"ecu":"$ecu","did":"$did"}""")
        }
    }

    private fun run(label: String, block: suspend (WebSocketRepository) -> String) {
        val r = repo ?: run {
            _output.value = "El panel avanzado solo está disponible en modo WiFi (backend)."
            return
        }
        if (_busy.value) return
        _busy.value = true
        _output.value = "Cargando: $label…"
        viewModelScope.launch {
            val raw = runCatching { block(r) }.getOrElse { "ERROR: ${it.message}" }
            _output.value = "[$label]\n${prettify(raw)}"
            _busy.value = false
        }
    }

    /** Formatea JSON para lectura; si no es JSON, lo deja crudo. */
    private fun prettify(raw: String): String = try {
        when {
            raw.trimStart().startsWith("{") -> JSONObject(raw).toString(2)
            raw.trimStart().startsWith("[") -> JSONArray(raw).toString(2)
            else -> raw
        }
    } catch (e: JSONException) {
        raw
    }
}
