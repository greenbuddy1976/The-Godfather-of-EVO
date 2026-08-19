# The Godfather of EVO 1.1.1

Clean-room Android/Java source tree for **Assetto Corsa EVO Setup Engineer**.

- Application ID: `com.greenbuddy.acevosetupengineer`
- Version: `1.1.1` (`versionCode 4`)
- Android: API 26 minimum, compile/target API 35, Build Tools 35.0.0
- Toolchain: JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.2
- Design: black/anthracite, yellow controls, white/light-grey text
- Copyright: © Greenbuddy1976

## Truthful release state

This repository deliberately contains **no guessed AC EVO binary writer, donor setup,
backup `.carsetup`, hidden default tuning values, or text export**. A release build is
hard-gated until the separately verified writer module is supplied. Debug builds show
the complete interaction flow but report `NICHT SICHER` instead of manufacturing a
file when verified vehicle data or a writer is missing.

The private verified writer is loaded through the `VerifiedWriterProvider` contract.
It must cover every supported car and layout for game version 0.8.1, perform a complete
decode/encode roundtrip, and report every individual verification flag. See
`docs/PRIVATE_WRITER_CONTRACT.md`.

## Build

Use a clean installation of Gradle 8.13 and JDK 17:

```bash
gradle --no-daemon --no-build-cache --no-configuration-cache clean testDebugUnitTest lintDebug assembleDebug
```

The public tree is intentionally unable to run `assembleRelease`. A signed release
requires the protected writer AAR and release keystore variables documented in the CI
workflow. This is a safety property, not a missing fallback.

## Evidence scope

- The 71 displayed vehicle identities and the track inventory are version-bound and
  carry provenance in source.
- The JVM safety matrix validates that the public tree refuses all 71 × 35 × 5 =
  12,425 requests instead of inventing binaries.
- The release instrumentation matrix validates real binaries from the protected writer.
- Automated verification is never described as a real driving test.
