# Verification policy

A result is exportable only if all of the following are true:

1. exact selected vehicle identity matches;
2. exact selected layout identity matches;
3. game version is exactly supported;
4. binary structure is accepted by the version-bound decoder;
5. vehicle signature is correct;
6. every written field is present, in range and aligned to its legal step;
7. Mustang GT3 `FAST ATTACK` has exactly `TC = 1`;
8. unsupported/unknown fields survive unchanged where a source document is modified;
9. decoding the freshly written bytes reproduces every written value;
10. SHA-256 is calculated from the final bytes.

The core computes SHA-256 itself and compares generated values with a fresh decode from
the protected `VerifiedBinaryInspector`. This remains an automated structural check;
actual AC EVO acceptance still requires a separately recorded game-load test before
release.

`LIVE EXACT` is not a quality claim. A community file must still pass all checks.
Network failure is recorded separately from an exact miss. After at most two complete
live rounds, the engineering path is attempted. If that path lacks verified profiles
or writer coverage, the app reports `NICHT SICHER` and does not export anything.

No test report may claim a real driving test unless a human actually drove and recorded
one. Current automated checks are structural and engineering-contract checks only.
