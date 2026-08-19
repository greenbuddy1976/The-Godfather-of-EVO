# Protected verified writer contract

Release CI expects a protected writer AAR at `app/libs/verified-writers.aar` and a
separately pinned inspector AAR at `app/libs/verified-inspector.aar`. Their
fully-qualified classes are configured in `VERIFIED_WRITER_PROVIDER_CLASS` and
`VERIFIED_BINARY_INSPECTOR_CLASS`. Both classes must have public no-argument
constructors and implement, respectively:

`com.greenbuddy.acevosetupengineer.engine.VerifiedWriterProvider`

`com.greenbuddy.acevosetupengineer.verification.VerifiedBinaryInspector`

The module owns the version-bound binary schema, legal field definitions, exact
engineering profiles, live-source adapters and roundtrip decoder. It must not contain
donor setups, emergency files, renamed text, guessed constants, or cross-car/near-track
fallback logic.

The inspector must be obtained from a different artifact with a different pinned hash
and identity from the engineering writer. The application compares every
displayed/written value with this fresh decode and independently hashes the bytes. A
writer-supplied success flag or display-only TC value is not sufficient. Separate
artifacts are still not proof of independent authorship, so retained external schema
and game-load evidence remains mandatory.

The instrumented release matrix invokes the writer and inspector for all 12,425 base combinations,
all 24 fine-tuning variants, the required BMW identities, and Mustang FAST ATTACK on
every layout. Any missing coverage, empty binary, invalid verification flag or TC value
fails the build.

Every fully verified report must also reference retained evidence that the exact
version-bound output structure was accepted by AC EVO. Automated Android tests must not
describe this as a driving test.

Both AARs are intentionally absent from the public repository. Release CI downloads
them from protected HTTPS addresses stored in secrets and requires distinct pinned
SHA-256 values before compilation. This avoids GitHub's 48 KB secret-size limit while
keeping the binaries and access addresses out of the public tree. They are packaged
only for the protected release checks and checked again after installation.
