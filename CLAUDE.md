# Auto-Diag Suite — guía para agentes

Suite de diagnóstico automotriz OBD-II / CAN / UDS, enfocada en VAG (VW T-Cross,
plataforma MQB A0). Dos piezas:

- **App Android nativa** (`auto-diag-suite/android/`) — Kotlin + Jetpack Compose.
  Es el producto principal: habla con un ELM327 por BLE/Bluetooth clásico y hace
  OBD-II (PIDs, DTCs, VIN), lectura/escritura VAG (UDS) y acceso de seguridad.
- **Backend Python** (`auto-diag-suite/obdcore/`, `auto-diag-suite/app/`) —
  FastAPI + núcleo UDS/CAN. Modo `sim` para tests sin hardware. Hoy es secundario
  (la app es autónoma); mantenerlo verde pero no es la prioridad.

## Reglas del proyecto

1. **Trabaja en la rama** `claude/trusting-hypatia-wml1kc` (PR #4 → `main`).
2. **Sí se escribe código.** Esto es una app real, no un generador de blueprints.
3. **Honestidad técnica por encima de todo.** No prometas desbloquear módulos
   protegidos por SFD: esos exigen token online de VW (ODIS/GeKo) y ninguna
   herramienta DIY los salta. Si un módulo responde `7F 27 11` a todos los niveles,
   es SFD — dilo claro, no lo disfraces de error.
4. **Nada de claves/semillas inventadas.** El algoritmo SA2 (`Sa2SeedKey.kt`) está
   verificado contra vectores oficiales; necesita el bytecode real del ODX/FRF del
   módulo. Sin ese dato, no hay clave — y así debe comunicarse.

## Estructura clave (Android)

- `data/` — transportes (`BleElm327`, `BluetoothElm327`, `WebSocketClient`),
  decoders (`PidDecoder`, `Mode09Decoder`, `DtcParser`, `UdsInterpreter`),
  seguridad (`UdsSecurity` puro + testeable, `Sa2SeedKey` VM), catálogo VAG.
- `viewmodel/` — `VagViewModel` (cerebro VAG), `DashboardViewModel`, etc.
- `ui/HomeScreen.kt` — pestañas Compose (Tablero, Sensores, Gráficas, Fallas,
  Mi Auto, Más). Componentes reutilizables: `VagCard`, `PillButton`.

## Build y verificación

- **CI**: `.github/workflows/android.yml` corre `testDebugUnitTest` y luego
  `assembleDebug`; publica el APK en el release `android-latest`. Si los tests
  fallan, no se publica APK.
- **No hay gradle local** en este entorno: se valida vía CI. Sube la versión en
  `app/build.gradle.kts` (`versionCode`/`versionName`) en cada cambio visible.
- **Backend**: `pytest` en modo `ADS_TRANSPORT=sim`; lint con `ruff` (línea 100).

## Tests

- `data/Sa2SeedKeyTest` — vectores oficiales de la VM SA2.
- `data/UdsSecurityE2ETest` — cadena e2e semilla→clave→escritura contra un módulo
  simulado. Al tocar seguridad UDS, extiende estos.
