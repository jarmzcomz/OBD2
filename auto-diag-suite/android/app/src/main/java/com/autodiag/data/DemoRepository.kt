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
}
