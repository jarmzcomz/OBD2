package com.autodiag.model

data class DtcCode(
    val code: String,
    val description: String = "",
    val severity: DtcSeverity = DtcSeverity.UNKNOWN
)

enum class DtcSeverity {
    UNKNOWN, LOW, MEDIUM, HIGH
}

fun DtcCode.inferSeverity(): DtcSeverity {
    return when {
        code.startsWith("P0") -> DtcSeverity.MEDIUM
        code.startsWith("P1") -> DtcSeverity.HIGH
        code.startsWith("P2") -> DtcSeverity.MEDIUM
        code.startsWith("P3") -> DtcSeverity.LOW
        code.startsWith("C") -> DtcSeverity.HIGH
        code.startsWith("B") -> DtcSeverity.LOW
        code.startsWith("U") -> DtcSeverity.MEDIUM
        else -> DtcSeverity.UNKNOWN
    }
}

// Common DTC descriptions lookup
val dtcDescriptions = mapOf(
    "P0100" to "Mass Air Flow Sensor Circuit Malfunction",
    "P0101" to "Mass Air Flow Sensor Circuit Range/Performance",
    "P0105" to "Manifold Absolute Pressure Circuit Malfunction",
    "P0110" to "Intake Air Temperature Circuit Malfunction",
    "P0115" to "Engine Coolant Temperature Circuit Malfunction",
    "P0120" to "Throttle Position Sensor Circuit Malfunction",
    "P0130" to "O2 Sensor Circuit Malfunction (Bank 1, Sensor 1)",
    "P0171" to "System Too Lean (Bank 1)",
    "P0172" to "System Too Rich (Bank 1)",
    "P0300" to "Random/Multiple Cylinder Misfire Detected",
    "P0301" to "Cylinder 1 Misfire Detected",
    "P0302" to "Cylinder 2 Misfire Detected",
    "P0303" to "Cylinder 3 Misfire Detected",
    "P0304" to "Cylinder 4 Misfire Detected",
    "P0400" to "Exhaust Gas Recirculation Flow Malfunction",
    "P0420" to "Catalyst System Efficiency Below Threshold",
    "P0440" to "Evaporative Emission Control System Malfunction",
    "P0500" to "Vehicle Speed Sensor Malfunction",
    "P0505" to "Idle Control System Malfunction",
    "P0600" to "Serial Communication Link Malfunction",
    "P0700" to "Transmission Control System Malfunction",
)
