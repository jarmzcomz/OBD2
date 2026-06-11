"""Tests del decodificador OBD-II y del cliente sobre el transporte simulado."""

from obdcore.obd2 import Obd2Client
from obdcore.obd2.dtc import decode_dtc, parse_dtc_response
from obdcore.obd2.pids import PIDS
from obdcore.transport import Elm327Transport


def test_pid_rpm_formula():
    # (A*256+B)/4 = (0x1A*256 + 0xF0)/4
    assert PIDS["010C"].decode([0x1A, 0xF0]) == (0x1A * 256 + 0xF0) / 4


def test_decode_dtc():
    assert decode_dtc(0x01, 0x03) == "P0103"
    assert decode_dtc(0xC1, 0x23) == "U0123"


def test_parse_dtc_response_skips_zero():
    assert parse_dtc_response([0x01, 0x03, 0x00, 0x00]) == ["P0103"]


def test_client_reads_simulated_pids():
    with Elm327Transport(simulated=True) as t:
        client = Obd2Client(t)
        values = client.read_all_pids()
        assert "rpm" in values
        assert values["speed"] >= 0


def test_client_reads_simulated_dtc():
    with Elm327Transport(simulated=True) as t:
        client = Obd2Client(t)
        assert client.read_dtcs() == ["P0103"]
        assert client.clear_dtcs() is True


def test_new_pids_in_table():
    for code in ("0111", "010F", "0104", "0110", "012F", "0142"):
        assert code in PIDS, f"PID {code} no encontrado en la tabla"


def test_sim_reads_new_pids():
    with Elm327Transport(simulated=True) as t:
        client = Obd2Client(t)
        values = client.read_all_pids()
        assert "throttle" in values
        assert "fuel_level" in values
        assert "voltage" in values
        assert 12 < values["voltage"] < 15  # 13.8V nominal
