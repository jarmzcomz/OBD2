"""Sniffer de bus CAN con soporte para modo simulado y SocketCAN.

En modo sim genera tramas sintéticas sin hardware para que tests y CI funcionen
en cualquier máquina. En modo real utiliza python-can con el canal indicado.
"""

from __future__ import annotations

import math
import os
import threading
import time
from dataclasses import dataclass
from typing import Callable, Iterator


@dataclass
class CanFrame:
    """Trama CAN capturada (o simulada)."""

    timestamp: float
    arbitration_id: int
    data: bytes
    channel: str = ""
    is_extended_id: bool = False

    def __repr__(self) -> str:
        hex_data = " ".join(f"{b:02X}" for b in self.data)
        return (
            f"CanFrame(ts={self.timestamp:.6f}, "
            f"id={self.arbitration_id:#05x}, data=[{hex_data}])"
        )


# Tipo de callback: recibe cada trama capturada
FrameCallback = Callable[[CanFrame], None]


class CanSniffer:
    """Captura tramas del bus CAN.

    Args:
        channel: interfaz CAN (p.ej. ``"vcan0"``, ``"can0"``).
                 Ignorado en modo sim.
        bustype: tipo de bus python-can (``"socketcan"``, ``"kvaser"``…).
        simulated: si True (o ``ADS_TRANSPORT=sim``) genera tramas sintéticas.
        frame_callback: función opcional llamada por cada trama en el hilo de captura.
    """

    def __init__(
        self,
        channel: str = "vcan0",
        bustype: str = "socketcan",
        simulated: bool | None = None,
        frame_callback: FrameCallback | None = None,
    ) -> None:
        self.channel = channel
        self.bustype = bustype
        if simulated is None:
            simulated = os.environ.get("ADS_TRANSPORT", "sim").lower() == "sim"
        self.simulated = simulated
        self.frame_callback = frame_callback

        self._bus = None
        self._running = False
        self._thread: threading.Thread | None = None
        self._frames: list[CanFrame] = []
        self._lock = threading.Lock()
        self._t0 = time.monotonic()

    # ------------------------------------------------------------------
    # Control
    # ------------------------------------------------------------------

    def start(self) -> None:
        """Abre el bus y lanza el hilo de captura."""
        if self._running:
            return
        if not self.simulated:
            import can  # import perezoso: sólo con hardware

            self._bus = can.Bus(channel=self.channel, bustype=self.bustype)
        self._running = True
        self._thread = threading.Thread(target=self._capture_loop, daemon=True)
        self._thread.start()

    def stop(self) -> None:
        """Detiene la captura y cierra el bus."""
        self._running = False
        if self._thread is not None:
            self._thread.join(timeout=2.0)
            self._thread = None
        if self._bus is not None:
            self._bus.shutdown()
            self._bus = None

    # ------------------------------------------------------------------
    # Acceso a tramas capturadas
    # ------------------------------------------------------------------

    def frames(self) -> list[CanFrame]:
        """Devuelve una copia de las tramas capturadas hasta ahora."""
        with self._lock:
            return list(self._frames)

    def clear(self) -> None:
        """Vacía el buffer de tramas."""
        with self._lock:
            self._frames.clear()

    def iter_frames(self, timeout: float = 1.0) -> Iterator[CanFrame]:
        """Generador: produce tramas durante *timeout* segundos."""
        deadline = time.monotonic() + timeout
        seen = 0
        while time.monotonic() < deadline:
            with self._lock:
                current = self._frames[seen:]
            for f in current:
                yield f
                seen += 1
            time.sleep(0.01)

    # ------------------------------------------------------------------
    # Hilo de captura
    # ------------------------------------------------------------------

    def _capture_loop(self) -> None:
        if self.simulated:
            self._sim_loop()
        else:
            self._real_loop()

    def _real_loop(self) -> None:
        while self._running:
            try:
                msg = self._bus.recv(timeout=0.1)
                if msg is None:
                    continue
                frame = CanFrame(
                    timestamp=msg.timestamp,
                    arbitration_id=msg.arbitration_id,
                    data=bytes(msg.data),
                    channel=str(msg.channel or self.channel),
                    is_extended_id=msg.is_extended_id,
                )
                self._store(frame)
            except Exception:
                if self._running:
                    time.sleep(0.05)

    def _sim_loop(self) -> None:
        """Genera tramas sintéticas plausibles a ~10 Hz sin hardware."""
        interval = 0.1  # 10 Hz
        while self._running:
            t = time.monotonic() - self._t0
            frames = _generate_sim_frames(t)
            for f in frames:
                self._store(f)
            time.sleep(interval)

    def _store(self, frame: CanFrame) -> None:
        with self._lock:
            self._frames.append(frame)
        if self.frame_callback is not None:
            try:
                self.frame_callback(frame)
            except Exception:
                pass

    # ------------------------------------------------------------------
    # Context manager
    # ------------------------------------------------------------------

    def __enter__(self) -> "CanSniffer":
        self.start()
        return self

    def __exit__(self, *exc) -> None:
        self.stop()


# ---------------------------------------------------------------------------
# Generador de tramas simuladas
# ---------------------------------------------------------------------------

def _generate_sim_frames(t: float) -> list[CanFrame]:
    """Devuelve un pequeño conjunto de tramas CAN sintéticas en el instante *t*."""
    ts = time.time()
    frames = []

    # 0x100: RPM (2 bytes big-endian, unidad 0.25 rpm/LSB)
    rpm = int((800 + 600 * (1 + math.sin(t / 2))) * 4) & 0xFFFF
    frames.append(CanFrame(
        timestamp=ts,
        arbitration_id=0x100,
        data=bytes([rpm >> 8, rpm & 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]),
        channel="sim",
    ))

    # 0x200: velocidad + temperatura
    speed = max(0, int(40 + 30 * math.sin(t / 5))) & 0xFF
    temp = (int(80 + 10 * math.sin(t / 10)) + 40) & 0xFF
    frames.append(CanFrame(
        timestamp=ts,
        arbitration_id=0x200,
        data=bytes([speed, temp, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]),
        channel="sim",
    ))

    # 0x7E8: respuesta UDS simulada genérica (TesterPresent positivo)
    frames.append(CanFrame(
        timestamp=ts,
        arbitration_id=0x7E8,
        data=bytes([0x02, 0x7E, 0x00, 0xAA, 0xAA, 0xAA, 0xAA, 0xAA]),
        channel="sim",
    ))

    return frames
