"""Decodificación de códigos de falla (DTC) OBD-II a su forma P/C/B/U."""

from __future__ import annotations

_PREFIX = {0: "P", 1: "C", 2: "B", 3: "U"}


def decode_dtc(byte_a: int, byte_b: int) -> str:
    """Convierte 2 bytes en un código DTC (p.ej. 0x01 0x03 -> "P0103").

    Bits 7-6 de A -> letra del sistema; bits 5-4 -> primer dígito; resto -> hex.
    """
    letter = _PREFIX[(byte_a & 0xC0) >> 6]
    d1 = (byte_a & 0x30) >> 4
    d2 = byte_a & 0x0F
    d3 = (byte_b & 0xF0) >> 4
    d4 = byte_b & 0x0F
    return f"{letter}{d1}{d2:X}{d3:X}{d4:X}"


def parse_dtc_response(data: list[int]) -> list[str]:
    """Toma los bytes de datos de un servicio 03 y devuelve la lista de DTCs."""
    codes = []
    for i in range(0, len(data) - 1, 2):
        if data[i] == 0 and data[i + 1] == 0:
            continue
        codes.append(decode_dtc(data[i], data[i + 1]))
    return codes
