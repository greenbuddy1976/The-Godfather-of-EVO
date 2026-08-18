package com.greenbuddy.acevosetupengineer;

import java.text.Normalizer;
import java.util.Locale;

final class SetupEngine {
    enum Mode {
        FAST_CONTROL("FAST CONTROL", "Empfohlen · schnell und berechenbar"),
        FAST_ATTACK("FAST ATTACK", "Direkt · Qualifying/Hotlap"),
        FAST_STABLE("FAST STABLE", "Ruhiges Heck · sichere Curbs"),
        FAST_SAFE("FAST SAFE", "Maximale Sicherheitsreserve"),
        FAST_LONG_RUN("FAST LONG", "Konstant · reifenschonend");

        final String title;
        final String subtitle;
        Mode(String title, String subtitle) { this.title = title; this.subtitle = subtitle; }
        @Override public String toString() { return title + " — " + subtitle; }
    }

    static String build(String car, String track, Mode mode, String[] fineTune) {
        boolean mustang = car.equals("Ford Mustang GT3");
        boolean highSpeed = containsAny(track, "Monza", "SPA", "Red Bull", "Paul Ricard", "Watkins", "Road Atlanta");
        boolean bumpy = containsAny(track, "Sebring", "Bathurst", "Nordschleife", "Oulton Park");
        boolean raceCar = containsAny(car, "GT2", "GT3", "GT4", "Cup", "Challenge", "F2004", "SF-25", "Rennsport", "LM", "Academy Racer", "CS Racing");

        StringBuilder out = new StringBuilder();
        out.append("THE GODFATHER OF EVO · SETUP 1.1.1\n\n")
                .append("Auto: ").append(car).append('\n')
                .append("Strecke: ").append(track).append('\n')
                .append("Profil: ").append(mode.title).append("\n\n")
                .append("Stabile Basis (Änderungen relativ zum Standardsetup)\n")
                .append("• Reifendruck: kalt unverändert starten; nach 3 sauberen Runden links/rechts angleichen.\n")
                .append("• Bremsbalance: +1 Klick nach vorn.\n")
                .append("• Differenzial Schub: +1 Klick für ruhigeres Anbremsen.\n")
                .append("• Differenzial Zug: -1 Klick für sanfteren Kurvenausgang.\n")
                .append("• Hinterer Stabilisator: -1 Klick.\n")
                .append("• Hintere Zugstufe: +1 Klick.\n")
                .append("• Hintere Spur: +1 Klick Richtung Vorspur.\n");

        if (raceCar) out.append("• Heckflügel: +1 Klick als stabile Ausgangsbasis.\n");
        else out.append("• Heckflügel: nur ändern, wenn das Fahrzeug ihn im Spiel freigibt.\n");
        if (bumpy) out.append("• Streckenanpassung: Bodenfreiheit +1 Klick, schnelle Dämpfung -1 Klick.\n");
        if (highSpeed) out.append("• Streckenanpassung: Aero-Heck nicht unter Standard; Hinterachse ruhig halten.\n");

        switch (mode) {
            case FAST_ATTACK -> {
                out.append("\nFAST ATTACK\n• Heckflügel: Basiswert; nur bei sicherem Heck -1 Klick testen.\n")
                        .append("• Hinterer Stabilisator: Basisänderung zurück auf Standard.\n")
                        .append("• Kraftstoff: nur benötigte Runden + Reserve.\n");
                out.append("• Traktionskontrolle: ").append(mustang ? "1 (Mustang-Pflicht auf jeder Strecke)." : "1, sofern vorhanden.").append('\n');
            }
            case FAST_STABLE -> out.append("\nFAST STABLE\n• Heckflügel: zusätzlich +1 Klick.\n• Hinterer Stabilisator: zusätzlich -1 Klick.\n• TC: 2, sofern vorhanden.\n");
            case FAST_SAFE -> out.append("\nFAST SAFE\n• Heckflügel: zusätzlich +2 Klicks.\n• Bremsbalance: zusätzlich +1 Klick nach vorn.\n• TC: 3, sofern vorhanden.\n");
            case FAST_LONG_RUN -> out.append("\nFAST LONG\n• Heckflügel: zusätzlich +1 Klick.\n• TC: 2, sofern vorhanden.\n• Kraftstoff: Renndistanz + eine Runde Reserve.\n• Keine aggressiven Druckänderungen vor dem Warmdruck-Check.\n");
            case FAST_CONTROL -> out.append("\nFAST CONTROL\n• Diese Basis unverändert als ersten Stint fahren.\n• TC: 1–2, sofern vorhanden.\n");
        }

        out.append("\nFeintuning (nur gewählte Abweichungen)\n");
        boolean changed = false;
        for (String choice : fineTune) {
            if (choice != null && !choice.endsWith(": Standard")) {
                out.append("• ").append(choice).append('\n');
                changed = true;
            }
        }
        if (!changed) out.append("• Keine zusätzlichen Änderungen.\n");

        out.append("\nPrüfreihenfolge\n")
                .append("1. Drei saubere Runden, Reifen warm.\n")
                .append("2. Immer nur eine Stellgruppe ändern.\n")
                .append("3. Bei instabilem Heck zuerst Flügel/ARB/Spur, nicht mehr Leistung erzwingen.\n")
                .append("4. Ein nicht verstellbares Feld bleibt unangetastet; kein Ersatzwert wird erfunden.\n");
        return out.toString();
    }

    static String key(String value) {
        String n = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replace("volkswagen", "vw").replace("mount panorama", "bathurst")
                .replace("spa francorchamps", "spa-francorchamps")
                .replace("nurburgring", "nurburgring");
        return n.replaceAll("[^a-z0-9]+", "");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private SetupEngine() {}
}
