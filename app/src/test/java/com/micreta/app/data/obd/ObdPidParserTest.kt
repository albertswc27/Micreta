package com.micreta.app.data.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObdPidParserTest {

    @Test
    fun parsesRpm() {
        assertEquals(1726, ObdPidParser.parseRpm("41 0C 1A F8"))
    }

    @Test
    fun parsesSpeed() {
        assertEquals(40, ObdPidParser.parseSpeed("41 0D 28"))
    }

    @Test
    fun parsesCoolantTemperature() {
        assertEquals(50, ObdPidParser.parseCoolantTemp("41 05 5A"))
    }

    @Test
    fun parsesFuelLevel() {
        assertEquals(50, ObdPidParser.parseFuelLevel("41 2F 80"))
    }

    @Test
    fun parsesControlModuleVoltage() {
        assertEquals(12.345, ObdPidParser.parseControlModuleVoltage("41 42 30 39")!!, 0.001)
    }

    @Test
    fun returnsNullForNoData() {
        assertNull(ObdPidParser.parseRpm("NO DATA"))
    }

    @Test
    fun returnsNullForMalformedResponse() {
        assertNull(ObdPidParser.parseRpm("41 0C GG"))
        assertNull(ObdPidParser.parseSpeed("41 0D"))
    }

    @Test
    fun parsesDtcCodes() {
        assertEquals(listOf("P0133"), ObdPidParser.parseDtcCodes("43 01 33 00 00 00"))
    }
}
