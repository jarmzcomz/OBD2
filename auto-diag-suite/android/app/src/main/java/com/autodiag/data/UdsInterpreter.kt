package com.autodiag.data

/**
 * Traduce respuestas crudas del adaptador (hex) a lenguaje claro en español.
 * Es el corazón de "que la app me diga qué pasa" en lugar de mostrar 7F 22 31.
 */
object UdsInterpreter {

    /** Códigos de rechazo UDS (NRC) explicados para un usuario no técnico. */
    private val negativeResponses: Map<String, String> = mapOf(
        "10" to "Rechazo general: la ECU no aceptó la petición.",
        "11" to "Esa función no existe en este módulo.",
        "12" to "Sub-función no soportada por este módulo.",
        "13" to "Formato del mensaje incorrecto (largo equivocado).",
        "22" to "Condiciones no cumplidas: prueba con el motor encendido o el auto en contacto.",
        "31" to "Petición fuera de rango: ese dato o función no aplica aquí.",
        "33" to "Necesitas ACCESO DE SEGURIDAD antes de hacer este cambio.",
        "35" to "La llave de seguridad enviada es incorrecta.",
        "36" to "Demasiados intentos: la ECU bloqueó el acceso un rato.",
        "37" to "Espera un momento antes de volver a intentar el acceso.",
        "7E" to "Sub-función no soportada en la sesión actual.",
        "7F" to "La función no está disponible en la sesión actual (abre sesión extendida).",
        "78" to "La ECU está procesando, espera la respuesta…"
    )

    /** Resultado interpretado de una respuesta. */
    data class Reading(val ok: Boolean, val plain: String, val raw: String)

    /**
     * Interpreta una respuesta cruda. Detecta respuestas negativas (7F) y las
     * traduce; si es positiva, lo confirma. [requestSid] = primer byte enviado.
     */
    fun interpret(raw: String, requestSid: String = ""): Reading {
        val clean = raw.uppercase().replace(Regex("[^0-9A-F]"), " ").trim()
        val bytes = clean.split(Regex("\\s+")).filter { it.length <= 2 && it.isNotEmpty() }

        // Errores típicos del propio ELM327.
        val u = raw.uppercase()
        when {
            u.contains("NO DATA") -> return Reading(false, "El módulo no respondió (puede no existir o estar dormido).", raw)
            u.contains("UNABLE TO CONNECT") -> return Reading(false, "No se pudo conectar al módulo.", raw)
            u.contains("BUS INIT") || u.contains("BUS ERROR") -> return Reading(false, "Error en el bus del auto. Revisa la conexión.", raw)
            u.contains("CAN ERROR") -> return Reading(false, "Error CAN: el módulo no contesta en este protocolo.", raw)
            u.contains("?") -> return Reading(false, "El adaptador no entendió el comando.", raw)
            bytes.isEmpty() -> return Reading(false, "Sin respuesta.", raw)
        }

        // Respuesta negativa UDS: 7F <sid> <nrc>
        val idx = bytes.indexOf("7F")
        if (idx >= 0 && idx + 2 < bytes.size) {
            val nrc = bytes[idx + 2]
            val msg = negativeResponses[nrc] ?: "La ECU rechazó la petición (código $nrc)."
            return Reading(false, "✗ $msg", raw)
        }

        // Respuesta positiva: el SID de respuesta = SID pedido + 0x40.
        return Reading(true, "✓ El módulo respondió correctamente.", raw)
    }

    /**
     * Intenta convertir una cadena hex ASCII a texto legible (VIN, nº de pieza,
     * nombre de módulo, etc.). Devuelve null si no parece texto.
     */
    fun hexToAscii(raw: String): String? {
        val hex = raw.uppercase().replace(Regex("[^0-9A-F]"), "")
        if (hex.length < 4 || hex.length % 2 != 0) return null
        val sb = StringBuilder()
        var printable = 0
        var i = 0
        while (i + 1 < hex.length) {
            val code = hex.substring(i, i + 2).toIntOrNull(16) ?: return null
            if (code in 32..126) { sb.append(code.toChar()); printable++ } else sb.append('·')
            i += 2
        }
        // Solo lo damos por "texto" si la mayoría son caracteres imprimibles.
        return if (printable >= sb.length * 0.6) sb.toString().trim() else null
    }
}
