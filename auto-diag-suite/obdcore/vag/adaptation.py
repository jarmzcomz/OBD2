"""Lectura/escritura de canales de adaptación VAG vía UDS.

TODA escritura pasa por el gate Zero-Brick (assert_writable, vía
UdsServices.write_data_by_identifier). No se calcula ni fuerza SecurityAccess:
se asume que el usuario ya concedió acceso con una llave legítima.
"""

from __future__ import annotations

from ..safety.backup import EcuBackup


class AdaptationChannel:
    """Descriptor de un canal de adaptación VAG (DID + metadatos opcionales)."""

    def __init__(self, did: int, name: str = "", desc: str = "") -> None:
        self.did = did
        self.name = name
        self.desc = desc

    def __repr__(self) -> str:
        return f"AdaptationChannel(did={self.did:#06x}, name={self.name!r})"


class AdaptationManager:
    """Lee y escribe canales de adaptación VAG vía UDS.

    Args:
        uds_services: instancia de UdsServices (provee read/write_data_by_identifier
                      y backup_did, con el gate Zero-Brick integrado).
        db: almacenamiento opcional para auditar cambios (no obligatorio).
    """

    def __init__(self, uds_services, db=None) -> None:
        self.uds = uds_services
        self.db = db

    def read_channel(self, did: int) -> bytes:
        """Lee el valor actual de un canal de adaptación (0x22, sólo lectura)."""
        return self.uds.read_data_by_identifier(did)

    def write_channel(self, did: int, data: bytes, backup: EcuBackup | None) -> bool:
        """Escribe un canal. Requiere backup verificado (Zero-Brick).

        Delega en uds.write_data_by_identifier, que re-lee el DID, llama a
        assert_writable y sólo entonces escribe. Sin backup válido -> ZeroBrickError.
        """
        ok = self.uds.write_data_by_identifier(did, data, backup)
        if ok and self.db is not None:
            try:
                self.db.log_adaptation(
                    ecu_addr=getattr(self.uds, "ecu_addr", "?"),
                    did=did,
                    new_value=data.hex(),
                )
            except Exception:
                pass  # la auditoría no debe romper la operación
        return ok
