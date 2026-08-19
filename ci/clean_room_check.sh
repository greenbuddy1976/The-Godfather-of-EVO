#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"
[[ ! -e "$root/cleanroom" ]]
[[ ! -e "$root/freshapp" ]]
[[ -f "$root/settings.gradle" ]]
[[ -f "$root/app/build.gradle" ]]
forbidden="$(find "$root" -type f \( -iname '*.apk' -o -iname '*.aab' -o -iname '*.carsetup' \
  -o -iname '*.jks' -o -iname '*.keystore' -o -iname '*.p12' -o -iname '*.zip' \) \
  -not -path '*/build/*' -print)"
if [[ -n "$forbidden" ]]; then
  echo "Forbidden clean-room files found:"
  echo "$forbidden"
  exit 1
fi

if grep -RInE 'text/plain|emergency[ _-]?setup|backup[ _-]?carsetup|donor[ _-]?car|nearest[ _-]?track|similar[ _-]?track' \
  "$root/app/src/main"; then
  echo "Forbidden fallback/text export logic found"
  exit 1
fi

if grep -RInE '#(F{2}0{4}|D32F2F|B71C1C|9C1C1C)' "$root/app/src/main/res"; then
  echo "Red active UI color found"
  exit 1
fi

echo "Clean-room and UI color checks passed"
