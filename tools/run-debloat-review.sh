#!/usr/bin/env bash
# Fresh biomic world + visible client using the newest build/libs/biomic-*-dev.jar. See tools/run_debloat_review.py.
set -euo pipefail
exec python3 "$(dirname "$0")/run_debloat_review.py" "$@"
