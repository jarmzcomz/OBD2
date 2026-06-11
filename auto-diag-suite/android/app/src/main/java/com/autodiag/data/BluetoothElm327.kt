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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.UUID

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

@SuppressLint("MissingPermission")
class BluetoothElm327Repository(
    private val device: BluetoothDevice,
    private val adapter: BluetoothAdapter
) : ObdRepository {

    private var socket: BluetoothSocket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    override fun connect() {
        // Discovery activo interfiere con el handshake RFCOMM y es la causa #1
        // de "read failed, socket might closed". Siempre cancelarlo antes.
        if (adapter.isDiscovering) adapter.cancelDiscovery()

        socket = try {
            // Camino estándar: funciona en dongles que publican bien el record SPP.
            device.createRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
        } catch (e: Exception) {
            // Fallback por reflexión al canal RFCOMM 1. Es lo que hace conectar a
            // los clones ELM327 (iCar Pro) que rechazan la conexión por UUID.
            connectViaReflection()
        }

        reader = BufferedReader(InputStreamReader(socket!!.inputStream))
        writer = PrintWriter(socket!!.outputStream, true)
        listOf("ATZ", "ATE0", "ATL0", "ATSP0").forEach { cmd ->
            writer!!.println("$cmd\r")
            Thread.sleep(100)
        }
    }

    private fun connectViaReflection(): BluetoothSocket {
        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        val fallback = method.invoke(device, 1) as BluetoothSocket
        try {
            fallback.connect()
        } catch (e: Exception) {
            // Un reintento tras un breve respiro: el primer connect tras un SDP
            // fallido suele tirar IOException aunque el siguiente sí enlace.
            Thread.sleep(300)
            fallback.connect()
        }
        return fallback
    }

    override fun disconnect() { socket?.close() }

    private fun query(cmd: String): String {
        writer?.println("$cmd\r") ?: return ""
        val sb = StringBuilder()
        while (true) {
            val line = reader?.readLine() ?: break
            if (line.trimEnd() == ">") break
            sb.appendLine(line)
        }
        return sb.toString()
    }

    override val liveData: Flow<Map<String, Double>> = flow {
        while (true) {
            val map = mutableMapOf<String, Double>()
            PidDecoder.PIDS.keys.forEach { code ->
                val raw = query(code)
                PidDecoder.decodeResponse(raw)?.let { (name, value) -> map[name] = value }
            }
            if (map.isNotEmpty()) emit(map)
            delay(500)
        }
    }.flowOn(Dispatchers.IO)

    override val dtcs: Flow<List<String>> = flow {
        val raw = query("03")
        // Parseo básico de respuesta de servicio 03
        val codes = raw.split("\n")
            .flatMap { it.trim().split(" ") }
            .filter { it.length == 2 }
            .chunked(2)
            .mapNotNull { pair ->
                if (pair.size == 2) decodeDtc(pair[0].toIntOrNull(16) ?: 0, pair[1].toIntOrNull(16) ?: 0) else null
            }
        emit(codes)
    }.flowOn(Dispatchers.IO)

    override suspend fun clearDtcs() { query("04") }

    private fun decodeDtc(a: Int, b: Int): String? {
        if (a == 0 && b == 0) return null
        val prefix = arrayOf("P", "C", "B", "U")[(a and 0xC0) shr 6]
        val d1 = (a and 0x30) shr 4
        val d2 = a and 0x0F
        val d3 = (b and 0xF0) shr 4
        val d4 = b and 0x0F
        return "$prefix$d1${d2.toString(16).uppercase()}${d3.toString(16).uppercase()}${d4.toString(16).uppercase()}"
    }
}
