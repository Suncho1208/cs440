#!/usr/bin/env bash
# Bundle a trained checkpoint with the current src/ Java sources for Gradescope.
# Usage (on SCC or any machine with bash):
#   ./scripts/export_gradescope.sh models/fresh124.model
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ "${#}" -lt 1 ]]; then
  echo "usage: $0 path/to/your_checkpoint.model" >&2
  echo "example: $0 models/fresh124.model" >&2
  exit 2
fi

SRC_MODEL="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
if [[ ! -f "${SRC_MODEL}" ]]; then
  echo "error: model file not found: ${SRC_MODEL}" >&2
  exit 1
fi

cp -f "${SRC_MODEL}" "${ROOT_DIR}/params.model"
echo "[export_gradescope] Using model: ${SRC_MODEL} ($(wc -c < "${ROOT_DIR}/params.model") bytes)"
"${ROOT_DIR}/prepare_gradescope_submission.sh"
echo "[export_gradescope] Upload: ${ROOT_DIR}/gradescope-risk-submission.zip"
