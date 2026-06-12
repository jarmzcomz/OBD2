package com.autodiag.data

data class PidInfo(val code: String, val name: String, val unit: String, val decode: (IntArray) -> Double)

object PidDecoder {

    // ~35 PIDs estándar de modo 01 (SAE J1979). El nombre se usa como clave
    // para la metadata de visualización en model/PidData.kt.
    val PIDS = linkedMapOf(
        "0104" to PidInfo("0104", "engine_load",   "%")   { d -> d[0] * 100.0 / 255 },
        "0105" to PidInfo("0105", "coolant_temp",  "°C")  { d -> (d[0] - 40).toDouble() },
        "0106" to PidInfo("0106", "stft_b1",       "%")   { d -> (d[0] - 128) * 100.0 / 128 },
        "0107" to PidInfo("0107", "ltft_b1",       "%")   { d -> (d[0] - 128) * 100.0 / 128 },
        "0108" to PidInfo("0108", "stft_b2",       "%")   { d -> (d[0] - 128) * 100.0 / 128 },
        "0109" to PidInfo("0109", "ltft_b2",       "%")   { d -> (d[0] - 128) * 100.0 / 128 },
        "010A" to PidInfo("010A", "fuel_pressure", "kPa") { d -> d[0] * 3.0 },
        "010B" to PidInfo("010B", "map",           "kPa") { d -> d[0].toDouble() },
        "010C" to PidInfo("010C", "rpm",           "rpm") { d -> (d[0] * 256 + d[1]) / 4.0 },
        "010D" to PidInfo("010D", "speed",         "km/h"){ d -> d[0].toDouble() },
        "010E" to PidInfo("010E", "timing_adv",    "°")   { d -> d[0] / 2.0 - 64 },
        "010F" to PidInfo("010F", "intake_temp",   "°C")  { d -> (d[0] - 40).toDouble() },
        "0110" to PidInfo("0110", "maf",           "g/s") { d -> (d[0] * 256 + d[1]) / 100.0 },
        "0111" to PidInfo("0111", "throttle",      "%")   { d -> d[0] * 100.0 / 255 },
        "0114" to PidInfo("0114", "o2_b1s1",       "V")   { d -> d[0] / 200.0 },
        "0115" to PidInfo("0115", "o2_b1s2",       "V")   { d -> d[0] / 200.0 },
        "011F" to PidInfo("011F", "run_time",      "s")   { d -> (d[0] * 256 + d[1]).toDouble() },
        "0121" to PidInfo("0121", "dist_mil",      "km")  { d -> (d[0] * 256 + d[1]).toDouble() },
        "0123" to PidInfo("0123", "fuel_rail_p",   "kPa") { d -> (d[0] * 256 + d[1]) * 10.0 },
        "012C" to PidInfo("012C", "egr",           "%")   { d -> d[0] * 100.0 / 255 },
        "012F" to PidInfo("012F", "fuel_level",    "%")   { d -> d[0] * 100.0 / 255 },
        "0131" to PidInfo("0131", "dist_clear",    "km")  { d -> (d[0] * 256 + d[1]).toDouble() },
        "0133" to PidInfo("0133", "baro",          "kPa") { d -> d[0].toDouble() },
        "0142" to PidInfo("0142", "voltage",       "V")   { d -> (d[0] * 256 + d[1]) / 1000.0 },
        "0143" to PidInfo("0143", "abs_load",      "%")   { d -> (d[0] * 256 + d[1]) * 100.0 / 255 },
        "0144" to PidInfo("0144", "afr",           "λ")   { d -> (d[0] * 256 + d[1]) * 2.0 / 65536 },
        "0145" to PidInfo("0145", "rel_throttle",  "%")   { d -> d[0] * 100.0 / 255 },
        "0146" to PidInfo("0146", "ambient_temp",  "°C")  { d -> (d[0] - 40).toDouble() },
        "0147" to PidInfo("0147", "abs_throttle_b","%")   { d -> d[0] * 100.0 / 255 },
        "0149" to PidInfo("0149", "accel_pedal_d", "%")   { d -> d[0] * 100.0 / 255 },
        "014C" to PidInfo("014C", "cmd_throttle",  "%")   { d -> d[0] * 100.0 / 255 },
        "015C" to PidInfo("015C", "oil_temp",      "°C")  { d -> (d[0] - 40).toDouble() },
        "015E" to PidInfo("015E", "fuel_rate",     "L/h") { d -> (d[0] * 256 + d[1]) / 20.0 },
    )

    /**
     * Decodifica una respuesta cruda del ELM327 (modo 01). Tolerante a:
     *  - Eco del comando activado: "010C\r41 0C 0F A0".
     *  - Respuestas con o sin espacios: "41 0C 0F A0" o "410C0FA0".
     *  - Ruido: "SEARCHING...", "NO DATA", "STOPPED", "BUS INIT".
     * Localiza el byte de respuesta 0x41 en cualquier posición y mapea el PID.
     */
    fun decodeResponse(raw: String): Pair<String, Double>? {
        val cleaned = raw.uppercase()
            .replace("SEARCHING", " ")
            .replace("NODATA", " ")
            .replace("STOPPED", " ")
            .replace("BUSINIT", " ")
        val hex = cleaned.filter { it in "0123456789ABCDEF" }
        if (hex.length < 6) return null

        var i = 0
        while (i + 4 <= hex.length) {
            if (hex[i] == '4' && hex[i + 1] == '1') {
                val pidByte = hex.substring(i + 2, i + 4)
                val info = PIDS["01$pidByte"]
                if (info != null) {
                    val data = hex.substring(i + 4).chunked(2)
                        .mapNotNull { if (it.length == 2) it.toIntOrNull(16) else null }
                        .toIntArray()
                    if (data.isNotEmpty()) {
                        return runCatching { Pair(info.name, info.decode(data)) }.getOrNull()
                    }
                }
            }
            i += 2
        }
        return null
    }
}
