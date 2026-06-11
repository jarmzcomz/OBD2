"""El gate Zero-Brick debe bloquear escrituras sin respaldo verificado."""

import pytest

from obdcore.safety import EcuBackup, ZeroBrickError, assert_writable
from obdcore.uds import UdsServices


def test_no_backup_blocks_write():
    with pytest.raises(ZeroBrickError):
        assert_writable(None, b"\x01\x02", "0x7E0", "DID:0xf190")


def test_mismatched_hash_blocks_write():
    backup = EcuBackup.create("0x7E0", "DID:0xf190", b"original")
    with pytest.raises(ZeroBrickError):
        assert_writable(backup, b"changed", "0x7E0", "DID:0xf190")


def test_valid_backup_allows_write():
    backup = EcuBackup.create("0x7E0", "DID:0xf190", b"original")
    # No lanza: hash íntegro, misma ECU/región.
    assert_writable(backup, b"original", "0x7E0", "DID:0xf190")


def test_uds_write_blocked_without_backup():
    # sender de ejemplo: responde una lectura fija y acepta escritura.
    def sender(req: bytes) -> bytes:
        if req[0] == 0x22:
            return bytes([0x62, req[1], req[2], 0xAA])
        return bytes([0x6E, req[1], req[2]])

    uds = UdsServices(sender)
    with pytest.raises(ZeroBrickError):
        uds.write_data_by_identifier(0xF190, b"\xBB", backup=None)


def test_uds_write_allowed_with_backup():
    state = {"val": b"\xAA"}

    def sender(req: bytes) -> bytes:
        if req[0] == 0x22:  # ReadDataByIdentifier
            return bytes([0x62, req[1], req[2]]) + state["val"]
        if req[0] == 0x2E:  # WriteDataByIdentifier
            state["val"] = req[3:]
            return bytes([0x6E, req[1], req[2]])
        return b""

    uds = UdsServices(sender)
    backup = uds.backup_did(0xF190)
    assert uds.write_data_by_identifier(0xF190, b"\xBB", backup=backup) is True
