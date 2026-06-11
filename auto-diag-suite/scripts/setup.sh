#!/usr/bin/env bash
# =============================================================================
# Auto-Diag Suite — Script de configuración multiplataforma
# =============================================================================
# Detecta el sistema operativo, muestra instrucciones específicas de hardware
# y configura un entorno Python listo para usar.
# =============================================================================

set -euo pipefail

VENV_DIR=".venv"
PYTHON_MIN="3.10"

# ── Colores ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }
header()  { echo -e "\n${BOLD}$*${NC}"; }

# ── Detección del OS ──────────────────────────────────────────────────────────
detect_os() {
    case "$(uname -s)" in
        Linux*)  echo "linux" ;;
        Darwin*) echo "macos" ;;
        CYGWIN*|MINGW*|MSYS*) echo "windows" ;;
        *)       echo "unknown" ;;
    esac
}

OS=$(detect_os)

# ── Instrucciones de hardware por OS ─────────────────────────────────────────
print_hardware_instructions() {
    header "=== Instrucciones de hardware para $OS ==="

    case "$OS" in
        linux)
            info "Instalando dependencias del sistema..."
            if command -v apt-get &>/dev/null; then
                sudo apt-get update -qq
                sudo apt-get install -y python3-venv python3-pip
                info "Para SocketCAN (adaptadores CAN profesionales):"
                echo "  sudo apt-get install -y can-utils"
                echo "  sudo modprobe can can_raw vcan"
                echo "  sudo ip link add dev vcan0 type vcan"
                echo "  sudo ip link set up vcan0"
                echo "  # Para hardware CAN real (ej. PEAK PCAN-USB):"
                echo "  sudo ip link set can0 up type can bitrate 500000"
            elif command -v dnf &>/dev/null; then
                sudo dnf install -y python3 python3-pip
            elif command -v pacman &>/dev/null; then
                sudo pacman -S --noconfirm python python-pip
            fi

            echo ""
            info "Adaptadores ELM327 en Linux:"
            echo "  USB:       Aparece como /dev/ttyUSB0 o /dev/ttyACM0"
            echo "             sudo usermod -aG dialout \$USER  # añadir usuario al grupo"
            echo "  Bluetooth: sudo apt-get install rfcomm"
            echo "             sudo rfcomm bind 0 AA:BB:CC:DD:EE:FF  # MAC de tu OBD2"
            echo "             -> Aparece como /dev/rfcomm0"
            echo ""
            info "Variable de entorno para ELM327:"
            echo "  export ADS_TRANSPORT=elm327"
            echo "  export ADS_SERIAL_PORT=/dev/ttyUSB0   # ajusta a tu puerto"
            ;;

        macos)
            info "Instrucciones para macOS:"
            if ! command -v brew &>/dev/null; then
                warn "Homebrew no encontrado. Instálalo primero:"
                echo "  /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
            else
                info "Instalando Python 3.12 via Homebrew..."
                brew install python@3.12 || warn "Python ya instalado o error en brew"
            fi

            echo ""
            info "Adaptadores ELM327 en macOS:"
            echo "  USB:       Aparece como /dev/cu.usbserial-* o /dev/cu.SLAB_USBtoUART"
            echo "  Bluetooth: Empareja el adaptador en Preferencias del Sistema -> Bluetooth"
            echo "             Luego busca: ls /dev/cu.* (suele ser /dev/cu.OBDII o /dev/cu.OBDLink)"
            echo ""
            warn "SocketCAN NO está disponible en macOS de forma nativa."
            echo "  Para desarrollo CAN usa una VM Linux o adaptador ELM327."
            echo ""
            info "Variable de entorno para ELM327:"
            echo "  export ADS_TRANSPORT=elm327"
            echo "  export ADS_SERIAL_PORT=/dev/cu.usbserial-0001   # ajusta a tu puerto"
            ;;

        windows)
            warn "Windows detectado (probable WSL2 o Git Bash)."
            echo ""
            info "Opciones en Windows:"
            echo ""
            echo "  Opción A — WSL2 (recomendado para SocketCAN):"
            echo "    1. Instala WSL2: wsl --install"
            echo "    2. Sigue las instrucciones de Linux dentro de WSL2"
            echo "    3. Para pasar el USB a WSL2: instala usbipd-win"
            echo "       winget install usbipd; usbipd wsl attach --busid <ID>"
            echo ""
            echo "  Opción B — Python nativo para ELM327 USB/BT:"
            echo "    1. Instala Python 3.12 desde python.org"
            echo "    2. El adaptador ELM327 aparece como COM3, COM4, etc."
            echo "    3. Configura: set ADS_TRANSPORT=elm327"
            echo "                  set ADS_SERIAL_PORT=COM3"
            echo ""
            warn "Para continuar este script usa WSL2 con Ubuntu."
            ;;

        *)
            warn "OS no reconocido: $(uname -s). Continuando de forma genérica..."
            ;;
    esac
}

# ── Verificar Python ──────────────────────────────────────────────────────────
check_python() {
    header "Verificando Python..."
    PYTHON=""
    for cmd in python3.12 python3.11 python3.10 python3 python; do
        if command -v "$cmd" &>/dev/null; then
            VER=$("$cmd" -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')")
            MAJOR=$(echo "$VER" | cut -d. -f1)
            MINOR=$(echo "$VER" | cut -d. -f2)
            if [ "$MAJOR" -ge 3 ] && [ "$MINOR" -ge 10 ]; then
                PYTHON="$cmd"
                success "Usando $cmd (v$VER)"
                break
            fi
        fi
    done

    if [ -z "$PYTHON" ]; then
        error "Python $PYTHON_MIN+ no encontrado. Instálalo primero."
    fi
}

# ── Crear entorno virtual ─────────────────────────────────────────────────────
setup_venv() {
    header "Configurando entorno virtual..."
    if [ ! -d "$VENV_DIR" ]; then
        "$PYTHON" -m venv "$VENV_DIR"
        success "Entorno virtual creado en $VENV_DIR"
    else
        warn "Entorno virtual ya existe en $VENV_DIR — reutilizando"
    fi

    # Activar
    # shellcheck disable=SC1091
    source "$VENV_DIR/bin/activate"
    success "Entorno activado"
}

# ── Instalar paquete ──────────────────────────────────────────────────────────
install_package() {
    header "Instalando auto-diag-suite..."
    pip install --upgrade pip -q
    pip install -e ".[dev]"
    success "Instalación completada"
}

# ── Ejecutar tests ────────────────────────────────────────────────────────────
run_tests() {
    header "Ejecutando tests (modo simulado)..."
    ADS_TRANSPORT=sim python -m pytest -q
    success "Todos los tests pasaron"
}

# ── Resumen final ─────────────────────────────────────────────────────────────
print_summary() {
    header "=== Configuración completada ==="
    echo ""
    echo "  Activa el entorno:   source $VENV_DIR/bin/activate"
    echo "  Prueba la CLI:       ADS_TRANSPORT=sim autodiag pids"
    echo "  Levanta el server:   ADS_TRANSPORT=sim uvicorn app.main:app --reload"
    echo "  Ejecuta los tests:   make test   (o: ADS_TRANSPORT=sim pytest -q)"
    echo ""
    info "Docs de hardware: docs/hardware.md"
    info "Referencia completa: README.md"
}

# ── Main ──────────────────────────────────────────────────────────────────────
main() {
    echo -e "${BOLD}"
    echo "╔═══════════════════════════════════════════╗"
    echo "║       Auto-Diag Suite — Setup             ║"
    echo "╚═══════════════════════════════════════════╝"
    echo -e "${NC}"

    print_hardware_instructions
    check_python
    setup_venv
    install_package
    run_tests
    print_summary
}

main "$@"
