package com.autodiag.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.sin
import kotlin.random.Random

/**
 * Repositorio de demostración: genera datos simulados de un vehículo en marcha
 * sin necesidad de hardware ni Bluetooth. Sirve para verificar que el dashboard,
 * los gauges, la lista de sensores y la pantalla de fallas se despliegan y
 * funcionan correctamente.
 */
class DemoRepository : ObdRepository {

    @Volatile private var t = 0.0

    override fun connect() { /* nada que conectar en demo */ }
    override fun disconnect() { /* nada que cerrar */ }

    override val liveData: Flow<Map<String, Double>> = flow {
        var rpmBase = 800.0
        while (true) {
            t += 0.5
            // Oscilaciones suaves para que parezca un motor real acelerando/desacelerando.
            val throttle = (30 + 25 * sin(t / 4)).coerceIn(0.0, 100.0)
            rpmBase = (rpmBase + (throttle * 28 - rpmBase) * 0.15)
                .coerceIn(750.0, 6500.0)
            val rpm = rpmBase + Random.nextDouble(-40.0, 40.0)
            val speed = ((rpmBase - 750) / 58).coerceIn(0.0, 180.0)

            val map = mapOf(
                "rpm" to rpm,
                "speed" to speed,
                "coolant_temp" to (88 + 4 * sin(t / 20)).coerceIn(70.0, 98.0),
                "throttle" to throttle,
                "intake_temp" to (34 + 3 * sin(t / 15)),
                "engine_load" to (throttle * 0.85 + Random.nextDouble(-3.0, 3.0)).coerceIn(0.0, 100.0),
                "maf" to (throttle * 1.8 + 5).coerceIn(2.0, 250.0),
                "fuel_level" to 62.0,
                "voltage" to (14.1 + 0.2 * sin(t / 10))
            )
            emit(map)
            delay(500)
        }
    }.flowOn(Dispatchers.IO)

    override val dtcs: Flow<List<String>> = flow {
        delay(600) // simular latencia de lectura
        // Un par de códigos de ejemplo para mostrar la pantalla de fallas.
        emit(listOf("P0301", "P0420", "P0171"))
    }.flowOn(Dispatchers.IO)

    override suspend fun clearDtcs() { delay(400) }

    override suspend fun sendRaw(command: String): String {
        delay(120) // simular latencia del adaptador
        val cmd = command.uppercase().replace(" ", "")
        return when {
            cmd.startsWith("ATI") || cmd == "AT@1" -> "ELM327 v1.5 (DEMO)"
            cmd.startsWith("ATRV") -> "14.2V"
            cmd.startsWith("ATDP") -> "AUTO, ISO 15765-4 (CAN 11/500)"
            cmd == "0902" -> "49 02 01 31 48 47 43 4D 38 32 36 33 33 41 30 30 34 33 35 32" // VIN demo
            cmd == "0904" -> "49 04 01 44 45 4D 4F 2D 43 41 4C 2D 31 32 33"
            cmd == "0100" -> "41 00 BE 3E B8 11"
            cmd == "0101" -> "41 01 00 07 E5 00" // monitores
            cmd == "03" -> "43 03 03 01 04 20 01 71"   // P0301 P0420 P0171
            cmd == "07" -> "47 01 01 33"               // P0133 pendiente
            cmd == "0A" -> "4A 00"                     // sin permanentes
            cmd.startsWith("01") -> {
                // Responder un PID en vivo si lo conocemos.
                val info = PidDecoder.PIDS["01" + cmd.takeLast(2)]
                if (info != null) "41 ${cmd.takeLast(2)} 7F 80" else "NO DATA"
            }
            cmd.startsWith("AT") -> "OK"
            else -> "NO DATA"
        }
    }

    override val capabilities = ObdCapabilities(rawTerminal = true, canMonitor = true)
}
