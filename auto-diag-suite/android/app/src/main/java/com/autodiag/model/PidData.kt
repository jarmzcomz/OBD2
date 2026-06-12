package com.autodiag.model

/**
 * A single live PID reading ready to be rendered by the dashboard gauges.
 * [code] is the stable identifier used as a list key (the internal PID name).
 */
data class PidData(
    val code: String,
    val name: String,
    val value: Double,
    val min: Float,
    val max: Float,
    val unit: String,
    val dangerThreshold: Float = 0.85f
)

/** Display metadata for a PID, keyed by the internal name emitted by the decoder. */
data class PidMeta(
    val label: String,
    val unit: String,
    val min: Float,
    val max: Float,
    val danger: Float = 0.85f
)

val pidMetadata: Map<String, PidMeta> = mapOf(
    "rpm" to PidMeta("RPM", "rpm", 0f, 8000f, 0.85f),
    "coolant_temp" to PidMeta("Refrigerante", "°C", -40f, 130f, 0.85f),
    "speed" to PidMeta("Velocidad", "km/h", 0f, 220f, 0.95f),
    "throttle" to PidMeta("Acelerador", "%", 0f, 100f, 0.90f),
    "intake_temp" to PidMeta("Aire admisión", "°C", -40f, 90f, 0.90f),
    "engine_load" to PidMeta("Carga motor", "%", 0f, 100f, 0.90f),
    "maf" to PidMeta("MAF", "g/s", 0f, 300f, 0.90f),
    "fuel_level" to PidMeta("Combustible", "%", 0f, 100f, 0.15f),
    "voltage" to PidMeta("Batería", "V", 0f, 16f, 0.95f),
    // Combustible / mezcla
    "stft_b1" to PidMeta("STFT B1", "%", -25f, 25f, 0.85f),
    "ltft_b1" to PidMeta("LTFT B1", "%", -25f, 25f, 0.85f),
    "stft_b2" to PidMeta("STFT B2", "%", -25f, 25f, 0.85f),
    "ltft_b2" to PidMeta("LTFT B2", "%", -25f, 25f, 0.85f),
    "fuel_pressure" to PidMeta("Presión comb.", "kPa", 0f, 765f, 0.90f),
    "fuel_rail_p" to PidMeta("Riel comb.", "kPa", 0f, 65535f, 0.90f),
    "fuel_rate" to PidMeta("Consumo", "L/h", 0f, 100f, 0.90f),
    "afr" to PidMeta("AFR λ", "λ", 0f, 2f, 0.90f),
    // Presiones / aire
    "map" to PidMeta("MAP", "kPa", 0f, 255f, 0.90f),
    "baro" to PidMeta("Barométrica", "kPa", 0f, 255f, 0.95f),
    // Temperaturas
    "oil_temp" to PidMeta("Aceite", "°C", -40f, 150f, 0.85f),
    "ambient_temp" to PidMeta("Ambiente", "°C", -40f, 70f, 0.95f),
    // Posiciones / aceleración
    "timing_adv" to PidMeta("Avance", "°", -64f, 64f, 0.95f),
    "rel_throttle" to PidMeta("Acel. rel.", "%", 0f, 100f, 0.90f),
    "abs_throttle_b" to PidMeta("Acel. abs. B", "%", 0f, 100f, 0.90f),
    "accel_pedal_d" to PidMeta("Pedal D", "%", 0f, 100f, 0.90f),
    "cmd_throttle" to PidMeta("Acel. ordenado", "%", 0f, 100f, 0.90f),
    "egr" to PidMeta("EGR", "%", 0f, 100f, 0.90f),
    "abs_load" to PidMeta("Carga abs.", "%", 0f, 255f, 0.90f),
    // O2
    "o2_b1s1" to PidMeta("O2 B1S1", "V", 0f, 1.275f, 0.95f),
    "o2_b1s2" to PidMeta("O2 B1S2", "V", 0f, 1.275f, 0.95f),
    // Contadores
    "run_time" to PidMeta("Tiempo marcha", "s", 0f, 65535f, 1.0f),
    "dist_mil" to PidMeta("Dist. MIL", "km", 0f, 65535f, 1.0f),
    "dist_clear" to PidMeta("Dist. desde borrado", "km", 0f, 65535f, 1.0f),
)

/** Build a [PidData] from a decoded reading, falling back to sane defaults. */
fun pidDataFrom(name: String, value: Double): PidData {
    val meta = pidMetadata[name] ?: PidMeta(label = name, unit = "", min = 0f, max = 100f)
    return PidData(
        code = name,
        name = meta.label,
        value = value,
        min = meta.min,
        max = meta.max,
        unit = meta.unit,
        dangerThreshold = meta.danger
    )
}
