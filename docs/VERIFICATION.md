# Verification record

## Version and inventory

- App game version: Assetto Corsa EVO `0.8.1`.
- Official update announcement date: 2026-07-22.
- Official inventory manifest: `project/app/src/main/assets/catalog-0.8.1.json`.
- EVO mechanical model/range identities: 68.
- Verified binary vehicle identities: 69.
- Official vehicle/name/thumbnail mappings: 71/71.
- Exact layout identities and engineering profiles: 24/24.
- Explicitly blocked variant ranges: Datsun 240Z Tuned, AE86 Tuned and Supra
  V2 Drift. The public extractor groups variant limit files but deliberately
  publishes the stock entry, so the app does not reuse it as a donor.

## Per-car range source

- Project: `SpeedHQ/RaceIQ`.
- Pinned commit: `0bb86a3d3b6a16f2d534bcbecd9c3d21f26dc91c`.
- Generated range-data commit: `8ff0c88cf504240062204ab4e63c8c3b5e3a6f14`
  (2026-07-25, after the official 0.8.1 release).
- Source blob: `fba7d88327c4e421189c37f44d3bbd23ad1c50ee`.
- Included SHA-256:
  `62b277050cf60544fcb1c3ceb833fbcbd96ba5f36ae86f666ab01f8cad6c791c`.
- Provenance: extracted from
  `content/cars/<ks_model>/data/setup/*.carsetuplimits` in the installed game.

Invalid or incomplete ranges are rejected, not repaired. The current source
contains one missing step (`audi_r8_lms_gt4_evo/rearARB`), one inverted
differential range (`ktm_x_bow_gt4/diffCoast`) and two inverted spring ranges
(`mini_jcs_1990`); all affected vehicle profiles are therefore rejected.

## Exact-layout engineering profiles

- Manifest: `project/app/src/main/assets/track-engineering-profiles-0.8.1.json`.
- Coverage: 24/24 catalog layout IDs; no nearest-track substitution.
- Nineteen rows are deterministically derived from the exact EVO centerline at
  pinned RaceIQ commit `0bb86a3d...`: path length and normalized curvature
  distribution produce speed, braking and traction demand without any setup
  values.
- Five layout-specific rows use exact public circuit facts for Nürburgring
  Sprint, Nürburgring 24h, Nordschleife Touristenfahrten, Oulton Park Fosters
  and Watkins Glen Short/Inner Loop. They are separate exact-layout records,
  not donor-track copies.
- Surface/bump demand remains neutral in model v1 and is not used as an
  evidence-free tuning input.

The manifest fingerprint plus exact layout ID is included in every SELF CALC
audit trail.

## SELF CALC and structure-carrier proof

SELF CALC is a transparent range-derived model. It starts from no setup file,
default, average, median or donor value. For every safely writable parameter it
computes an explicit fraction from the selected vehicle's version-pinned
minimum/maximum/step, the exact layout demands and the selected driving mode.
The hard guard prevents starting on a range end stop. Optional natural-language
fine-tuning is then applied in verified steps.

A user-selected `.carsetup` is accepted only when its decoded signature matches
the selected exact vehicle. It supplies protobuf field placement only. Before
export, all adjustable fields in the authorized profile are replaced. Per-wheel
fields are written absolutely left/right, so stored asymmetry cannot survive as
an input. Regression coverage proves that carriers with different stored tyre
numbers produce the same newly generated tyre bytes.

The current clean-room data authorizes this complete SELF CALC write path for
29 vehicle range identities. Thirty-six further range identities expose an ARB
slider in clicks while the binary may contain per-car N/m stiffness. Because a
complete per-car click-to-stiffness LUT is not publicly available, those models
are refused for SELF CALC instead of guessing. The three malformed profiles
listed above are also refused. This limitation is explicit and fail-closed.

## Vehicle thumbnails

- Manifest: `project/app/src/main/assets/vehicle-thumbnails-0.8.1.json`.
- Source: the official Assetto Corsa WordPress media API at
  `https://assettocorsa.gg/wp-json/wp/v2/media`.
- Vehicle-ID/name mappings: 71/71.
- Live response audit: 71/71 HTTP images decoded as JPEG at exactly 300 × 169
  pixels; maximum response size 18,371 bytes.
- Every mapping records the official media ID, caption, thumbnail URL, source
  endpoint and accessible alternative text. Two captions differ only in
  official typography (narrow no-break space/curly quotation marks).
- Tuned Datsun 240Z, tuned Toyota AE86 and Supra V2 Drift use their own
  separately captioned media records.

The image bytes are not bundled or relicensed. The app downloads only the
allowlisted HTTPS thumbnails from `assettocorsa.gg`, caches successful files,
and otherwise displays a neutral local vehicle pictogram. A generation token
prevents a late response for a previous spinner selection from appearing under
the newly selected vehicle.

## Real `.carsetup` audit

Audit source: public RacePlace `ACE Baseline Setups by DTVR 0.8.1` package,
download endpoint `https://raceplace.racing/download/12287/`.

- Package SHA-256:
  `9b896e930a1330faa453cf7497a8490064214e8de861b663c1b3b1e5803706b2`.
- Files inspected: 42.
- Vehicles: Ferrari 296 GT3, Ford Mustang GT3, Audi R8 LMS GT3 Evo II,
  Porsche 992 GT3 R Rennsport and BMW M4 GT3 EVO.
- Binary structure + decoded vehicle identity: 42/42 passed.
- Decode followed by a no-change patch: 42/42 remained byte-identical.
- Decoded field checks against the matching per-car `carsetuplimits`: 968.
- Out-of-range values: 0.
- Fields unavailable in the corresponding public range profile: 32 and
  deliberately skipped.

Community setup numbers are not bundled and are never used as SELF CALC
anchors. The audit establishes binary field identity, range compatibility and
round-trip behavior only.

## Fine-tuning safety

The codec does not expose damper fields `#2` and `#4` as fast bump/rebound:
public slider-diff verification identifies those as underlying fixed rates.
Adjustable slow bump/rebound are fields `#1` and `#3`.

ARB writing is disabled in range profiles because the setup file may store
stiffness while `carsetuplimits` exposes click values. Engine-map and brake
pressure writing are also excluded until their unit/index mapping is verified.

Every changed export must be re-decoded, match the original vehicle signature,
match requested values, remain within the pinned per-car ranges and leave
unmodified decoded knobs unchanged.

## Automated verification and release

`scripts/verify_source.py` fails closed on inventory counts, duplicate IDs,
thumbnail coverage/hosts, exact layout/profile coverage, all 6,816 vehicle ×
layout × mode combinations, binary/range identity counts and the pinned range
dataset SHA-256.

The GitHub workflow verifies the source ZIP hash, extracts a fresh `project/`,
compares it with the checked-out source, runs Gradle 8.13 without build cache on
JDK 17 / Android 35, executes unit tests and lint, reconstructs the release JKS
from repository secrets, verifies alias `acevosetup`, builds the signed release,
and verifies the APK certificate against public `EXPECTED_CERT_SHA256` before
uploading the APK and reports.
