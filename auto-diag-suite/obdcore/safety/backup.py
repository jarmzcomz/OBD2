"""Respaldo íntegro previo a cualquier escritura (Zero-Brick Protocol)."""

from __future__ import annotations

import hashlib
import time
from dataclasses import dataclass


@dataclass(frozen=True)
class EcuBackup:
    ecu_addr: str
    did_or_region: str
    data: bytes
    sha256: str
    created_at: float

    @classmethod
    def create(cls, ecu_addr: str, did_or_region: str, data: bytes) -> "EcuBackup":
        return cls(
            ecu_addr=ecu_addr,
            did_or_region=did_or_region,
            data=data,
            sha256=hashlib.sha256(data).hexdigest(),
            created_at=time.time(),
        )

    def verify(self, fresh_data: bytes) -> bool:
        """Compara el backup contra una relectura fresca de la ECU."""
        return hashlib.sha256(fresh_data).hexdigest() == self.sha256
