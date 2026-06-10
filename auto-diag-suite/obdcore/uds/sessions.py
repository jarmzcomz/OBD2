"""Gestión de sesiones UDS (ISO 14229-1 §9).

Sólo admite sesiones de diagnóstico estándar y extendida. NO implementa
SecurityAccess (0x27) ni ningún mecanismo de Seed/Key o bypass de seguridad.
"""

from __future__ import annotations

import time
from enum import IntEnum
from typing import Callable


class DiagnosticSession(IntEnum):
    DEFAULT = 0x01
    PROGRAMMING = 0x02
    EXTENDED = 0x03


SID_DIAGNOSTIC_SESSION_CONTROL = 0x10
SID_TESTER_PRESENT = 0x3E

_RESPONSE_OFFSET = 0x40


class UdsSessionError(RuntimeError):
    """Error al negociar o mantener una sesión UDS."""


class UdsSession:
    """Gestor de sesión UDS sobre un *sender* inyectado (bytes → bytes).

    El *sender* puede ser cualquier callable que envíe una trama ISO-TP y
    devuelva la respuesta cruda: un lambda de simulación, udsoncan, o un
    SocketCanTransport ajustado.

    Args:
        sender: función ``bytes -> bytes`` de transporte.
        ecu_addr: dirección de la ECU (sólo informativo / logging).
        keep_alive_s: intervalo en segundos para TesterPresent automático.
    """

    def __init__(
        self,
        sender: Callable[[bytes], bytes],
        ecu_addr: str = "0x7E0",
        keep_alive_s: float = 2.0,
    ) -> None:
        self.sender = sender
        self.ecu_addr = ecu_addr
        self.keep_alive_s = keep_alive_s
        self._current: DiagnosticSession = DiagnosticSession.DEFAULT
        self._session_started_at: float | None = None
        self._last_activity: float = time.monotonic()

    # ------------------------------------------------------------------
    # Propiedades públicas
    # ------------------------------------------------------------------

    @property
    def current_session(self) -> DiagnosticSession:
        return self._current

    @property
    def is_active(self) -> bool:
        return self._session_started_at is not None

    # ------------------------------------------------------------------
    # Control de sesión
    # ------------------------------------------------------------------

    def open(self, session: DiagnosticSession = DiagnosticSession.EXTENDED) -> None:
        """Envía DiagnosticSessionControl y verifica la respuesta positiva."""
        req = bytes([SID_DIAGNOSTIC_SESSION_CONTROL, int(session)])
        resp = self.sender(req)
        expected_sid = SID_DIAGNOSTIC_SESSION_CONTROL + _RESPONSE_OFFSET
        if not resp or resp[0] != expected_sid:
            raise UdsSessionError(
                f"No se pudo abrir sesión {session.name}: respuesta={resp!r}"
            )
        self._current = session
        self._session_started_at = time.monotonic()
        self._last_activity = time.monotonic()

    def close(self) -> None:
        """Vuelve a la sesión DEFAULT (estado seguro)."""
        if self._current != DiagnosticSession.DEFAULT:
            try:
                req = bytes([SID_DIAGNOSTIC_SESSION_CONTROL, DiagnosticSession.DEFAULT])
                self.sender(req)
            except Exception:
                pass  # El cierre no debe lanzar
        self._current = DiagnosticSession.DEFAULT
        self._session_started_at = None

    def tester_present(self) -> None:
        """Envía TesterPresent (0x3E 0x00) para mantener viva la sesión."""
        req = bytes([SID_TESTER_PRESENT, 0x00])
        self.sender(req)
        self._last_activity = time.monotonic()

    def keep_alive_if_needed(self) -> bool:
        """Envía TesterPresent si ha pasado más de *keep_alive_s* sin actividad.

        Returns:
            True si se envió TesterPresent, False si no fue necesario.
        """
        if time.monotonic() - self._last_activity >= self.keep_alive_s:
            self.tester_present()
            return True
        return False

    def touch(self) -> None:
        """Actualiza el timestamp de última actividad (llamar tras cada request)."""
        self._last_activity = time.monotonic()

    # ------------------------------------------------------------------
    # Context manager
    # ------------------------------------------------------------------

    def __enter__(self) -> "UdsSession":
        self.open()
        return self

    def __exit__(self, *exc) -> None:
        self.close()


class SessionManager:
    """API simplificada de gestión de sesiones UDS (SID 0x10 / 0x3E).

    Pensada para integrarse en rutas REST donde el ciclo de vida de la sesión
    puede cruzar peticiones HTTP.  NO implementa SecurityAccess (0x27).

    Args:
        sender: callable ``bytes -> bytes`` de transporte.
    """

    def __init__(self, sender: Callable[[bytes], bytes]) -> None:
        self._session = UdsSession(sender)
        self._current: DiagnosticSession = DiagnosticSession.DEFAULT

    # ------------------------------------------------------------------
    # API pública
    # ------------------------------------------------------------------

    def open_session(self, session_type: DiagnosticSession) -> bool:
        """Envía DiagnosticSessionControl (SID 0x10).

        Returns:
            True si la ECU respondió con respuesta positiva, False en caso de error.
        """
        try:
            self._session.open(session_type)
            self._current = session_type
            return True
        except (UdsSessionError, Exception):
            return False

    def tester_present(self, suppress_response: bool = True) -> None:
        """Envía TesterPresent (SID 0x3E) para mantener viva la sesión.

        Args:
            suppress_response: si True envía subfunction 0x80 (sin respuesta ECU),
                               si False envía 0x00 (con respuesta).
        """
        subfunction = 0x80 if suppress_response else 0x00
        req = bytes([SID_TESTER_PRESENT, subfunction])
        try:
            self._session.sender(req)
        except Exception:
            pass  # TesterPresent no debe propagar errores
        self._session._last_activity = time.monotonic()

    def close_session(self) -> None:
        """Vuelve a la sesión DEFAULT (estado seguro)."""
        self._session.close()
        self._current = DiagnosticSession.DEFAULT

    @property
    def current(self) -> DiagnosticSession:
        return self._current
