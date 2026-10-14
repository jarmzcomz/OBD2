package com.autodiag.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifica el decodificador de PIDs (modo 01): fórmulas SAE J1979, tolerancia al
 * eco del comando y al ruido ("NO DATA").
 */
class PidDecoderTest {

    @Test fun rpm_010C() {
        val r = PidDecoder.decodeResponse("41 0C 0F A0")!!
        assertEquals("rpm", r.first)
        assertEquals(1000.0, r.second, 0.01)   // (0x0F*256 + 0xA0) / 4
    }

    @Test fun speed_010D() {
        val r = PidDecoder.decodeResponse("41 0D 50")!!
        assertEquals("speed", r.first)
        assertEquals(80.0, r.second, 0.01)
    }

    @Test fun coolant_0105() {
        val r = PidDecoder.decodeResponse("41 05 5A")!!
        assertEquals("coolant_temp", r.first)
        assertEquals(50.0, r.second, 0.01)     // 0x5A - 40
    }

    @Test fun toleratesCommandEcho() {
        val r = PidDecoder.decodeResponse("010C\r41 0C 1A F8")!!
        assertEquals("rpm", r.first)
        assertEquals(1726.0, r.second, 0.01)
    }

    @Test fun noData_returnsNull() {
        assertNull(PidDecoder.decodeResponse("NO DATA"))
    }
}
