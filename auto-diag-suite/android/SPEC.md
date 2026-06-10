# Android (Fase 2) — Especificación

App Kotlin para uso en carretera/taller que reutiliza la **especificación** de protocolo del
núcleo `obdcore` (no el código Python).

## Alcance Fase 2
- Conexión a adaptador **ELM327 por Bluetooth (RFCOMM/SPP)**.
- Lectura de PIDs en vivo (mismos comandos que `obdcore/obd2/pids.py`) y DTCs (servicios 03/04).
- Dashboard con gauges y logging local (Room/SQLite).

## Reutilización
- Reimplementar la tabla de PIDs y el decodificador DTC (`decode_dtc`) tal cual en Kotlin.
- Mantener los mismos nombres de PID (`rpm`, `coolant_temp`, ...) para compatibilidad de logs.
- Cualquier función de escritura debe replicar el **Zero-Brick Protocol** (`obdcore/safety`).

## Fuera de alcance (igual que el núcleo)
Sin Immo Off, sin Seed/Key cracking, sin bypass de HSM, sin clonado de inmovilizador.
