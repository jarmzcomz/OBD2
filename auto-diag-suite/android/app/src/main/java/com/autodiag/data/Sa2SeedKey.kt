package com.autodiag.data

/**
 * Máquina virtual del algoritmo SA2 de seed-key de VAG (servicio UDS 0x27 en
 * sesión de programación / codificación protegida del MQB).
 *
 * Es un porte directo y verificado de las implementaciones open source:
 *   • bri3d/sa2_seed_key   (Python)  — https://github.com/bri3d/sa2_seed_key
 *   • jpbede/sa2go         (Go)      — https://github.com/jpbede/sa2go
 *
 * Cómo funciona: el ECU entrega una "semilla" (seed) de 4 bytes. El algoritmo NO
 * es una fórmula fija: es un pequeño PROGRAMA (bytecode) que se ejecuta sobre la
 * semilla para producir la "clave" (key). Esta clase es el intérprete de ese
 * bytecode.
 *
 * ⚠ IMPORTANTE — el bytecode NO es universal: es ÚNICO por módulo y por versión de
 * software, y viene dentro del contenedor de flasheo de VW (ODX / FRF / SGML) de
 * ESE módulo. Sin el bytecode correcto de tu T-Cross no se puede calcular la clave;
 * por eso no existe un "decodificador universal" para MQB. Esta VM queda lista para
 * el día que tengamos el bytecode de tu módulo de confort (09).
 *
 * Set de instrucciones (8/16/40 bits según opcode):
 *   0x81 rsl  — rota el registro 1 bit a la IZQUIERDA a través del carry
 *   0x82 rsr  — rota el registro 1 bit a la DERECHA a través del carry
 *   0x93 add  — registro += operando de 4 bytes (carry si desborda 32 bits)
 *   0x84 sub  — registro -= operando de 4 bytes (carry si baja de 0)
 *   0x87 eor  — registro ^= operando de 4 bytes
 *   0x68 for  — inicia bucle (operando = nº de iteraciones)
 *   0x49 next — fin de bucle (decrementa y salta atrás, o continúa)
 *   0x4A bcc  — salta (operando+2) SOLO si el carry está en 0
 *   0x6B bra  — salta (operando+2) siempre
 *   0x4C fin  — termina (avanza el puntero)
 *
 * Verificado contra los vectores oficiales de jpbede/sa2go:
 *   seed 0x06060607 → key 0xE376C3C5
 *   seed 0xA04EB1ED → key 0x3C7876D8
 *   seed 0x0CF4233D → key 0xC1393A1C
 */
class Sa2SeedKey(private val opcode: IntArray) {

    private var pointer = 0
    private var carryFlag = 0L
    private var register = 0L
    private val forIterations = ArrayDeque<Int>()
    private val forPointers = ArrayDeque<Int>()

    /** Ejecuta el bytecode sobre [seed] (entero sin signo de 32 bits) y devuelve la clave. */
    fun execute(seed: Long): Long {
        register = seed and 0xFFFFFFFFL
        pointer = 0
        carryFlag = 0
        forIterations.clear(); forPointers.clear()
        // Guarda anti-cuelgue: bytecode mal formado no debe colgar la app.
        var guard = 0
        while (pointer < opcode.size && guard++ < 1_000_000) {
            when (opcode[pointer] and 0xFF) {
                0x81 -> rsl()
                0x82 -> rsr()
                0x93 -> add()
                0x84 -> sub()
                0x87 -> eor()
                0x68 -> forLoop()
                0x49 -> nextLoop()
                0x4A -> bcc()
                0x6B -> bra()
                0x4C -> finish()
                else -> pointer += 1   // opcode desconocido: avanza para no atascarse
            }
        }
        return register and 0xFFFFFFFFL
    }

    /** Ejecuta sobre una semilla dada como bytes big-endian. */
    fun execute(seedBytes: List<Int>): Long {
        val s = seedBytes.take(4).fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xFF) }
        return execute(s)
    }

    private fun operand4(): Long =
        ((opcode[pointer + 1].toLong() and 0xFF) shl 24) or
        ((opcode[pointer + 2].toLong() and 0xFF) shl 16) or
        ((opcode[pointer + 3].toLong() and 0xFF) shl 8) or
        (opcode[pointer + 4].toLong() and 0xFF)

    private fun rsl() {
        carryFlag = register and 0x80000000L
        register = (register shl 1) and 0xFFFFFFFFL
        if (carryFlag > 0) register = register or 0x1L
        pointer += 1
    }

    private fun rsr() {
        carryFlag = register and 0x1L
        register = register shr 1
        if (carryFlag > 0) register = register or 0x80000000L
        pointer += 1
    }

    private fun add() {
        carryFlag = 0
        var out = register + operand4()
        if (out > 0xFFFFFFFFL) { carryFlag = 1; out = out and 0xFFFFFFFFL }
        register = out
        pointer += 5
    }

    private fun sub() {
        carryFlag = 0
        var out = register - operand4()
        if (out < 0) { carryFlag = 1; out = out and 0xFFFFFFFFL }
        register = out
        pointer += 5
    }

    private fun eor() {
        register = register xor operand4()
        pointer += 5
    }

    private fun forLoop() {
        forIterations.addFirst((opcode[pointer + 1] and 0xFF) - 1)
        pointer += 2
        forPointers.addFirst(pointer)
    }

    private fun nextLoop() {
        if (forIterations.isEmpty()) { pointer += 1; return }
        if (forIterations.first() > 0) {
            forIterations[0] = forIterations.first() - 1
            pointer = forPointers.first()
        } else {
            forIterations.removeFirst()
            forPointers.removeFirst()
            pointer += 1
        }
    }

    private fun bcc() {
        val skip = (opcode[pointer + 1] and 0xFF) + 2
        pointer += if (carryFlag == 0L) skip else 2
    }

    private fun bra() {
        pointer += (opcode[pointer + 1] and 0xFF) + 2
    }

    private fun finish() { pointer += 1 }

    companion object {
        /**
         * Convierte un bytecode SA2 en texto hex ("68 03 81 93 ...") a IntArray.
         * Acepta separadores y mayúsculas/minúsculas. Devuelve null si está vacío.
         */
        fun parseBytecode(hex: String): IntArray? {
            val bytes = Regex("[0-9A-Fa-f]{2}").findAll(hex).map { it.value.toInt(16) }.toList()
            return if (bytes.isEmpty()) null else bytes.toIntArray()
        }
    }
}
