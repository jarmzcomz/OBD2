package com.autodiag.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.AutoDiagApplication
import com.autodiag.data.UdsInterpreter
import com.autodiag.data.VagCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de un módulo tras el escaneo. */
data class ModuleStatus(
    val address: String,
    val name: String,
    val present: Boolean,
    val detail: String
)

/** Estado de un interruptor de función. */
data class FeatureState(
    val feature: VagCatalog.Feature,
    val known: Boolean,     // ¿pudimos leer su estado?
    val enabled: Boolean,   // estado actual
    val note: String        // explicación en texto plano
)

/**
 * Cerebro de la experiencia VAG en lenguaje claro: escanea las centralitas,
 * lee identificación (VIN, nombre de sistema) y traduce todo a español. Para los
 * interruptores lee SIEMPRE el estado real antes de permitir cambios.
 */
class VagViewModel(application: Application) : AndroidViewModel(application) {

    private val _modules = MutableStateFlow<List<ModuleStatus>>(emptyList())
    val modules: StateFlow<List<ModuleStatus>> = _modules.asStateFlow()

    private val _features = MutableStateFlow<List<FeatureState>>(emptyList())
    val features: StateFlow<List<FeatureState>> = _features.asStateFlow()

    private val _status = MutableStateFlow("Listo. Toca \"Escanear el auto\" para empezar.")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /**
     * Código/llave de seguridad que el usuario pega manualmente (ej. el "código de
     * acceso" de VCDS/OBDeleven o una llave hex). Si está vacío, NO intentamos
     * acceso de seguridad para evitar bloquear el módulo con llaves equivocadas.
     */
    private val _securityCode = MutableStateFlow("")
    val securityCode: StateFlow<String> = _securityCode.asStateFlow()

    fun setSecurityCode(code: String) { _securityCode.value = code.trim() }

    val available: Boolean
        get() = getApplication<AutoDiagApplication>().activeRepository?.capabilities?.rawTerminal == true

    private fun repo() = getApplication<AutoDiagApplication>().activeRepository

    /** Recorre todos los módulos VAG conocidos y reporta cuáles responden, en texto. */
    fun scanModules() {
        val r = repo() ?: return
        if (_busy.value) return
        _busy.value = true
        _status.value = "Escaneando módulos del auto…"
        viewModelScope.launch {
            val result = mutableListOf<ModuleStatus>()
            for (m in VagCatalog.modules) {
                _status.value = "Consultando: ${m.name}…"
                selectModule(r, m)
                // Pedimos el nombre del sistema / VIN para confirmar presencia.
                val resp = runCatching { r.sendRaw("22 F197") }.getOrElse { "" }
                val reading = UdsInterpreter.interpret(resp, "22")
                val ascii = UdsInterpreter.hexToAscii(resp)
                val present = reading.ok || ascii != null
                val detail = when {
                    ascii != null && ascii.length > 2 -> "Identificado como: $ascii"
                    reading.ok -> "Responde correctamente."
                    else -> reading.plain
                }
                result.add(ModuleStatus(m.address, m.name, present, detail))
                _modules.value = result.toList()
            }
            val n = result.count { it.present }
            _status.value = "Escaneo terminado: $n de ${result.size} módulos respondieron."
            _busy.value = false
        }
    }

    /** Lee el VIN del auto y lo muestra en texto. */
    fun readVin() = oneShot("Leyendo número de serie (VIN)…") { r ->
        r.sendRaw("AT SH 7E0")
        val resp = r.sendRaw("09 02")
        val ascii = UdsInterpreter.hexToAscii(resp)
        if (ascii != null && ascii.length >= 10) "VIN del vehículo: ${ascii.takeLast(17)}"
        else "No se pudo leer el VIN. Respuesta: ${UdsInterpreter.interpret(resp).plain}"
    }

    /** Lee el estado actual de todos los interruptores de funciones. */
    fun loadFeatures() {
        val r = repo() ?: return
        if (_busy.value) return
        _busy.value = true
        _status.value = "Leyendo configuración actual del auto…"
        viewModelScope.launch {
            val states = mutableListOf<FeatureState>()
            for (f in VagCatalog.features) {
                val mod = VagCatalog.moduleByAddress(f.moduleAddress)
                if (mod == null) { states.add(FeatureState(f, false, false, "Módulo desconocido.")); continue }
                selectModule(r, mod)
                runCatching { r.sendRaw("10 03") } // sesión extendida
                val resp = runCatching { r.sendRaw("22 ${f.didRead}") }.getOrElse { "" }
                val block = parseBlock(resp, f.didRead)
                val state = if (block != null && f.byteIndex < block.size) {
                    val bitSet = (block[f.byteIndex] and f.bitMask) != 0
                    val enabled = if (f.onMeansSet) bitSet else !bitSet
                    FeatureState(f, true, enabled, if (enabled) "Activado ahora" else "Desactivado ahora")
                } else {
                    FeatureState(f, false, false, "Tu modelo no expone esta función o no respondió.")
                }
                states.add(state)
                _features.value = states.toList()
            }
            _status.value = "Configuración leída. Toca un interruptor para cambiarlo."
            _busy.value = false
        }
    }

    /**
     * Cambia una función: lee el bloque, voltea el bit y lo reescribe. Si la ECU
     * pide acceso de seguridad, lo explica en texto plano (sin dejar al usuario
     * con un código hex).
     */
    fun toggleFeature(f: VagCatalog.Feature, turnOn: Boolean) {
        val r = repo() ?: return
        if (_busy.value) return
        val mod = VagCatalog.moduleByAddress(f.moduleAddress) ?: return
        _busy.value = true
        _status.value = "Aplicando: ${f.name} → ${if (turnOn) "ACTIVAR" else "DESACTIVAR"}…"
        viewModelScope.launch {
            selectModule(r, mod)
            runCatching { r.sendRaw("10 03") }
            // Acceso de seguridad con el login conocido del módulo (o el que pegó el usuario).
            val secNote = attemptSecurityAccess(r, mod)
            val resp = runCatching { r.sendRaw("22 ${f.didRead}") }.getOrElse { "" }
            val block = parseBlock(resp, f.didRead)?.toMutableList()
            if (block == null || f.byteIndex >= block.size) {
                _status.value = "No se pudo leer la configuración para modificarla con seguridad. Cambio cancelado."
                _busy.value = false; return@launch
            }
            val setBit = if (f.onMeansSet) turnOn else !turnOn
            block[f.byteIndex] = if (setBit) block[f.byteIndex] or f.bitMask
            else block[f.byteIndex] and f.bitMask.inv()
            val payload = block.joinToString(" ") { "%02X".format(it and 0xFF) }
            val write = runCatching { r.sendRaw("2E ${f.didRead} $payload") }.getOrElse { "" }
            val reading = UdsInterpreter.interpret(write, "2E")
            _status.value = if (reading.ok) "✓ Listo: '${f.name}' quedó ${if (turnOn) "activado" else "desactivado"}."
            else "No se pudo cambiar '${f.name}'. ${reading.plain}$secNote"
            loadFeatures()
            _busy.value = false
        }
    }

    /**
     * Acceso de seguridad UDS estático (servicio 0x27 nivel 1/2), como hace VCDS
     * para codificar en MQB pre-SFD. Usa el login conocido del módulo (ej. 09 →
     * 31347) o, si el usuario pegó uno, ese. Pide la semilla (27 01), codifica el
     * login en big-endian con el mismo número de bytes que la semilla y lo envía
     * (27 02). UN SOLO intento: si falla, NO reintenta (así no se dispara el
     * bloqueo de ~20 min por exceso de intentos).
     */
    private suspend fun attemptSecurityAccess(r: com.autodiag.data.ObdRepository, mod: VagCatalog.Module): String {
        // Prioridad: código pegado por el usuario; si no, login conocido del módulo.
        val userCode = _securityCode.value.replace(Regex("[^0-9]"), "").toIntOrNull()
        val login = userCode ?: mod.login
        if (login == null) return "  (Sin código de seguridad para este módulo: se intentó escribir directo.)"

        val seedResp = runCatching { r.sendRaw("27 01") }.getOrElse { "" }
        val seed = UdsInterpreter.interpret(seedResp, "27")
        if (!seed.ok) return "  (No se obtuvo la semilla: ${seed.plain})"
        // Si la semilla es toda ceros, el módulo ya está desbloqueado.
        val seedBytes = parseSeedBytes(seedResp)
        if (seedBytes != null && seedBytes.all { it == 0 }) return "  (El módulo ya estaba desbloqueado ✓)"

        val width = (seedBytes?.size ?: 3).coerceIn(2, 4)
        val key = (0 until width).joinToString(" ") { i ->
            "%02X".format((login shr (8 * (width - 1 - i))) and 0xFF)
        }
        val keyResp = runCatching { r.sendRaw("27 02 $key") }.getOrElse { "" }
        val res = UdsInterpreter.interpret(keyResp, "27")
        return if (res.ok) "  (Acceso de seguridad concedido con login $login ✓)"
        else "  (Login $login no aceptado: ${res.plain} — no se reintenta para no bloquear el módulo)"
    }

    /**
     * Selecciona un módulo en el ELM327. No basta con poner el header de envío
     * (AT SH): hay que decirle al adaptador qué ID CAN aceptar de respuesta
     * (AT CRA), si no filtra todo lo que no sea 7E8-7EF y los módulos como 09/17
     * (que responden en 778/77E) parecen "no responder". También configuramos el
     * control de flujo ISO-TP para respuestas multi-trama (codificaciones largas).
     */
    private suspend fun selectModule(r: com.autodiag.data.ObdRepository, mod: VagCatalog.Module) {
        runCatching { r.sendRaw("AT SH ${mod.reqId}") }
        runCatching { r.sendRaw("AT CRA ${mod.rspId}") }
        runCatching { r.sendRaw("AT FC SH ${mod.reqId}") }
        runCatching { r.sendRaw("AT FC SD 30 00 00") }
        runCatching { r.sendRaw("AT FC SM 1") }
    }

    /** Extrae los bytes de semilla de una respuesta '67 01 <seed...>'. */
    private fun parseSeedBytes(raw: String): List<Int>? {
        val hex = raw.uppercase().replace(Regex("[^0-9A-F]"), "")
        val pos = hex.indexOf("6701")
        if (pos < 0) return null
        val data = hex.substring(pos + 4)
        if (data.length < 2) return null
        return data.chunked(2).mapNotNull { it.toIntOrNull(16) }
    }

    /** Extrae los bytes de datos de una respuesta '62 <did> <bytes...>'. */
    private fun parseBlock(raw: String, did: String): List<Int>? {
        val hex = raw.uppercase().replace(Regex("[^0-9A-F]"), "")
        val didClean = did.uppercase().replace(" ", "")
        // Respuesta positiva a 22 es 62; busca 62 + DID y toma lo que sigue.
        val marker = "62$didClean"
        val pos = hex.indexOf(marker)
        if (pos < 0) return null
        val dataHex = hex.substring(pos + marker.length)
        if (dataHex.length < 2) return null
        return dataHex.chunked(2).mapNotNull { it.toIntOrNull(16) }
    }

    private fun oneShot(msg: String, block: suspend (com.autodiag.data.ObdRepository) -> String) {
        val r = repo() ?: return
        if (_busy.value) return
        _busy.value = true
        _status.value = msg
        viewModelScope.launch {
            _status.value = runCatching { block(r) }.getOrElse { "Error: ${it.message}" }
            _busy.value = false
        }
    }
}
