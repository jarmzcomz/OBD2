"""Paquete VAG: coding y adaptación legítima del propietario (estilo VCDS/OBDeleven).

ALCANCE: VW T-Cross 1.0 TSI 2026 (MQB-A0) y módulos VAG similares. Sólo coding,
adaptación y lectura no destructiva. Toda escritura pasa por el gate Zero-Brick.
NO incluye cracking de SecurityAccess, cálculo de llaves, Immo Off ni flash de
firmware.
"""

from __future__ import annotations

from .adaptation import AdaptationChannel, AdaptationManager
from .discovery import BitChange, BitFinder, diff_codings
from .known_codings import KNOWN_CODINGS
from .long_coding import LongCoding
from .modules import VAG_MODULES, VagModule
from .scanner import ModuleScanner

__all__ = [
    "VAG_MODULES",
    "VagModule",
    "ModuleScanner",
    "LongCoding",
    "AdaptationChannel",
    "AdaptationManager",
    "KNOWN_CODINGS",
    "BitFinder",
    "BitChange",
    "diff_codings",
]
