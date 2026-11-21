#!/usr/bin/env bash
# Fails if the built frontend contains the variable name or anything shaped like a Google API key
# (quickstart V12, constitution Principle I). Run after `npm run build`.
set -euo pipefail
DIST="${1:-dist}"
if [ ! -d "$DIST" ]; then
  echo "check-bundle-secrets: $DIST not found. Run npm run build first." >&2
  exit 2
fi
if grep -rIlE 'GOOGLE_API_KEY|AIza[0-9A-Za-z_-]{35}' "$DIST"; then
  echo "check-bundle-secrets: FAIL. Secret-like content found in the bundle above." >&2
  exit 1
fi
echo "check-bundle-secrets: OK. No secret-like content in $DIST."
