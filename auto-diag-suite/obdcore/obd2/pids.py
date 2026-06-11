"""Tabla de PIDs OBD-II (modo 01) con fórmulas de decodificación.

Cada PID define: nombre, comando ELM327, número de bytes de datos y una fórmula que
recibe la lista de bytes de datos (A, B, C, D...) y devuelve el valor en su unidad.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Callable


@dataclass(frozen=True)
class Pid:
    code: str          # comando OBD-II, p.ej. "010C"
    name: str
    unit: str
    decode: Callable[[list[int]], float]


PIDS: dict[str, Pid] = {
    "010C": Pid("010C", "rpm", "rpm", lambda d: (d[0] * 256 + d[1]) / 4),
    "0105": Pid("0105", "coolant_temp", "°C", lambda d: d[0] - 40),
    "010D": Pid("010D", "speed", "km/h", lambda d: float(d[0])),
    "0111": Pid("0111", "throttle", "%", lambda d: d[0] * 100 / 255),
    "010F": Pid("010F", "intake_temp", "°C", lambda d: d[0] - 40),
    "0104": Pid("0104", "engine_load", "%", lambda d: d[0] * 100 / 255),
    "0110": Pid("0110", "maf", "g/s", lambda d: (d[0] * 256 + d[1]) / 100),
    "012F": Pid("012F", "fuel_level", "%", lambda d: d[0] * 100 / 255),
    "0142": Pid("0142", "voltage", "V", lambda d: (d[0] * 256 + d[1]) / 1000),
}

# Acceso por nombre legible además de por código.
PIDS_BY_NAME: dict[str, Pid] = {p.name: p for p in PIDS.values()}
