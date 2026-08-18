# The Godfather of EVO

Clean-room Android setup engineer for Assetto Corsa EVO.

The release contract is intentionally strict:

- two complete LIVE rounds across every configured provider;
- exact vehicle, exact layout, exact game version and decoded vehicle identity;
- SELF CALC uses only verified per-car limits, a verified exact-layout profile,
  independently derived engineering rules and a verified binary carrier;
- no AC1 setup values, donor vehicles, donor tracks, medians, nearest-match
  substitutions or hidden numeric fallbacks;
- every exported `.carsetup` must pass patch → decode → vehicle identity →
  range → plausibility checks.

The app is released under AGPL-3.0. Third-party provenance is recorded in
`NOTICE.md`.

## What the app does

- Shows the exact official vehicle name and an independently mapped official
  300 × 169 thumbnail for every one of the 71 catalog vehicles.
- Searches RacePlace and SetupsMarket in exactly two complete rounds. A
  candidate is accepted only after version, vehicle, layout, binary signature
  and file-structure verification.
- Applies five purpose-built driving modes and understood natural-language
  feedback (for example, a nervous or sluggish rear axle) only through the
  selected car's verified range and step definitions.
- Recommends `FAST CONTROL` as the quick, stable all-round setup. `FAST ATTACK`
  is the hotlap profile; for the Ford Mustang GT3 it enforces TC `1` on every
  exact layout. `FAST STABLE`, `FAST SAFE` and `FAST LONG RUN` add progressively
  more rear-axle, braking or stint reserve.
- If no exact setup exists, it automatically resolves a verified same-car
  structure from the release bundle, the LIVE results or an integrity-checked
  per-car cache. A user-selected file is only an optional override and, after
  verification, is remembered for that car across every layout. Carrier
  numbers are never model inputs; SELF CALC rewrites every verified field that
  the real same-car protobuf actually serializes, leaves absent fixed controls
  alone and labels the output
  `ENGINEERING MODEL`.
- Refuses export if an identity, range, exact layout, writable binary mapping
  or round-trip proof is missing. A refusal is a safety result, not a fallback.

The complete source/data audit is reproducible with:

```bash
python3 scripts/verify_source.py
```

The non-negotiable rebuild prompt and acceptance checklist are recorded in
[`docs/QUALITY_CONTRACT.md`](docs/QUALITY_CONTRACT.md).
