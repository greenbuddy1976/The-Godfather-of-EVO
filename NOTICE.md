# Third-party notices

## AC EVO per-car setup limits

`project/app/src/main/assets/evo-carsetuplimits-0.8.1.json` is reproduced from
the public RaceIQ repository (`SpeedHQ/RaceIQ`) at commit
`0bb86a3d3b6a16f2d534bcbecd9c3d21f26dc91c`:

- Source file: `shared/games/ac-evo/setup-ranges.json`
- Source blob: `fba7d88327c4e421189c37f44d3bbd23ad1c50ee`
- Included-file SHA-256: `62b277050cf60544fcb1c3ceb833fbcbd96ba5f36ae86f666ab01f8cad6c791c`
- License: GNU Affero General Public License v3.0 (`LICENSE`)
- Provenance documented by RaceIQ: generated from each vehicle's
  `carsetuplimits` record in an installed Assetto Corsa EVO `content.kspkg`.

The app treats a missing, malformed, step-less, inverted, or otherwise
unverified range as unavailable. It does not substitute another vehicle's
range.

## AC EVO exact-layout geometry

`track-engineering-profiles-0.8.1.json` references exact AC EVO centerline data
from `SpeedHQ/RaceIQ` at the same pinned commit for 19 layouts. It stores only
derived path length/curvature demands and source URLs, not the centerline files.
The five remaining exact variant records cite the corresponding official
Nürburgring, Oulton Park or Watkins Glen circuit information. No track setup
values are included.

## Official vehicle thumbnails

`vehicle-thumbnails-0.8.1.json` contains references and descriptive metadata
for 71 remote media records published by Assetto Corsa at
`assettocorsa.gg/wp-json/wp/v2/media`. Image bytes are not included in this
repository. They remain subject to their owners' copyright, trademark and
other rights and are requested from the official host only when the
corresponding exact vehicle is selected.

## Trademarks

Assetto Corsa and Assetto Corsa EVO are trademarks of their respective owners.
This independent project is not affiliated with or endorsed by KUNOS
Simulazioni or 505 Games.

## RacePlace 0.8.1 structure carriers

Five `.carsetup` files from the freely published “RacePlace ACE Baseline
Setups by DTVR 0.8.1” package are included as integrity-manifested binary
structure carriers. Source: `https://raceplace.racing/download/12287/`.
RacePlace/DTVR retain all rights to their setup files. The app does not use
their stored setup numbers as model inputs; it verifies the same-car signature
and rewrites every field authorized by the independent range model.
