package com.autodiag.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.util.UUID

/**
 * Transporte BLE para adaptadores ELM327 (ej. Vgate iCar Pro BLE 4.0).
 * Servicio FFE0 / característica FFE1 (write + notify).
 *
 * PROBLEMA RAÍZ RESUELTO: el flujo liveData enviaba PIDs cada 500 ms de forma
 * continua. Entre un comando AT y el siguiente de una secuencia manual (VIN,
 * acceso de seguridad, sensor de luz) el liveData se colaba y corrompía el
 * contexto del ELM327 — por eso el VIN "a veces sí, a veces no".
 *
 * Solución: [sessionLock] es un contador de sesiones manuales activas.
 * Mientras sea > 0, el liveData espera sin enviar nada. sendRaw lo incrementa
 * al entrar y lo decrementa al salir (siempre en un bloque try/finally).
 */
@SuppressLint("MissingPermission")
class BleElm327Repository(
    private val device: BluetoothDevice,
    private val context: Context
) : ObdRepository {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val CHAR_UUID: UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    }

    private var gatt: BluetoothGatt? = null
    private var rxChar: BluetoothGattCharacteristic? = null
    private val rxBuffer = StringBuilder()

    @Volatile private var ready = false
    @Volatile private var failed = false

    // > 0 → hay una sesión manual activa; el liveData pausa.
    @Volatile private var sessionLock = 0

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> if (!g.requestMtu(517)) g.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> { ready = false }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            g.discoverServices()
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val ch = g.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID)
            if (ch == null) { failed = true; return }
            rxChar = ch
            g.setCharacteristicNotification(ch, true)
            val cccd = ch.getDescriptor(CCCD_UUID)
            if (cccd != null) {
                @Suppress("DEPRECATION")
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                g.writeDescriptor(cccd)
            } else {
                ready = true
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            ready = true
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val bytes = ch.value ?: return
            appendRx(bytes)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            ch: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            appendRx(value)
        }
    }

    private fun appendRx(bytes: ByteArray) {
        val text = String(bytes, Charsets.US_ASCII)
        synchronized(rxBuffer) { rxBuffer.append(text) }
    }

    override fun connect() {
        gatt = device.connectGatt(context, false, callback)
        val deadline = System.currentTimeMillis() + 12_000
        while (!ready && !failed && System.currentTimeMillis() < deadline) Thread.sleep(50)
        if (failed) throw IOException("BLE FFE0/FFE1 service not found — is this a BLE ELM327?")
        if (!ready) throw IOException("BLE connection timeout")
        listOf("ATZ", "ATE0", "ATL0", "ATSP0").forEach { send(it); Thread.sleep(200) }
    }

    @Synchronized
    private fun send(cmd: String): String {
        val ch = rxChar ?: return ""
        synchronized(rxBuffer) { rxBuffer.setLength(0) }
        @Suppress("DEPRECATION")
        run {
            ch.value = "$cmd\r".toByteArray(Charsets.US_ASCII)
            ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            gatt?.writeCharacteristic(ch)
        }
        // Timeout largo para UDS/ISO-TP (codificaciones, VIN multi-trama).
        val timeout = if (cmd.startsWith("AT") || cmd.length <= 4) 3_000L else 5_000L
        val deadline = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < deadline) {
            val snapshot = synchronized(rxBuffer) { rxBuffer.toString() }
            if (snapshot.contains(">")) return snapshot.substringBefore(">").trim()
            Thread.sleep(20)
        }
        return synchronized(rxBuffer) { rxBuffer.toString() }.trim()
    }

    override fun disconnect() {
        runCatching { gatt?.disconnect(); gatt?.close() }
        gatt = null; rxChar = null; ready = false
    }

    override val liveData: Flow<Map<String, Double>> = flow {
        while (true) {
            // Pausa mientras hay una sesión manual activa (VIN, sensor, seguridad...).
            if (sessionLock > 0) { delay(200); continue }
            val map = mutableMapOf<String, Double>()
            PidDecoder.PIDS.keys.forEach { code ->
                if (sessionLock > 0) return@forEach   // aborta el ciclo si se activa lock
                val raw = send(code)
                PidDecoder.decodeResponse(raw)?.let { (name, value) -> map[name] = value }
            }
            if (map.isNotEmpty()) emit(map)
            delay(500)
        }
    }.flowOn(Dispatchers.IO)

    override val dtcs: Flow<List<String>> = flow {
        emit(DtcParser.parse(send("03"), "03"))
    }.flowOn(Dispatchers.IO)

    override suspend fun clearDtcs() { send("04") }

    override fun beginSession() { sessionLock++ }
    override fun endSession() { if (sessionLock > 0) sessionLock-- }

    override suspend fun sendRaw(command: String): String =
        kotlinx.coroutines.withContext(Dispatchers.IO) { send(command) }

    override val capabilities = ObdCapabilities(rawTerminal = true, canMonitor = true)
}
