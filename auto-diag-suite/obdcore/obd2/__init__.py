"""Módulo OBD-II: PIDs, DTCs y cliente de lectura."""

from .client import Obd2Client
from .pids import PIDS, PIDS_BY_NAME

__all__ = ["Obd2Client", "PIDS", "PIDS_BY_NAME"]
