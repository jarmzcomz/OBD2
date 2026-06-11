"""Transporte SocketCAN vía python-can (incluye bus ``virtual`` para tests)."""

from __future__ import annotations

import os

from .base import Transport


class SocketCanTransport(Transport):
    """Envoltura mínima sobre python-can. Usa el bus ``virtual`` cuando no hay hardware."""

    def __init__(self, channel: str | None = None, interface: str | None = None,
                 fd: bool = False) -> None:
        self.channel = channel or os.environ.get("ADS_CAN_CHANNEL", "vcan0")
        # 'virtual' permite tests sin kernel SocketCAN; 'socketcan' para hardware real.
        self.interface = interface or os.environ.get("ADS_CAN_INTERFACE", "virtual")
        self.fd = fd
        self._bus = None

    @property
    def is_open(self) -> bool:
        return self._bus is not None

    def open(self) -> None:
        import can  # import perezoso

        self._bus = can.Bus(channel=self.channel, interface=self.interface, fd=self.fd)

    def close(self) -> None:
        if self._bus is not None:
            self._bus.shutdown()
            self._bus = None

    def query(self, request: bytes) -> bytes:
        # Para flujos OBD-II/UDS reales se usa ISO-TP (ver transport/isotp.py).
        # Aquí exponemos send/recv de una sola trama para sniffing/diagnóstico básico.
        raise NotImplementedError(
            "Use ISO-TP (obdcore.transport.isotp) para peticiones OBD-II/UDS sobre CAN."
        )

    def send_frame(self, arb_id: int, data: bytes) -> None:
        import can

        self._bus.send(can.Message(arbitration_id=arb_id, data=data, is_fd=self.fd))

    def recv_frame(self, timeout: float = 1.0):
        return self._bus.recv(timeout=timeout)
