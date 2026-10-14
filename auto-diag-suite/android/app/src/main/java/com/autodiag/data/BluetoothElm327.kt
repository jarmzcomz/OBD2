package com.autodiag.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/**
 * Transporte Bluetooth Clásico (RFCOMM/SPP) para ELM327.
 *
 * Mismo fix de concurrencia que BleElm327: sessionLock pausa el liveData
 * mientras sendRaw está ejecutando una secuencia manual. Sin esto, el liveData
 * manda PIDs entre los pasos de "AT SH → 27 01 → 27 02" y corrompe el contexto.
 */
@SuppressLint("MissingPermission")
class BluetoothElm327Repository(
    private val device: BluetoothDevice,
    private val adapter: BluetoothAdapter
) : ObdRepository {

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    // > 0 → sesión manual activa; liveData pausa.
    @Volatile private var sessionLock = 0

    override fun connect() {
        if (adapter.isDiscovering) adapter.cancelDiscovery()
        socket = try {
            device.createRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
        } catch (e: Exception) {
            connectViaReflection()
        }
        input = socket!!.inputStream
        output = socket!!.outputStream
        listOf("ATZ", "ATE0", "ATL0", "ATSP0").forEach { cmd ->
            query(cmd); Thread.sleep(150)
        }
    }

    private fun connectViaReflection(): BluetoothSocket {
        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        val fallback = method.invoke(device, 1) as BluetoothSocket
        try { fallback.connect() } catch (e: Exception) { Thread.sleep(300); fallback.connect() }
        return fallback
    }

    override fun disconnect() { runCatching { socket?.close() } }

    @Synchronized
    private fun query(cmd: String): String {
        val out = output ?: return ""
        val inp = input ?: return ""
        out.write("$cmd\r".toByteArray(Charsets.US_ASCII))
        out.flush()
        val sb = StringBuilder()
        // Timeout más largo para respuestas UDS/multi-trama.
        val timeout = if (cmd.startsWith("AT") || cmd.length <= 4) 3_000L else 5_000L
        val deadline = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < deadline) {
            if (inp.available() > 0) {
                val c = inp.read()
                if (c == -1) break
                val ch = c.toChar()
                if (ch == '>') break
                sb.append(ch)
            } else {
                Thread.sleep(10)
            }
        }
        return sb.toString().trim()
    }

    override val liveData: Flow<Map<String, Double>> = flow {
        while (true) {
            if (sessionLock > 0) { delay(200); continue }
            val map = mutableMapOf<String, Double>()
            PidDecoder.PIDS.keys.forEach { code ->
                if (sessionLock > 0) return@forEach
                PidDecoder.decodeResponse(query(code))?.let { (name, value) -> map[name] = value }
            }
            if (map.isNotEmpty()) emit(map)
            delay(500)
        }
    }.flowOn(Dispatchers.IO)

    override val dtcs: Flow<List<String>> = flow {
        emit(DtcParser.parse(query("03"), "03"))
    }.flowOn(Dispatchers.IO)

    override suspend fun clearDtcs() { query("04") }

    override fun beginSession() { sessionLock++ }
    override fun endSession() { if (sessionLock > 0) sessionLock-- }

    override suspend fun sendRaw(command: String): String =
        withContext(Dispatchers.IO) { query(command) }

    override val capabilities = ObdCapabilities(rawTerminal = true, canMonitor = true)
}
