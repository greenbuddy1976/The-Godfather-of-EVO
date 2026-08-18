# The Godfather of EVO 1.1.1

Eigenständiger Android-Neuaufbau für nachvollziehbare AC-EVO-Setuppläne.

- 71 Fahrzeuge, darunter BMW M2, beide BMW M3 und Ford Mustang GT3
- 24 exakte Layouts
- fünf Profile: FAST CONTROL, FAST ATTACK, FAST STABLE, FAST SAFE, FAST LONG
- acht aufklappbare Feintuning-Gruppen einschließlich Heckflügel
- schwarz-gelbe Oberfläche ohne rote Aktionsflächen
- Erststart-Index für SetupsMarket und RacePlace mit lokal gespeichertem Trefferstand
- Mustang FAST ATTACK erzwingt auf jedem Layout TC 1
- keine eingebettete `.carsetup`, keine Notfall-Datei, kein Spenderauto und kein ähnliches Layout

Die App erzeugt für jede Kombination einen manuellen, relativ zum Standardsetup beschriebenen
Setupplan. Echte Online-Dateien werden ausschließlich als exakte Fahrzeug-/Layout-Treffer verlinkt;
ein nicht belegter Binärexport wird nicht vorgetäuscht.

Build: `gradle -p cleanroom clean testDebugUnitTest lintRelease assembleRelease`
