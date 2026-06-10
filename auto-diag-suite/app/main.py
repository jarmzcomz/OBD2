"""Backend FastAPI: REST + WebSocket para el dashboard local.

Escucha sólo en 127.0.0.1 por defecto. Ejecutar:
    ADS_TRANSPORT=sim uvicorn app.main:app --reload
"""

from __future__ import annotations

import asyncio
import pathlib
import uuid
from typing import Optional

from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from obdcore.obd2 import Obd2Client
from obdcore.safety.backup import EcuBackup
from obdcore.safety.guard import ZeroBrickError
from obdcore.storage.db import Database
from obdcore.transport import make_transport
from obdcore.uds.services import UdsServices

app = FastAPI(title="Auto-Diag Suite", version="0.1.0")

# ---------------------------------------------------------------------------
# Estado en memoria para sesiones activas y backups
# ---------------------------------------------------------------------------

# session_id (str uuid) -> {"db_id": int, "transport": str, "vin": str|None}
_sessions: dict[str, dict] = {}

# backup_id (str uuid) -> EcuBackup
_backups: dict[str, EcuBackup] = {}

_db = Database()


def _client() -> Obd2Client:
    transport = make_transport()
    transport.open()
    return Obd2Client(transport)


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.get("/pids")
def pids() -> dict:
    client = _client()
    try:
        return client.read_all_pids()
    finally:
        client.transport.close()


@app.get("/dtc")
def dtc() -> dict:
    client = _client()
    try:
        return {"dtcs": client.read_dtcs()}
    finally:
        client.transport.close()


@app.delete("/dtc")
def clear_dtc() -> dict:
    client = _client()
    try:
        return {"cleared": client.clear_dtcs()}
    finally:
        client.transport.close()


@app.websocket("/ws/live")
async def ws_live(ws: WebSocket) -> None:
    """Emite los PIDs en vivo (~2 Hz) al dashboard."""
    await ws.accept()
    client = _client()
    try:
        while True:
            await ws.send_json(client.read_all_pids())
            await asyncio.sleep(0.5)
    except WebSocketDisconnect:
        pass
    finally:
        client.transport.close()


# ---------------------------------------------------------------------------
# Modelos Pydantic para las nuevas rutas
# ---------------------------------------------------------------------------

class SessionCreate(BaseModel):
    transport: str = "sim"
    vin: Optional[str] = None


class UdsReadBody(BaseModel):
    did_hex: str  # e.g. "F190"


class UdsBackupBody(BaseModel):
    ecu_addr: str  # e.g. "0x7E0"
    did_hex: str


class UdsWriteBody(BaseModel):
    ecu_addr: str
    did_hex: str
    data_hex: str
    backup_id: str


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _get_session_or_404(session_id: str) -> dict:
    if session_id not in _sessions:
        raise HTTPException(status_code=404, detail="Sesión no encontrada")
    return _sessions[session_id]


def _make_sim_uds_sender(ecu_addr: str):
    """Sender simulado para UDS: devuelve respuestas positivas estándar."""
    def sender(req: bytes) -> bytes:
        sid = req[0]
        if sid == 0x22:  # ReadDataByIdentifier
            return bytes([0x62, req[1], req[2], 0xAA, 0xBB, 0xCC, 0xDD])
        if sid == 0x2E:  # WriteDataByIdentifier
            return bytes([0x6E, req[1], req[2]])
        if sid == 0x10:  # DiagnosticSessionControl
            return bytes([0x50, req[1]])
        if sid == 0x3E:  # TesterPresent
            return bytes([0x7E, 0x00])
        return b""
    return sender


# ---------------------------------------------------------------------------
# Nuevas rutas REST de sesión
# ---------------------------------------------------------------------------

@app.post("/session")
def create_session(body: SessionCreate) -> dict:
    """Abre una sesión de diagnóstico y la registra en la DB."""
    db_id = _db.create_session(transport=body.transport, vin=body.vin)
    session_id = str(uuid.uuid4())
    _sessions[session_id] = {
        "db_id": db_id,
        "transport": body.transport,
        "vin": body.vin,
    }
    return {"session_id": session_id}


@app.get("/session/{session_id}/dtc")
def session_dtc(session_id: str) -> dict:
    """Lee DTCs usando el transporte de la sesión."""
    _get_session_or_404(session_id)
    transport = make_transport("sim")  # modo sim; en prod usar sesión.transport
    transport.open()
    try:
        client = Obd2Client(transport)
        dtcs = client.read_dtcs()
    finally:
        transport.close()
    return {"dtcs": dtcs}


@app.delete("/session/{session_id}/dtc")
def session_clear_dtc(session_id: str) -> dict:
    """Borra DTCs de la ECU."""
    _get_session_or_404(session_id)
    transport = make_transport("sim")
    transport.open()
    try:
        client = Obd2Client(transport)
        cleared = client.clear_dtcs()
    finally:
        transport.close()
    return {"cleared": cleared}


@app.post("/session/{session_id}/uds/read")
def session_uds_read(session_id: str, body: UdsReadBody) -> dict:
    """Lee un DID UDS (ReadDataByIdentifier)."""
    _get_session_or_404(session_id)
    did = int(body.did_hex, 16)
    ecu_addr = "0x7E0"
    sender = _make_sim_uds_sender(ecu_addr)
    uds = UdsServices(sender, ecu_addr)
    raw = uds.read_data_by_identifier(did)
    return {"data_hex": raw.hex()}


@app.post("/session/{session_id}/uds/backup")
def session_uds_backup(session_id: str, body: UdsBackupBody) -> dict:
    """Lee un DID y crea un backup verificado (Zero-Brick)."""
    sess = _get_session_or_404(session_id)
    did = int(body.did_hex, 16)
    sender = _make_sim_uds_sender(body.ecu_addr)
    uds = UdsServices(sender, body.ecu_addr)
    backup = uds.backup_did(did)

    # Guardar en memoria
    backup_id = str(uuid.uuid4())
    _backups[backup_id] = backup

    # Persistir en DB
    _db.save_backup(
        session_id=sess["db_id"],
        ecu_addr=backup.ecu_addr,
        did_or_region=backup.did_or_region,
        data=backup.data,
        sha256=backup.sha256,
    )

    return {"backup_id": backup_id, "sha256": backup.sha256}


@app.post("/session/{session_id}/uds/write")
def session_uds_write(session_id: str, body: UdsWriteBody) -> dict:
    """Escribe un DID UDS. Bloqueado sin backup verificado (Zero-Brick)."""
    _get_session_or_404(session_id)

    backup = _backups.get(body.backup_id)
    if backup is None:
        raise HTTPException(
            status_code=409,
            detail="No existe backup verificado para este backup_id. "
                   "Llama a /uds/backup primero.",
        )

    did = int(body.did_hex, 16)
    data = bytes.fromhex(body.data_hex)
    sender = _make_sim_uds_sender(body.ecu_addr)
    uds = UdsServices(sender, body.ecu_addr)

    try:
        ok = uds.write_data_by_identifier(did, data, backup)
    except ZeroBrickError as exc:
        raise HTTPException(status_code=409, detail=str(exc))

    return {"ok": ok}


WEB_DIR = pathlib.Path(__file__).parent.parent / "web"
if WEB_DIR.exists():
    app.mount("/static", StaticFiles(directory=str(WEB_DIR)), name="static")

@app.get("/", include_in_schema=False)
def root():
    return FileResponse(str(WEB_DIR / "index.html"))
