#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./run.sh [sequential-train-args...]
# Example:
#   ./run.sh --numCycles 200 --numTrainingGames 200 --numEvalGames 40

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

PROJECTNB_DIR="${PROJECTNB_DIR:-/projectnb/cs440/students/suncho/cs440}"
mkdir -p "${PROJECTNB_DIR}/logs" "${PROJECTNB_DIR}/models"

STAMP="$(date +%Y%m%d-%H%M%S)"
LOG_FILE="${PROJECTNB_DIR}/logs/train-${STAMP}.log"
OUT_PREFIX="${PROJECTNB_DIR}/models/params"

echo "[run.sh] Compiling Risk sources..."
javac -cp "./lib/*:./src:." -d ./src @risk.srcs

echo "[run.sh] Launching SequentialTrain..."
echo "[run.sh] Log: ${LOG_FILE}"
java -cp "./lib/*:./src:." edu.bu.pas.risk.SequentialTrain \
  --outFile "${OUT_PREFIX}" \
  "$@" | tee "${LOG_FILE}"
