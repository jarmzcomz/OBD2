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
     *
     * Las respuestas multi-trama del ELM327 llegan con un prefijo de índice de
     * trama ("0:", "1:", "2:") y a veces una línea de longitud total ("014").
     * Hay que QUITAR ese prefijo "N:" de cada línea ANTES de juntar el hex, o el
     * dígito del contador se mezcla con los bytes y desalinea todo el VIN.
     */
    private fun extractAscii(raw: String, headerPid: String): String {
        // headerPid "0902" -> el header de respuesta es "4902".
        val responseHeader = "49" + headerPid.takeLast(2)
        // Junta TODO el hex de las tramas (sin headers de índice) en un solo bloque.
        val joined = StringBuilder()

        raw.split('\r', '\n').forEach { rawLine ->
            // Quita el prefijo de índice de trama tipo "0:" o "1:" (dígito hex + ':').
            var line = rawLine.trim()
            val colon = line.indexOf(':')
            if (colon in 1..2 && line.substring(0, colon).all { it.isDigit() || it in "ABCDEFabcdef" }) {
                line = line.substring(colon + 1)
            }
            val hex = line.uppercase().filter { it in "0123456789ABCDEF" }
            // Ignora líneas que son solo el contador de longitud total (ej. "014").
            if (hex.length <= 3 && hex.indexOf(responseHeader) < 0) return@forEach
            joined.append(hex)
        }

        // En el bloque completo, localiza CADA header "4902 <count>" y toma el
        // ASCII que sigue. Algunos adaptadores repiten el header por trama.
        val full = joined.toString()
        val sb = StringBuilder()
        var search = 0
        var found = false
        while (true) {
            val idx = full.indexOf(responseHeader, search)
            if (idx < 0) break
            found = true
            // Tras "49 0X" hay 1 byte de conteo (solo en la 1ª aparición); saltarlo.
            val start = idx + responseHeader.length + 2
            val end = full.indexOf(responseHeader, start).let { if (it < 0) full.length else it }
            if (start <= full.length) appendAscii(full.substring(start, end), sb)
            search = end
        }
        // Si nunca apareció el header (adaptador ya lo quitó), convierte todo.
        if (!found) appendAscii(full, sb)
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
