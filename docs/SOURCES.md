# Inventory provenance

Inventory snapshot date: **2026-08-19**. Supported game version target: **0.8.1**.

Primary public references:

- Official current AC EVO content page: https://assettocorsa.gg/assetto-corsa-evo/
- Official 0.8 patch notes: https://support.505games.com/support/solutions/articles/150000229709-assetto-corsa-evo-0-8-patch-notes
- Official 0.8.1 patch notes: https://support.505games.com/support/solutions/articles/150000230407-assetto-corsa-evo-patch-notes-0-8-1
- Official Steam product content history: https://store.steampowered.com/app/3058630/Assetto_Corsa_EVO/

The public website lists vehicle display identities and track/layout display names, but
does not publish the complete `.carsetup` schema, all writable fields, ranges, steps,
or canonical internal folder/signature IDs. Therefore those values are not inferred in
this repository. They are release-gated and must be provided by a verified module made
from legally obtained, version-matched game data.

The 35 layout entries are directly supported by primary sources. `Suzuka West` remains
excluded because it was not directly confirmed in the primary-source audit. Stable app
IDs are not claimed to be AC EVO filesystem IDs. The verified writer must map every
entry to the game's exact identity before release.
