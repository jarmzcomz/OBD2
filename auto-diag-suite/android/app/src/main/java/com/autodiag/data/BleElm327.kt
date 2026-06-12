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
 * Transporte para adaptadores ELM327 que hablan Bluetooth Low Energy (BLE/GATT),
 * como el Vgate iCar Pro BLE 4.0. Estos NO usan RFCOMM/SPP: exponen un modulo
 * serial-BLE con el servicio FFE0 y la caracteristica FFE1 (write + notify).
 *
 * El flujo es: connectGatt -> discoverServices -> activar notificaciones en FFE1
 * -> escribir comandos AT en FFE1 -> acumular las notificaciones hasta el prompt
 * '>' del ELM327.
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

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                // Pedir MTU grande ANTES de descubrir servicios: con el MTU por
                // defecto (23) las respuestas multi-byte (VIN, freeze frame) se
                // fragmentan. Si requestMtu falla, descubrimos igual.
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

        // Android < 13 (API < 33): callback sin value explicito.
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val bytes = ch.value ?: return
            appendRx(bytes)
        }

        // Android 13+ (API 33+): SOLO se invoca este; sin el, los telefonos
        // modernos no recibirian las respuestas del ELM327.
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
        listOf("ATZ", "ATE0", "ATL0", "ATSP0").forEach { send(it); Thread.sleep(150) }
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
        val deadline = System.currentTimeMillis() + 3000
        while (System.currentTimeMillis() < deadline) {
            val snapshot = synchronized(rxBuffer) { rxBuffer.toString() }
            if (snapshot.contains(">")) return snapshot.substringBefore(">").trim()
            Thread.sleep(20)
        }
        return synchronized(rxBuffer) { rxBuffer.toString() }.trim()
    }

    override fun disconnect() {
        runCatching {
            gatt?.disconnect()
            gatt?.close()
        }
        gatt = null
        rxChar = null
        ready = false
    }

    override val liveData: Flow<Map<String, Double>> = flow {
        while (true) {
            val map = mutableMapOf<String, Double>()
            PidDecoder.PIDS.keys.forEach { code ->
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

    override suspend fun sendRaw(command: String): String =
        kotlinx.coroutines.withContext(Dispatchers.IO) { send(command) }

    override val capabilities = ObdCapabilities(rawTerminal = true, canMonitor = true)
}
