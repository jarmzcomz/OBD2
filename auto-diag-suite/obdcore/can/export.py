"""Exportación de tramas CAN al formato ASC (Vector CANalyzer / CANdb++).

El formato ASC es texto plano ampliamente soportado por herramientas de
análisis CAN (CANalyzer, cantools, Wireshark con plugin, etc.).
"""

from __future__ import annotations

import io
import time
from pathlib import Path
from typing import Sequence

from .sniffer import CanFrame


# Cabecera estándar de un archivo ASC
_ASC_HEADER = """\
date {date}
base hex  timestamps absolute
no internal events logged
// ========== Auto-Diag Suite — ASC Export ==========
"""


def frames_to_asc(
    frames: Sequence[CanFrame],
    channel: int = 1,
    comment: str = "",
) -> str:
    """Convierte una lista de :class:`CanFrame` a texto ASC.

    Args:
        frames: tramas a exportar (en orden cronológico).
        channel: número de canal CAN a escribir en cada línea (1-based).
        comment: comentario opcional que se añade a la cabecera.

    Returns:
        Cadena con el contenido completo del archivo ASC.
    """
    buf = io.StringIO()

    # Cabecera
    date_str = time.strftime("%a %b %d %I:%M:%S %p %Y")
    buf.write(_ASC_HEADER.format(date=date_str))
    if comment:
        buf.write(f"// {comment}\n")
    buf.write("\n")

    # Timestamp de referencia (primer frame o 0)
    t_ref = frames[0].timestamp if frames else 0.0

    for f in frames:
        rel_ts = f.f_ts(t_ref) if hasattr(f, "f_ts") else f.timestamp - t_ref
        arb_id = f"{f.arbitration_id:X}"
        dlc = len(f.data)
        hex_bytes = " ".join(f"{b:02X}" for b in f.data)
        # Formato: <ts> <channel> <id> Rx d <dlc> <bytes>
        buf.write(
            f"   {rel_ts:.6f} {channel} {arb_id}x Rx d {dlc} {hex_bytes}\n"
            if f.is_extended_id
            else f"   {rel_ts:.6f} {channel} {arb_id} Rx d {dlc} {hex_bytes}\n"
        )

    buf.write("\nend triggerblock\n")
    return buf.getvalue()


def export_to_file(
    frames: Sequence[CanFrame],
    path: str | Path,
    channel: int = 1,
    comment: str = "",
) -> Path:
    """Escribe las tramas en un archivo ASC.

    Args:
        frames: tramas a exportar.
        path: ruta del archivo de salida (se crea si no existe).
        channel: número de canal CAN.
        comment: comentario opcional.

    Returns:
        El :class:`Path` del archivo escrito.
    """
    out = Path(path)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(frames_to_asc(frames, channel=channel, comment=comment), encoding="utf-8")
    return out


def export_asc(frames: list[dict], path: str) -> None:
    """Exporta una lista de tramas CAN (formato dict) a un archivo ASC.

    Cada frame es un dict con claves:
        - ``id`` (int): arbitration ID.
        - ``data`` (bytes): payload.
        - ``ts`` (float): timestamp absoluto en segundos.

    El archivo se crea (o sobreescribe) en *path*.

    Args:
        frames: lista de dicts ``{id, data, ts}``.
        path: ruta del archivo ASC de salida.
    """
    can_frames = [
        CanFrame(
            timestamp=f["ts"],
            arbitration_id=f["id"],
            data=f["data"],
        )
        for f in frames
    ]
    export_to_file(can_frames, path)


def parse_asc(text: str) -> list[CanFrame]:
    """Parser básico de ASC: devuelve los frames de datos (líneas con 'd').

    Útil para round-trip tests y para importar archivos ASC existentes.

    Args:
        text: contenido completo del archivo ASC.

    Returns:
        Lista de :class:`CanFrame` reconstruidos.
    """
    frames: list[CanFrame] = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith(("date", "base", "no ", "//", "end")):
            continue
        parts = line.split()
        # Formato esperado: <ts> <ch> <id>[x] Rx d <dlc> <bytes...>
        if len(parts) < 6:
            continue
        try:
            ts = float(parts[0])
        except ValueError:
            continue
        raw_id = parts[2]
        is_ext = raw_id.endswith("x")
        arb_id_str = raw_id.rstrip("x")
        try:
            arb_id = int(arb_id_str, 16)
        except ValueError:
            continue
        # Buscar 'd' para inicio de datos
        if "d" not in parts:
            continue
        d_idx = parts.index("d")
        try:
            dlc = int(parts[d_idx + 1])
        except (IndexError, ValueError):
            continue
        data_tokens = parts[d_idx + 2: d_idx + 2 + dlc]
        try:
            data = bytes(int(x, 16) for x in data_tokens)
        except ValueError:
            continue
        frames.append(
            CanFrame(
                timestamp=ts,
                arbitration_id=arb_id,
                data=data,
                is_extended_id=is_ext,
            )
        )
    return frames
