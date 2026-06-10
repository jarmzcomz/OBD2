"""Persistencia SQLite: sesiones, muestras, DTCs y respaldos."""

from __future__ import annotations

import os
import sqlite3
import time
from contextlib import contextmanager

SCHEMA = """
CREATE TABLE IF NOT EXISTS session (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  started_at TEXT NOT NULL,
  vehicle_vin TEXT,
  transport TEXT NOT NULL,
  notes TEXT
);
CREATE TABLE IF NOT EXISTS sample_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id INTEGER NOT NULL REFERENCES session(id),
  ts REAL NOT NULL, source TEXT NOT NULL,
  key TEXT NOT NULL, value REAL, unit TEXT
);
CREATE TABLE IF NOT EXISTS dtc_record (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id INTEGER NOT NULL REFERENCES session(id),
  code TEXT NOT NULL, status TEXT, description TEXT
);
CREATE TABLE IF NOT EXISTS ecu_backup (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id INTEGER NOT NULL REFERENCES session(id),
  ecu_addr TEXT NOT NULL, did_or_region TEXT NOT NULL,
  data BLOB NOT NULL, sha256 TEXT NOT NULL, created_at TEXT NOT NULL
);
"""


class Database:
    def __init__(self, path: str | None = None) -> None:
        self.path = path or os.environ.get("ADS_DB_PATH", "autodiag.db")
        self._init()

    def _init(self) -> None:
        with self.connect() as con:
            con.executescript(SCHEMA)

    @contextmanager
    def connect(self):
        con = sqlite3.connect(self.path)
        con.row_factory = sqlite3.Row
        try:
            yield con
            con.commit()
        finally:
            con.close()

    def create_session(self, transport: str, vin: str | None = None) -> int:
        with self.connect() as con:
            cur = con.execute(
                "INSERT INTO session(started_at, vehicle_vin, transport) VALUES(?,?,?)",
                (time.strftime("%Y-%m-%dT%H:%M:%S"), vin, transport),
            )
            return cur.lastrowid

    def log_sample(self, session_id: int, key: str, value: float,
                   unit: str = "", source: str = "pid") -> None:
        with self.connect() as con:
            con.execute(
                "INSERT INTO sample_log(session_id, ts, source, key, value, unit) "
                "VALUES(?,?,?,?,?,?)",
                (session_id, time.time(), source, key, value, unit),
            )

    def log_dtcs(self, session_id: int, codes: list[str]) -> None:
        with self.connect() as con:
            con.executemany(
                "INSERT INTO dtc_record(session_id, code, status) VALUES(?,?,?)",
                [(session_id, c, "stored") for c in codes],
            )

    def save_backup(
        self,
        session_id: int,
        ecu_addr: str,
        did_or_region: str,
        data: bytes,
        sha256: str,
    ) -> int:
        """Persiste un respaldo de ECU en la tabla ``ecu_backup``.

        Args:
            session_id: sesión activa a la que pertenece el respaldo.
            ecu_addr: dirección de la ECU (p.ej. ``"0x7E0"``).
            did_or_region: identificador del DID o región respaldada.
            data: bytes del respaldo.
            sha256: hash SHA-256 del respaldo (hex string).

        Returns:
            El ``id`` de la fila insertada.
        """
        created_at = time.strftime("%Y-%m-%dT%H:%M:%S")
        with self.connect() as con:
            cur = con.execute(
                "INSERT INTO ecu_backup"
                "(session_id, ecu_addr, did_or_region, data, sha256, created_at)"
                " VALUES(?,?,?,?,?,?)",
                (session_id, ecu_addr, did_or_region, data, sha256, created_at),
            )
            return cur.lastrowid
