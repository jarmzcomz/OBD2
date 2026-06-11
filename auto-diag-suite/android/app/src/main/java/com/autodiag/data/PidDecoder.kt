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

    fun decodeResponse(raw: String): Pair<String, Double>? {
        val tokens = raw.trim().uppercase().split("\\s+".toRegex())
        if (tokens.size < 3 || tokens[0] != "41") return null
        val pidCode = "01${tokens[1]}"
        val info = PIDS[pidCode] ?: return null
        val data = tokens.drop(2).mapNotNull { it.toIntOrNull(16) }.toIntArray()
        return Pair(info.name, info.decode(data))
    }
}
