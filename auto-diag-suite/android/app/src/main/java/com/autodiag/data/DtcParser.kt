package com.autodiag.data

/**
 * Parser correcto de respuestas de DTC para los modos 03 (almacenados), 07
 * (pendientes) y 0A (permanentes) del protocolo OBD-II / ELM327.
 *
 * El bug del parser anterior era tratar TODOS los pares de bytes como DTCs,
 * incluyendo el header de respuesta (0x43/0x47/0x4A) y el byte de conteo de
 * DTCs. Eso inventaba códigos falsos (p.ej. "P1008").
 *
 * Formato real de una respuesta multi-trama de modo 03 (con headers ATH0):
 *   43 02 01 33 00 00     -> "43" header, "02" = nº de DTCs, luego pares
 * o con direccionamiento de varias ECUs / ISO-TP el ELM327 puede devolir
 * líneas como:
 *   7E8 06 43 02 01 33 00 00
 * Este parser es tolerante: localiza el header de respuesta correcto, salta
 * el byte de conteo y decodifica solo pares de DTC válidos (descartando 00 00).
 */
object DtcParser {

    /** @param mode comando enviado: "03", "07" o "0A". */
    fun parse(raw: String, mode: String): List<String> {
        val responseHeader = when (mode.uppercase()) {
            "03" -> "43"
            "07" -> "47"
            "0A" -> "4A"
            else -> "43"
        }

        val result = LinkedHashSet<String>()

        // Procesar línea por línea: cada línea puede ser una trama de una ECU.
        raw.split('\r', '\n').forEach { line ->
            val hex = line.uppercase().filter { it in "0123456789ABCDEF" }
            if (hex.length < 4) return@forEach

            // Buscar el header de respuesta del modo en cualquier posición.
            var idx = hex.indexOf(responseHeader)
            while (idx >= 0) {
                // Después del header viene 1 byte de conteo, luego los pares DTC.
                val afterHeader = idx + 2
                if (afterHeader + 2 <= hex.length) {
                    // Saltar el byte de conteo de DTCs.
                    val dtcStart = afterHeader + 2
                    val dtcBytes = hex.substring(dtcStart)
                    parsePairs(dtcBytes).forEach { result.add(it) }
                }
                idx = hex.indexOf(responseHeader, idx + 2)
                // Solo el primer header relevante por línea suele bastar; seguir
                // por si hay tramas concatenadas no rompe (descarta 00 00).
            }
        }
        return result.toList()
    }

    private fun parsePairs(hex: String): List<String> {
        val codes = mutableListOf<String>()
        var i = 0
        while (i + 4 <= hex.length) {
            val a = hex.substring(i, i + 2).toIntOrNull(16)
            val b = hex.substring(i + 2, i + 4).toIntOrNull(16)
            i += 4
            if (a == null || b == null) continue
            if (a == 0 && b == 0) continue // relleno, no es DTC
            decodeDtc(a, b)?.let { codes.add(it) }
        }
        return codes
    }

    /** Convierte 2 bytes en un código tipo "P0301" según SAE J2012. */
    fun decodeDtc(a: Int, b: Int): String? {
        if (a == 0 && b == 0) return null
        val prefix = arrayOf("P", "C", "B", "U")[(a and 0xC0) shr 6]
        val d1 = (a and 0x30) shr 4
        val d2 = a and 0x0F
        val d3 = (b and 0xF0) shr 4
        val d4 = b and 0x0F
        return "$prefix$d1${d2.toString(16).uppercase()}${d3.toString(16).uppercase()}${d4.toString(16).uppercase()}"
    }
}
