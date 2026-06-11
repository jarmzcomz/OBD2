"""Cliente OBD-II: lectura de PIDs en vivo y de DTCs (modos 01/03/04)."""

from __future__ import annotations

from ..transport.base import Transport
from .dtc import parse_dtc_response
from .pids import PIDS, PIDS_BY_NAME, Pid


def _parse_hex_bytes(raw: bytes) -> list[int]:
    """Extrae bytes hex de una respuesta ELM327 ASCII ('41 0C 1A F0' -> [...])."""
    text = raw.decode(errors="ignore").replace("\r", " ").replace(">", " ")
    if "NO DATA" in text or "UNABLE" in text:
        return []
    out = []
    for tok in text.split():
        if len(tok) == 2:
            try:
                out.append(int(tok, 16))
            except ValueError:
                pass
    return out


class Obd2Client:
    def __init__(self, transport: Transport) -> None:
        self.transport = transport

    def read_pid(self, code_or_name: str) -> float | None:
        """Lee un PID por código ("010C") o nombre ("rpm"). Devuelve valor o None."""
        pid: Pid | None = PIDS.get(code_or_name) or PIDS_BY_NAME.get(code_or_name)
        if pid is None:
            raise KeyError(f"PID desconocido: {code_or_name}")
        raw = self.transport.query(pid.code.encode())
        data = _parse_hex_bytes(raw)
        # Respuesta válida: 0x41 + nº de PID + datos. Descartamos las 2 cabeceras.
        if len(data) < 3 or data[0] != 0x41:
            return None
        return round(pid.decode(data[2:]), 2)

    def read_all_pids(self) -> dict[str, float]:
        """Lee todos los PIDs conocidos. Útil para el stream del dashboard."""
        result = {}
        for pid in PIDS.values():
            val = self.read_pid(pid.code)
            if val is not None:
                result[pid.name] = val
        return result

    def read_dtcs(self) -> list[str]:
        """Servicio 03: lee DTCs almacenados."""
        raw = self.transport.query(b"03")
        data = _parse_hex_bytes(raw)
        if not data or data[0] != 0x43:
            return []
        return parse_dtc_response(data[1:])

    def clear_dtcs(self) -> bool:
        """Servicio 04: borra DTCs y la luz MIL. Operación destructiva."""
        raw = self.transport.query(b"04")
        return b"44" in raw
