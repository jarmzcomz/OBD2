"""Gate Zero-Brick: ninguna escritura UDS ocurre sin un backup verificado.

Esta política es NO NEGOCIABLE. Las rutinas de escritura/erase del cliente UDS deben
llamar a ``assert_writable`` antes de tocar la ECU. No existe forma de saltarlo.
"""

from __future__ import annotations

from .backup import EcuBackup


class ZeroBrickError(RuntimeError):
    """Se intentó escribir sin un respaldo íntegro y verificado."""


def assert_writable(backup: EcuBackup | None, fresh_data: bytes,
                    ecu_addr: str, did_or_region: str) -> None:
    """Lanza ZeroBrickError si no hay backup válido para esta región/ECU.

    Args:
        backup: respaldo previamente tomado (o None si no existe).
        fresh_data: relectura inmediata de la misma región para revalidar el hash.
        ecu_addr / did_or_region: deben coincidir con el backup.
    """
    if backup is None:
        raise ZeroBrickError(
            "Escritura bloqueada: no existe respaldo. Toma un EcuBackup antes de escribir."
        )
    if backup.ecu_addr != ecu_addr or backup.did_or_region != did_or_region:
        raise ZeroBrickError(
            "Escritura bloqueada: el respaldo no corresponde a esta ECU/región."
        )
    if not backup.verify(fresh_data):
        raise ZeroBrickError(
            "Escritura bloqueada: el hash del respaldo no coincide con la relectura actual."
        )
    # OK: respaldo íntegro y verificado. La escritura puede proceder.
