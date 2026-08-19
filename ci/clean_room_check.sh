#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"
[[ ! -e "$root/cleanroom" ]]
[[ ! -e "$root/freshapp" ]]
[[ -f "$root/settings.gradle" ]]
[[ -f "$root/app/build.gradle" ]]
forbidden="$(find "$root" -type f \( -iname '*.apk' -o -iname '*.aab' -o -iname '*.carsetup' \
  -o -iname '*.jks' -o -iname '*.keystore' -o -iname '*.p12' -o -iname '*.zip' \
  -o -iname '*.aar' -o -iname '*.jar' -o -iname '*.bin' \) \
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

if grep -RInE 'Color\.RED|android:color="@android:color/holo_red|@color/red' \
  "$root/app/src/main"; then
  echo "Red active UI color reference found"
  exit 1
fi

while IFS= read -r color; do
  case "${color^^}" in
    '#000000'|'#080808'|'#171717'|'#383838'|'#FFD600'|'#E6C100'|'#FFFFFF'|'#C7C7C7'|'#555555') ;;
    *) echo "Unapproved UI color found: $color"; exit 1 ;;
  esac
done < <(grep -RhoE '#[[:xdigit:]]{6}([[:xdigit:]]{2})?' "$root/app/src/main/res" | sort -u)

echo "Clean-room and UI color checks passed"
