"""Tests para export_asc y funciones de exportación CAN."""

from __future__ import annotations

import time
from pathlib import Path


from obdcore.can.export import export_asc, export_to_file, frames_to_asc, parse_asc
from obdcore.can.sniffer import CanFrame


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

SAMPLE_FRAMES_DICT = [
    {"id": 0x100, "data": bytes([0x01, 0x02, 0x03, 0x04]), "ts": 1000.0},
    {"id": 0x200, "data": bytes([0xAA, 0xBB]), "ts": 1000.1},
    {"id": 0x7E8, "data": bytes([0x02, 0x7E, 0x00]), "ts": 1000.2},
]


# ---------------------------------------------------------------------------
# Tests export_asc (API dict)
# ---------------------------------------------------------------------------

def test_export_asc_creates_file(tmp_path: Path):
    """export_asc debe crear el archivo en la ruta indicada."""
    out = tmp_path / "capture.asc"
    export_asc(SAMPLE_FRAMES_DICT, str(out))
    assert out.exists(), "El archivo ASC no fue creado"
    assert out.stat().st_size > 0, "El archivo ASC está vacío"


def test_export_asc_contains_frames(tmp_path: Path):
    """El archivo ASC debe contener los IDs de los frames exportados."""
    out = tmp_path / "capture.asc"
    export_asc(SAMPLE_FRAMES_DICT, str(out))
    content = out.read_text(encoding="utf-8")
    # Verificar que los IDs aparecen en el contenido
    assert "100" in content, "ID 0x100 no encontrado en el ASC"
    assert "200" in content, "ID 0x200 no encontrado en el ASC"
    assert "7E8" in content, "ID 0x7E8 no encontrado en el ASC"


def test_export_asc_header_format(tmp_path: Path):
    """El archivo ASC debe tener la cabecera estándar Vector."""
    out = tmp_path / "capture.asc"
    export_asc(SAMPLE_FRAMES_DICT, str(out))
    content = out.read_text(encoding="utf-8")
    assert "date" in content.lower()
    assert "base hex" in content
    assert "timestamps absolute" in content


def test_export_asc_data_bytes(tmp_path: Path):
    """Los bytes de datos deben aparecer en hexadecimal en el archivo."""
    out = tmp_path / "single.asc"
    frames = [{"id": 0x123, "data": bytes([0xDE, 0xAD, 0xBE, 0xEF]), "ts": 0.0}]
    export_asc(frames, str(out))
    content = out.read_text(encoding="utf-8")
    assert "DE" in content
    assert "AD" in content
    assert "BE" in content
    assert "EF" in content


def test_export_asc_empty_frames(tmp_path: Path):
    """export_asc con lista vacía no debe lanzar excepción."""
    out = tmp_path / "empty.asc"
    export_asc([], str(out))
    assert out.exists()


# ---------------------------------------------------------------------------
# Tests export_to_file + frames_to_asc (API CanFrame)
# ---------------------------------------------------------------------------

def test_frames_to_asc_roundtrip():
    """Exportar frames y parsearlos debe recuperar los mismos datos."""
    frames = [
        CanFrame(timestamp=1.0, arbitration_id=0x100, data=bytes([0x01, 0x02])),
        CanFrame(timestamp=1.5, arbitration_id=0x200, data=bytes([0xFF, 0x00])),
    ]
    asc_text = frames_to_asc(frames)
    recovered = parse_asc(asc_text)
    assert len(recovered) == 2
    assert recovered[0].arbitration_id == 0x100
    assert recovered[1].arbitration_id == 0x200
    assert recovered[0].data == bytes([0x01, 0x02])


def test_export_to_file_creates_valid_asc(tmp_path: Path):
    """export_to_file debe crear un archivo parseable."""
    frames = [
        CanFrame(timestamp=time.time(), arbitration_id=0x300, data=bytes([0xAA])),
    ]
    out_path = export_to_file(frames, tmp_path / "test.asc")
    assert out_path.exists()
    content = out_path.read_text(encoding="utf-8")
    recovered = parse_asc(content)
    assert len(recovered) == 1
    assert recovered[0].arbitration_id == 0x300
