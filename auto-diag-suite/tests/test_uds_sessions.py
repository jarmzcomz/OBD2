"""Tests para SessionManager y DiagnosticSession (obdcore.uds.sessions)."""

from __future__ import annotations


from obdcore.uds.sessions import DiagnosticSession, SessionManager


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_sender(responses: dict | None = None):
    """Crea un sender mock que devuelve respuestas positivas por defecto."""
    def sender(req: bytes) -> bytes:
        sid = req[0]
        # Respuesta positiva: SID + 0x40
        positive = bytes([sid + 0x40]) + req[1:]
        if responses and sid in responses:
            return responses[sid]
        return positive
    return sender


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

def test_open_session_default():
    """open_session con DEFAULT debe devolver True y no lanzar excepción."""
    sender = _make_sender()
    mgr = SessionManager(sender)
    result = mgr.open_session(DiagnosticSession.DEFAULT)
    assert result is True
    assert mgr.current == DiagnosticSession.DEFAULT


def test_open_extended_session():
    """open_session con EXTENDED debe devolver True y actualizar current."""
    sender = _make_sender()
    mgr = SessionManager(sender)
    result = mgr.open_session(DiagnosticSession.EXTENDED)
    assert result is True
    assert mgr.current == DiagnosticSession.EXTENDED


def test_tester_present_no_raise():
    """tester_present no debe lanzar excepción en ningún escenario."""
    sender = _make_sender()
    mgr = SessionManager(sender)
    # No debe lanzar aunque el sender devuelva vacío
    mgr.tester_present(suppress_response=True)
    mgr.tester_present(suppress_response=False)


def test_tester_present_suppress_uses_0x80():
    """Con suppress_response=True se envía subfunction 0x80."""
    captured: list[bytes] = []

    def sender(req: bytes) -> bytes:
        captured.append(req)
        return bytes([req[0] + 0x40, req[1]])

    mgr = SessionManager(sender)
    mgr.tester_present(suppress_response=True)
    assert len(captured) == 1
    assert captured[0][1] == 0x80


def test_tester_present_no_suppress_uses_0x00():
    """Con suppress_response=False se envía subfunction 0x00."""
    captured: list[bytes] = []

    def sender(req: bytes) -> bytes:
        captured.append(req)
        return bytes([req[0] + 0x40, req[1]])

    mgr = SessionManager(sender)
    mgr.tester_present(suppress_response=False)
    assert len(captured) == 1
    assert captured[0][1] == 0x00


def test_close_session_returns_to_default():
    """close_session debe dejar current en DEFAULT."""
    sender = _make_sender()
    mgr = SessionManager(sender)
    mgr.open_session(DiagnosticSession.EXTENDED)
    assert mgr.current == DiagnosticSession.EXTENDED
    mgr.close_session()
    assert mgr.current == DiagnosticSession.DEFAULT


def test_open_session_returns_false_on_negative_response():
    """Si la ECU devuelve respuesta negativa, open_session devuelve False."""
    def bad_sender(req: bytes) -> bytes:
        return b"\x7F\x10\x22"  # NRC: conditionNotCorrect

    mgr = SessionManager(bad_sender)
    result = mgr.open_session(DiagnosticSession.EXTENDED)
    assert result is False


def test_diagnostic_session_values():
    """Verifica los valores de enum según ISO 14229."""
    assert DiagnosticSession.DEFAULT == 0x01
    assert DiagnosticSession.PROGRAMMING == 0x02
    assert DiagnosticSession.EXTENDED == 0x03
