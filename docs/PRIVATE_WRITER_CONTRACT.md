# Protected verified writer contract

Release CI expects a protected Android AAR at `app/libs/verified-writers.aar` and a
fully-qualified provider class in `VERIFIED_WRITER_PROVIDER_CLASS`. The class must have
a public no-argument constructor and implement:

`com.greenbuddy.acevosetupengineer.engine.VerifiedWriterProvider`

The module owns the version-bound binary schema, legal field definitions, exact
engineering profiles, live-source adapters and roundtrip decoder. It must not contain
donor setups, emergency files, renamed text, guessed constants, or cross-car/near-track
fallback logic.

`binaryInspector()` must return a decoder/test-oracle implementation separated from the
engineering generation path. The application compares every displayed/written value
with this fresh decode and independently hashes the bytes. A writer-supplied success
flag or display-only TC value is not sufficient.

The instrumented release matrix invokes the writer and inspector for all 12,425 base combinations,
all 24 fine-tuning variants, the required BMW identities, and Mustang FAST ATTACK on
every layout. Any missing coverage, empty binary, invalid verification flag or TC value
fails the build.

Every fully verified report must also reference retained evidence that the exact
version-bound output structure was accepted by AC EVO. Automated Android tests must not
describe this as a driving test.

The AAR is intentionally absent from the public repository. Release CI downloads it
from a protected HTTPS address stored in a secret and requires a separately pinned
SHA-256 before compilation. This avoids GitHub's 48 KB secret-size limit while keeping
the binary and its access address out of the public tree. It is packaged only for the
protected release checks and checked again after installation.
