"""Servicios UDS (ISO 14229) de diagnóstico legítimo.

Sólo lectura y escritura estándar. NO incluye cálculo de Seed/Key, Immo Off ni bypass
de HSM. La escritura SIEMPRE pasa por el gate Zero-Brick (obdcore.safety).
"""

from __future__ import annotations

from typing import Callable

from ..safety.backup import EcuBackup
from ..safety.guard import assert_writable

# SIDs de UDS usados.
SID_DIAGNOSTIC_SESSION_CONTROL = 0x10
SID_READ_DATA_BY_IDENTIFIER = 0x22
SID_READ_DTC = 0x19
SID_WRITE_DATA_BY_IDENTIFIER = 0x2E
SID_TESTER_PRESENT = 0x3E


class UdsServices:
    """Operaciones UDS sobre un 'sender' que envía request y devuelve response.

    ``sender`` es una función ``bytes -> bytes`` (p.ej. ISO-TP sobre python-can o un
    cliente udsoncan). Se inyecta para mantener el núcleo testeable sin hardware.
    """

    def __init__(self, sender: Callable[[bytes], bytes], ecu_addr: str = "0x7E0") -> None:
        self.sender = sender
        self.ecu_addr = ecu_addr

    def read_data_by_identifier(self, did: int) -> bytes:
        req = bytes([SID_READ_DATA_BY_IDENTIFIER, did >> 8, did & 0xFF])
        resp = self.sender(req)
        return resp[3:] if len(resp) >= 3 else b""

    def backup_did(self, did: int) -> EcuBackup:
        """Lee un DID y lo guarda como respaldo verificable. Pre-requisito de escritura."""
        data = self.read_data_by_identifier(did)
        return EcuBackup.create(self.ecu_addr, f"DID:{did:#06x}", data)

    def write_data_by_identifier(self, did: int, data: bytes,
                                 backup: EcuBackup | None) -> bool:
        """WriteDataByIdentifier (0x2E) — BLOQUEADO sin respaldo verificado.

        Revalida el respaldo releyendo el DID antes de escribir (Zero-Brick).
        Asume que ya se concedió SecurityAccess con una llave legítima del usuario;
        este código NO calcula ni fuerza llaves.
        """
        region = f"DID:{did:#06x}"
        fresh = self.read_data_by_identifier(did)
        assert_writable(backup, fresh, self.ecu_addr, region)
        req = bytes([SID_WRITE_DATA_BY_IDENTIFIER, did >> 8, did & 0xFF]) + data
        resp = self.sender(req)
        return len(resp) >= 1 and resp[0] == (SID_WRITE_DATA_BY_IDENTIFIER + 0x40)
