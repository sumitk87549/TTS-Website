#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Start the Voisetu TTS FastAPI service (Supertonic-3 engine)
#
# Usage:
#   ./start-tts-service.sh          # default port 8000
#   ./start-tts-service.sh 8001     # custom port
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
VENV="${PROJECT_ROOT}/supertonic-env"
PORT="${1:-8000}"

if [[ ! -d "${VENV}" ]]; then
  echo "❌  supertonic-env not found at ${VENV}"
  echo "    Create it with: python3 -m venv supertonic-env && supertonic-env/bin/pip install supertonic fastapi uvicorn soundfile"
  exit 1
fi

# ── Auto-clear the port if already in use ────────────────────────────────────
if command -v fuser &>/dev/null && fuser "${PORT}/tcp" &>/dev/null; then
  echo "⚠️   Port ${PORT} is already in use — freeing port..."
  fuser -k -9 "${PORT}/tcp" &>/dev/null || true
  while fuser "${PORT}/tcp" &>/dev/null 2>&1; do
    sleep 0.2
  done
fi

echo "🚀  Starting Voisetu TTS service on http://127.0.0.1:${PORT}"
echo "    Model: supertonic-3 (cached at ~/.cache/supertonic3)"
echo "    Press Ctrl+C to stop."
echo ""

cd "${SCRIPT_DIR}"
exec "${VENV}/bin/uvicorn" main:app \
  --host 127.0.0.1 \
  --port "${PORT}" \
  --log-level info
