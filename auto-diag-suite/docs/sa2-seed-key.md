# Acceso de seguridad MQB (SA2 seed-key) — cómo obtener la clave real

La codificación protegida del MQB usa el algoritmo **SA2**: el ECU entrega una
*semilla* y la herramienta debe responder con la *clave* correcta. Esa clave se
calcula ejecutando un **bytecode** (mini-programa) sobre la semilla.

- El **algoritmo** (la máquina virtual SA2) ya está en la app: `Sa2SeedKey.kt`,
  verificado contra los vectores oficiales de `jpbede/sa2go`.
- El **bytecode** NO es universal: es único por módulo y por versión de software,
  y vive dentro del archivo de flasheo (`.frf`) de VW de ESE módulo. Por eso no
  existe un "decodificador universal" para MQB.

## Paso a paso

1. **Identifica tu módulo** (en la app, pestaña *Mi Auto*):
   - Toca **"Leer módulo confort"** → anota el **número de pieza** (DID `F187`) y la
     **versión de software** (DID `F189`). Eso define el `.frf` exacto.

2. **Consigue el `.frf`** de esa pieza/versión:
   - Portal **erWin** de VW (de pago, por tiempo), u **ODIS/MirrorDir** si tienes acceso.
   - El nombre del `.frf` corresponde al número de pieza del software.

3. **Descifra y extrae el ODX** con [bri3d/VW_Flash](https://github.com/bri3d/VW_Flash)
   (trae la llave de descifrado FRF):
   ```bash
   python -m frf.decryptfrf  tu_archivo.frf  ./salida
   ```

4. **Extrae el bytecode SA2** del ODX con el script de este repo:
   ```bash
   python auto-diag-suite/scripts/extract_sa2.py ./salida
   ```
   Imprime uno o más candidatos en hex (empiezan por `68`, terminan en `4C`).

5. **Pégalo en la app**: campo **"🧬 Bytecode SA2"** de la pestaña *Mi Auto*.
   A partir de ahí, "Probar cálculo de clave" usa el **algoritmo real** (el
   diagnóstico mostrará *Método de clave: VM SA2*).

## Límite honesto: SFD

El SA2 es el algoritmo de la **sesión de programación**. En T-Cross recientes con
**SFD** activo, la *escritura* además exige un **token online de VW** (ODIS +
GeKo/cuenta) que ni VCDS evita. El SA2 por sí solo no salta SFD. La **lectura**
siempre funciona. Si tu módulo es **pre-SFD**, el SA2 + login basta para codificar.

## Referencias

- [bri3d/sa2_seed_key](https://github.com/bri3d/sa2_seed_key) — VM SA2 (Python)
- [jpbede/sa2go](https://github.com/jpbede/sa2go) — VM SA2 (Go), fuente de los vectores de test
- [bri3d/VW_Flash](https://github.com/bri3d/VW_Flash) — extractor FRF y descifrado
