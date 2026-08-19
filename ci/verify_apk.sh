#!/usr/bin/env bash
set -euo pipefail

apk="${1:?APK path required}"
report="${2:-verification-reports/apksigner.txt}"
mkdir -p "$(dirname "$report")"
zipalign -c -P 16 4 "$apk" | tee verification-reports/zipalign.txt
apksigner verify --verbose --print-certs "$apk" | tee "$report"
grep -q 'Verified using v2 scheme (APK Signature Scheme v2): true' "$report"
grep -q 'Verified using v3 scheme (APK Signature Scheme v3): true' "$report"
expected="fc1f41829114f92d64de6f11f69927aecb97e701250146472a9bc77d86ed832f"
actual="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$report" \
  | head -n 1 | tr -d ':' | tr '[:upper:]' '[:lower:]')"
[[ "$actual" == "$expected" ]]
if unzip -l "$apk" | grep -Ei 'backup|emergency|fallback|\.carsetup$'; then
  echo "Forbidden setup/fallback payload packaged in APK"
  exit 1
fi
sha256sum "$apk" | tee verification-reports/apk-sha256.txt
