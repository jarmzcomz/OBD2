# Hardware Soportado

> **Modo simulado:** Si no tienes hardware, usa `ADS_TRANSPORT=sim`. Todo funciona sin ningún
> adaptador. Ver sección [Simulación](#simulación-sin-hardware) al final.

---

## Adaptadores ELM327 (recomendado para empezar)

El ELM327 es el chip más popular para OBD-II. Cuesta entre 8€ y 30€ y funciona con
prácticamente cualquier vehículo fabricado después de 2001 en Europa (2008 obligatorio para CAN).

### Adaptadores probados y compatibles

| Adaptador | Interfaz | Notas |
|-----------|----------|-------|
| Vgate iCar Pro | Bluetooth 4.0 / Wi-Fi | Buena calidad, protocolos correctos |
| OBDLink MX+ | Bluetooth 4.0 | Alta velocidad, soporte MS-CAN (Ford) |
| OBDLink LX | Bluetooth 2.0 | Sólido, económico |
| BAFX Products 34t5 | Bluetooth 2.0 | Muy fiable en Linux |
| Veepeak OBDCheck BLE | Bluetooth LE | Compatible iOS/macOS |
| Adaptadores USB genéricos CH340 | USB | Funciona bien en Linux, drivers incluidos |

> **Evitar:** Adaptadores ELM327 genéricos de <5€ en AliExpress — suelen usar firmware
> falsificado (versión 1.5 clónica) que da lecturas erróneas o se cuelga.

### Configuración por sistema operativo

#### Linux

```bash
# USB (aparece automáticamente al conectar):
ls /dev/ttyUSB*      # → /dev/ttyUSB0
ls /dev/ttyACM*      # → /dev/ttyACM0 (algunos clones)

# Añadir tu usuario al grupo dialout (una sola vez, requiere relogin):
sudo usermod -aG dialout $USER

# Configurar auto-diag:
export ADS_TRANSPORT=elm327
export ADS_SERIAL_PORT=/dev/ttyUSB0
autodiag pids
```

#### Bluetooth en Linux (rfcomm)

```bash
# Instalar rfcomm:
sudo apt-get install rfcomm bluez

# Buscar MAC de tu adaptador:
bluetoothctl scan on     # espera ~10s, anota la MAC (ej. AA:BB:CC:DD:EE:FF)

# Vincular:
bluetoothctl pair AA:BB:CC:DD:EE:FF
bluetoothctl trust AA:BB:CC:DD:EE:FF

# Crear dispositivo serie:
sudo rfcomm bind 0 AA:BB:CC:DD:EE:FF
# → Crea /dev/rfcomm0

export ADS_TRANSPORT=elm327
export ADS_SERIAL_PORT=/dev/rfcomm0
```

Para que sea persistente entre reinicios, añade en `/etc/rfcomm.conf`:
```
rfcomm0 { bind yes; device AA:BB:CC:DD:EE:FF; channel 1; comment "OBD2"; }
```

#### macOS

```bash
# USB:
ls /dev/cu.*         # → /dev/cu.usbserial-0001 o /dev/cu.SLAB_USBtoUART

# Bluetooth: empareja en Preferencias del Sistema → Bluetooth
# Luego:
ls /dev/cu.*         # → /dev/cu.OBDII o /dev/cu.OBDLink-BLE

export ADS_TRANSPORT=elm327
export ADS_SERIAL_PORT=/dev/cu.usbserial-0001
```

#### Windows

```batch
REM El adaptador USB/BT aparece como puerto COM:
REM Administrador de dispositivos → Puertos COM y LPT → COMx

set ADS_TRANSPORT=elm327
set ADS_SERIAL_PORT=COM3
```

---

## Interfaces CAN profesionales (para SocketCAN)

Estas interfaces permiten acceso directo al bus CAN sin pasar por el protocolo OBD-II.
Son necesarias para sniffing, decoding DBC y comunicación UDS de baja latencia.

### Hardware soportado via SocketCAN (Linux)

| Adaptador | Precio aprox. | Velocidad | Notas |
|-----------|---------------|-----------|-------|
| **PEAK PCAN-USB** | ~120€ | CAN 2.0 / CAN-FD | Referencia de la industria, drivers en kernel |
| **Kvaser Leaf Light v2** | ~180€ | CAN 2.0 | Alta fiabilidad, soportado por python-can |
| **CANable v2 (USB)** | ~25€ | CAN 2.0 | Open-source, excelente relación calidad-precio |
| **Macchina M2** | ~100€ | CAN 2.0 dual | Basado en Arduino, perfecto para proyectos DIY |
| **Canable Pro** | ~40€ | CAN 2.0 | Firmware slcan o candlelight (plug-and-play) |

### Comandos Linux para activar la interfaz

```bash
# Cargar módulos del kernel (una sola vez por sesión):
sudo modprobe can can_raw

# Interfaz CAN real (ej. PEAK PCAN-USB, CANable con firmware candlelight):
sudo ip link set can0 up type can bitrate 500000
# Para CAN-FD:
sudo ip link set can0 up type can bitrate 500000 dbitrate 2000000 fd on

# Verificar:
ip link show can0
# → can0: <NOARP,UP,LOWER_UP> mtu 16 ...

# Usar con auto-diag:
export ADS_TRANSPORT=socketcan
export ADS_CAN_CHANNEL=can0
```

### Diferencia entre CAN 2.0 y CAN-FD

| Característica | CAN 2.0 | CAN-FD |
|----------------|---------|--------|
| Velocidad datos | 1 Mbps máx. | 8 Mbps |
| Payload máximo | 8 bytes | 64 bytes |
| Uso en vehículos | 2000–2018 | 2018+ (progresivo) |
| Compatibilidad | Universal | Solo con nodos CAN-FD |
| Hardware necesario | Cualquier SocketCAN | CAN-FD compatible |

La mayoría de vehículos hasta 2020 usa CAN 2.0 a **500 kbps** (tren motriz) o
**125 kbps** (carrocería). Para sniffing básico con `ADS_TRANSPORT=socketcan`
se asume 500 kbps si no se configura `ADS_CAN_BITRATE`.

### Bus virtual (desarrollo sin hardware)

```bash
# Crear interfaz virtual (solo Linux, sin hardware físico):
sudo modprobe vcan
sudo ip link add dev vcan0 type vcan
sudo ip link set up vcan0

export ADS_TRANSPORT=socketcan
export ADS_CAN_CHANNEL=vcan0
```

---

## DoIP — Diagnóstico sobre IP (vehículos modernos)

### ¿Qué es DoIP?

DoIP (Diagnostics over Internet Protocol, ISO 13400) permite comunicación de diagnóstico
UDS a través de Ethernet en lugar del bus CAN. Ofrece mucho mayor ancho de banda (hasta
100 Mbps) para operaciones como flasheo de ECU.

### Vehículos que usan DoIP

- **VW/Audi/SEAT/Skoda** — desde ~2018 (plataforma MQB evolutivo, MLB evo)
- **BMW/MINI** — desde ~2017 (serie 5 G30+, X3 G01+)
- **Mercedes-Benz** — desde ~2019 (plataforma MRA2, plataforma EVA2)
- **Porsche** — desde ~2019 (Taycan, 992)
- Vehículos eléctricos modernos en general

### Hardware necesario para DoIP

Para conectarse al conector DoIP (normalmente un conector Ethernet automotriz de 8 pines
en el habitáculo o junto al OBD-II):

| Opción | Descripción |
|--------|-------------|
| **Interfaz Ethernet automotriz** | Adaptadores como Dearborn Group VSI-2534 o Softing TDX |
| **Adaptador doble CAN+DoIP** | OBDLink Pro, Autel MaxiFlash JVCI |
| **Adaptador DIY** | Raspberry Pi con interfaz 100BASE-T1 (BroadR-Reach) — avanzado |

> **Nota de compatibilidad:** El OBD-II DLC estándar (J1962) de 16 pines incluye los pines
> 1, 3, 8, 9, 11, 12, 13 reservados — algunos fabricantes usan estos pines para DoIP.
> Consulta el diagrama de pines específico de tu vehículo antes de conectar.

### Estado en Auto-Diag Suite

**DoIP está en la hoja de ruta — Fase 3.**

En la Fase 3 se añadirá:
- Transport layer DoIP (`obdcore/transport/doip.py`)
- Soporte para `ADS_TRANSPORT=doip` con `ADS_DOIP_HOST` y `ADS_DOIP_PORT`
- Autenticación por certificado (algunos fabricantes)

---

## Simulación (sin hardware)

### Modo `ADS_TRANSPORT=sim` (completamente virtual)

El modo simulado no requiere absolutamente ningún hardware ni módulo de hardware Python.
Responde a los comandos OBD-II con datos realistas pregenerados.

```bash
# Todo funciona sin hardware:
ADS_TRANSPORT=sim autodiag pids
ADS_TRANSPORT=sim autodiag dtc
ADS_TRANSPORT=sim uvicorn app.main:app --reload
ADS_TRANSPORT=sim python -m pytest -q
```

Datos que simula:
- PIDs modo 01: RPM (~850), velocidad, temperatura de refrigerante, carga del motor, etc.
- DTCs modo 03: códigos P0300, P0171 de ejemplo
- Respuestas UDS simuladas para sesiones de diagnóstico

### Bus virtual vcan0 (Linux)

Para desarrollo que requiere tráfico CAN real pero sin hardware físico:

```bash
# Crear bus virtual:
sudo modprobe vcan
sudo ip link add dev vcan0 type vcan
sudo ip link set up vcan0

# Inyectar tráfico de prueba:
cansend vcan0 7DF#0201050000000000   # request PID 05 (coolant temp)
candump vcan0                         # capturar todo el tráfico

# Usar con auto-diag:
export ADS_TRANSPORT=socketcan
export ADS_CAN_CHANNEL=vcan0
```

Esto es especialmente útil para:
- Probar el decoder DBC (`obdcore/can/decoder.py`) con datos reales
- Simular tramas de múltiples ECUs para testing de integración
- Desarrollo de nuevos transportes sin acceso a vehículo
