# Rebuild prompt and quality contract

This is the prompt that must be followed for every rebuild of **The Godfather
of EVO**. A release is not finished because the UI opens or Gradle produces an
APK. It is finished only when every applicable check below is green.

> Rebuild the Android app as a dependable Assetto Corsa EVO setup engineer for
> the complete version-pinned catalog. Every supported car/layout combination
> must receive five clearly different profiles: FAST CONTROL (recommended,
> fast and predictable), FAST ATTACK (hotlap), FAST STABLE (calm rear and
> curbs), FAST SAFE (maximum reserve) and FAST LONG RUN (consistent and
> tyre-conscious). Ford Mustang GT3 FAST ATTACK must always export TC = 1 on
> every layout. Use only the selected car's verified min/max/step data and the
> exact layout profile. Never insert a donor car, donor track, average, median,
> guessed binary field or hidden fallback. Accept only a `.carsetup` whose
> decoded vehicle identity matches the selected car. Rewrite every verified
> control that this real same-car protobuf serializes; explicitly skip absent
> fixed fields without blocking the rest. Re-decode the result, prove vehicle
> identity, range, step alignment and requested values, then build and
> certificate-check the signed APK in GitHub Actions. If evidence is missing,
> refuse clearly instead of inventing a setup.

## Release acceptance checklist

- Catalog: 71 named vehicles and 24 exact layouts load without duplicate IDs.
- Profiles: exactly five modes exist and each applies at least six explicit
  stability/performance adjustments where the car exposes those controls.
- Recommended behavior: FAST CONTROL is measurably calmer at the rear than FAST
  ATTACK over the complete verified demand matrix.
- Mustang rule: Ford Mustang GT3 + FAST ATTACK exports TC `1` for all 24 layouts
  and the post-patch decoder reads `1` back.
- Range safety: every generated number is finite, inside the selected car's
  version-pinned range and aligned to its verified step.
- Binary safety: same-car signature before and after patch is identical; unknown
  bytes remain untouched; absent fixed controls do not invalidate present
  suspension, tyre, brake or balance controls.
- Carrier reliability: release carrier, LIVE carrier and persistent per-car
  cache are tried automatically. A manually verified carrier is remembered for
  every later layout of that car.
- Matrix: all 8,520 catalog routes are classified; 8,160 range-backed routes are
  numerically checked and the 360 routes without variant-specific evidence fail
  closed.
- Build: clean JDK 17 / Gradle 8.13 / Android 35 tests and lint pass from the
  checked source ZIP; the signed release APK matches the expected certificate.

Automated proof is necessary but does not pretend to be physical track testing.
Community laps remain the final validation of feel and lap-time performance;
their feedback must be used as versioned, car-specific evidence in a later
calibration rather than as an untracked global tweak.
