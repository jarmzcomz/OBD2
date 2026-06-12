package com.autodiag.data

/**
 * Decodifica respuestas del modo 09 (información del vehículo): VIN (PID 02),
 * Calibration IDs (PID 04), etc. Estas respuestas son ASCII y suelen llegar en
 * varias tramas ISO-TP; el ELM327 las devuelve como múltiples líneas con
 * posibles marcadores de índice ("0:", "1:") y el header "49 0X".
 */
object Mode09Decoder {

    /** Caracteres válidos en un VIN (SAE J272: sin I, O, Q). */
    private const val VIN_CHARS = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789"

    /** Extrae el VIN (17 chars) de la respuesta cruda al comando "0902". */
    fun decodeVin(raw: String): String? {
        val ascii = extractAscii(raw, headerPid = "0902")
        // Filtrar solo caracteres válidos de VIN y quedarnos con 17.
        val vin = ascii.filter { it.uppercaseChar() in VIN_CHARS }
        return if (vin.length >= 17) vin.takeLast(17) else vin.ifBlank { null }
    }

    /** Extrae los Calibration IDs de la respuesta a "0904". */
    fun decodeCalibrationId(raw: String): String? {
        val ascii = extractAscii(raw, headerPid = "0904").trim()
        return ascii.ifBlank { null }
    }

    /**
     * Toma una respuesta cruda de modo 09, localiza el header "49 0X", descarta
     * el byte de conteo y convierte los bytes restantes a ASCII imprimible.
     */
    private fun extractAscii(raw: String, headerPid: String): String {
        // headerPid "0902" -> el header de respuesta es "4902".
        val responseHeader = "49" + headerPid.takeLast(2)
        val sb = StringBuilder()

        raw.split('\r', '\n').forEach { line ->
            val hex = line.uppercase().filter { it in "0123456789ABCDEF" }
            var idx = hex.indexOf(responseHeader)
            if (idx < 0) {
                // Línea de continuación (sin header): convertir todo a ASCII.
                appendAscii(hex, sb)
            } else {
                // Tras "49 0X" hay 1 byte de conteo; saltarlo y tomar el resto.
                val start = idx + responseHeader.length + 2
                if (start <= hex.length) appendAscii(hex.substring(start), sb)
            }
        }
        return sb.toString()
    }

    private fun appendAscii(hex: String, sb: StringBuilder) {
        var i = 0
        while (i + 2 <= hex.length) {
            val code = hex.substring(i, i + 2).toIntOrNull(16)
            i += 2
            if (code != null && code in 0x20..0x7E) sb.append(code.toChar())
        }
    }
}
