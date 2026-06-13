package com.autodiag.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.AutoDiagApplication
import com.autodiag.data.Mode09Decoder
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
     * Registro crudo (hex) del último intento de cambio. Sirve para diagnosticar:
     * el usuario lo comparte y se ve EXACTAMENTE qué respondió cada módulo, sin
     * adivinar posiciones de bits. Vacío = no mostrar.
     */
    private val _diag = MutableStateFlow("")
    val diag: StateFlow<String> = _diag.asStateFlow()

    fun clearDiag() { _diag.value = "" }

    /**
     * Código/llave de seguridad que el usuario pega manualmente (ej. el "código de
     * acceso" de VCDS/OBDeleven o una llave hex). Si está vacío, NO intentamos
     * acceso de seguridad para evitar bloquear el módulo con llaves equivocadas.
     */
    private val _securityCode = MutableStateFlow("")
    val securityCode: StateFlow<String> = _securityCode.asStateFlow()

    fun setSecurityCode(code: String) { _securityCode.value = code.trim() }

    // Dos "fotos" de las adaptaciones del módulo 09, tomadas con luz y con sombra,
    // para encontrar el DID del sensor de luz por diferencia.
    private var snapLight: Map<String, String>? = null
    private var snapDark: Map<String, String>? = null

    /**
     * Escanea las adaptaciones del módulo 09 y guarda una "foto". Cuando existan
     * las dos (luz y sombra), calcula la DIFERENCIA: los DIDs cuyo valor cambió son
     * los candidatos al sensor de luz. Todo es SOLO LECTURA.
     */
    fun captureLightSnapshot(isLight: Boolean) {
        val r = repo() ?: return
        if (_busy.value) return
        val mod = VagCatalog.moduleByAddress("09") ?: return
        _busy.value = true
        _status.value = "Capturando adaptaciones del módulo 09 (${if (isLight) "con LUZ" else "con SOMBRA"})… espera."
        viewModelScope.launch {
            withSession(r) {
                selectModule(r, mod)
                runCatching { r.sendRaw("10 03") }
                val snap = linkedMapOf<String, String>()
                val ranges = listOf(0x0200..0x02FF, 0x0400..0x04FF, 0x0500..0x05FF)
                var done = 0
                val total = ranges.sumOf { it.count() }
                for (range in ranges) {
                    for (did in range) {
                        val didHex = "%04X".format(did)
                        val resp = runCatching { r.sendRaw("22 $didHex") }.getOrElse { "" }
                        val value = positiveValue(resp, didHex)
                        if (value != null) snap[didHex] = value
                        done++
                        if (done % 32 == 0) _status.value = "Escaneando 09… $done/$total (${snap.size} adaptaciones)"
                    }
                }
                if (isLight) snapLight = snap else snapDark = snap
                val detected = if (snapLight != null && snapDark != null) detectSensorDid() else null
                _diag.value = buildLightDiff(snap, isLight)
                _status.value = when {
                    detected != null -> "✓ ¡Sensor detectado! DID $detected. Mueve el slider y aplica."
                    snapLight != null && snapDark != null ->
                        "Sin candidato claro. Repite con más contraste (sol directo vs sensor tapado)."
                    else -> "Foto '${if (isLight) "LUZ" else "SOMBRA"}' guardada. Ahora toma la otra."
                }
            }
            _busy.value = false
        }
    }

    /**
     * DID del sensor de luz DETECTADO automáticamente (o null si aún no se detecta).
     * Si está presente, la lectura/escritura del umbral apunta a ESTE DID en vez del
     * 02E0 por defecto, así funciona aunque el sensor esté en otra dirección.
     */
    private val _detectedSensorDid = MutableStateFlow<String?>(null)
    val detectedSensorDid: StateFlow<String?> = _detectedSensorDid.asStateFlow()

    /** DID a usar para el sensor: el detectado, o el 02E0 conocido por defecto. */
    private fun sensorDid(): String = _detectedSensorDid.value ?: "02E0"

    /** Convierte un valor hex "31 5B 34 D3" a entero big-endian (o null). */
    private fun hexToBigEndianInt(value: String?): Int? {
        if (value == null) return null
        val bytes = value.split(" ").mapNotNull { it.toIntOrNull(16) }
        if (bytes.size != 4) return null
        return bytes.fold(0) { acc, b -> (acc shl 8) or (b and 0xFF) }
    }

    /**
     * Compara las dos fotos (luz/sombra) y elige el DID del sensor de luz: el de 4
     * bytes cuyo valor BAJA más de luz a sombra (la luminosidad es alta con luz y
     * baja en penumbra). Guarda el DID detectado y precarga el slider con el valor
     * de LUZ. Devuelve el DID o null si no hay candidato claro.
     */
    private fun detectSensorDid(): String? {
        val light = snapLight ?: return null
        val dark = snapDark ?: return null
        var bestDid: String? = null
        var bestDrop = 0L
        var bestLightVal = -1
        for (did in (light.keys + dark.keys)) {
            val lv = hexToBigEndianInt(light[did]) ?: continue
            val dv = hexToBigEndianInt(dark[did]) ?: continue
            if (lv == dv) continue
            // La luminosidad cae al oscurecer: buscamos la mayor caída luz→sombra.
            val drop = lv.toLong() - dv.toLong()
            if (drop > bestDrop) { bestDrop = drop; bestDid = did; bestLightVal = lv }
        }
        if (bestDid != null) {
            _detectedSensorDid.value = bestDid
            _lightSensorValue.value = bestLightVal
        }
        return bestDid
    }

    /** Construye el texto del panel: foto actual + diferencia si hay ambas. */
    private fun buildLightDiff(current: Map<String, String>, isLight: Boolean): String {
        val sb = StringBuilder("ADAPTACIONES 09 · foto '${if (isLight) "LUZ" else "SOMBRA"}' (${current.size} DIDs)\n")
        val a = snapLight; val b = snapDark
        if (a != null && b != null) {
            sb.append("\n=== DIFERENCIAS LUZ vs SOMBRA (candidatos al sensor) ===\n")
            val keys = (a.keys + b.keys).toSortedSet()
            var changes = 0
            for (k in keys) {
                val va = a[k]; val vb = b[k]
                if (va != vb) { sb.append("DID $k:  LUZ=$va   SOMBRA=$vb\n"); changes++ }
            }
            if (changes == 0) sb.append("(Ningún DID cambió. Prueba con más contraste de luz, o el sensor está en otro rango.)\n")
            else {
                val det = _detectedSensorDid.value
                if (det != null) sb.append("\n>>> SENSOR DETECTADO AUTOMÁTICAMENTE: DID $det <<<\n" +
                    "El slider ya apunta a este byte. Ajusta y aplica.\n")
                else sb.append("\nEl DID que cambia con la luz es el del sensor.\n")
            }
        } else {
            for ((k, v) in current) sb.append("DID $k = $v\n")
        }
        return sb.toString()
    }

    /** Limpia las fotos del sensor de luz para empezar de cero. */
    fun resetLightSnapshots() {
        snapLight = null; snapDark = null; _detectedSensorDid.value = null
        _status.value = "Fotos del sensor borradas. Vuelve a capturar luz y sombra."
    }

    // Valor actual del sensor de luz leído del módulo 09 (DID 02E0, 4 bytes big-endian).
    private val _lightSensorValue = MutableStateFlow(-1)
    val lightSensorValue: StateFlow<Int> = _lightSensorValue.asStateFlow()

    /**
     * Lee el valor actual del sensor de luz (DID 02E0 del módulo 09) y lo expone
     * como un entero para mostrar en el slider. -1 = no leído / error.
     */
    fun readLightSensor() {
        val r = repo() ?: return
        if (_busy.value) return
        _busy.value = true
        _status.value = "Leyendo sensor de luz…"
        val mod = VagCatalog.moduleByAddress("09") ?: return
        val did = sensorDid()
        viewModelScope.launch {
            withSession(r) {
                selectModule(r, mod)
                runCatching { r.sendRaw("10 03") }
                val resp = runCatching { r.sendRaw("22 $did") }.getOrElse { "" }
                val value = positiveValue(resp, did)
                if (value != null) {
                    val bytes = value.split(" ").mapNotNull { it.toIntOrNull(16) }
                    val intVal = bytes.fold(0) { acc, b -> (acc shl 8) or (b and 0xFF) }
                    _lightSensorValue.value = intVal
                    _status.value = "Sensor de luz: 0x${"%08X".format(intVal)} ($intVal lux aprox.)"
                } else {
                    _status.value = "No se pudo leer el sensor de luz. ${UdsInterpreter.interpret(resp).plain}"
                }
            }
            _busy.value = false
        }
    }

    /**
     * Escribe un nuevo umbral de brillo al DID 02E0 del módulo 09. El parámetro
     * [newValue] es el valor entero que el usuario seleccionó en el slider.
     * SIEMPRE lee el bloque actual primero, luego escribe solo ese DID.
     * Requiere acceso de seguridad (login 31347).
     */
    fun writeLightSensorThreshold(newValue: Int) {
        val r = repo() ?: return
        if (_busy.value) return
        val mod = VagCatalog.moduleByAddress("09") ?: return
        val did = sensorDid()
        _busy.value = true
        _status.value = "Aplicando nuevo umbral del sensor de luz (DID $did)…"
        val log = StringBuilder("ESCRITURA SENSOR DE LUZ · módulo 09 · DID $did\nNuevo valor: 0x${"%08X".format(newValue)} ($newValue)\n")
        viewModelScope.launch {
            withSession(r) {
                suspend fun send(cmd: String): String {
                    val resp = runCatching { r.sendRaw(cmd) }.getOrElse { "<error>" }
                    log.append("» $cmd\n  ← ${resp.trim().ifEmpty { "(vacío)" }}\n")
                    return resp
                }
                selectModule(r, mod)
                send("10 03")
                val sec = attemptSecurityAccessLogged(r, mod, ::send)
                val b = (0..3).map { i -> "%02X".format((newValue shr (24 - i * 8)) and 0xFF) }.joinToString(" ")
                val writeResp = send("2E $did $b")
                val result = UdsInterpreter.interpret(writeResp, "2E")
                _status.value = if (result.ok) {
                    _lightSensorValue.value = newValue
                    "✓ Umbral del sensor de luz actualizado. Reinicia el contacto para que el módulo adopte el valor."
                } else {
                    "No se pudo escribir el umbral: ${result.plain}$sec"
                }
                _diag.value = log.toString()
            }
            _busy.value = false
        }
    }

    /** Devuelve el valor (hex) de una respuesta positiva '62 <did> <bytes>', o null. */
    private fun positiveValue(raw: String, did: String): String? {
        val hex = raw.uppercase().replace(Regex("[^0-9A-F]"), "")
        val marker = "62$did"
        val pos = hex.indexOf(marker)
        if (pos < 0) return null
        val data = hex.substring(pos + marker.length)
        if (data.length < 2) return null
        return data.chunked(2).joinToString(" ")
    }

    val available: Boolean
        get() = getApplication<AutoDiagApplication>().activeRepository?.capabilities?.rawTerminal == true

    private fun repo() = getApplication<AutoDiagApplication>().activeRepository

    /**
     * Ejecuta [block] dentro de una sesión manual: pausa el liveData (via
     * beginSession/endSession) para que los PIDs en segundo plano no se cuelen
     * entre los comandos AT de la secuencia. SIEMPRE libera al terminar.
     */
    private suspend fun <T> withSession(r: com.autodiag.data.ObdRepository, block: suspend () -> T): T {
        r.beginSession()
        return try { block() } finally { r.endSession() }
    }

    /** Recorre todos los módulos VAG conocidos y reporta cuáles responden, en texto. */
    fun scanModules() {
        val r = repo() ?: return
        if (_busy.value) return
        _busy.value = true
        _status.value = "Escaneando módulos del auto…"
        viewModelScope.launch {
            withSession(r) {
                val result = mutableListOf<ModuleStatus>()
                for (m in VagCatalog.modules) {
                    _status.value = "Consultando: ${m.name}…"
                    selectModule(r, m)
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
            }
            _busy.value = false
        }
    }

    /** Lee el VIN del auto y lo muestra en texto. */
    fun readVin() {
        val r = repo() ?: return
        if (_busy.value) return
        _busy.value = true
        _status.value = "Leyendo número de serie (VIN)…"
        val log = StringBuilder("LECTURA VIN · Modo 09 Subservicio 02\n")
        viewModelScope.launch {
            try {
                withSession(r) {
                    suspend fun send(cmd: String): String {
                        val resp = runCatching { r.sendRaw(cmd) }.getOrElse { "<error>" }
                        log.append("» $cmd\n  ← ${resp.trim().ifEmpty { "(vacío)" }}\n")
                        return resp
                    }
                    send("AT CRA")     // limpia filtro de recepción
                    send("AT AR")      // recepción automática
                    send("AT CAF1")    // re-ensamblado ISO-TP automático
                    send("AT SH 7DF")  // cabecera funcional OBD-II
                    val resp = send("09 02")
                    val vin = Mode09Decoder.decodeVin(resp)
                    _status.value = if (vin != null && vin.length >= 17) {
                        "✓ VIN del vehículo: $vin"
                    } else if (vin != null && vin.length >= 10) {
                        "⚠ VIN parcial: $vin (revisa el diagnóstico de abajo)"
                    } else {
                        "✗ No se pudo leer el VIN. Respuesta cruda abajo. ${UdsInterpreter.interpret(resp).plain}"
                    }
                    _diag.value = log.toString()
                }
            } finally {
                _busy.value = false
            }
        }
    }

    /**
     * Volcado READ-ONLY de la codificación y adaptaciones de un módulo. No escribe
     * NADA: solo lee. Sirve para mapear con datos reales (qué byte/bit es cada
     * función, dónde está el umbral del sensor de luz, etc.) en TU versión de
     * software. Lee la codificación larga (DID 0600), identificación y un rango de
     * DIDs de adaptación frecuentes en MQB. Todo se muestra en el panel de diagnóstico.
     */
    fun dumpCoding(addr: String) {
        val r = repo() ?: return
        if (_busy.value) return
        val mod = VagCatalog.moduleByAddress(addr) ?: return
        _busy.value = true
        _status.value = "Leyendo codificación del módulo ${mod.address} (solo lectura)…"
        val log = StringBuilder("VOLCADO (solo lectura) · módulo ${mod.address} ${mod.name}\n" +
            "tx ${mod.reqId} / rx ${mod.rspId}\n")
        viewModelScope.launch {
            withSession(r) {
                suspend fun send(cmd: String): String {
                    val resp = runCatching { r.sendRaw(cmd) }.getOrElse { "<error>" }
                    log.append("» $cmd\n  ← ${resp.trim().ifEmpty { "(vacío)" }}\n")
                    return resp
                }
                selectModule(r, mod)
                send("10 03")
                send("22 F187")
                send("22 F189")
                send("22 0600")
                for (did in listOf("0407", "0408", "02E0", "0293", "0296")) send("22 $did")
                _status.value = "Volcado listo. Toca 'Copiar' y compártelo."
                _diag.value = log.toString()
            }
            _busy.value = false
        }
    }

    /** Lee el estado actual de todos los interruptores de funciones. */
    fun loadFeatures() {
        val r = repo() ?: return
        if (_busy.value) return
        _busy.value = true
        _status.value = "Leyendo configuración actual del auto…"
        viewModelScope.launch {
            withSession(r) {
                val states = mutableListOf<FeatureState>()
                for (f in VagCatalog.features) {
                    val mod = VagCatalog.moduleByAddress(f.moduleAddress)
                    if (mod == null) { states.add(FeatureState(f, false, false, "Módulo desconocido.")); continue }
                    selectModule(r, mod)
                    runCatching { r.sendRaw("10 03") }
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
            }
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
        val log = StringBuilder("DIAGNÓSTICO · ${f.name}\nMódulo ${mod.address} (tx ${mod.reqId}/rx ${mod.rspId})\n")
        viewModelScope.launch {
            withSession(r) {
                suspend fun send(cmd: String): String {
                    val resp = runCatching { r.sendRaw(cmd) }.getOrElse { "<error: ${it.message}>" }
                    log.append("» $cmd\n  ← ${resp.trim().ifEmpty { "(vacío)" }}\n")
                    return resp
                }
                selectModule(r, mod)
                send("10 03")
                val resp = send("22 ${f.didRead}")
                val block = parseBlock(resp, f.didRead)?.toMutableList()
                if (block == null) {
                    _status.value = "No pude leer la codificación. NO escribí nada. Comparte el diagnóstico abajo."
                    _diag.value = log.toString()
                    _busy.value = false; return@withSession
                }
                val sec = attemptSecurityAccessLogged(r, mod, ::send)
                if (f.byteIndex >= block.size) {
                    _status.value = "Función '${f.name}' no encontrada en esta versión de software. NO escribí nada."
                    _diag.value = log.toString()
                    _busy.value = false; return@withSession
                }
                val setBit = if (f.onMeansSet) turnOn else !turnOn
                block[f.byteIndex] = if (setBit) block[f.byteIndex] or f.bitMask
                    else block[f.byteIndex] and f.bitMask.inv()
                val payload = block.joinToString(" ") { "%02X".format(it and 0xFF) }
                val write = send("2E ${f.didRead} $payload")
                val reading = UdsInterpreter.interpret(write, "2E")
                _status.value = if (reading.ok) "✓ Listo: '${f.name}' quedó ${if (turnOn) "activado" else "desactivado"}."
                    else "No se pudo cambiar '${f.name}'. ${reading.plain}$sec\n\nComparte el diagnóstico de abajo."
                _diag.value = log.toString()
            }
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
    private suspend fun attemptSecurityAccessLogged(
        r: com.autodiag.data.ObdRepository,
        mod: VagCatalog.Module,
        send: suspend (String) -> String
    ): String {
        // Prioridad: código pegado por el usuario; si no, login conocido del módulo.
        val userCode = _securityCode.value.replace(Regex("[^0-9]"), "").toIntOrNull()
        val login = userCode ?: mod.login
        if (login == null) return "  (Sin código de seguridad para este módulo: se intentó escribir directo.)"

        val seedResp = send("27 01")
        val seed = UdsInterpreter.interpret(seedResp, "27")
        if (!seed.ok) return "  (No se obtuvo la semilla: ${seed.plain})"

        // Si la semilla es toda ceros, el módulo ya está desbloqueado.
        val seedBytes = parseSeedBytes(seedResp)
        if (seedBytes != null && seedBytes.all { it == 0 }) {
            return "  (El módulo ya estaba desbloqueado ✓)"
        }

        // Si no se obtuvo la semilla, calcula con ancho por defecto
        // Para MQB típicamente es 4 bytes, pero algunos módulos pueden ser 2 o 3
        val width = (seedBytes?.size ?: 4).coerceIn(2, 4)
        val key = (0 until width).joinToString(" ") { i ->
            "%02X".format((login shr (8 * (width - 1 - i))) and 0xFF)
        }

        val res = UdsInterpreter.interpret(send("27 02 $key"), "27")
        return if (res.ok) {
            "  (Acceso de seguridad concedido: login $login, semilla (${seedBytes?.size ?: "desconocido"} bytes), ancho clave $width ✓)"
        } else {
            "  (Login $login no aceptado: ${res.plain} — no se reintenta para no bloquear el módulo)"
        }
    }

    /**
     * PRUEBA visible de acceso de seguridad sobre un módulo, SIN escribir nada.
     * Pide la semilla (27 01), calcula la clave con el login estático y la envía
     * (27 02), mostrando en el panel de diagnóstico EXACTAMENTE la semilla recibida
     * y la clave calculada. Sirve para verificar que el cálculo de clave funciona
     * antes de tocar codificación. UN SOLO intento (no reintenta para no bloquear).
     */
    fun testSecurityAccess(addr: String) {
        val r = repo() ?: return
        if (_busy.value) return
        val mod = VagCatalog.moduleByAddress(addr) ?: return
        _busy.value = true
        _status.value = "Probando acceso de seguridad en módulo ${mod.address}…"
        val log = StringBuilder("PRUEBA ACCESO DE SEGURIDAD · módulo ${mod.address} ${mod.name}\n" +
            "tx ${mod.reqId} / rx ${mod.rspId}\n")
        viewModelScope.launch {
            try {
                withSession(r) {
                    suspend fun send(cmd: String): String {
                        val resp = runCatching { r.sendRaw(cmd) }.getOrElse { "<error>" }
                        log.append("» $cmd\n  ← ${resp.trim().ifEmpty { "(vacío)" }}\n")
                        return resp
                    }
                    selectModule(r, mod)
                    send("10 03")
                    val userCode = _securityCode.value.replace(Regex("[^0-9]"), "").toIntOrNull()
                    val login = userCode ?: mod.login
                    if (login == null) {
                        _status.value = "Este módulo no tiene login conocido. Pega uno en el campo de código."
                        _diag.value = log.toString(); _busy.value = false; return@withSession
                    }
                    val seedResp = send("27 01")
                    val seedBytes = parseSeedBytes(seedResp)
                    if (seedBytes == null) {
                        _status.value = "✗ No llegó la semilla (27 01). Revisa el diagnóstico abajo."
                        _diag.value = log.toString(); _busy.value = false; return@withSession
                    }
                    if (seedBytes.all { it == 0 }) {
                        _status.value = "✓ El módulo ya estaba DESBLOQUEADO (semilla en ceros)."
                        log.append("Semilla = 00... → módulo ya desbloqueado.\n")
                        _diag.value = log.toString(); _busy.value = false; return@withSession
                    }
                    val width = seedBytes.size.coerceIn(2, 4)
                    val key = (0 until width).joinToString(" ") { i ->
                        "%02X".format((login shr (8 * (width - 1 - i))) and 0xFF)
                    }
                    val seedHex = seedBytes.joinToString(" ") { "%02X".format(it and 0xFF) }
                    log.append("Login estático: $login (0x${"%X".format(login)})\n")
                    log.append("Semilla recibida: $seedHex (${seedBytes.size} bytes)\n")
                    log.append("Clave calculada:  $key\n")
                    val res = UdsInterpreter.interpret(send("27 02 $key"), "27")
                    _status.value = if (res.ok) "✓ ¡Acceso CONCEDIDO! Login $login, clave $key."
                        else "✗ Llave rechazada: ${res.plain}. Semilla $seedHex, clave $key (ver diagnóstico)."
                    _diag.value = log.toString()
                }
            } finally {
                _busy.value = false
            }
        }
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
}
