package com.autodiag.data

/**
 * Resultado de un intento de acceso de seguridad UDS (servicio 0x27).
 */
sealed class SecurityResult {
    /** Módulo desbloqueado: se aceptó la clave (o ya venía desbloqueado con semilla en ceros). */
    data class Unlocked(val level: Int, val seed: List<Int>, val key: String) : SecurityResult()
    /** El módulo entregó semilla pero rechazó la clave (NRC, típicamente 35 = invalidKey). */
    data class KeyRejected(val level: Int, val seed: List<Int>, val key: String, val nrc: String) : SecurityResult()
    /** Ningún nivel entregó semilla y TODOS respondieron 7F 27 11 → módulo bajo SFD. */
    data class SfdLocked(val triedLevels: List<Int>) : SecurityResult()
    /** Ningún nivel entregó semilla, pero no fue el patrón SFD. */
    data class NoSeed(val detail: String) : SecurityResult()
}

/**
 * Lógica PURA del acceso de seguridad UDS + escritura, SIN dependencias de Android,
 * para poder probarla end-to-end contra un módulo simulado.
 *
 * Recibe [send]: una función que manda un comando (ej. "27 03") y devuelve la
 * respuesta cruda del ELM327 (ej. "67 03 09 2D B7 8D"). Es el MISMO camino que usa
 * la app en el auto real; aislarlo aquí permite verificar toda la cadena
 * semilla → clave → escritura sin hardware.
 */
class UdsSecurity(
    private val send: suspend (String) -> String,
    private val levels: List<Int> = DEFAULT_LEVELS
) {

    /**
     * Intenta desbloquear el módulo probando cada nivel de seguridad. En cuanto un
     * nivel entrega semilla, calcula la clave (con el bytecode SA2 si se provee, o el
     * login estático) y la envía. Devuelve el [SecurityResult] correspondiente.
     */
    suspend fun unlock(login: Int, bytecodeHex: String = ""): SecurityResult {
        var allServiceNotSupported = true
        for (level in levels) {
            val seedResp = send("27 %02X".format(level))
            val seed = parseSeed(seedResp, level)
            if (seed == null) {
                if (nrcOf(seedResp, "7F27") != "11") allServiceNotSupported = false
                continue
            }
            if (seed.all { it == 0 }) return SecurityResult.Unlocked(level, seed, "")
            val key = computeKey(seed, login, bytecodeHex)
            val keyResp = send("27 %02X %s".format(level + 1, key))
            val kh = keyResp.uppercase().replace(NON_HEX, "")
            return if (kh.contains("67%02X".format(level + 1))) {
                SecurityResult.Unlocked(level, seed, key)
            } else {
                SecurityResult.KeyRejected(level, seed, key, nrcOf(keyResp, "7F27"))
            }
        }
        return if (allServiceNotSupported) SecurityResult.SfdLocked(levels)
        else SecurityResult.NoSeed("ningún nivel entregó semilla")
    }

    /** Escribe un DID por servicio 0x2E. Devuelve true si la respuesta es positiva (6E <did>). */
    suspend fun writeDid(did: String, valueBytes: String): Boolean {
        val resp = send("2E $did $valueBytes")
        val hex = resp.uppercase().replace(NON_HEX, "")
        return hex.contains("6E" + did.uppercase().replace(" ", ""))
    }

    companion object {
        val DEFAULT_LEVELS = listOf(0x03, 0x01, 0x05, 0x07)
        private val NON_HEX = Regex("[^0-9A-F]")

        /** Extrae los bytes de semilla de '67 <nivel> <seed...>' o null si es negativa. */
        fun parseSeed(raw: String, level: Int): List<Int>? {
            val hex = raw.uppercase().replace(NON_HEX, "")
            val marker = "67" + "%02X".format(level)
            val pos = hex.indexOf(marker)
            if (pos < 0) return null
            val data = hex.substring(pos + marker.length)
            if (data.length < 2) return null
            return data.chunked(2).mapNotNull { it.toIntOrNull(16) }
        }

        /**
         * Calcula la clave. Si [bytecodeHex] trae un bytecode SA2 válido, ejecuta la VM
         * SA2 (algoritmo real MQB) sobre la semilla y devuelve 4 bytes. Si no, usa el
         * login estático en big-endian con el ancho de la semilla (método legacy).
         */
        fun computeKey(seed: List<Int>, login: Int, bytecodeHex: String): String {
            val bc = Sa2SeedKey.parseBytecode(bytecodeHex)
            if (bc != null) {
                val k = Sa2SeedKey(bc).execute(seed)
                return (0..3).joinToString(" ") { i -> "%02X".format(((k shr (24 - i * 8)) and 0xFF).toInt()) }
            }
            val w = seed.size.coerceIn(2, 4)
            return (0 until w).joinToString(" ") { i -> "%02X".format((login shr (8 * (w - 1 - i))) and 0xFF) }
        }

        /** Devuelve el NRC (2 hex) de una respuesta negativa que empieza por [prefix]. */
        private fun nrcOf(raw: String, prefix: String): String {
            val hex = raw.uppercase().replace(NON_HEX, "")
            val idx = hex.indexOf(prefix)
            return if (idx >= 0 && hex.length >= idx + 6) hex.substring(idx + 4, idx + 6) else ""
        }
    }
}
