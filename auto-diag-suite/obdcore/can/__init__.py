"""Sniffing y decodificación CAN (vía DBC con cantools, cuando hay hardware)."""

from .decoder import CanDecoder

__all__ = ["CanDecoder"]
