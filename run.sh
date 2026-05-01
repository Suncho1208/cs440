#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./run.sh [sequential-train-args...]
#
# SequentialTrain requires positional agent args first (see course Piazza examples):
#   <agentToTrain> <opponent1> [opponent2 ...]  then optional flags.
# Default here: train RiskQAgent vs two RandomAgents (3-player).
# Override by exporting SEQUENTIAL_TRAIN_AGENTS="pas.risk.agent.RiskQAgent static random"
# (space-separated fully-qualified class names or built-ins like random/static).
#
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

IFS=' ' read -r -a AGENTS <<< "${SEQUENTIAL_TRAIN_AGENTS:-pas.risk.agent.RiskQAgent random random}"

java -cp "./lib/*:./src:." edu.bu.pas.risk.SequentialTrain \
  "${AGENTS[@]}" \
  --outFile "${OUT_PREFIX}" \
  "$@" | tee "${LOG_FILE}"
