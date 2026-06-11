"""Transporte ELM327 sobre serie/Bluetooth, con un modo simulado integrado.

El modo simulado (``simulated=True`` o env ``ADS_TRANSPORT=sim``) responde PIDs
sintéticos sin hardware, de forma que tests y CI corran en cualquier máquina.
"""

from __future__ import annotations

import math
import os
import time

from .base import Transport


class Elm327Transport(Transport):
    def __init__(self, port: str | None = None, baudrate: int = 38400,
                 simulated: bool | None = None) -> None:
        self.port = port or os.environ.get("ADS_SERIAL_PORT")
        self.baudrate = baudrate
        if simulated is None:
            simulated = os.environ.get("ADS_TRANSPORT", "sim").lower() == "sim"
        self.simulated = simulated
        self._serial = None
        self._open = False
        self._t0 = time.monotonic()

    @property
    def is_open(self) -> bool:
        return self._open

    def open(self) -> None:
        if self.simulated:
            self._open = True
            return
        import serial  # import perezoso: sólo si hay hardware

        self._serial = serial.Serial(self.port, self.baudrate, timeout=2)
        for cmd in (b"ATZ\r", b"ATE0\r", b"ATL0\r", b"ATSP0\r"):
            self._serial.write(cmd)
            time.sleep(0.1)
            self._serial.read(self._serial.in_waiting or 1)
        self._open = True

    def close(self) -> None:
        if self._serial is not None:
            self._serial.close()
        self._open = False

    def query(self, request: bytes) -> bytes:
        if not self._open:
            raise RuntimeError("transporte no abierto")
        if self.simulated:
            return self._simulate(request.strip().upper())
        self._serial.write(request.strip() + b"\r")
        return _read_until_prompt(self._serial)

    # --- simulación ----------------------------------------------------------
    def _simulate(self, req: bytes) -> bytes:
        """Genera respuestas ELM327 plausibles para unos cuantos PIDs y servicios."""
        t = time.monotonic() - self._t0
        if req == b"010C":  # RPM: (A*256+B)/4
            rpm = 800 + 600 * (1 + math.sin(t / 2))
            raw = int(rpm * 4) & 0xFFFF
            return _resp("41 0C", [raw >> 8, raw & 0xFF])
        if req == b"0105":  # temperatura refrigerante: A-40
            temp = 80 + 10 * math.sin(t / 10)
            return _resp("41 05", [int(temp) + 40])
        if req == b"010D":  # velocidad: A
            speed = max(0, int(40 + 30 * math.sin(t / 5)))
            return _resp("41 0D", [speed & 0xFF])
        if req == b"03":  # DTCs almacenados: un P0301 de ejemplo
            return _resp("43", [0x01, 0x03, 0x01])
        if req == b"04":  # borrar DTCs
            return b"44\r\r>"
        if req == b"0902":  # VIN (simplificado)
            return b"49 02 01 31 32 33 34\r\r>"
        if req == b"0111":  # posición del acelerador: A*100/255, oscila 5-80 %
            pct = 5 + 75 * (0.5 + 0.5 * math.sin(t / 3))
            return _resp("41 11", [int(pct * 255 / 100) & 0xFF])
        if req == b"010F":  # temperatura del aire de admisión: A-40, varía 20-40 °C
            temp = 30 + 10 * math.sin(t / 20)
            return _resp("41 0F", [int(temp) + 40])
        if req == b"0104":  # carga del motor: A*100/255, correlacionada con RPM
            rpm = 800 + 600 * (1 + math.sin(t / 2))
            load = rpm / 2800 * 100  # aprox. proporcional a RPM
            return _resp("41 04", [int(load * 255 / 100) & 0xFF])
        if req == b"0110":  # flujo MAF: ((A*256)+B)/100, simula 10-200 g/s
            maf = 10 + 190 * (0.5 + 0.5 * math.sin(t / 2))
            raw = int(maf * 100) & 0xFFFF
            return _resp("41 10", [raw >> 8, raw & 0xFF])
        if req == b"0114":  # sensor de oxígeno: A*0.005 V, B=0 trim, rango 0-1.25 V
            volt = 0.625 + 0.625 * math.sin(t / 1.5)
            a = int(volt / 0.005) & 0xFF
            return _resp("41 14", [a, 0x00])
        if req == b"012F":  # nivel de combustible: A*100/255, fijo al 75 %
            a = int(75 * 255 / 100) & 0xFF
            return _resp("41 2F", [a])
        if req == b"0142":  # voltaje del módulo de control: ((A*256)+B)/1000, ~13.8 V
            raw = int(13.8 * 1000) & 0xFFFF
            return _resp("41 42", [raw >> 8, raw & 0xFF])
        return b"NO DATA\r\r>"


def _resp(header: str, data: list[int]) -> bytes:
    body = header + " " + " ".join(f"{b:02X}" for b in data)
    return (body + "\r\r>").encode()


def _read_until_prompt(serial) -> bytes:
    buf = bytearray()
    while True:
        chunk = serial.read(1)
        if not chunk:
            break
        buf += chunk
        if chunk == b">":
            break
    return bytes(buf)
