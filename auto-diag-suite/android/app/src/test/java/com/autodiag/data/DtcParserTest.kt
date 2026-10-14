package com.autodiag.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica el parser de DTC: que salte el header (0x43/0x47/0x4A) y el byte de
 * conteo, decodifique solo pares válidos y descarte el relleno 00 00.
 */
class DtcParserTest {

    @Test fun decodeDtc_powertrain() {
        assertEquals("P0133", DtcParser.decodeDtc(0x01, 0x33))
    }

    @Test fun decodeDtc_network() {
        assertEquals("U0100", DtcParser.decodeDtc(0xC1, 0x00))
    }

    @Test fun decodeDtc_zeroIsNull() {
        assertNull(DtcParser.decodeDtc(0, 0))
    }

    @Test fun parse_mode03_singleDtc() {
        // 43 header, 01 conteo, 01 33 = P0133.
        assertEquals(listOf("P0133"), DtcParser.parse("43 01 01 33", "03"))
    }

    @Test fun parse_skipsHeaderAndCount_andPadding() {
        // 43 header, 02 conteo, P0133 + P0420, 00 00 relleno ignorado.
        assertEquals(listOf("P0133", "P0420"), DtcParser.parse("43 02 01 33 04 20 00 00", "03"))
    }

    @Test fun parse_mode07_usesHeader47() {
        assertEquals(listOf("P0420"), DtcParser.parse("47 01 04 20", "07"))
    }

    @Test fun parse_noData_isEmpty() {
        assertTrue(DtcParser.parse("NO DATA", "03").isEmpty())
    }
}
