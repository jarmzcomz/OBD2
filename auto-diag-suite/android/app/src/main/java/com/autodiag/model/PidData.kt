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
    "coolant_temp" to PidMeta("Coolant", "°C", -40f, 130f, 0.85f),
    "speed" to PidMeta("Speed", "km/h", 0f, 220f, 0.95f),
    "throttle" to PidMeta("Throttle", "%", 0f, 100f, 0.90f),
    "intake_temp" to PidMeta("Intake Air", "°C", -40f, 90f, 0.90f),
    "engine_load" to PidMeta("Engine Load", "%", 0f, 100f, 0.90f),
    "maf" to PidMeta("MAF", "g/s", 0f, 300f, 0.90f),
    "fuel_level" to PidMeta("Fuel", "%", 0f, 100f, 0.15f),
    "voltage" to PidMeta("Battery", "V", 0f, 16f, 0.95f),
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
