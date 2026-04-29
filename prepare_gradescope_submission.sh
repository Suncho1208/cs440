#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

SUBMISSION_DIR="$ROOT_DIR/gradescope_submission"
ZIP_PATH="$ROOT_DIR/gradescope-risk-submission.zip"

if [[ ! -f "$ROOT_DIR/params.model" ]]; then
  latest_model="$(ls -t "$ROOT_DIR"/params*.model 2>/dev/null | head -n 1 || true)"
  if [[ -z "${latest_model}" ]]; then
    echo "No model checkpoint found. Expected params.model or params*.model."
    exit 1
  fi
  cp "$latest_model" "$ROOT_DIR/params.model"
  echo "Created params.model from: $latest_model"
fi

rm -rf "$SUBMISSION_DIR"
mkdir -p "$SUBMISSION_DIR/src/pas/risk/senses" "$SUBMISSION_DIR/src/pas/risk/rewards" "$SUBMISSION_DIR/src/pas/risk/agent"

cp "$ROOT_DIR/src/pas/risk/senses/MyStateSensorArray.java" "$SUBMISSION_DIR/src/pas/risk/senses/"
cp "$ROOT_DIR/src/pas/risk/senses/MyActionSensorArray.java" "$SUBMISSION_DIR/src/pas/risk/senses/"
cp "$ROOT_DIR/src/pas/risk/senses/MyPlacementSensorArray.java" "$SUBMISSION_DIR/src/pas/risk/senses/"
cp "$ROOT_DIR/src/pas/risk/rewards/MyActionRewardFunction.java" "$SUBMISSION_DIR/src/pas/risk/rewards/"
cp "$ROOT_DIR/src/pas/risk/rewards/MyPlacementRewardFunction.java" "$SUBMISSION_DIR/src/pas/risk/rewards/"
cp "$ROOT_DIR/src/pas/risk/agent/RiskQAgent.java" "$SUBMISSION_DIR/src/pas/risk/agent/"
cp "$ROOT_DIR/params.model" "$SUBMISSION_DIR/"

rm -f "$ZIP_PATH"
(
  cd "$SUBMISSION_DIR"
  zip -r "$ZIP_PATH" src params.model >/dev/null
)

echo "Created: $ZIP_PATH"
echo "Upload the same zip to PA3 EASY, MEDIUM, and HARD."
