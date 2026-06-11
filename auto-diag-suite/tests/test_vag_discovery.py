"""Tests del descubrimiento empírico de bits de coding (diff/bisección)."""

from obdcore.vag.discovery import BitFinder, diff_codings


def test_diff_single_bit():
    # 0x01 vs 0x00 en el byte 0: bit 0 cambia 1->0
    changes = diff_codings("01 00 00", "00 00 00")
    assert len(changes) == 1
    assert changes[0].byte_index == 0
    assert changes[0].bit == 0
    assert (changes[0].old, changes[0].new) == (1, 0)


def test_diff_no_change():
    assert diff_codings("0A 14 1E", "0A 14 1E") == []


def test_diff_multiple_bits():
    # 0x00 -> 0x05 = bits 0 y 2 activados en el byte 1
    changes = diff_codings("00 00", "00 05")
    bits = sorted(c.bit for c in changes)
    assert bits == [0, 2]
    assert all(c.byte_index == 1 for c in changes)


def test_diff_different_length():
    # byte extra en 'after' se compara contra 0
    changes = diff_codings("00", "00 01")
    assert len(changes) == 1
    assert changes[0].byte_index == 1
    assert changes[0].bit == 0


def test_bitfinder_workflow():
    finder = BitFinder()
    finder.snapshot("antes", "00 04 12 80")
    finder.snapshot("despues", "01 04 12 80")  # se activó el bit 0 del byte 0 (DRL ejemplo)
    cands = finder.candidates("antes", "despues")
    assert len(cands) == 1
    assert cands[0]["byte"] == 0 and cands[0]["bit"] == 0
    assert "byte 0 · bit 0" in cands[0]["label"]
