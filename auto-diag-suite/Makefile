.PHONY: install-dev test lint run run-hardware cli-pids clean

# Instala el proyecto con todas las dependencias de desarrollo
install-dev:
	pip install -e ".[dev]"

# Ejecuta la suite de tests en modo simulado (sin hardware)
test:
	ADS_TRANSPORT=sim python -m pytest -q

# Linting con ruff (errores de estilo + imports)
lint:
	ruff check . --select E,F,W

# Levanta el servidor FastAPI en modo simulado (hot-reload activado)
run:
	ADS_TRANSPORT=sim uvicorn app.main:app --reload --host 127.0.0.1 --port 8000

# Levanta el servidor con hardware ELM327 real (sin hot-reload)
run-hardware:
	ADS_TRANSPORT=elm327 uvicorn app.main:app --host 127.0.0.1 --port 8000

# Consulta PIDs disponibles vía CLI en modo simulado
cli-pids:
	ADS_TRANSPORT=sim autodiag pids

# Elimina artefactos de compilación y base de datos local
clean:
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -name "*.pyc" -delete
	rm -f autodiag.db
