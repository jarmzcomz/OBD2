"""Decodificación de tramas CAN a señales usando un archivo DBC (cantools)."""

from __future__ import annotations


class CanDecoder:
    """Envoltura sobre cantools. El DBC se carga sólo si se va a decodificar."""

    def __init__(self, dbc_path: str | None = None) -> None:
        self.dbc_path = dbc_path
        self._db = None
        if dbc_path:
            self.load(dbc_path)

    def load(self, dbc_path: str) -> None:
        import cantools  # import perezoso

        self._db = cantools.database.load_file(dbc_path)
        self.dbc_path = dbc_path

    def decode(self, arb_id: int, data: bytes) -> dict:
        """Decodifica una trama a un dict de señales. Requiere DBC cargado."""
        if self._db is None:
            raise RuntimeError("No hay DBC cargado. Llama a load() primero.")
        return self._db.decode_message(arb_id, data)
