package com.autodiag.data

data class PidInfo(val code: String, val name: String, val unit: String, val decode: (IntArray) -> Double)

object PidDecoder {
    val PIDS = mapOf(
        "010C" to PidInfo("010C", "rpm",         "rpm")   { d -> (d[0] * 256 + d[1]) / 4.0 },
        "0105" to PidInfo("0105", "coolant_temp", "°C")   { d -> (d[0] - 40).toDouble() },
        "010D" to PidInfo("010D", "speed",        "km/h") { d -> d[0].toDouble() },
        "0111" to PidInfo("0111", "throttle",     "%")    { d -> d[0] * 100.0 / 255 },
        "010F" to PidInfo("010F", "intake_temp",  "°C")   { d -> (d[0] - 40).toDouble() },
        "0104" to PidInfo("0104", "engine_load",  "%")    { d -> d[0] * 100.0 / 255 },
        "0110" to PidInfo("0110", "maf",          "g/s")  { d -> (d[0] * 256 + d[1]) / 100.0 },
        "012F" to PidInfo("012F", "fuel_level",   "%")    { d -> d[0] * 100.0 / 255 },
        "0142" to PidInfo("0142", "voltage",      "V")    { d -> (d[0] * 256 + d[1]) / 1000.0 },
    )

    /**
     * Decodifica una respuesta cruda del ELM327. Es tolerante a:
     *  - Eco del comando activado (ATE0 no aplicado): "010C\r41 0C 0F A0".
     *  - Respuestas con o sin espacios: "41 0C 0F A0" o "410C0FA0".
     *  - Líneas de ruido: "SEARCHING...", "NO DATA", "STOPPED", "BUS INIT".
     *  - Saltos de línea CR/LF intercalados.
     *
     * El truco clave es localizar el byte de respuesta de modo 1 (0x41) en
     * cualquier posición, en lugar de exigir que sea el primer token.
     */
    fun decodeResponse(raw: String): Pair<String, Double>? {
        // Quitar todo lo que no sea hex y juntar en una sola cadena continua.
        val cleaned = raw.uppercase()
            .replace("SEARCHING", " ")
            .replace("NODATA", " ")
            .replace("STOPPED", " ")
            .replace("BUSINIT", " ")
        // Tomar solo caracteres hexadecimales, descartando espacios/CR/LF/eco.
        val hex = cleaned.filter { it in "0123456789ABCDEF" }
        if (hex.length < 6) return null // mínimo: 41 + PID + 1 byte

        // Buscar "41" (respuesta a modo 01) seguido de un PID conocido.
        var i = 0
        while (i + 4 <= hex.length) {
            if (hex[i] == '4' && hex[i + 1] == '1') {
                val pidByte = hex.substring(i + 2, i + 4)
                val pidCode = "01$pidByte"
                val info = PIDS[pidCode]
                if (info != null) {
                    val dataHex = hex.substring(i + 4)
                    val data = dataHex.chunked(2)
                        .mapNotNull { if (it.length == 2) it.toIntOrNull(16) else null }
                        .toIntArray()
                    if (data.isNotEmpty()) {
                        return runCatching { Pair(info.name, info.decode(data)) }.getOrNull()
                    }
                }
            }
            i += 2 // avanzar de byte en byte
        }
        return null
    }
}
