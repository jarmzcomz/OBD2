"""Descubrimiento empírico del byte/bit exacto de una función de coding.

Método legítimo de comparación (diff / bisección), el mismo que usa la comunidad:
se leen dos estados del long coding (antes/después de un cambio conocido) y se
deduce qué bits cambiaron. NO calcula SecurityAccess ni llaves: solo compara
codings que el usuario ya pudo leer de su vehículo.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class BitChange:
    byte_index: int
    bit: int
    old: int
    new: int

    @property
    def label(self) -> str:
        return f"byte {self.byte_index} · bit {self.bit}: {self.old} → {self.new}"


def _to_bytes(coding: str | bytes | bytearray) -> bytes:
    if isinstance(coding, str):
        return bytes.fromhex(coding.replace(" ", ""))
    return bytes(coding)


def diff_codings(before: str | bytes, after: str | bytes) -> list[BitChange]:
    """Devuelve la lista de bits que cambiaron entre dos long codings.

    Si los codings tienen distinta longitud, se comparan los bytes comunes y los
    bytes extra se reportan como cambios desde/hacia 0.
    """
    a, b = _to_bytes(before), _to_bytes(after)
    n = max(len(a), len(b))
    changes: list[BitChange] = []
    for i in range(n):
        ba = a[i] if i < len(a) else 0
        bb = b[i] if i < len(b) else 0
        if ba == bb:
            continue
        for bit in range(8):
            old = (ba >> bit) & 1
            new = (bb >> bit) & 1
            if old != new:
                changes.append(BitChange(i, bit, old, new))
    return changes


class BitFinder:
    """Asistente de descubrimiento: registra snapshots y deduce el bit de una función.

    Flujo típico:
        finder = BitFinder()
        finder.snapshot("antes", coding_leido_1)
        # el usuario activa la función por el método normal y vuelve a leer
        finder.snapshot("despues", coding_leido_2)
        cambios = finder.deduce("antes", "despues")
    """

    def __init__(self) -> None:
        self._snapshots: dict[str, bytes] = {}

    def snapshot(self, label: str, coding: str | bytes) -> None:
        self._snapshots[label] = _to_bytes(coding)

    @property
    def labels(self) -> list[str]:
        return list(self._snapshots)

    def deduce(self, before_label: str, after_label: str) -> list[BitChange]:
        if before_label not in self._snapshots or after_label not in self._snapshots:
            raise KeyError("snapshot no encontrado; usa snapshot() primero")
        return diff_codings(self._snapshots[before_label], self._snapshots[after_label])

    def candidates(self, before_label: str, after_label: str) -> list[dict]:
        """Igual que deduce pero como dicts serializables (para la API/UI)."""
        return [
            {
                "byte": c.byte_index,
                "bit": c.bit,
                "old": c.old,
                "new": c.new,
                "label": c.label,
            }
            for c in self.deduce(before_label, after_label)
        ]
