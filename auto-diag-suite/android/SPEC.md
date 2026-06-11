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

---

## Android Studio Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 35 (API level 35)
- JDK 11 or later
- An ELM327 OBD-II adapter (Bluetooth or WiFi)

### 1. Configure SDK path
```bash
cp local.properties.template local.properties
# Edit local.properties and set sdk.dir to your Android SDK path
# macOS:   sdk.dir=/Users/yourname/Library/Android/sdk
# Linux:   sdk.dir=/home/yourname/Android/Sdk
# Windows: sdk.dir=C\:\\Users\\yourname\\AppData\\Local\\Android\\Sdk
```

### 2. Open in Android Studio
1. Open Android Studio → **File → Open** → select `android/` directory
2. Wait for Gradle sync to complete
3. Connect an Android device (API 26+) or start an emulator

### 3. Build and Run
- Press **Run ▶** (Shift+F10) to build and deploy
- Or from terminal: `./gradlew assembleDebug`
- APK output: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Permissions Required at Runtime
- **Android 12+**: `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` — grant when prompted
- **Android 6–11**: `ACCESS_FINE_LOCATION` — required for BT device discovery

### 5. Connection Modes

#### WiFi / WebSocket
1. Ensure the FastAPI backend (`server/`) is running on the same network
2. Default endpoint: `ws://<IP>:8000/ws/live`
3. Enter the server IP in the Connect screen

#### Bluetooth ELM327
1. Pair your ELM327 adapter in **Android Settings → Bluetooth** first
2. Common pairing PIN: `1234` or `0000`
3. The adapter will appear in the "Paired Devices" list in the app

### 6. Project Structure
```
android/
├── app/src/main/
│   ├── java/com/autodiag/
│   │   ├── MainActivity.kt          # Entry point + Navigation
│   │   ├── data/
│   │   │   ├── ObdRepository.kt     # Interface
│   │   │   ├── WebSocketClient.kt   # WiFi/WS implementation
│   │   │   ├── BluetoothElm327.kt   # BT RFCOMM implementation
│   │   │   └── PidDecoder.kt        # ELM327 hex parsing
│   │   ├── model/
│   │   │   ├── PidData.kt           # Live data model
│   │   │   └── DtcCode.kt           # DTC model + descriptions
│   │   ├── viewmodel/
│   │   │   ├── DashboardViewModel.kt
│   │   │   └── DtcViewModel.kt
│   │   └── ui/
│   │       ├── ConnectScreen.kt
│   │       ├── DashboardScreen.kt
│   │       ├── DtcScreen.kt
│   │       └── components/
│   │           ├── GaugeView.kt     # Animated arc gauge
│   │           └── DtcCard.kt       # Fault code card
│   └── res/
│       └── values/
│           ├── strings.xml
│           ├── colors.xml
│           └── themes.xml
└── app/build.gradle.kts             # Dependencies
```
