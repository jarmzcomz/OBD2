package com.autodiag.data

/**
 * Catálogo VAG (VW / Audi / SEAT / Škoda) en lenguaje claro.
 *
 * Mapea módulos y "funciones ocultas" conocidas a su dirección CAN/UDS y a la
 * receta de bytes, para que el usuario vea interruptores en español en vez de
 * hexadecimal. Las direcciones corresponden al esquema UDS de 11 bits típico de
 * plataformas modernas (MQB). En plataformas viejas (TP2.0) algunos módulos no
 * responden; por eso la app SIEMPRE lee primero el estado real antes de tocar nada.
 */
object VagCatalog {

    /** Un módulo/centralita del vehículo con su par de IDs CAN para UDS. */
    data class Module(
        val address: String,   // nº VAG clásico (ej. "09")
        val name: String,      // nombre claro en español
        val reqId: String,     // CAN tx (header ELM327)
        val rspId: String      // CAN rx esperado
    )

    /** Módulos VAG más comunes, en orden de utilidad para el usuario. */
    val modules: List<Module> = listOf(
        Module("01", "Motor", "7E0", "7E8"),
        Module("02", "Transmisión", "7E1", "7E9"),
        Module("03", "Frenos / ABS", "713", "77D"),
        Module("08", "Climatización / A/C", "746", "7B0"),
        Module("09", "Electrónica central (luces, confort)", "70E", "778"),
        Module("15", "Airbags", "715", "77F"),
        Module("17", "Tablero / Instrumentos", "714", "77E"),
        Module("19", "Gateway (red del auto)", "710", "77A"),
        Module("42", "Puerta conductor", "74F", "7B9"),
        Module("44", "Dirección asistida", "712", "77C"),
        Module("5F", "Sistema multimedia (MIB)", "773", "7DD")
    )

    /**
     * Una función configurable presentada como interruptor. La lectura/escritura
     * real se hace sobre el bloque de codificación larga del módulo.
     *
     * [didRead] identificador de dato a leer (codificación). [byteIndex] qué byte
     * del bloque contiene el bit. [bitMask] el bit. [onMeansSet] si "activado"
     * equivale a poner el bit en 1.
     */
    data class Feature(
        val id: String,
        val name: String,
        val description: String,
        val moduleAddress: String,
        val didRead: String,
        val byteIndex: Int,
        val bitMask: Int,
        val onMeansSet: Boolean = true,
        val risk: String = "Bajo"
    )

    /**
     * Funciones populares y ampliamente documentadas en la comunidad VAG. La
     * disponibilidad EXACTA depende del modelo/año; por eso cada interruptor
     * muestra el estado leído del auto y, si el módulo no la soporta, lo dice.
     */
    val features: List<Feature> = listOf(
        Feature(
            id = "lane_assist_default",
            name = "Asistente de carril siempre encendido",
            description = "Que el asistente de mantenimiento de carril arranque activo al encender el auto.",
            moduleAddress = "03", didRead = "0600", byteIndex = 0, bitMask = 0x01
        ),
        Feature(
            id = "needle_sweep",
            name = "Barrido de agujas al arrancar",
            description = "Las agujas del tablero hacen un barrido completo al encender (efecto deportivo).",
            moduleAddress = "17", didRead = "0600", byteIndex = 1, bitMask = 0x02
        ),
        Feature(
            id = "coming_home",
            name = "Luces 'Coming/Leaving Home'",
            description = "Los faros se quedan encendidos un rato al salir del auto para iluminarte el camino.",
            moduleAddress = "09", didRead = "0600", byteIndex = 2, bitMask = 0x04
        ),
        Feature(
            id = "mirror_fold",
            name = "Plegado de espejos al cerrar",
            description = "Los espejos retrovisores se pliegan solos al cerrar el auto con la llave.",
            moduleAddress = "09", didRead = "0600", byteIndex = 3, bitMask = 0x08
        ),
        Feature(
            id = "seatbelt_chime",
            name = "Silenciar aviso de cinturón",
            description = "Desactiva el 'bip' insistente del cinturón (úsalo con responsabilidad).",
            moduleAddress = "17", didRead = "0600", byteIndex = 0, bitMask = 0x10, risk = "Medio"
        ),
        Feature(
            id = "drl_dimming",
            name = "Luces diurnas atenuadas con direccional",
            description = "La luz de día se atenúa del lado donde pones la direccional.",
            moduleAddress = "09", didRead = "0600", byteIndex = 4, bitMask = 0x01
        ),
        Feature(
            id = "speed_lock",
            name = "Cierre automático en movimiento",
            description = "Las puertas se aseguran solas al pasar cierta velocidad.",
            moduleAddress = "09", didRead = "0600", byteIndex = 5, bitMask = 0x20
        )
    )

    fun moduleByAddress(addr: String): Module? = modules.firstOrNull { it.address == addr }
    fun featuresFor(addr: String): List<Feature> = features.filter { it.moduleAddress == addr }
}
