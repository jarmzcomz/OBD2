"""Capa de transporte: ELM327 (con modo sim) y SocketCAN."""

from __future__ import annotations

import os

from .base import Transport
from .elm327 import Elm327Transport


def make_transport(kind: str | None = None) -> Transport:
    """Factory de transporte según ``ADS_TRANSPORT`` (``sim``/``elm327``/``socketcan``)."""
    kind = (kind or os.environ.get("ADS_TRANSPORT", "sim")).lower()
    if kind in ("sim", "elm327"):
        return Elm327Transport(simulated=(kind == "sim"))
    if kind == "socketcan":
        from .isotp import SocketCanTransport

        return SocketCanTransport()
    raise ValueError(f"transporte desconocido: {kind}")


__all__ = ["Transport", "Elm327Transport", "make_transport"]
