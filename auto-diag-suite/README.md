# Auto-Diag Suite

[![CI](https://github.com/jarmzcomz/OBD2/actions/workflows/ci.yml/badge.svg)](https://github.com/jarmzcomz/OBD2/actions/workflows/ci.yml)

Herramienta **local** de diagnóstico automotriz (OBD-II / CAN / UDS) para taller y vehículo
propio. Núcleo Python reutilizable + dashboard web local. Android queda planificado como Fase 2.

## ⚠️ Uso legítimo

Esta herramienta es para **diagnóstico y mantenimiento** sobre vehículos **propios o con
consentimiento del propietario**. **No** implementa —ni implementará— evasión de seguridad:
sin Immo Off, sin cracking/brute-force de Seed→Key (UDS 0x27), sin bypass de HSM/Secure Boot,
sin clonado de inmovilizador ni manipulación de odómetro. Toda escritura UDS está protegida por
el **Zero-Brick Protocol** (respaldo verificado por hash obligatorio antes de escribir).

---

## Requisitos mínimos

| Componente | Mínimo | Recomendado |
|------------|--------|-------------|
| Python | 3.10 | 3.12 |
| Hardware | ninguno (modo `sim`) | Adaptador ELM327 USB (~10€) |
| SO | Linux / macOS / Windows (WSL2) | Ubuntu 22.04+ |
| RAM | 256 MB | 512 MB |
| Disco | 50 MB | 200 MB |

> **Modo simulado:** Puedes instalar y probar **todo** sin ningún adaptador físico usando
> `ADS_TRANSPORT=sim`. Todos los tests pasan en modo simulado.

---

## Hardware soportado (resumen)

| Tipo | Ejemplos | SO | Precio aprox. |
|------|----------|----|---------------|
| **ELM327 USB** | Vgate iCar Pro, OBDLink LX, CH340 genérico | Linux / macOS / Windows | 8–30€ |
| **ELM327 Bluetooth** | OBDLink MX+, BAFX 34t5, Veepeak BLE | Linux / macOS | 15–50€ |
| **SocketCAN USB** | CANable v2, PEAK PCAN-USB, Kvaser Leaf | Linux | 25–180€ |
| **SocketCAN virtual** | vcan0 (sin hardware) | Linux | gratis |
| **DoIP** | OBDLink Pro, Softing TDX | Linux *(Fase 3)* | 100–500€ |

Documentación completa de hardware: [`docs/hardware.md`](docs/hardware.md)

---

## ¿Puedo usarlo en PC?

**Sí, completamente.** Hay dos modos:

### Modo simulado (sin hardware, para empezar ya)

```bash
git clone <repo> && cd auto-diag-suite
python -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"

# CLI
ADS_TRANSPORT=sim autodiag pids
ADS_TRANSPORT=sim autodiag dtc

# Dashboard web
ADS_TRANSPORT=sim uvicorn app.main:app --reload
# Abre http://127.0.0.1:8000
```

### Modo con hardware real (ELM327 USB/BT)

```bash
pip install -e ".[hardware]"   # instala pyserial, python-can, udsoncan, cantools

# Linux/macOS:
export ADS_TRANSPORT=elm327
export ADS_SERIAL_PORT=/dev/ttyUSB0   # ajusta a tu puerto
autodiag pids

# Windows:
set ADS_TRANSPORT=elm327
set ADS_SERIAL_PORT=COM3
autodiag pids
```

**Setup automático** (detecta tu OS y configura todo):
```bash
bash scripts/setup.sh
```

---

## ¿Android?

Android está planificado como **Fase 2**. El diseño ya está en [`android/`](android/).

En Fase 2 se añadirá:
- App Android nativa (Kotlin + Jetpack Compose)
- Comunicación con el núcleo Python via API REST/WebSocket local (misma red WiFi) o USB OTG
- Soporte Bluetooth LE para adaptadores ELM327 BLE directamente desde la app
- Interfaz adaptada a pantalla pequeña con gauges y alertas DTC

Si necesitas Android ya, puedes usar el dashboard web desde el navegador móvil mientras tanto.

---

## Instalación

```bash
# Setup automático (recomendado):
bash scripts/setup.sh

# O manual:
python -m venv .venv && source .venv/bin/activate
pip install -e .              # núcleo + backend (modo simulado, sin hardware)
pip install -e ".[hardware]"  # backends reales (python-can, udsoncan, pyserial, cantools)
pip install -e ".[dev]"       # pytest + ruff
```

---

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

| Transporte | Variables | Puerto típico |
|------------|-----------|---------------|
| ELM327 USB/BT | `ADS_TRANSPORT=elm327`, `ADS_SERIAL_PORT` | `/dev/ttyUSB0`, `COM3` |
| SocketCAN | `ADS_TRANSPORT=socketcan`, `ADS_CAN_CHANNEL` | `can0`, `vcan0` |
| DoIP *(Fase 3)* | `ADS_TRANSPORT=doip`, `ADS_DOIP_HOST` | `192.168.1.10` |

---

## Makefile (desarrollo)

```bash
make install-dev   # pip install -e ".[dev]"
make test          # ADS_TRANSPORT=sim pytest -q
make lint          # ruff check . --select E,F,W
make run           # uvicorn en modo sim, hot-reload
make run-hardware  # uvicorn con ADS_TRANSPORT=elm327
make cli-pids      # autodiag pids (sim)
make clean         # elimina __pycache__, *.pyc, autodiag.db
```

---

## Tests

```bash
ADS_TRANSPORT=sim python -m pytest -q   # 13 tests, sin hardware
make test                                # equivalente via Makefile
```

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│                    Clientes                             │
│   CLI (autodiag)   │   Browser (dashboard)   │  Android │
│                    │   (Fase 2)               │ (Fase 2) │
└────────┬───────────┴──────────────┬──────────┴──────────┘
         │                          │ HTTP REST / WebSocket
         │                          ▼
         │              ┌───────────────────────┐
         │              │   FastAPI (app/)       │
         │              │  /pids  /dtc  /ws/live │
         │              └──────────┬────────────┘
         │                         │
         └─────────────────────────┤
                                   ▼
                    ┌──────────────────────────┐
                    │      obdcore/            │
                    │  ┌─────────┐ ┌────────┐  │
                    │  │  obd2/  │ │  uds/  │  │
                    │  │ PIDs    │ │ ISO    │  │
                    │  │ DTCs    │ │ 14229  │  │
                    │  └────┬────┘ └───┬────┘  │
                    │       │          │        │
                    │  ┌────▼──────────▼─────┐  │
                    │  │    transport/        │  │
                    │  │  sim │ elm327        │  │
                    │  │  socketcan │ (doip)  │  │
                    │  └─────────────────────┘  │
                    │                            │
                    │  ┌──────────┐ ┌─────────┐  │
                    │  │ storage/ │ │ safety/ │  │
                    │  │ SQLite   │ │ Zero-   │  │
                    │  │ sessions │ │ Brick   │  │
                    │  └──────────┘ └─────────┘  │
                    └──────────────────────────┘
                                   │
              ┌────────────────────┼─────────────────────┐
              ▼                    ▼                      ▼
        ELM327 USB/BT        SocketCAN                 DoIP
        /dev/ttyUSB0         can0 / vcan0            Ethernet
        COM3                 PEAK, CANable            (Fase 3)
              │                    │
              └────────────────────┘
                         │
                    ┌────▼─────┐
                    │ Bus CAN  │
                    │ del coche│
                    └──────────┘
```

---

## Estructura del proyecto

```
auto-diag-suite/
├── obdcore/
│   ├── transport/     # ELM327 (+ modo sim), SocketCAN, ISO-TP
│   ├── obd2/          # PIDs, DTCs, cliente modo 01/03/04
│   ├── uds/           # cliente ISO 14229 (lectura + escritura gated)
│   ├── can/           # sniffer y decoder DBC
│   ├── storage/       # SQLite (sesiones, muestras, DTCs, backups)
│   └── safety/        # Zero-Brick gate
├── app/               # FastAPI (REST + WebSocket)
├── tests/             # 13 tests, todos pasan en modo sim
├── scripts/
│   └── setup.sh       # setup multiplataforma
├── docs/
│   └── hardware.md    # guía detallada de hardware
├── android/           # spec Fase 2
├── web/               # assets frontend
├── Makefile
└── pyproject.toml
```

El blueprint completo está en `../output/auto-diag-suite-blueprint.md`.
