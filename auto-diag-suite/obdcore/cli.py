"""CLI mínima: leer PIDs y DTCs desde la terminal."""

from __future__ import annotations

import argparse

from .obd2 import Obd2Client
from .transport import make_transport


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="autodiag", description="Diagnóstico OBD-II")
    parser.add_argument("--transport", default=None, help="sim | elm327 | socketcan")
    sub = parser.add_subparsers(dest="cmd", required=True)
    sub.add_parser("pids", help="Leer todos los PIDs soportados")
    sub.add_parser("dtc", help="Leer DTCs almacenados")
    args = parser.parse_args(argv)

    transport = make_transport(args.transport)
    with transport:
        client = Obd2Client(transport)
        if args.cmd == "pids":
            for name, value in client.read_all_pids().items():
                print(f"{name:>14}: {value}")
        elif args.cmd == "dtc":
            codes = client.read_dtcs()
            print("DTCs:", ", ".join(codes) if codes else "ninguno")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
