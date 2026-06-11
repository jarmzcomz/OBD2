"""Tests del paquete VAG (coding/adaptación). Usa senders simulados bytes->bytes."""

import pytest

from obdcore.safety import ZeroBrickError
from obdcore.uds import UdsServices
from obdcore.vag import (
    VAG_MODULES,
    AdaptationManager,
    LongCoding,
    ModuleScanner,
)
from obdcore.vag.modules import VagModule

SID_READ = 0x22
SID_TP = 0x3E


def test_modules_table_has_bcm():
    assert "0x09" in VAG_MODULES
    bcm = VAG_MODULES["0x09"]
    assert isinstance(bcm, VagModule)
    assert "BCM" in bcm.name
    assert bcm.short == "BCM"


def test_long_coding_get_set_bit():
    lc = LongCoding("00")
    assert lc.get_bit(0, 3) is False
    lc.set_bit(0, 3, True)
    assert lc.get_bit(0, 3) is True
    lc.set_bit(0, 3, False)
    assert lc.get_bit(0, 3) is False


def test_long_coding_diff():
    a = LongCoding("0000")
    b = LongCoding("0100")
    b.set_bit(0, 0, True)  # byte0 -> 0x02 vs 0x01
    changes = a.diff(b)
    assert any(c["byte"] == 0 for c in changes)
    detail = next(c for c in changes if c["byte"] == 0)
    assert detail["old"] != detail["new"]
    assert isinstance(detail["bits_changed"], list)


def test_long_coding_to_hex():
    lc = LongCoding("DEADBEEF")
    assert lc.to_hex() == "DEADBEEF"
    lc.set_byte(0, 0x00)
    assert len(lc.to_hex()) == 8  # longitud (en chars) preservada


def _sim_sender(module: VagModule):
    """Sender simulado: responde a TesterPresent, 0xF187 y un par de DIDs."""
    def sender(req: bytes) -> bytes:
        if req[0] == SID_TP:
            return bytes([SID_TP + 0x40, 0x00])
        if req[0] == SID_READ:
            did = (req[1] << 8) | req[2]
            if did == 0xF187:
                return bytes([0x62, req[1], req[2]]) + b"5Q0937087"
            if did in (0xF190, 0xF18C):
                return bytes([0x62, req[1], req[2]]) + b"\xAA\xBB"
            return bytes([0x7F, SID_READ, 0x31])  # requestOutOfRange
        return b""
    return sender


def test_scanner_sim():
    scanner = ModuleScanner(_sim_sender)
    mods = scanner.scan_modules()
    assert any(m["present"] and m["part_number"] == "5Q0937087" for m in mods)

    bcm = VAG_MODULES["0x09"]
    dids = scanner.scan_dids(bcm, start=0xF187, end=0xF190)
    found = {d["did"] for d in dids}
    assert "0xF187" in found
    assert "0xF190" in found
    assert all(d["readable"] for d in dids)


def test_adaptation_write_requires_backup():
    state = {"val": b"\xAA"}

    def sender(req: bytes) -> bytes:
        if req[0] == SID_READ:
            return bytes([0x62, req[1], req[2]]) + state["val"]
        if req[0] == 0x2E:
            state["val"] = req[3:]
            return bytes([0x6E, req[1], req[2]])
        return b""

    uds = UdsServices(sender)
    mgr = AdaptationManager(uds)
    with pytest.raises(ZeroBrickError):
        mgr.write_channel(0xF190, b"\xBB", backup=None)

    # Con backup verificado sí escribe.
    backup = uds.backup_did(0xF190)
    assert mgr.write_channel(0xF190, b"\xBB", backup=backup) is True
