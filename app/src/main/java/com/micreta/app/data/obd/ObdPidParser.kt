package com.micreta.app.data.obd

import com.micreta.app.core.logging.EventLogger

/**
 * Parses raw ELM327 ASCII responses into typed values.
 *
 * The ELM327 returns hex bytes separated by spaces, e.g. for PID 010C (RPM):
 *
 *     41 0C 1A F8
 *
 * - 41 = positive response (40 + service number, here service 01)
 * - 0C = the PID we asked about
 * - 1A F8 = data bytes
 *
 * Pattern used by every public method here:
 *  1. Strip whitespace, newlines, the prompt `>`, the echo, etc.
 *  2. Locate the "41 XX" header.
 *  3. Slice the data bytes after the header.
 *  4. Apply the SAE J1979 formula.
 *
 * If anything looks off (NO DATA, malformed, wrong header) we return `null`
 * and let the caller surface "no disponible" — we never invent a value.
 */
object ObdPidParser {

    /** RPM = ((A*256)+B)/4 — PID 0C */
    fun parseRpm(raw: String): Int? = parseDoubleByte(raw, expectedPid = "0C") { a, b ->
        ((a * 256) + b) / 4
    }

    /** Speed in km/h = A — PID 0D */
    fun parseSpeed(raw: String): Int? = parseSingleByte(raw, expectedPid = "0D") { a -> a }

    /** Coolant temp = A - 40 °C — PID 05 */
    fun parseCoolantTemp(raw: String): Int? = parseSingleByte(raw, expectedPid = "05") { a -> a - 40 }

    /** Engine load % = A*100/255 — PID 04 */
    fun parseEngineLoad(raw: String): Int? = parseSingleByte(raw, expectedPid = "04") { a -> (a * 100) / 255 }

    /** Throttle position % = A*100/255 — PID 11 */
    fun parseThrottle(raw: String): Int? = parseSingleByte(raw, expectedPid = "11") { a -> (a * 100) / 255 }

    /** Fuel level % = A*100/255 — PID 2F (not always supported) */
    fun parseFuelLevel(raw: String): Int? = parseSingleByte(raw, expectedPid = "2F") { a -> (a * 100) / 255 }

    /** Intake air temp = A - 40 °C — PID 0F */
    fun parseIntakeAirTemp(raw: String): Int? = parseSingleByte(raw, expectedPid = "0F") { a -> a - 40 }

    /** Control module voltage = ((A*256)+B)/1000 V — PID 42 (not always supported) */
    fun parseControlModuleVoltage(raw: String): Double? = parseDoubleByteDouble(raw, expectedPid = "42") { a, b ->
        ((a * 256) + b) / 1000.0
    }

    /**
     * Parses DTCs from a mode-03 response. Format:
     *
     *     43 NN xx xx xx xx ...  (NN = number of codes, then 2 bytes per code)
     *
     * The leading byte of each pair encodes the system letter:
     *  - 0x = P (powertrain)
     *  - 4x = C (chassis)
     *  - 8x = B (body)
     *  - Cx = U (network)
     */
    fun parseDtcCodes(raw: String): List<String> {
        val clean = clean(raw)
        if (clean.isEmpty()) return emptyList()
        val tokens = clean.split(" ").filter { it.length == 2 }
        if (tokens.isEmpty()) return emptyList()

        val headerIdx = tokens.indexOf("43")
        if (headerIdx < 0) return emptyList()
        val body = tokens.drop(headerIdx + 1)

        // Some ELM clones include a count byte, others don't — we just iterate pairs.
        return body.chunked(2)
            .mapNotNull { pair ->
                if (pair.size != 2) return@mapNotNull null
                val hi = pair[0].toIntOrNull(16) ?: return@mapNotNull null
                val lo = pair[1].toIntOrNull(16) ?: return@mapNotNull null
                if (hi == 0 && lo == 0) return@mapNotNull null
                val letter = when ((hi and 0xC0) shr 6) {
                    0 -> 'P'
                    1 -> 'C'
                    2 -> 'B'
                    3 -> 'U'
                    else -> 'P'
                }
                val firstDigit = (hi and 0x30) shr 4
                val secondDigit = hi and 0x0F
                val third = (lo and 0xF0) shr 4
                val fourth = lo and 0x0F
                "%c%X%X%X%X".format(letter, firstDigit, secondDigit, third, fourth)
            }
            .filter { it.length == 5 }
    }

    // --- internals --------------------------------------------------------

    private fun parseSingleByte(raw: String, expectedPid: String, formula: (Int) -> Int): Int? =
        parseTyped(raw, expectedPid, dataBytes = 1) { bytes -> formula(bytes[0]) }

    private fun parseDoubleByte(raw: String, expectedPid: String, formula: (Int, Int) -> Int): Int? =
        parseTyped(raw, expectedPid, dataBytes = 2) { bytes -> formula(bytes[0], bytes[1]) }

    private fun parseDoubleByteDouble(raw: String, expectedPid: String, formula: (Int, Int) -> Double): Double? {
        val tokens = tokensAfterHeader(raw, expectedPid, dataBytes = 2) ?: return null
        return try {
            formula(tokens[0], tokens[1])
        } catch (t: Throwable) {
            EventLogger.warn(TAG, "Parse error for PID $expectedPid: ${t.message}")
            null
        }
    }

    private fun <T> parseTyped(raw: String, expectedPid: String, dataBytes: Int, mapper: (List<Int>) -> T): T? {
        val tokens = tokensAfterHeader(raw, expectedPid, dataBytes) ?: return null
        return try {
            mapper(tokens)
        } catch (t: Throwable) {
            EventLogger.warn(TAG, "Parse error for PID $expectedPid: ${t.message}")
            null
        }
    }

    private fun tokensAfterHeader(raw: String, expectedPid: String, dataBytes: Int): List<Int>? {
        val clean = clean(raw)
        if (clean.contains("NO DATA", ignoreCase = true)) return null
        val tokens = clean.split(" ").filter { it.length == 2 }
        // Find "41 XX" pair anywhere in the response.
        for (i in 0 until tokens.size - 1) {
            if (tokens[i].equals("41", ignoreCase = true) &&
                tokens[i + 1].equals(expectedPid, ignoreCase = true)
            ) {
                val start = i + 2
                if (start + dataBytes > tokens.size) return null
                val bytes = tokens.subList(start, start + dataBytes).map { it.toIntOrNull(16) ?: return null }
                return bytes
            }
        }
        return null
    }

    private fun clean(raw: String): String =
        raw.replace("\r", " ")
            .replace("\n", " ")
            .replace(">", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .uppercase()

    private const val TAG = "ObdParse"
}
