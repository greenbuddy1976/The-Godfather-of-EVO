# The Godfather of EVO LIVE 2.0.0

Eigenständiger Android-Neuaufbau für echte Assetto-Corsa-EVO-`.carsetup`-Dateien.

## Was gegenüber 1.1.1 korrigiert wurde

- Kein `text/plain`-Export und keine `.txt`-Datei mehr.
- Keine generierten Klick-Empfehlungen, die als echte Setup-Datei ausgegeben werden.
- Kein `SharedPreferences`-Cache und keine alten Setupwerte.
- Zwei vollständige LIVE-Runden über SetupsMarket und RacePlace/DTVR.
- Nur Schnittmengen-Treffer aus beiden Runden werden freigegeben.
- Fahrzeug, Layout und Spielversion `0.8.x` müssen exakt passen.
- Dropdown-Menüs für Fahrzeug, Layout, Stil, optionalen Wunsch und konkrete Quelle.
- Stil und Wunsch sortieren nur Quellen; Binärdateien werden niemals verändert.
- Beim Speichern wird die gewählte Originaldatei erneut frisch geladen.
- Vor dem Export werden ZIP-, HTML- und JSON-Fehlerantworten abgewehrt.
- Ausgabe ausschließlich als `application/octet-stream` mit Endung `.carsetup`.

## Datenschutz und Grenzen

Die App benötigt Internetzugriff. Sie speichert keinen Online-Katalog dauerhaft. Wenn keine
exakte aktuelle Datei vorhanden ist oder eine Quelle fehlschlägt, bleibt Speichern gesperrt.
Es wird weder ein Spenderauto noch ein ähnliches Layout verwendet.

## Build

```text
gradle -p freshapp --no-daemon --no-build-cache clean testDebugUnitTest lintDebug assembleDebug
```
