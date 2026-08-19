#!/usr/bin/env bash
set -euo pipefail

keystore="${RELEASE_KEYSTORE_PATH:?RELEASE_KEYSTORE_PATH missing}"
alias_name="${RELEASE_KEY_ALIAS:-acevosetup}"
expected="fc1f41829114f92d64de6f11f69927aecb97e701250146472a9bc77d86ed832f"
actual="$(keytool -list -v -keystore "$keystore" -alias "$alias_name" \
  -storepass "${RELEASE_STORE_PASSWORD:?}" 2>/dev/null \
  | awk -F': ' '/SHA256:/{print $2; exit}' | tr -d ':' | tr '[:upper:]' '[:lower:]')"
[[ "$alias_name" == "acevosetup" ]]
[[ "$actual" == "$expected" ]]
echo "Certificate SHA-256 verified: $actual"
