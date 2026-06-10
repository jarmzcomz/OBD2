"""Interfaz común de transporte. Todo acceso al bus pasa por aquí."""

from __future__ import annotations

from abc import ABC, abstractmethod


class Transport(ABC):
    """Contrato mínimo de un transporte hacia el vehículo o un simulador."""

    @abstractmethod
    def open(self) -> None:
        """Abre la conexión (puerto serie, canal CAN, socket DoIP...)."""

    @abstractmethod
    def close(self) -> None:
        """Cierra la conexión y libera recursos."""

    @abstractmethod
    def query(self, request: bytes) -> bytes:
        """Envía una petición y devuelve la respuesta cruda.

        Para OBD-II/ELM327 la petición suele ser ASCII (p.ej. b"010C").
        Las implementaciones de bus binario pueden envolver tramas según su capa.
        """

    @property
    @abstractmethod
    def is_open(self) -> bool: ...

    def __enter__(self) -> "Transport":
        self.open()
        return self

    def __exit__(self, *exc) -> None:
        self.close()
