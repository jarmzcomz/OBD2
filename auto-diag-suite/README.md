# Auto-Diag Suite

Herramienta **local** de diagnóstico automotriz (OBD-II / CAN / UDS) para taller y vehículo
propio. Núcleo Python reutilizable + dashboard web local. Android queda planificado como Fase 2.

## ⚠️ Uso legítimo

Esta herramienta es para **diagnóstico y mantenimiento** sobre vehículos **propios o con
consentimiento del propietario**. **No** implementa —ni implementará— evasión de seguridad:
sin Immo Off, sin cracking/brute-force de Seed→Key (UDS 0x27), sin bypass de HSM/Secure Boot,
sin clonado de inmovilizador ni manipulación de odómetro. Toda escritura UDS está protegida por
el **Zero-Brick Protocol** (respaldo verificado por hash obligatorio antes de escribir).

## Instalación

```bash
python -m venv .venv && source .venv/bin/activate
pip install -e .              # núcleo + backend (modo simulado, sin hardware)
pip install -e ".[hardware]"  # backends reales (python-can, udsoncan, pyserial, cantools)
pip install -e ".[dev]"       # pytest + ruff
```

## Uso rápido (sin hardware)

```bash
# CLI
ADS_TRANSPORT=sim autodiag pids
ADS_TRANSPORT=sim autodiag dtc

# Dashboard / API
ADS_TRANSPORT=sim uvicorn app.main:app --reload
# -> GET http://127.0.0.1:8000/pids , WS ws://127.0.0.1:8000/ws/live
```

## Con hardware real

| Transporte | Variable | Ejemplo |
|------------|----------|---------|
| ELM327 serie/BT | `ADS_TRANSPORT=elm327`, `ADS_SERIAL_PORT` | `/dev/ttyUSB0`, `COM3` |
| SocketCAN | `ADS_TRANSPORT=socketcan`, `ADS_CAN_CHANNEL` | `can0`, `vcan0` |

## Tests

```bash
pytest      # corre sin hardware (transporte sim + bus virtual)
```

## Estructura

- `obdcore/transport/` — ELM327 (con modo `sim`), SocketCAN, ISO-TP
- `obdcore/obd2/` — PIDs, DTCs, cliente modo 01/03/04
- `obdcore/uds/` — cliente ISO 14229 (lectura + escritura *gated*)
- `obdcore/can/` — sniffer y decoder DBC
- `obdcore/storage/` — SQLite (sesiones, muestras, DTCs, backups)
- `obdcore/safety/` — Zero-Brick gate
- `app/` — FastAPI (REST + WebSocket)
- `android/` — spec de la Fase 2

El blueprint completo está en `../output/auto-diag-suite-blueprint.md`.
