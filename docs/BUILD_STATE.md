# Build state

Snapshot: 2026-08-19, target AC EVO 0.8.1.

Locally verified in the clean workspace:

- 71 unique official vehicle display identities;
- 35 layouts backed by primary-source evidence;
- five setup styles and eight fine-tuning problems with strengths 1–3;
- 12,425 public safety-matrix requests refuse export when no verified writer exists;
- core Java compiles with JDK 17 and `-Xlint:all -Werror`;
- Android resource XML and GitHub workflow YAML parse successfully;
- no `.carsetup`, APK, keystore, donor, emergency or renamed text export is present.

Release remains hard-blocked until protected CI receives all of the following:

1. a legally produced AC EVO 0.8.1 writer AAR and separately sourced inspector AAR
   with verified per-car schema, limits, steps and real game-load evidence for the
   full matrix;
2. approved source adapters for SetupsMarket and RacePlace;
3. the existing release keystore and passwords whose alias and certificate digest match
   the required release identity.

The signing job runs only after a push to `main` or an explicit manual dispatch and is
bound to the `verified-release` GitHub Environment. Configure required reviewers and
store all signing/writer inputs in that protected environment; pull requests run only
the unprivileged quality workflow.

No signed APK or successful driving test is claimed by this snapshot.
