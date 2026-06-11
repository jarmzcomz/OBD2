# Coding VAG — VW T-Cross 1.0 TSI 2026 (MQB-A0)

Guía práctica para el coding y la adaptación **legítima del propietario** con el
módulo `obdcore/vag/` de Auto-Diag Suite. Pensada para el dueño del vehículo que
quiere personalizar funciones de forma segura, con backups y verificación.

> **Importante sobre los valores:** los bytes/bits de este documento son **puntos
> de partida orientativos** procedentes de documentación comunitaria
> (foros VW, VCDS Wiki, OBDeleven). **NO son cifras oficiales de fábrica.** En el
> coding largo (long coding), el mapeo real **cambia entre versiones de software,
> mercados y revisiones de hardware**. Siempre debes **VERIFICAR el byte/bit contra
> tu coding real** antes de escribir. Nunca asumas.

---

## 1. Introducción y advertencias

### 1.1. Coding vs Adaptación vs Remap (lee esto primero)

Son tres cosas distintas y conviene no confundirlas:

| Concepto | Qué es | ¿Soportado en esta app? |
|----------|--------|--------------------------|
| **Coding (long coding)** | Activar/desactivar funciones ya presentes en un módulo cambiando bits de una cadena de configuración. Ej.: apagar las DRL. | **Sí.** Es el caso de uso principal. |
| **Adaptación (adaptation channels)** | Ajustar el **valor** de un parámetro dentro de un rango definido por el fabricante. Ej.: número de parpadeos del intermitente confort, duración del coming-home. | **Sí.** |
| **Remap / Chiptuning** | Reescribir el **firmware/mapa del motor** (ECU 0x01) para cambiar inyección, presión de turbo, límites de par. Aumentar potencia/torque entra aquí. | **NO.** |

> **Aumentar par o potencia NO es coding.** Es un **remap de firmware del motor**,
> una reescritura del software de la ECU. Esta app **no lo soporta, no lo
> recomienda y no lo facilita.** Implica riesgo mecánico (motor, embrague, caja,
> turbo), invalida la garantía y puede afectar emisiones y la legalidad del
> vehículo. Si lo que buscas es más potencia, eso queda **fuera del alcance** de
> esta herramienta.

El coding/adaptación, en cambio, solo **conmuta o ajusta funciones que el hardware
del coche ya tiene**. No toca el comportamiento del motor.

### 1.2. Disclaimer legal

- Usa esta guía **únicamente sobre un vehículo de tu propiedad**.
- **Algunas modificaciones de iluminación pueden no ser legales para circular en
  vía pública** según tu país/región (p. ej. DRL como luz de posición, ciertos
  patrones de intermitente, intensidades). La homologación de luces es estricta en
  muchas jurisdicciones (UE/ECE, etc.).
- **Tú eres el único responsable** del uso que des al vehículo y de cumplir la
  normativa local de circulación e ITV/inspección técnica.
- Esta documentación se ofrece "tal cual", sin garantía. Los autores no se hacen
  responsables de daños, multas o pérdidas derivadas de su uso.

### 1.3. Advertencia de garantía

- **VW puede detectar cambios de coding.** Los módulos registran cambios y un
  taller oficial puede comparar tu coding con el de fábrica.
- Un coding modificado puede ser motivo para **rechazar una reclamación de
  garantía** relacionada con la función afectada.
- Antes de una visita al concesionario o de la entrega de un coche de leasing,
  considera **restaurar el coding original** desde tu backup (ver sección 6).

---

## 2. Hardware necesario

### 2.1. Adaptador OBD-II

| Tipo | ¿Lee datos? | ¿Coding fiable VAG? |
|------|-------------|----------------------|
| ELM327 genérico/barato (clon) | Sí, lee DTCs y datos en vivo bien | **Riesgo alto.** No recomendado para escribir. |
| ELM327 de calidad (chip bueno, marca reputada) | Sí | Aceptable con cuidado. |
| Adaptador "full" (compatible VCDS, OBDeleven, Ross-Tech) | Sí | **Recomendado.** Soporta el protocolo completo de forma estable. |

- Para **LEER** (DTCs, parámetros en vivo) casi cualquier ELM327 sirve.
- Para **CODING fiable en VAG** conviene un **adaptador full** (tipo compatible
  VCDS/OBDeleven) o, como mínimo, un **ELM327 de buena calidad con chip genuino**.
- **Riesgo de los adaptadores chinos baratos:** muchos son **clones con firmware
  defectuoso** que pueden **corromper una escritura a medias**, perder bytes o
  cortar la sesión durante un write. Una escritura corrupta puede dejar un módulo
  en mal estado. **No escribas coding con un clon dudoso.** Para leer pasan; para
  escribir, no merece la pena el riesgo.

### 2.2. Dónde está el puerto OBD-II en el T-Cross

- **Debajo del volante, lado izquierdo del conductor**, en la zona baja del
  salpicadero (cerca de la columna de dirección / pedales). En algunos acabados
  está tapado por una pequeña cubierta de plástico.
- Conecta con el **contacto puesto** (encendido en posición ON), pero el motor
  apagado, salvo que un paso concreto pida lo contrario.
- Asegúrate de tener **batería cargada**; las escrituras no deben interrumpirse por
  caída de tensión. Si vas a hacer varias operaciones, usa un mantenedor de batería.

---

## 3. Mapa de módulos del T-Cross (MQB-A0)

Direcciones de diagnóstico estilo VCDS. Coinciden con `obdcore/vag/modules.py`.
Recuerda que en ese fichero algunos CAN-IDs están marcados como **aproximados
(`approx`)** y deben confirmarse contra el bus real del coche.

| Módulo | Dirección | Qué controla | Ejemplos de coding |
|--------|-----------|--------------|--------------------|
| **Motor (Engine Control)** | 0x01 | ECU 1.0 TSI | **NO codear a ciegas** — remap fuera de alcance |
| **Transmisión (TCU)** | 0x02 | Caja de cambios | No tocar sin necesidad |
| **ABS / ESP** | 0x03 | Frenos, estabilidad | **Crítico — no tocar** |
| **Climatronic / HVAC** | 0x08 | Clima | Opciones limitadas (memoria, recirculación) |
| **BCM (Central Electrics)** | 0x09 | Luces, intermitentes, confort eléctrico | DRL on/off, coming/leaving home, intermitente confort |
| **Parking Aid / ACC** | 0x10 | Asistente de aparcamiento | Sensibilidad/volumen del parking |
| **Steering Column** | 0x16 | Columna de dirección | Limitado |
| **Airbag** | 0x15 | Airbags, pretensores | **Crítico — NO tocar nunca a ciegas** |
| **Instrument Cluster (Cuadro)** | 0x17 | Display, cuadro | Velocidad digital, needle sweep, menús (staging) |
| **Gateway (CAN)** | 0x19 | Lista de módulos instalados | Avanzado — solo si sabes lo que haces |
| **Power Steering** | 0x44 | Dirección asistida | Limitado |
| **Comfort System (Komfort)** | 0x46 | Cierre, ventanas, retrovisores | Auto-lock, plegado de espejos, ventanas con mando |
| **Sound System** | 0x47 | Audio | Limitado |
| **Door Electronics (Drv/Pas/RL/RR)** | 0x42 / 0x52 / 0x62 / 0x72 | Puertas, espejos, ventanas | Mirror dip en reversa, confort de ventanas |
| **Information Electronics (MIB)** | 0x5F | Infotainment | Funciones de pantalla/menús |
| **Back-up Camera** | 0x6C | Cámara trasera | Líneas guía, calibración |
| **Front Assist** | 0xA5 | Asistente frontal/radar | Avanzado — con cuidado |

> Módulos **críticos** (no codear sin saber exactamente qué haces y por qué):
> **0x01 motor, 0x02 caja, 0x03 ABS/ESP, 0x15 airbag.** Ver sección 6.

---

## 4. Procedimiento paso a paso: apagar las DRL (luces diurnas)

Ejemplo completo y seguro sobre el **BCM (0x09)**. El mismo flujo sirve de patrón
para cualquier coding de long coding.

1. **Conectar y arrancar la app.** Enchufa el adaptador al puerto OBD-II, pon el
   contacto en ON (motor apagado) y abre Auto-Diag Suite en **modo `elm327`**.
   Comprueba que la app ve el bus y lista los módulos.

2. **Abrir sesión de diagnóstico EXTENDED en 0x09.** El coding requiere una sesión
   de diagnóstico extendida (UDS `DiagnosticSessionControl` → Extended). La app la
   abre contra el módulo BCM (0x09).

3. **SecurityAccess (si el módulo lo exige).** Para escribir puede pedirse un
   desbloqueo (`SecurityAccess`, seed→key). **La app NO calcula ni crackea esta
   llave.** Si tu módulo la requiere, debes introducir la llave de coding que
   figura en tu manual/documentación legítima. Esta herramienta solo gestiona el
   intercambio estándar; no incluye generadores de llaves.

4. **LEER y GUARDAR el long coding actual (backup Zero-Brick).** Lee el long coding
   completo del 0x09 y **guárdalo como backup**. La app fuerza este backup
   ("Zero-Brick") antes de permitir cualquier escritura. Este es tu seguro: te
   permite **volver al estado original** si algo sale mal.

5. **Identificar el byte/bit del DRL.** El bit de DRL vive dentro del long coding
   del BCM. **No asumas la posición.** Para encontrarla:
   - Compara tu coding real con documentación comunitaria (foros VW T-Cross,
     OBDeleven "One-Click", VCDS Label Files) para tu **versión de software**.
   - Cruza la información: si varias fuentes coinciden para tu HW/SW, gana
     confianza.
   - Si dudas, **no escribas**. Un bit equivocado puede activar/desactivar otra
     función. En T-Cross/MQB-A0 el DRL suele estar en la zona de bytes de
     iluminación del long coding, pero **la posición exacta debes confirmarla en tu
     coche**.

6. **Cambiar el bit y revisar el DIFF.** Modifica solo el bit objetivo. La app
   muestra un **DIFF** (antes → después) resaltando exactamente qué cambia.
   Revísalo: debe cambiar **un solo bit**, el que esperas. Si el diff toca más de
   lo previsto, **cancela**.

7. **Escribir.** Confirma la escritura. La app **exige un backup verificado** antes
   de permitir el write; si no hay backup válido, no escribe.

8. **Verificar en el coche y restaurar si hace falta.** Apaga y enciende el
   contacto, comprueba el comportamiento real de las luces. Revisa que no aparezcan
   DTCs nuevos. **Si algo va mal, RESTAURA el backup** del paso 4 inmediatamente
   (sección 6).

---

## 5. Funciones populares conocidas (con disclaimer)

Mods que la comunidad documenta para T-Cross / MQB-A0. **Para todos vale la misma
nota: verifica el byte/bit contra TU coding real antes de escribir.** Los valores
de partida cambian según software/mercado.

| Función | Módulo | Tipo | Nota |
|---------|--------|------|------|
| **DRL on/off** | BCM 0x09 | Coding (bit) | Posición de bit a verificar |
| **DRL como luz de posición** | BCM 0x09 | Coding (bit) | **Puede no ser legal en vía pública según país** |
| **Coming / Leaving home (duración)** | BCM 0x09 | Adaptación | Ajustar segundos dentro del rango |
| **Intermitente confort (3 o 5 parpadeos)** | BCM 0x09 | Adaptación | Nº de parpadeos por toque |
| **Mirror dip en reversa (espejo baja)** | Comfort 0x46 / Puerta 0x42/0x52 | Coding/adaptación | Requiere retrovisores eléctricos con memoria |
| **Auto-lock al circular / auto-unlock al apagar** | Comfort 0x46 | Coding/adaptación | Bloqueo automático a cierta velocidad |
| **Velocidad digital en el cuadro** | Cluster 0x17 | Coding | Depende de variante de cuadro |
| **Needle sweep / staging al arrancar** | Cluster 0x17 | Coding | Barrido de agujas / animación de bienvenida |
| **Window comfort open/close con el mando** | Comfort 0x46 | Coding/adaptación | Subir/bajar ventanas manteniendo el botón del mando |

> Recordatorio legal: **DRL como posición** y ciertos patrones de intermitente
> pueden **no estar homologados** para circular en algunos países. Comprueba tu
> normativa antes de usarlos en vía pública.

---

## 6. Seguridad y recuperación

### 6.1. Regla de oro

- **SIEMPRE backup antes de escribir.** La app lo **fuerza** (Zero-Brick): sin un
  backup verificado del módulo, no permite el write. No desactives esta protección.
- Guarda los backups en lugar seguro y **anota qué versión de software** tenía el
  módulo cuando los hiciste.

### 6.2. Cómo restaurar si el módulo se comporta mal

1. Abre la app, selecciona el módulo afectado.
2. Abre sesión **EXTENDED** y, si aplica, el `SecurityAccess` con tu llave.
3. Carga el **backup** correspondiente a ese módulo.
4. **Escribe el backup** para devolver el long coding/adaptación a su estado previo.
5. Apaga/enciende el contacto y verifica que la función vuelve a la normalidad.

### 6.3. Qué NO tocar

- **Airbag (0x15):** seguridad pasiva. Un coding erróneo puede **desactivar
  airbags o pretensores**. **Nunca codear a ciegas.**
- **ABS / ESP (0x03):** frenos y estabilidad. No tocar sin necesidad y conocimiento
  exactos.
- **Motor (0x01) / Transmisión (0x02):** comportamiento mecánico. El remap está
  **fuera de alcance**; no codees parámetros del motor a ciegas.
- **Gateway (0x19):** controla la lista de módulos; un error puede hacer
  "desaparecer" módulos del diagnóstico.

Regla general: **si no sabes exactamente qué hace un byte/bit, no lo escribas.**

### 6.4. Qué hacer si aparecen DTCs tras un cambio

1. **Lee los DTCs** del módulo afectado (y módulos vecinos).
2. **Bórralos** una vez (algunos quedan latentes tras conectar/desconectar).
3. Reproduce la condición y vuelve a leer:
   - Si **no reaparecen**, era un código transitorio por la sesión de diagnóstico.
   - Si **persisten**, el cambio probablemente es la causa: **revierte al backup**
     (6.2) y vuelve a comprobar.
4. Si tras restaurar el backup los DTCs siguen, no continúes codeando y consulta a
   un especialista con equipo VCDS/ODIS.

---

## 7. Glosario

- **UDS (Unified Diagnostic Services, ISO 14229):** protocolo de diagnóstico sobre
  CAN que usan los módulos VAG modernos para sesiones, lectura/escritura y seguridad.
- **DID (Data Identifier):** identificador de un dato concreto dentro de un módulo
  (se lee/escribe con `ReadDataByIdentifier` / `WriteDataByIdentifier`).
- **Long coding (coding largo):** cadena de bytes de configuración de un módulo.
  Cada **bit** activa/desactiva una función. Es lo que se edita en la sección 4.
- **Adaptation channel (canal de adaptación):** parámetro ajustable a un **valor**
  dentro de un rango definido por el fabricante (no un simple on/off).
- **SecurityAccess (seed→key):** mecanismo UDS que desbloquea la escritura. El
  módulo da un "seed" y espera la "key" correcta. **La app no calcula la llave**;
  la introduce el usuario desde su documentación legítima.
- **DiagnosticSessionControl / sesión EXTENDED:** comando UDS para pasar a una
  sesión de diagnóstico extendida, necesaria para codear.
- **DTC (Diagnostic Trouble Code):** código de avería almacenado en un módulo.
- **MQB-A0:** plataforma modular de VAG para coches pequeños (T-Cross, Polo, Ibiza,
  Arona...). Define la electrónica y el set de módulos disponibles.
- **BCM (Body Control Module) / Central Electrics (0x09):** controla iluminación,
  intermitentes y confort eléctrico de carrocería.
- **Cluster (0x17):** cuadro de instrumentos (display, agujas, menús).
- **Komfort / Comfort System (0x46):** cierre centralizado, ventanas, retrovisores.
- **Gateway (0x19):** módulo central que enruta el CAN y lista los módulos
  instalados.
- **Zero-Brick:** mecanismo de la app que **obliga a un backup verificado** antes de
  cualquier escritura, para poder restaurar el estado original.
- **DRL (Daytime Running Lights):** luces de circulación diurna.
- **Needle sweep / staging:** barrido de agujas y animación de bienvenida del cuadro
  al arrancar.
