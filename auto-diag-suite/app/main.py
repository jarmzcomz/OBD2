"""Backend FastAPI: REST + WebSocket para el dashboard local.

Escucha sólo en 127.0.0.1 por defecto. Ejecutar:
    ADS_TRANSPORT=sim uvicorn app.main:app --reload
"""

from __future__ import annotations

import asyncio
import uuid
from typing import Optional

from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
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


from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
import pathlib

WEB_DIR = pathlib.Path(__file__).parent.parent / "web"
if WEB_DIR.exists():
    app.mount("/static", StaticFiles(directory=str(WEB_DIR)), name="static")

@app.get("/", include_in_schema=False)
def root():
    return FileResponse(str(WEB_DIR / "index.html"))
