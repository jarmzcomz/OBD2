#!/usr/bin/env bash
# Empaqueta Auto-Diag Suite como binario standalone con PyInstaller
# Incluye la carpeta web/ como datos estáticos
# Crea dist/autodiag-suite (Linux/macOS) o dist/autodiag-suite.exe (Windows via Wine/CI)
set -e
pip install pyinstaller
pyinstaller --onefile \
  --name autodiag-suite \
  --add-data "web:web" \
  --add-data "obdcore:obdcore" \
  --hidden-import uvicorn.logging \
  --hidden-import uvicorn.loops \
  --hidden-import uvicorn.loops.auto \
  --hidden-import uvicorn.protocols \
  --hidden-import uvicorn.protocols.http \
  --hidden-import uvicorn.protocols.http.auto \
  --hidden-import uvicorn.lifespan \
  --hidden-import uvicorn.lifespan.on \
  scripts/launcher.py
echo "Binario en dist/autodiag-suite"
