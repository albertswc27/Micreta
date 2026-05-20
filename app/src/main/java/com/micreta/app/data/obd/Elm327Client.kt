package com.micreta.app.data.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.micreta.app.core.logging.EventLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Minimal ELM327 client over Bluetooth RFCOMM.
 *
 * Lifecycle:
 *  1. [connect] — opens the socket and runs an init sequence.
 *  2. [query]   — sends an OBD PID, returns the raw ASCII response.
 *  3. [close]   — releases the socket.
 *
 * This client does **no** parsing — that's [ObdPidParser]'s job. The split
 * keeps each layer testable on its own (parser has zero Android deps).
 */
class Elm327Client(private val context: Context) {

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    val isConnected: Boolean get() = socket?.isConnected == true

    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter = manager?.adapter
            ?: return@withContext Result.failure(IOException("Bluetooth adapter not available."))
        if (!adapter.isEnabled) return@withContext Result.failure(IOException("Bluetooth está apagado."))

        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            return@withContext Result.failure(IOException("Dirección MAC inválida: $address"))
        }

        try {
            val s = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            try { adapter.cancelDiscovery() } catch (_: SecurityException) {}
            s.connect()
            socket = s
            input = s.inputStream
            output = s.outputStream
            EventLogger.info(TAG, "ELM327 socket connected to $address.")

            // Init sequence — ignore responses, they're often just "OK" echoes.
            initLine("ATZ", waitMs = 800)
            initLine("ATE0")  // echo off
            initLine("ATL0")  // line feeds off
            initLine("ATS0")  // spaces off — we'll add our own in the parser
            initLine("ATH0")  // headers off
            initLine("ATSP0") // auto protocol
            EventLogger.info(TAG, "ELM327 init sequence complete.")
            Result.success(Unit)
        } catch (e: Throwable) {
            close()
            EventLogger.error(TAG, "ELM327 connect failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun initLine(cmd: String, waitMs: Long = 200) {
        try {
            writeLine(cmd)
            delay(waitMs)
            readUntilPrompt()
        } catch (e: Throwable) {
            EventLogger.warn(TAG, "init '$cmd' failed: ${e.message}")
        }
    }

    /** Sends an OBD command (e.g. "010C") and returns the raw ASCII response. */
    suspend fun query(command: String, timeoutMs: Long = 1500): Result<String> = withContext(Dispatchers.IO) {
        val out = output ?: return@withContext Result.failure(IOException("Not connected"))
        try {
            writeLine(command)
            val response = readUntilPrompt(timeoutMs)
            Result.success(response)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun close() {
        try { input?.close() } catch (_: Throwable) {}
        try { output?.close() } catch (_: Throwable) {}
        try { socket?.close() } catch (_: Throwable) {}
        input = null
        output = null
        socket = null
        EventLogger.info(TAG, "ELM327 socket closed.")
    }

    // --- IO helpers -------------------------------------------------------

    private fun writeLine(cmd: String) {
        val out = output ?: throw IOException("Not connected")
        out.write((cmd + "\r").toByteArray())
        out.flush()
    }

    /** Reads bytes until the ELM327 prompt `>` is seen, or timeout. */
    private fun readUntilPrompt(timeoutMs: Long = 1500): String {
        val inp = input ?: throw IOException("Not connected")
        val deadline = System.currentTimeMillis() + timeoutMs
        val buffer = StringBuilder()
        val tmp = ByteArray(64)
        while (System.currentTimeMillis() < deadline) {
            if (inp.available() > 0) {
                val n = inp.read(tmp)
                if (n > 0) {
                    val chunk = String(tmp, 0, n)
                    buffer.append(chunk)
                    if (chunk.contains('>')) break
                }
            } else {
                try { Thread.sleep(10) } catch (_: InterruptedException) { break }
            }
        }
        return buffer.toString().replace(">", "").trim()
    }

    companion object {
        /** Serial Port Profile UUID — standard for ELM327 clones. */
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val TAG = "ELM327"
    }
}
