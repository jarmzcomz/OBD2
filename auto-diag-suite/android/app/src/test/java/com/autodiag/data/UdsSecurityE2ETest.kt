package com.autodiag.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test END-TO-END del acceso de seguridad: simula un módulo VAG real y verifica que
 * la app saca la semilla, calcula la clave y (según el caso) desbloquea y ESCRIBE.
 *
 * El módulo simulado (FakeVagModule) responde UDS igual que una centralita: 10 03,
 * 27 <nivel> (semilla), 27 <nivel+1> (valida la clave con el MISMO algoritmo SA2),
 * y 2E (solo escribe si está desbloqueado). Así probamos TODA la cadena sin carro.
 */
class UdsSecurityE2ETest {

    /**
     * Módulo simulado protegido por SA2 (pre-SFD). Entrega [seed] en el nivel
     * [level] y solo acepta la clave que produce [bytecode] sobre esa semilla.
     */
    private class FakeVagModule(
        val seed: List<Int>,
        val bytecode: IntArray,
        val level: Int = 0x03
    ) {
        var unlocked = false; private set
        val written = mutableMapOf<String, String>()
        val expectedKey: Long get() = Sa2SeedKey(bytecode).execute(seed)

        suspend fun send(cmd: String): String {
            val c = cmd.uppercase().replace(Regex("\\s+"), " ").trim()
            val seedReq = "27 %02X".format(level)
            val keyReq = "27 %02X".format(level + 1)
            return when {
                c == "10 03" -> "50 03 00 32 01 F4"
                c == seedReq -> "67 %02X ".format(level) + seed.joinToString(" ") { "%02X".format(it) }
                c.startsWith(keyReq) -> {
                    val given = c.removePrefix(keyReq).replace(" ", "").chunked(2).mapNotNull { it.toIntOrNull(16) }
                    val expect = (0..3).map { ((expectedKey shr (24 - it * 8)) and 0xFF).toInt() }
                    if (given == expect) { unlocked = true; "67 %02X".format(level + 1) } else "7F 27 35"
                }
                c.startsWith("27 ") -> "7F 27 11"            // otros niveles: no soportado
                c.startsWith("2E ") -> {
                    if (!unlocked) return "7F 2E 33"          // securityAccessDenied
                    val parts = c.removePrefix("2E ").trim().split(" ")
                    val did = parts[0]
                    written[did] = parts.drop(1).joinToString(" ")
                    "6E $did"
                }
                else -> "7F ${c.take(2)} 11"
            }
        }
    }

    /** Módulo bajo SFD: rechaza TODO acceso de seguridad con 7F 27 11. */
    private class FakeSfdModule {
        suspend fun send(cmd: String): String {
            val c = cmd.uppercase().trim()
            return when {
                c == "10 03" -> "50 03 00 32 01 F4"
                c.startsWith("27 ") -> "7F 27 11"
                c.startsWith("2E ") -> "7F 2E 33"
                else -> "7F 22 11"
            }
        }
    }

    // Vector oficial SA2 (de jpbede/sa2go): seed 0x0CF4233D → key 0xC1393A1C.
    private val sa2Bytecode = intArrayOf(0x68, 0x05, 0x81, 0x4A, 0x05, 0x87, 0x5F, 0xBD, 0x5D, 0xBD, 0x49, 0x4C)
    private val sa2BytecodeHex = "68 05 81 4A 05 87 5F BD 5D BD 49 4C"
    private val seed = listOf(0x0C, 0xF4, 0x23, 0x3D)

    @Test
    fun e2e_conBytecodeSA2_desbloquea_y_escribe() = runBlocking {
        val module = FakeVagModule(seed, sa2Bytecode)
        val uds = UdsSecurity({ module.send(it) })

        // 1) Con el bytecode SA2 correcto, la app calcula la clave y desbloquea.
        val result = uds.unlock(login = 20103, bytecodeHex = sa2BytecodeHex)
        assertTrue("Debe desbloquear con el bytecode correcto", result is SecurityResult.Unlocked)
        result as SecurityResult.Unlocked
        assertEquals(0x03, result.level)
        assertEquals("C1 39 3A 1C", result.key)

        // 2) Ya desbloqueado, la escritura del DID del sensor tiene éxito.
        val ok = uds.writeDid("0296", "00 05")
        assertTrue("La escritura debe ser aceptada tras desbloquear", ok)
        assertEquals("00 05", module.written["0296"])
    }

    @Test
    fun e2e_sinBytecode_loginEstatico_esRechazado() = runBlocking {
        // Sin bytecode, se usa el login estático → NO coincide con la clave SA2 real.
        val module = FakeVagModule(seed, sa2Bytecode)
        val uds = UdsSecurity({ module.send(it) })

        val result = uds.unlock(login = 20103, bytecodeHex = "")
        assertTrue("El login estático debe ser rechazado por un módulo SA2", result is SecurityResult.KeyRejected)
        result as SecurityResult.KeyRejected
        assertEquals("35", result.nrc)          // invalidKey

        // Y sin desbloquear, la escritura falla.
        assertTrue(!uds.writeDid("0296", "00 05"))
    }

    @Test
    fun e2e_moduloSFD_seDetecta() = runBlocking {
        val module = FakeSfdModule()
        val uds = UdsSecurity({ module.send(it) })

        val result = uds.unlock(login = 31347, bytecodeHex = sa2BytecodeHex)
        assertTrue("Todos los niveles 7F 27 11 → SFD", result is SecurityResult.SfdLocked)

        // Escribir sin desbloqueo se rechaza (securityAccessDenied).
        assertTrue(!uds.writeDid("0296", "00 05"))
    }

    @Test
    fun e2e_semillaEnCeros_yaDesbloqueado() = runBlocking {
        val module = FakeVagModule(listOf(0, 0, 0, 0), sa2Bytecode)
        val uds = UdsSecurity({ module.send(it) })
        val result = uds.unlock(login = 20103, bytecodeHex = sa2BytecodeHex)
        assertTrue("Semilla en ceros = ya desbloqueado", result is SecurityResult.Unlocked)
    }

    @Test
    fun parseSeed_extraeBytesCorrectos() {
        assertEquals(seed, UdsSecurity.parseSeed("67 03 0C F4 23 3D", 0x03))
        assertEquals(null, UdsSecurity.parseSeed("7F 27 11", 0x03))
    }

    @Test
    fun computeKey_conBytecode_usaSA2() {
        assertEquals("C1 39 3A 1C", UdsSecurity.computeKey(seed, 20103, sa2BytecodeHex))
    }
}
