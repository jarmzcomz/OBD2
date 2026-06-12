<p align="center">
  <h1 align="center">Auto-Diag Suite</h1>
  <p align="center">
    <strong>Suite de diagnóstico vehicular OBD-II con soporte Bluetooth y WebSocket</strong>
  </p>
  <p align="center">
    Lectura y escritura de parámetros de diagnóstico en vehículos OBD-II · Soporte múltiples transportes · Dashboard en vivo
  </p>
  <p align="center">
    <a href="#español">Español</a> &middot;
    <a href="#english">English</a>
  </p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-Kotlin-green?style=for-the-badge" alt="Android Kotlin" />
  <img src="https://img.shields.io/badge/Bluetooth-Classic%20%26%20LE-blue?style=for-the-badge" alt="Bluetooth Classic & LE" />
  <img src="https://img.shields.io/badge/OBD--II-ELM327-orange?style=for-the-badge" alt="OBD-II ELM327" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="MIT License" />
</p>

---

# Español

## ¿Qué es Auto-Diag Suite?

Auto-Diag Suite es una **suite completa de diagnóstico automotriz** que te permite:

- ✅ **Leer parámetros en vivo** (RPM, temperatura, presión, posición del acelerador, MAF, etc.)
- ✅ **Leer y borrar códigos de falla (DTCs)** desde la computadora del vehículo
- ✅ **Conectarse vía Bluetooth Clásico** (iCar Pro 3.0) o **Bluetooth Low Energy** (iCar Pro 4.0)
- ✅ **Dashboard en vivo** con gauges interactivos
- ✅ **Soporte WebSocket** para conexión remota
- ✅ **Interfaz reactiva** con Android Compose

Diseñada para **talleres y dueños de vehículos** que necesitan diagnosticar y monitorear el estado de sus autos sin herramientas profesionales costosas.

---

## Requisitos de Hardware

### Adaptadores OBD-II Soportados

| Adaptador | Transporte | Estado | Notas |
|-----------|-----------|--------|-------|
| **iCar Pro 3.0** (Vgate) | Bluetooth Clásico (RFCOMM/SPP) | ✅ Soportado | Conexión estándar ELM327 |
| **iCar Pro 4.0 BLE** (Vgate) | Bluetooth Low Energy (GATT) | ✅ Soportado | Protocolo GATT FFE0/FFE1 |
| **Genéricos ELM327** | Bluetooth Clásico | ✅ Soportado | Con fallback a canal RFCOMM 1 |
| **WebSocket (remoto)** | TCP/WebSocket | ✅ Soportado | Para testing y conexión remota |

### Requerimientos del Dispositivo

- **Android 8.0+** (API 26+)
- **Bluetooth 4.0+** (para Bluetooth Clásico)
- **Bluetooth 5.0+** (recomendado para BLE)
- **Permisos de Bluetooth** (solicitados en tiempo de ejecución)

---

## Características

### Monitoreo en Vivo
- PIDs en tiempo real: RPM, velocidad, temperatura de agua, aire entrante, presión de gasolina, posición acelerador, MAF, etc.
- Actualización cada 500ms
- Gauges visuales con rango de valores

### Diagnóstico
- **Lectura de DTCs (Códigos de Falla):**
  - Prefijos: P (Powertrain), C (Chassis), B (Body), U (Network)
  - Descripción completa de cada código
  - Historial de faltas
- **Borrado de DTCs** con confirmación
- **Lectura de PIDs soportados** por el vehículo

### Transportes
- **Bluetooth Clásico (RFCOMM):** Compatible con iCar Pro 3.0 y clones
- **Bluetooth Low Energy (GATT):** Compatible con iCar Pro 4.0 BLE
- **Fallback automático:** Si la conexión estándar falla, intenta canal RFCOMM 1
- **WebSocket:** Modo remoto para testing y conexión a servidor

### UI Inteligente
- Descubrimiento automático de dispositivos Bluetooth pareados
- Escaneo BLE con lista de dispositivos disponibles
- Entrada manual de dirección MAC como fallback
- Indicador de estado de conexión
- Manejo de permisos en tiempo de ejecución

---

## Instalación

### Requisitos Previos

- Android Studio 2023.1+
- JDK 17+
- Gradle 8.0+
- SDK Android 35 (compileSdk)
- Mínimo SDK 26 (Android 8.0)

### Clonación y Setup

```bash
# Clona el repositorio
git clone https://github.com/jarmzcomz/OBD2.git
cd OBD2/auto-diag-suite

# Abre en Android Studio
# File > Open > selecciona la carpeta android/

# O construye desde línea de comandos
cd android
./gradlew build
```

### Instalación en Dispositivo

```bash
# Descarga el APK más reciente desde Releases
# O construye y despliega:
cd android
./gradlew installDebug
```

**Descargar APK:**
La versión más reciente está disponible en [GitHub Releases](https://github.com/jarmzcomz/OBD2/releases/download/android-latest/app-debug.apk)

---

## Uso Básico

### Paso 1: Emparejar el Dongle
1. En **Configuración > Bluetooth** del teléfono, empareja el dongle iCar Pro
2. Asegúrate de que Bluetooth esté **activado**

### Paso 2: Abre la App
1. Abre Auto-Diag Suite
2. Verás la pantalla de conexión con dos modos:

**Opción A: Bluetooth Clásico (iCar Pro 3.0)**
- Selecciona **"BT Clásico (iCar Pro 3.0)"**
- Elige el dongle de la lista de dispositivos pareados
- Toca **Conectar**

**Opción B: Bluetooth LE (iCar Pro 4.0)**
- Selecciona **"BT LE (iCar Pro 4.0)"**
- Toca **Escanear** para buscar dispositivos BLE
- Selecciona tu dongle de la lista
- Toca **Conectar**

**Opción C: WiFi (Testing)**
- Selecciona **"WiFi"**
- Ingresa dirección IP:puerto del servidor WebSocket
- Toca **Conectar**

### Paso 3: Dashboard
Una vez conectado, verás:
- **PIDs en Vivo:** RPM, temperatura, velocidad, etc.
- **Botón DTCs:** Lee códigos de falla activos
- **Indicador:** Verde = conectado, Rojo = desconectado

### Paso 4: Leer Códigos de Falla
1. Toca **LEER DTCs**
2. La app mostrará lista de códigos con descripción
3. Para borrar, toca **BORRAR DTCs** (requiere confirmación)

---

## Permisos Requeridos

| Permiso | Propósito | Min API |
|---------|-----------|---------|
| `BLUETOOTH` | Conexión a dispositivos | 8.0+ |
| `BLUETOOTH_CONNECT` | Conectar a pareados | 12.0+ (runtime) |
| `BLUETOOTH_SCAN` | Escaneo BLE | 12.0+ (runtime) |
| `BLUETOOTH_ADMIN` | Control de Bluetooth | 8.0+ |

La app solicita permisos en tiempo de ejecución en Android 12+.

---

## Estructura del Código (Android)

```
auto-diag-suite/android/app/src/main/java/com/autodiag/
├── data/
│   ├── BluetoothElm327Repository.kt    # Transporte Bluetooth Clásico
│   ├── BleElm327Repository.kt          # Transporte Bluetooth LE
│   ├── WebSocketRepository.kt          # Transporte WebSocket
│   ├── PidDecoder.kt                   # Decodificación de PIDs
│   └── ObdRepository.kt                # Interfaz común
├── model/
│   ├── DtcCode.kt                      # Modelo de código de falla
│   ├── PidData.kt                      # Modelo de parámetro en vivo
│   └── DtcDescriptions.kt              # Catálogo de descripciones
├── viewmodel/
│   ├── DashboardViewModel.kt           # Estado del dashboard
│   └── DtcViewModel.kt                 # Estado de DTCs
└── ui/
    ├── ConnectScreen.kt                # Pantalla de conexión
    ├── DashboardScreen.kt              # Pantalla principal
    ├── DtcScreen.kt                    # Pantalla de DTCs
    └── components/
        ├── GaugeChart.kt               # Widget de medidor
        └── PidCard.kt                  # Tarjeta de parámetro
```

---

## Protocolos

### Bluetooth Clásico (RFCOMM/SPP)
```
ELM327 SPP UUID: 00001101-0000-1000-8000-00805F9B34FB
Flujo:
1. createRfcommSocketToServiceRecord(UUID)
2. Si falla → createRfcommSocket(1) por reflexión
3. Enviar: "ATZ\r" (reset)
4. Enviar: "ATE0\r", "ATL0\r", "ATSP0\r" (configuración)
5. Enviar comandos OBD-II: "0101\r" (leer PIDs)
```

### Bluetooth LE (GATT)
```
Servicio: 0000FFE0-0000-1000-8000-00805F9B34FB
Característica: 0000FFE1-0000-1000-8000-00805F9B34FB
CCCD: 00002902-0000-1000-8000-00805F9B34FB

Flujo:
1. connectGatt()
2. discoverServices()
3. setCharacteristicNotification(FFE1, true)
4. writeDescriptor(CCCD, ENABLE_NOTIFICATION_VALUE)
5. writeCharacteristic(FFE1, "ATZ\r")
6. Acumular notificaciones hasta encontrar '>'
```

---

## Troubleshooting

### "No aparecen dispositivos pareados"
- ✅ Verifica que Bluetooth esté **ON**
- ✅ Asegúrate de haber emparejado el dongle en Configuración
- ✅ Android 12+: Verifica que la app tenga permiso **BLUETOOTH_CONNECT**
- ✅ Toca **Actualizar** en la pantalla de conexión

### "Conexión exitosa pero sin datos"
- ✅ El dongle iCar Pro 3.0 usa **Bluetooth Clásico** (modo "BT Clásico")
- ✅ El dongle iCar Pro 4.0 BLE requiere modo **"BT LE"** (diferente protocolo)
- ✅ Verifica que el motor esté **encendido** (necesario para OBD-II)

### "Error: 'read failed, socket might closed'"
- ✅ Cancela descubrimiento de Bluetooth antes de conectar
- ✅ Intenta desemparejar y emparejar nuevamente
- ✅ Reinicia Bluetooth en el teléfono

### "BLE: FFE0/FFE1 service not found"
- ✅ El dispositivo no es BLE, intenta modo **"BT Clásico"**
- ✅ Asegúrate de tener permiso **BLUETOOTH_SCAN** en Android 12+

---

## Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el repositorio
2. Crea una rama: `git checkout -b feature/mi-feature`
3. Commit: `git commit -am 'Agrego mi feature'`
4. Push: `git push origin feature/mi-feature`
5. Abre Pull Request

### Áreas de Mejora
- [ ] Soporte UDS (ISO 14229) para lectura/escritura avanzada
- [ ] Sniffer CAN (si el hardware lo permite)
- [ ] Exportación de logs a CSV/PDF
- [ ] Historial de sesiones
- [ ] Tema oscuro

---

## Licencia

Licencia MIT. Ver [LICENSE](LICENSE) para detalles.

---

## Seguridad

⚠️ **Uso Legítimo Solamente**

Esta herramienta está diseñada para:
- ✅ Diagnóstico de vehículos propios
- ✅ Lectura de parámetros en vivo
- ✅ Lectura de códigos de falla
- ✅ Mantenimiento preventivo

**NO** se debe usar para:
- ❌ Manipulación de odómetro
- ❌ Desactivación del airbag
- ❌ Bypass de sistemas de seguridad
- ❌ Modificación no autorizada de vehículos ajenos

El usuario es responsable del cumplimiento legal del uso de esta herramienta en su jurisdicción.

---

## Créditos

- **Vgate iCar Pro**: Hardware OBD-II Bluetooth
- **ELM327**: Protocolo de comunicación
- **Android Jetpack Compose**: UI Framework
- **Kotlin Coroutines**: Asincronía

---

---

# English

## What is Auto-Diag Suite?

Auto-Diag Suite is a **complete automotive diagnostic suite** that allows you to:

- ✅ **Read live parameters** (RPM, temperature, pressure, throttle position, MAF, etc.)
- ✅ **Read and clear fault codes (DTCs)** from the vehicle's computer
- ✅ **Connect via Bluetooth Classic** (iCar Pro 3.0) or **Bluetooth Low Energy** (iCar Pro 4.0)
- ✅ **Live dashboard** with interactive gauges
- ✅ **WebSocket support** for remote connection
- ✅ **Reactive UI** with Android Compose

Designed for **repair shops and vehicle owners** who need to diagnose and monitor their cars without expensive professional tools.

---

## Hardware Requirements

### Supported OBD-II Adapters

| Adapter | Transport | Status | Notes |
|---------|-----------|--------|-------|
| **iCar Pro 3.0** (Vgate) | Bluetooth Classic (RFCOMM/SPP) | ✅ Supported | Standard ELM327 connection |
| **iCar Pro 4.0 BLE** (Vgate) | Bluetooth Low Energy (GATT) | ✅ Supported | GATT FFE0/FFE1 protocol |
| **Generic ELM327** | Bluetooth Classic | ✅ Supported | With fallback to RFCOMM channel 1 |
| **WebSocket (remote)** | TCP/WebSocket | ✅ Supported | For testing and remote connection |

### Device Requirements

- **Android 8.0+** (API 26+)
- **Bluetooth 4.0+** (for Bluetooth Classic)
- **Bluetooth 5.0+** (recommended for BLE)
- **Bluetooth permissions** (requested at runtime)

---

## Features

### Live Monitoring
- Real-time PIDs: RPM, speed, coolant temperature, intake air, fuel pressure, throttle position, MAF, etc.
- 500ms update interval
- Visual gauges with value ranges

### Diagnostics
- **DTC Reading (Fault Codes):**
  - Prefixes: P (Powertrain), C (Chassis), B (Body), U (Network)
  - Full description for each code
  - Fault history
- **DTC Clearing** with confirmation
- **Supported PIDs** reading from vehicle

### Transports
- **Bluetooth Classic (RFCOMM):** Compatible with iCar Pro 3.0 and clones
- **Bluetooth Low Energy (GATT):** Compatible with iCar Pro 4.0 BLE
- **Automatic fallback:** If standard connection fails, tries RFCOMM channel 1
- **WebSocket:** Remote mode for testing and server connection

### Smart UI
- Automatic discovery of paired Bluetooth devices
- BLE scanning with available devices list
- Manual MAC address input as fallback
- Connection status indicator
- Runtime permission handling

---

## Installation

### Prerequisites

- Android Studio 2023.1+
- JDK 17+
- Gradle 8.0+
- Android SDK 35 (compileSdk)
- Minimum SDK 26 (Android 8.0)

### Clone and Setup

```bash
# Clone the repository
git clone https://github.com/jarmzcomz/OBD2.git
cd OBD2/auto-diag-suite

# Open in Android Studio
# File > Open > select the android/ folder

# Or build from command line
cd android
./gradlew build
```

### Install on Device

```bash
# Download the latest APK from Releases
# Or build and deploy:
cd android
./gradlew installDebug
```

**Download APK:**
The latest version is available at [GitHub Releases](https://github.com/jarmzcomz/OBD2/releases/download/android-latest/app-debug.apk)

---

## Basic Usage

### Step 1: Pair the Dongle
1. Go to **Settings > Bluetooth** on your phone
2. Pair the iCar Pro dongle
3. Make sure Bluetooth is **enabled**

### Step 2: Open the App
1. Launch Auto-Diag Suite
2. You'll see the connection screen with two modes:

**Option A: Bluetooth Classic (iCar Pro 3.0)**
- Select **"BT Classic (iCar Pro 3.0)"**
- Choose the dongle from your paired devices list
- Tap **Connect**

**Option B: Bluetooth LE (iCar Pro 4.0)**
- Select **"BT LE (iCar Pro 4.0)"**
- Tap **Scan** to search for BLE devices
- Select your dongle from the list
- Tap **Connect**

**Option C: WiFi (Testing)**
- Select **"WiFi"**
- Enter WebSocket server IP:port
- Tap **Connect**

### Step 3: Dashboard
Once connected, you'll see:
- **Live PIDs:** RPM, temperature, speed, etc.
- **DTC Button:** Read active fault codes
- **Status:** Green = connected, Red = disconnected

### Step 4: Read Fault Codes
1. Tap **READ DTCs**
2. The app will display codes with descriptions
3. To clear, tap **CLEAR DTCs** (requires confirmation)

---

## Required Permissions

| Permission | Purpose | Min API |
|-----------|---------|---------|
| `BLUETOOTH` | Device connection | 8.0+ |
| `BLUETOOTH_CONNECT` | Connect to paired devices | 12.0+ (runtime) |
| `BLUETOOTH_SCAN` | BLE scanning | 12.0+ (runtime) |
| `BLUETOOTH_ADMIN` | Bluetooth control | 8.0+ |

The app requests permissions at runtime on Android 12+.

---

## Code Structure (Android)

```
auto-diag-suite/android/app/src/main/java/com/autodiag/
├── data/
│   ├── BluetoothElm327Repository.kt    # Bluetooth Classic transport
│   ├── BleElm327Repository.kt          # Bluetooth LE transport
│   ├── WebSocketRepository.kt          # WebSocket transport
│   ├── PidDecoder.kt                   # PID decoding
│   └── ObdRepository.kt                # Common interface
├── model/
│   ├── DtcCode.kt                      # Fault code model
│   ├── PidData.kt                      # Live parameter model
│   └── DtcDescriptions.kt              # Descriptions catalog
├── viewmodel/
│   ├── DashboardViewModel.kt           # Dashboard state
│   └── DtcViewModel.kt                 # DTC state
└── ui/
    ├── ConnectScreen.kt                # Connection screen
    ├── DashboardScreen.kt              # Main screen
    ├── DtcScreen.kt                    # DTC screen
    └── components/
        ├── GaugeChart.kt               # Gauge widget
        └── PidCard.kt                  # Parameter card
```

---

## Protocols

### Bluetooth Classic (RFCOMM/SPP)
```
ELM327 SPP UUID: 00001101-0000-1000-8000-00805F9B34FB
Flow:
1. createRfcommSocketToServiceRecord(UUID)
2. If fails → createRfcommSocket(1) via reflection
3. Send: "ATZ\r" (reset)
4. Send: "ATE0\r", "ATL0\r", "ATSP0\r" (config)
5. Send OBD-II commands: "0101\r" (read PIDs)
```

### Bluetooth LE (GATT)
```
Service: 0000FFE0-0000-1000-8000-00805F9B34FB
Characteristic: 0000FFE1-0000-1000-8000-00805F9B34FB
CCCD: 00002902-0000-1000-8000-00805F9B34FB

Flow:
1. connectGatt()
2. discoverServices()
3. setCharacteristicNotification(FFE1, true)
4. writeDescriptor(CCCD, ENABLE_NOTIFICATION_VALUE)
5. writeCharacteristic(FFE1, "ATZ\r")
6. Accumulate notifications until '>' is found
```

---

## Troubleshooting

### "No paired devices appear"
- ✅ Verify Bluetooth is **ON**
- ✅ Make sure you've paired the dongle in Settings
- ✅ Android 12+: Check that the app has **BLUETOOTH_CONNECT** permission
- ✅ Tap **Refresh** on the connection screen

### "Connected but no data"
- ✅ iCar Pro 3.0 uses **Bluetooth Classic** (mode "BT Classic")
- ✅ iCar Pro 4.0 BLE requires **"BT LE"** mode (different protocol)
- ✅ Make sure the engine is **ON** (required for OBD-II)

### "Error: 'read failed, socket might closed'"
- ✅ Cancel Bluetooth discovery before connecting
- ✅ Try unpairing and pairing again
- ✅ Restart Bluetooth on your phone

### "BLE: FFE0/FFE1 service not found"
- ✅ Device is not BLE, try **"BT Classic"** mode
- ✅ Make sure you have **BLUETOOTH_SCAN** permission on Android 12+

---

## Contributing

Contributions are welcome. Please:

1. Fork the repository
2. Create a branch: `git checkout -b feature/my-feature`
3. Commit: `git commit -am 'Add my feature'`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request

### Areas for Improvement
- [ ] UDS (ISO 14229) support for advanced read/write
- [ ] CAN sniffer (if hardware permits)
- [ ] Export logs to CSV/PDF
- [ ] Session history
- [ ] Dark mode

---

## License

MIT License. See [LICENSE](LICENSE) for details.

---

## Security

⚠️ **Legitimate Use Only**

This tool is designed for:
- ✅ Diagnosing your own vehicles
- ✅ Reading live parameters
- ✅ Reading fault codes
- ✅ Preventive maintenance

**MUST NOT** be used for:
- ❌ Odometer manipulation
- ❌ Airbag disabling
- ❌ Security system bypass
- ❌ Unauthorized vehicle modification

The user is responsible for legal compliance with the use of this tool in their jurisdiction.

---

## Credits

- **Vgate iCar Pro:** Bluetooth OBD-II Hardware
- **ELM327:** Communication protocol
- **Android Jetpack Compose:** UI Framework
- **Kotlin Coroutines:** Async runtime

---
