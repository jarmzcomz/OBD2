"""Tabla orientativa de funciones de coding conocidas por la comunidad (T-Cross / MQB-A0).

==================================  DISCLAIMER  ==================================
Estos byte/bit son ORIENTATIVOS, recopilados de la comunidad (VCDS/OBDeleven). NO
son una verdad universal: varían según versión de firmware, equipamiento y revisión
del módulo. DEBEN verificarse leyendo el LONG CODING REAL del vehículo antes de
escribir nada. Toda escritura pasa por el gate Zero-Brick (backup obligatorio).

Este módulo es CODING/ADAPTACIÓN legítima del propietario. No incluye Immo Off,
manipulación de odómetro ni bypass de seguridad.
=================================================================================
"""

from __future__ import annotations

_VERIFY = "Byte/bit aproximado de la comunidad - VERIFICAR con el long coding real del vehículo"

# {modulo: {nombre_funcion: {byte, bit, descripcion, valores, note}}}
KNOWN_CODINGS: dict[str, dict[str, dict]] = {
    "0x09": {  # BCM - Central Electrics (luces, intermitentes)
        "drl_disable": {
            "byte": 0, "bit": 0,
            "desc": "Apagar luces diurnas (DRL)",
            "values": {0: "DRL activas", 1: "DRL apagadas"},
            "note": _VERIFY,
        },
        "coming_home": {
            "byte": 1, "bit": 2,
            "desc": "Activar función Coming Home",
            "values": {0: "desactivado", 1: "activado"},
            "note": _VERIFY,
        },
        "leaving_home": {
            "byte": 1, "bit": 3,
            "desc": "Activar función Leaving Home",
            "values": {0: "desactivado", 1: "activado"},
            "note": _VERIFY,
        },
        "comfort_turn_signals": {
            "byte": 2, "bit": 0,
            "desc": "Número de parpadeos del intermitente confort (one-touch)",
            "values": {0: "off", 3: "3 parpadeos", 5: "5 parpadeos"},
            "note": _VERIFY + " (este campo suele ser un valor, no un solo bit)",
        },
    },
    "0x17": {  # Instrument Cluster
        "needle_sweep": {
            "byte": 0, "bit": 0,
            "desc": "Barrido de agujas (needle sweep) al arrancar",
            "values": {0: "desactivado", 1: "activado"},
            "note": _VERIFY,
        },
        "digital_speed": {
            "byte": 0, "bit": 4,
            "desc": "Mostrar velocidad digital",
            "values": {0: "desactivado", 1: "activado"},
            "note": _VERIFY,
        },
        "staging_menu": {
            "byte": 3, "bit": 1,
            "desc": "Habilitar menú oculto (staging) del cluster",
            "values": {0: "oculto", 1: "visible"},
            "note": _VERIFY,
        },
    },
    "0x46": {  # Comfort System (Komfort)
        "auto_lock": {
            "byte": 0, "bit": 1,
            "desc": "Bloqueo automático de puertas en marcha (auto-lock)",
            "values": {0: "desactivado", 1: "activado"},
            "note": _VERIFY,
        },
        "mirror_fold": {
            "byte": 1, "bit": 0,
            "desc": "Plegado de espejos al cerrar el coche",
            "values": {0: "desactivado", 1: "activado"},
            "note": _VERIFY,
        },
        "window_comfort": {
            "byte": 2, "bit": 2,
            "desc": "Apertura/cierre de ventanas con el mando (comfort windows)",
            "values": {0: "desactivado", 1: "activado"},
            "note": _VERIFY,
        },
    },
}
