#!/usr/bin/env python3
"""
Extrae el bytecode SA2 (algoritmo seed-key de VAG) desde un archivo ODX ya
descifrado, para pegarlo en el campo "🧬 Bytecode SA2" de la app AutoDiag.

CONTEXTO
--------
El MQB calcula la clave del acceso de seguridad (UDS 0x27) ejecutando un pequeño
programa (bytecode SA2) sobre la semilla que entrega el ECU. Ese bytecode es ÚNICO
por módulo y por versión de software, y vive dentro del contenedor de flasheo de VW.

FLUJO COMPLETO (de FRF a la app)
--------------------------------
1. Lee en la app, módulo 09: "Leer módulo confort" → anota el número de pieza
   (DID F187) y la versión de software (DID F189). Eso identifica el FRF exacto.
2. Consigue el .frf de ESA pieza/versión (portal erWin de VW, u ODIS/MirrorDir).
3. Descífralo y saca el ODX con el extractor de bri3d/VW_Flash:
       python -m frf.decryptfrf  tu_archivo.frf  ./salida
   (VW_Flash trae la llave de descifrado; este script NO la necesita.)
4. Pasa el/los ODX resultantes por ESTE script:
       python extract_sa2.py ./salida
5. Copia el bytecode hex que imprime y pégalo en la app.

NOTA: el SA2 sigue siendo el algoritmo de la SESIÓN DE PROGRAMACIÓN. Si tu módulo
tiene SFD activo (T-Cross recientes), la ESCRITURA además exige un token online de
VW que ni VCDS evita; el SA2 por sí solo no lo salta. La LECTURA siempre funciona.
"""
import re
import sys
from pathlib import Path

# Opcodes válidos de la VM SA2 (ver Sa2SeedKey.kt). Un bytecode real empieza casi
# siempre por 0x68 (for) y termina en 0x4C (finish), usando solo estos bytes de
# control mezclados con operandos arbitrarios.
SA2_OPCODES = {0x81, 0x82, 0x93, 0x84, 0x87, 0x68, 0x49, 0x4A, 0x6B, 0x4C}


def _looks_like_sa2(hexstr: str) -> bool:
    """Heurística: ¿esta cadena hex parece un bytecode SA2 plausible?"""
    raw = bytes.fromhex(hexstr)
    if len(raw) < 4 or len(raw) > 256:
        return False
    # Debe terminar en finish (0x4C) y empezar por un opcode de control conocido.
    return raw[0] in SA2_OPCODES and raw[-1] == 0x4C


def from_xml(path: Path):
    """Busca SA2 en la estructura del ODX: SECURITY-METHOD = SA2 + el blob hex
    hermano (FW-SIGNATURE / VALIDITY-FOR según el ODX). Devuelve lista de hex."""
    found = []
    try:
        text = path.read_text(errors="ignore")
    except Exception:
        return found

    # 1) Estructurado: localiza bloques <SECURITY>...</SECURITY> con método SA2.
    for sec in re.findall(r"<SECURITY\b.*?</SECURITY>", text, re.DOTALL | re.IGNORECASE):
        if "SA2" not in sec.upper():
            continue
        blob_re = r"<(?:FW-SIGNATURE|VALIDITY-FOR|SECURITY-METHOD)[^>]*>([^<]+)<"
        for blob in re.findall(blob_re, sec, re.IGNORECASE):
            h = re.sub(r"[^0-9A-Fa-f]", "", blob)
            if len(h) % 2 == 0 and len(h) >= 8 and _looks_like_sa2(h):
                found.append(h.upper())

    # 2) Fallback: cualquier blob hex largo del archivo que pase la heurística SA2.
    if not found:
        for blob in re.findall(r"[0-9A-Fa-f]{8,512}", text):
            if len(blob) % 2 == 0 and _looks_like_sa2(blob):
                found.append(blob.upper())

    return found


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        print("Uso: python extract_sa2.py <archivo.odx | carpeta>")
        sys.exit(1)

    target = Path(sys.argv[1])
    files = []
    if target.is_dir():
        for ext in ("*.odx", "*.odx-f", "*.xml", "*.odx-c"):
            files += list(target.rglob(ext))
    else:
        files = [target]

    if not files:
        print("No se encontraron ODX/XML en la ruta dada.")
        sys.exit(2)

    seen = set()
    hits = 0
    for f in files:
        for hexcode in from_xml(f):
            if hexcode in seen:
                continue
            seen.add(hexcode)
            hits += 1
            spaced = " ".join(hexcode[i:i + 2] for i in range(0, len(hexcode), 2))
            print(f"\n=== Candidato SA2 en {f.name} ({len(hexcode)//2} bytes) ===")
            print(spaced)

    if hits == 0:
        print("\nNo se halló un bytecode SA2 plausible. Verifica que el ODX esté ")
        print("DESCIFRADO (pásalo antes por frf.decryptfrf de VW_Flash).")
    else:
        print(f"\n✓ {hits} candidato(s). Pega el correcto en el campo 'Bytecode SA2' de la app.")
        print("  Si hay varios, prueba el que empiece por 68 (for) y termine en 4C (finish).")


if __name__ == "__main__":
    main()
