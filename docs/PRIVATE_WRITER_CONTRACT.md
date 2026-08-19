# Protected verified writer contract

Release CI expects a protected Android AAR at `app/libs/verified-writers.aar` and a
fully-qualified provider class in `VERIFIED_WRITER_PROVIDER_CLASS`. The class must have
a public no-argument constructor and implement:

`com.greenbuddy.acevosetupengineer.engine.VerifiedWriterProvider`

The module owns the version-bound binary schema, legal field definitions, exact
engineering profiles, live-source adapters and roundtrip decoder. It must not contain
donor setups, emergency files, renamed text, guessed constants, or cross-car/near-track
fallback logic.

The instrumented release matrix invokes the provider for all 12,425 base combinations,
all 24 fine-tuning variants, the required BMW identities, and Mustang FAST ATTACK on
every layout. Any missing coverage, empty binary, invalid verification flag or TC value
fails the build.

The AAR is intentionally absent from the public repository. It is injected from a
protected CI secret, packaged in the release APK, and checked again after installation.
