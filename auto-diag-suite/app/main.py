"""Backend FastAPI: REST + WebSocket para el dashboard local.

Escucha sólo en 127.0.0.1 por defecto. Ejecutar:
    ADS_TRANSPORT=sim uvicorn app.main:app --reload
"""

from __future__ import annotations

import asyncio

from fastapi import FastAPI, WebSocket, WebSocketDisconnect

from obdcore.obd2 import Obd2Client
from obdcore.transport import make_transport

app = FastAPI(title="Auto-Diag Suite", version="0.1.0")


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
