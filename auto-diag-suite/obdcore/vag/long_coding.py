"""Decodificación y edición del 'long coding' de un módulo VAG.

El long coding es una secuencia de bytes donde cada bit activa/desactiva funciones.
Esta clase es puramente local (no toca la ECU); la escritura del coding resultante
debe hacerse vía AdaptationManager, que pasa por el gate Zero-Brick.
"""

from __future__ import annotations


class LongCoding:
    """Representa y edita el long coding de un módulo VAG (string hex de bytes)."""

    def __init__(self, coding_hex: str) -> None:
        self.bytes = bytearray.fromhex(coding_hex.replace(" ", ""))

    def _check_bounds(self, byte_index: int, bit: int | None = None) -> None:
        if not 0 <= byte_index < len(self.bytes):
            raise IndexError(f"byte_index {byte_index} fuera de rango (0..{len(self.bytes) - 1})")
        if bit is not None and not 0 <= bit <= 7:
            raise ValueError(f"bit debe estar en 0..7, recibido {bit}")

    def get_bit(self, byte_index: int, bit: int) -> bool:
        self._check_bounds(byte_index, bit)
        return bool(self.bytes[byte_index] & (1 << bit))

    def set_bit(self, byte_index: int, bit: int, value: bool) -> None:
        self._check_bounds(byte_index, bit)
        if value:
            self.bytes[byte_index] |= (1 << bit)
        else:
            self.bytes[byte_index] &= ~(1 << bit) & 0xFF

    def get_byte(self, byte_index: int) -> int:
        self._check_bounds(byte_index)
        return self.bytes[byte_index]

    def set_byte(self, byte_index: int, value: int) -> None:
        self._check_bounds(byte_index)
        if not 0 <= value <= 0xFF:
            raise ValueError(f"value debe ser 0..255, recibido {value}")
        self.bytes[byte_index] = value

    def to_hex(self) -> str:
        return self.bytes.hex().upper()

    def diff(self, other: "LongCoding") -> list[dict]:
        """Lista bytes/bits cambiados respecto a otro coding.

        Pensado para mostrar al usuario qué va a cambiar ANTES de escribir.

        Returns:
            [{byte, old, new, bits_changed: [int, ...]}]
        """
        changes: list[dict] = []
        n = max(len(self.bytes), len(other.bytes))
        for i in range(n):
            old = other.bytes[i] if i < len(other.bytes) else 0
            new = self.bytes[i] if i < len(self.bytes) else 0
            if old != new:
                xor = old ^ new
                bits_changed = [b for b in range(8) if xor & (1 << b)]
                changes.append({
                    "byte": i,
                    "old": old,
                    "new": new,
                    "bits_changed": bits_changed,
                })
        return changes
