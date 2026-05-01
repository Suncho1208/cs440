#!/usr/bin/env bash
#$ -N risk-train
#$ -j y
#$ -cwd
#$ -V
#$ -l h_rt=12:00:00
#$ -l mem_per_core=4G
#$ -pe omp 1

set -euo pipefail

cd /projectnb/cs440/students/suncho/cs440

# Load Java runtime on SCC if needed.
module load java/21 || true

# Default arguments are intentionally modest for queue friendliness.
./run.sh \
  --numCycles 250 \
  --numTrainingGames 150 \
  --numEvalGames 40 \
  ${EXTRA_ARGS:-}

# Resume example:
# ./run.sh \
#   --inFile /projectnb/cs440/students/suncho/cs440/models/params10.model \
#   --outOffset 11 \
#   --numCycles 250 \
#   --numTrainingGames 150 \
#   --numEvalGames 40
