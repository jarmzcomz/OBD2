"""El transporte simulado y la factory deben funcionar sin hardware."""

from obdcore.transport import Elm327Transport, make_transport


def test_factory_defaults_to_sim(monkeypatch):
    monkeypatch.delenv("ADS_TRANSPORT", raising=False)
    t = make_transport()
    assert isinstance(t, Elm327Transport)
    assert t.simulated is True


def test_sim_query_returns_response():
    with Elm327Transport(simulated=True) as t:
        resp = t.query(b"010C")
        assert b"41 0C" in resp


def test_unknown_pid_returns_no_data():
    with Elm327Transport(simulated=True) as t:
        assert b"NO DATA" in t.query(b"01FF")
