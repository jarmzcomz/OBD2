"""Mapa de módulos de diagnóstico VAG para un VW T-Cross 1.0 TSI (plataforma MQB-A0).

Direccionamiento UDS sobre CAN. Para el motor/transmisión se usan los IDs OBD-II
"legacy" (0x7E0/0x7E8...). El resto de módulos VAG MQB usan direccionamiento UDS
extendido. Donde los IDs reales no estén confirmados se usa el patrón aproximado
0x700+addr / 0x700+addr+8 y se marca con una nota para VERIFICAR contra el hardware
real del vehículo.

ALCANCE LEGÍTIMO: coding/adaptación del propietario (estilo VCDS/OBDeleven). No se
incluyen mecanismos de cracking de SecurityAccess, cálculo de llaves, Immo Off ni
manipulación de firmware/odómetro.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class VagModule:
    address: str       # dirección de diagnóstico VAG, p.ej. "0x09"
    can_id_req: int    # arbitration ID request
    can_id_resp: int   # arbitration ID response
    name: str          # nombre completo legible
    short: str         # abreviatura tipo VCDS


def _approx(addr: int) -> tuple[int, int]:
    """Patrón aproximado 0x700+addr / 0x700+addr+8. VERIFICAR con hardware real."""
    req = 0x700 + addr
    return req, req + 8


# Mapa realista de módulos MQB-A0 (T-Cross). Los IDs marcados con la nota "approx"
# usan el patrón _approx() y deben confirmarse leyendo el bus real del vehículo.
VAG_MODULES: dict[str, VagModule] = {
    # Motor y transmisión: direccionamiento OBD-II legacy confirmado.
    "0x01": VagModule("0x01", 0x7E0, 0x7E8, "Engine Control (1.0 TSI)", "ECU"),
    "0x02": VagModule("0x02", 0x7E1, 0x7E9, "Transmission Electronics", "TCU"),
    # ABS/ESP: IDs UDS extendidos confirmados en MQB.
    "0x03": VagModule("0x03", 0x713, 0x77D, "ABS / ESP Brakes", "ABS"),
    "0x08": VagModule("0x08", 0x746, 0x74E, "Climatronic / HVAC", "HVAC"),
    # BCM: aquí viven luces/DRL/intermitentes confort.
    "0x09": VagModule("0x09", 0x70E, 0x77E, "Central Electrics (BCM)", "BCM"),
    "0x10": VagModule("0x10", 0x710, 0x77A, "Parking Aid / ACC", "ParkAid"),
    "0x13": VagModule("0x13", *_approx(0x13), "Adaptive Cruise Control", "ACC"),  # approx
    "0x15": VagModule("0x15", 0x715, 0x77F, "Airbag", "Airbag"),
    "0x16": VagModule("0x16", *_approx(0x16), "Steering Column Electronics", "SteerCol"),  # approx
    "0x17": VagModule("0x17", 0x714, 0x77C, "Instrument Cluster", "Cluster"),
    "0x19": VagModule("0x19", 0x710, 0x77A, "Gateway (CAN)", "Gateway"),  # approx, comparte rango
    "0x1B": VagModule("0x1B", *_approx(0x1B), "Active Steering", "ActSteer"),  # approx
    "0x25": VagModule("0x25", *_approx(0x25), "Immobilizer", "Immo"),  # approx (solo lectura)
    "0x26": VagModule("0x26", *_approx(0x26), "Auto Roof Electronics", "Roof"),  # approx
    "0x42": VagModule("0x42", *_approx(0x42), "Door Electronics Driver", "DoorDrv"),  # approx
    "0x44": VagModule("0x44", 0x712, 0x77B, "Power Steering", "Steering"),
    "0x46": VagModule("0x46", *_approx(0x46), "Comfort System (Komfort)", "Comfort"),  # approx
    "0x47": VagModule("0x47", *_approx(0x47), "Sound System", "Sound"),  # approx
    "0x52": VagModule("0x52", *_approx(0x52), "Door Electronics Passenger", "DoorPas"),  # approx
    "0x53": VagModule("0x53", *_approx(0x53), "Parking Brake", "ParkBrake"),  # approx
    "0x5F": VagModule("0x5F", 0x773, 0x7DD, "Information Electronics (MIB)", "Infotain"),
    "0x62": VagModule("0x62", *_approx(0x62), "Door Electronics Rear Left", "DoorRL"),  # approx
    "0x6C": VagModule("0x6C", *_approx(0x6C), "Back-up Camera", "RearCam"),  # approx
    "0x72": VagModule("0x72", *_approx(0x72), "Door Electronics Rear Right", "DoorRR"),  # approx
    "0x75": VagModule("0x75", *_approx(0x75), "Telematics / Emergency Call", "Telematics"),
    "0x77": VagModule("0x77", *_approx(0x77), "Telephone", "Phone"),  # approx
    "0xA5": VagModule("0xA5", *_approx(0xA5), "Front Sensors Driver Assist", "FrontAssist"),
}
