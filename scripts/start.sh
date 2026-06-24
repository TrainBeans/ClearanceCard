#!/usr/bin/env bash
# start.sh — run ClearanceCard as a Docker container with persistent H2 storage.
#
# Usage:
#   ./scripts/start.sh                              # use default railroad name
#   APP_RAILROAD_NAME="My Railroad" ./scripts/start.sh
#   IMAGE=ghcr.io/trainbeans/clearancecard:master ./scripts/start.sh
#
# The H2 database files are kept in a Docker named volume called
# 'clearancecard-data' so that cards survive container removal and recreation.
#
# To back up the volume:
#   docker run --rm \
#     -v clearancecard-data:/data \
#     -v "$(pwd)":/backup \
#     busybox tar czf /backup/clearancecard-data-backup.tar.gz /data
#
# To restore a backup:
#   docker run --rm \
#     -v clearancecard-data:/data \
#     -v "$(pwd)":/backup \
#     busybox tar xzf /backup/clearancecard-data-backup.tar.gz -C /
#
# To wipe all data and start fresh:
#   docker volume rm clearancecard-data

set -euo pipefail

CONTAINER_NAME="${CONTAINER_NAME:-clearancecard}"
IMAGE="${IMAGE:-clearancecard:latest}"
PORT="${PORT:-8080}"
VOLUME_NAME="${VOLUME_NAME:-clearancecard-data}"
RAILROAD_NAME="${APP_RAILROAD_NAME:-}"

# ── Build the image from source if no IMAGE override was given ───────────────
if [[ "${IMAGE}" == "clearancecard:latest" ]]; then
    echo "Building image '${IMAGE}' from source…"
    docker build -t "${IMAGE}" "$(dirname "$0")/.."
fi

# ── Remove any stopped container with the same name ─────────────────────────
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Removing existing container '${CONTAINER_NAME}'…"
    docker rm -f "${CONTAINER_NAME}"
fi

# ── Start the container ──────────────────────────────────────────────────────
echo "Starting '${CONTAINER_NAME}' on port ${PORT}…"

ENV_ARGS=()
if [[ -n "${RAILROAD_NAME}" ]]; then
    ENV_ARGS+=(-e "APP_RAILROAD_NAME=${RAILROAD_NAME}")
fi

docker run -d \
    --name "${CONTAINER_NAME}" \
    --restart unless-stopped \
    -p "${PORT}:8080" \
    -v "${VOLUME_NAME}:/app/data" \
    "${ENV_ARGS[@]}" \
    "${IMAGE}"

echo "Container started. Application available at http://localhost:${PORT}"
echo "H2 console at http://localhost:${PORT}/h2-console  (JDBC URL: jdbc:h2:file:./data/clearancecard)"
echo ""
echo "To follow logs:  docker logs -f ${CONTAINER_NAME}"
echo "To stop:         docker stop ${CONTAINER_NAME}"

