#!/usr/bin/env bash
set -euo pipefail

keystore="${RELEASE_KEYSTORE_PATH:?RELEASE_KEYSTORE_PATH missing}"
alias_name="${RELEASE_KEY_ALIAS:-acevosetup}"
expected="fc1f41829114f92d64de6f11f69927aecb97e701250146472a9bc77d86ed832f"
actual="$(keytool -exportcert -keystore "$keystore" -alias "$alias_name" \
  -storepass:env RELEASE_STORE_PASSWORD 2>/dev/null \
  | sha256sum | awk '{print $1}')"
[[ "$alias_name" == "acevosetup" ]]
[[ "$actual" == "$expected" ]]
mkdir -p verification-reports
printf '%s\n' "Certificate SHA-256 verified: $actual" \
  | tee verification-reports/certificate-sha256.txt
