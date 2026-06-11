"""Descubrimiento NO destructivo de módulos VAG y enumeración de sus DIDs.

Sólo emite TesterPresent (0x3E) y ReadDataByIdentifier (0x22). NUNCA escribe durante
el escaneo.
"""

from __future__ import annotations

from typing import Callable

from .modules import VAG_MODULES, VagModule

SID_READ_DATA_BY_IDENTIFIER = 0x22
SID_TESTER_PRESENT = 0x3E
_POSITIVE_OFFSET = 0x40

# DID estándar de número de pieza VAG (System Name / Part Number).
DID_PART_NUMBER = 0xF187


class ModuleScanner:
    """Descubre qué módulos VAG responden y enumera sus DIDs disponibles."""

    def __init__(self, sender_factory: Callable[[VagModule], Callable[[bytes], bytes]]):
        # sender_factory(module) -> Callable[[bytes], bytes]
        # devuelve un sender UDS (bytes->bytes) por módulo. En simulación,
        # produce respuestas sintéticas.
        self._sender_factory = sender_factory

    @staticmethod
    def _is_positive(resp: bytes, sid: int) -> bool:
        return len(resp) >= 1 and resp[0] == (sid + _POSITIVE_OFFSET)

    def _read_did(self, sender: Callable[[bytes], bytes], did: int) -> bytes | None:
        """Devuelve el payload de un 0x22 positivo, o None si respondió negativo/nada."""
        req = bytes([SID_READ_DATA_BY_IDENTIFIER, did >> 8, did & 0xFF])
        resp = sender(req)
        if self._is_positive(resp, SID_READ_DATA_BY_IDENTIFIER) and len(resp) >= 3:
            return resp[3:]
        return None

    def scan_modules(self) -> list[dict]:
        """Sondea cada módulo con TesterPresent + ReadDataByIdentifier 0xF187.

        Returns:
            [{address, name, present: bool, part_number: str|None}]
        """
        results: list[dict] = []
        for addr, module in VAG_MODULES.items():
            sender = self._sender_factory(module)
            present = False
            part_number: str | None = None
            try:
                tp = sender(bytes([SID_TESTER_PRESENT, 0x00]))
                present = self._is_positive(tp, SID_TESTER_PRESENT)
                payload = self._read_did(sender, DID_PART_NUMBER)
                if payload is not None:
                    present = True
                    part_number = payload.decode("ascii", errors="replace").strip("\x00 ")
            except Exception:
                present = False
            results.append({
                "address": addr,
                "name": module.name,
                "present": present,
                "part_number": part_number or None,
            })
        return results

    def scan_dids(self, module: VagModule, start: int = 0xF180,
                  end: int = 0xF1FF) -> list[dict]:
        """Enumera DIDs en [start, end] leyendo 0x22 (sólo lectura, no escribe).

        Returns:
            [{did: '0xF190', size: int, value_hex: str, readable: True}]
        """
        sender = self._sender_factory(module)
        found: list[dict] = []
        for did in range(start, end + 1):
            payload = self._read_did(sender, did)
            if payload is not None:
                found.append({
                    "did": f"0x{did:04X}",
                    "size": len(payload),
                    "value_hex": payload.hex(),
                    "readable": True,
                })
        return found
