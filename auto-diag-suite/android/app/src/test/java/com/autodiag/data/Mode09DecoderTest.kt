package com.autodiag.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifica el decodificador de modo 09 (VIN). El caso clave es multi-trama: si no
 * se quitan los marcadores de índice ("0:", "1:") ANTES de juntar el hex, el
 * contador de trama se mezcla con los bytes y corrompe el VIN.
 */
class Mode09DecoderTest {

    private val vin = "9BWAG45U5MT123456"

    @Test fun singleFrame() {
        val raw = "49 02 01 39 42 57 41 47 34 35 55 35 4D 54 31 32 33 34 35 36"
        assertEquals(vin, Mode09Decoder.decodeVin(raw))
    }

    @Test fun multiFrame_stripsFrameCounters() {
        val raw = "0: 49 02 01 39 42 57 41\r1: 47 34 35 55 35 4D 54\r2: 31 32 33 34 35 36"
        assertEquals(vin, Mode09Decoder.decodeVin(raw))
    }

    @Test fun noData_returnsNull() {
        assertNull(Mode09Decoder.decodeVin("NO DATA"))
    }
}
