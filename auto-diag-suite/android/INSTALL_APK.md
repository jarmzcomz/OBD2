# Cómo conseguir e instalar el APK en tu móvil

No necesitas un PC con Android Studio: **GitHub compila el APK por ti** en la nube.

## 1. Descargar el APK desde GitHub Actions

1. Entra en el repositorio en GitHub → pestaña **Actions**
2. Abre el workflow **"Android APK"** (el último run en verde ✅)
3. Abajo, en **Artifacts**, descarga **`auto-diag-suite-debug-apk`**
4. Descomprime el `.zip` → dentro está `app-debug.apk`

> Cada vez que se sube código al proyecto Android, GitHub regenera el APK
> automáticamente. También puedes lanzarlo a mano con **"Run workflow"**.

## 2. Instalar el APK en tu móvil Android

1. Pasa el `app-debug.apk` al teléfono (cable USB, Google Drive, Telegram, etc.)
2. Ábrelo desde el móvil
3. Android pedirá permitir **"Instalar apps de orígenes desconocidos"** → acéptalo
   (es normal: el APK no viene de Play Store)
4. Pulsa **Instalar**

## 3. Primer uso

1. Abre **Auto-Diag Suite**
2. Concede permisos de **Bluetooth** (Android 12+) o **Ubicación** (Android 6–11)
3. Pantalla de conexión — dos modos:
   - **Bluetooth ELM327:** empareja antes tu adaptador en Ajustes → Bluetooth
     (PIN típico `1234` o `0000`), luego elígelo en la app
   - **WiFi:** si tienes el servidor Python corriendo en tu PC, introduce su IP

## Notas

- Es un **APK de debug** (sin firmar para Play Store). Perfecto para uso personal.
- Para coding VAG fiable, recuerda que un ELM327 genérico a veces falla al escribir;
  el dashboard web (PC) es más robusto para escrituras. La app móvil es ideal para
  **leer datos en vivo y DTCs** sobre la marcha.
- Si la instalación se bloquea: Ajustes → Apps → tu navegador/gestor de archivos →
  "Instalar apps desconocidas" → permitir.
