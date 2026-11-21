#!/usr/bin/env bash
# run.sh: start the backend and frontend together, wait until both are ready, then open the
# chat in Google Chrome. Press Ctrl+C to stop both servers.
#
# Usage:
#   ./run.sh
#
# Optional environment variables:
#   BACKEND_PORT   Backend port. Default: first free port from 8080 up.
#   FRONTEND_PORT  Frontend port. Default: first free port from 5173 up.
#   NO_OPEN=1      Start both servers but do not open Chrome.
#
# Needs: java, mvn, node, npm, curl, and a .env file at the repository root with GOOGLE_API_KEY
# (copy .env.example). The key is read by the backend and never printed here.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT/run-logs"
READY_TIMEOUT=180
PIDS=()

log() { printf '[run] %s\n' "$*"; }
die() { printf '[run] ERROR: %s\n' "$*" >&2; exit 1; }

# True when something is already listening on localhost:$1.
port_in_use() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }

# First free port at or above $1.
first_free_port() {
  local port=$1
  while port_in_use "$port"; do
    port=$((port + 1))
    [ "$port" -le 65535 ] || die "no free port found from $1 upward"
  done
  echo "$port"
}

cleanup() {
  trap - EXIT INT TERM
  if [ "${#PIDS[@]}" -gt 0 ]; then
    log "stopping servers"
    for pid in "${PIDS[@]}"; do
      # Each server runs in its own process group (see set -m), so this also stops its children.
      kill -- "-$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
    done
    wait 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

# Waits until $2 answers HTTP 200. Fails if the process $3 exits first or the timeout passes.
wait_ready() {
  local name=$1 url=$2 pid=$3 logfile=$4
  local deadline=$((SECONDS + READY_TIMEOUT))
  log "waiting for $name at $url"
  until curl -fsS -o /dev/null "$url" 2>/dev/null; do
    if ! kill -0 "$pid" 2>/dev/null; then
      tail -n 30 "$logfile" >&2 || true
      die "$name stopped during startup. Full log: $logfile"
    fi
    if [ "$SECONDS" -ge "$deadline" ]; then
      tail -n 30 "$logfile" >&2 || true
      die "$name was not ready after ${READY_TIMEOUT}s. Full log: $logfile"
    fi
    sleep 1
  done
  log "$name is ready"
}

open_chrome() {
  local url=$1
  case "$(uname -s)" in
    Darwin)
      open -a "Google Chrome" "$url"
      ;;
    Linux)
      local chrome=""
      for candidate in google-chrome google-chrome-stable; do
        if command -v "$candidate" >/dev/null 2>&1; then
          chrome=$candidate
          break
        fi
      done
      [ -n "$chrome" ] || { log "Google Chrome not found. Open $url yourself."; return 1; }
      # Detached, so Chrome stays open after this script stops.
      nohup "$chrome" "$url" >/dev/null 2>&1 &
      ;;
    *)
      log "unsupported OS. Open $url yourself."
      return 1
      ;;
  esac
  log "opened $url in Google Chrome"
}

# ---- Preflight -------------------------------------------------------------------------------
for tool in java mvn node npm curl; do
  command -v "$tool" >/dev/null 2>&1 || die "'$tool' is not installed or not on PATH"
done

[ -f "$ROOT/.env" ] || die "no .env at the repository root. Copy .env.example to .env and set GOOGLE_API_KEY."
key_line="$(grep -E '^GOOGLE_API_KEY=' "$ROOT/.env" | tail -n 1 || true)"
key_value="${key_line#GOOGLE_API_KEY=}"
if [ -z "$key_value" ] || [ "$key_value" = "replace-me" ]; then
  die "GOOGLE_API_KEY is not set in .env"
fi

if [ ! -d "$ROOT/frontend/node_modules" ]; then
  log "installing frontend dependencies (first run)"
  (cd "$ROOT/frontend" && npm install)
fi

# ---- Ports -----------------------------------------------------------------------------------
BACKEND_PORT="${BACKEND_PORT:-$(first_free_port 8080)}"
FRONTEND_PORT="${FRONTEND_PORT:-$(first_free_port 5173)}"
if port_in_use "$BACKEND_PORT"; then die "backend port $BACKEND_PORT is already in use"; fi
if port_in_use "$FRONTEND_PORT"; then die "frontend port $FRONTEND_PORT is already in use"; fi

BACKEND_URL="http://localhost:$BACKEND_PORT"
FRONTEND_URL="http://localhost:$FRONTEND_PORT"
mkdir -p "$LOG_DIR"

# ---- Start both servers concurrently ----------------------------------------------------------
# Job control gives each server its own process group, so cleanup can stop the whole tree.
set -m

log "starting backend on $BACKEND_URL (log: run-logs/backend.log)"
(
  cd "$ROOT/backend"
  exec mvn spring-boot:run \
    -Dspring-boot.run.arguments="--server.port=$BACKEND_PORT --caramel.cors.allowed-origin=$FRONTEND_URL"
) >"$LOG_DIR/backend.log" 2>&1 &
PIDS+=("$!")
BACKEND_PID=$!

log "starting frontend on $FRONTEND_URL (log: run-logs/frontend.log)"
(
  cd "$ROOT/frontend"
  exec env VITE_API_BASE_URL="$BACKEND_URL" npm run dev -- --port "$FRONTEND_PORT" --strictPort
) >"$LOG_DIR/frontend.log" 2>&1 &
PIDS+=("$!")
FRONTEND_PID=$!

# ---- Wait until both are ready, then open the browser -----------------------------------------
wait_ready "frontend" "$FRONTEND_URL" "$FRONTEND_PID" "$LOG_DIR/frontend.log"
wait_ready "backend" "$BACKEND_URL/actuator/health" "$BACKEND_PID" "$LOG_DIR/backend.log"

if [ "${NO_OPEN:-0}" = "1" ]; then
  log "NO_OPEN=1: not opening Chrome. App: $FRONTEND_URL"
else
  open_chrome "$FRONTEND_URL" || true
fi

log "running. App: $FRONTEND_URL  API: $BACKEND_URL  Press Ctrl+C to stop."
wait "${PIDS[@]}" || true
log "a server has stopped"
