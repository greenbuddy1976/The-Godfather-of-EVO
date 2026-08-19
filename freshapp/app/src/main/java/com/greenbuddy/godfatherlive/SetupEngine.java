package com.greenbuddy.godfatherlive;

import java.util.ArrayList;
import java.util.List;

final class SetupEngine {
    enum Profile {
        FAST("FAST / HOTLAP", "Maximale Pace mit kleinen, kontrollierten Eingriffen"),
        FAST_CONTROL("FAST CONTROL", "Schnell, berechenbar und stabil beim Bremsen"),
        STABLE_LEARNING("STABLE LEARNING", "Gutmuetig, ruhig und leicht zu lernen"),
        LONG_RUN("LONG RUN", "Konstanz, Reifenruhe und sichere Stints"),
        STABLE_FAST("STABLE + FAST", "Sehr stabil, aber ohne die Pace abzuwuergen");

        final String title;
        final String subtitle;
        Profile(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
        @Override public String toString() { return title + " — " + subtitle; }
    }

    enum Operation { ADD, SCALE, SET }

    static final class Change {
        final String path;
        final Operation operation;
        final float value;
        final String reason;

        Change(String path, Operation operation, float value, String reason) {
            this.path = path;
            this.operation = operation;
            this.value = value;
            this.reason = reason;
        }
    }

    static List<Change> changes(String car, String track, Profile profile, int[] fineTune) {
        List<Change> out = new ArrayList<>();
        boolean mustang = QueryLogic.key(car).equals(QueryLogic.key("Ford Mustang GT3"));
        boolean highSpeed = containsAny(track, "Monza", "SPA", "Spa", "Paul Ricard", "Red Bull", "Fuji", "Watkins");
        boolean bumpy = containsAny(track, "Bathurst", "Sebring", "Nordschleife", "Nürburgring 24h", "Oulton", "Road Atlanta");
        boolean technical = containsAny(track, "Brands Hatch", "Donington", "Imola", "Laguna", "Suzuka", "Kyalami", "Circuit of the Americas");

        switch (profile) {
            case FAST -> {
                add(out, "1.3.1", -0.30f, "Bremsbalance leicht nach hinten fuer Rotation");
                add(out, "1.4.3", -5f, "Etwas weniger Diff-Vorspannung fuer Rotation");
                add(out, "4[0].2", -0.10f, "Mehr negativer Sturz vorne links");
                add(out, "4[1].2", -0.10f, "Mehr negativer Sturz vorne rechts");
                add(out, "4[0].1", -0.10f, "Hotlap-Druck leicht reduzieren");
                add(out, "4[1].1", -0.10f, "Hotlap-Druck leicht reduzieren");
                add(out, "4[2].1", -0.10f, "Hotlap-Druck leicht reduzieren");
                add(out, "4[3].1", -0.10f, "Hotlap-Druck leicht reduzieren");
                if (highSpeed) add(out, "6.5", -1f, "Auf Highspeed-Strecken einen Fluegel-Schritt weniger");
            }
            case FAST_CONTROL -> {
                add(out, "1.3.1", 0.20f, "Etwas mehr Stabilitaet beim Anbremsen");
                add(out, "1.4.3", 5f, "Diff-Uebergang etwas ruhiger");
                scale(out, "2[2].1", 0.98f, "Hinterachse minimal weicher fuer Traktion");
                scale(out, "2[3].1", 0.98f, "Hinterachse minimal weicher fuer Traktion");
            }
            case STABLE_LEARNING -> {
                add(out, "1.3.1", 0.50f, "Bremsstabilitaet erhoehen");
                add(out, "1.4.3", 10f, "Mehr Diff-Vorspannung fuer ein ruhigeres Heck");
                scale(out, "2[2].1", 0.95f, "Hinterachse weicher fuer Traktion");
                scale(out, "2[3].1", 0.95f, "Hinterachse weicher fuer Traktion");
                add(out, "4[2].3", 0.02f, "Mehr Hinterachs-Vorspur links");
                add(out, "4[3].3", 0.02f, "Mehr Hinterachs-Vorspur rechts");
                add(out, "6.5", 1f, "Mehr Heckstabilitaet durch Fluegel");
            }
            case LONG_RUN -> {
                add(out, "4[0].1", -0.20f, "Etwas Reserve fuer steigenden Reifendruck");
                add(out, "4[1].1", -0.20f, "Etwas Reserve fuer steigenden Reifendruck");
                add(out, "4[2].1", -0.20f, "Etwas Reserve fuer steigenden Reifendruck");
                add(out, "4[3].1", -0.20f, "Etwas Reserve fuer steigenden Reifendruck");
                add(out, "1.3.1", 0.20f, "Konstante Bremsstabilitaet ueber den Stint");
                scale(out, "2[2].1", 0.98f, "Hinterachse leicht reifenschonender");
                scale(out, "2[3].1", 0.98f, "Hinterachse leicht reifenschonender");
                add(out, "6.5", 1f, "Mehr Sicherheitsreserve im langen Stint");
            }
            case STABLE_FAST -> {
                add(out, "1.3.1", 0.30f, "Stabil bremsen ohne starke Untersteuer-Tendenz");
                add(out, "1.4.3", 7f, "Ruhiger Diff-Uebergang");
                scale(out, "2[2].1", 0.97f, "Mehr Traktion an der Hinterachse");
                scale(out, "2[3].1", 0.97f, "Mehr Traktion an der Hinterachse");
                add(out, "4[0].2", -0.05f, "Frontgrip erhalten");
                add(out, "4[1].2", -0.05f, "Frontgrip erhalten");
                add(out, "6.5", 1f, "Schnelle, aber sichere Aero-Balance");
            }
        }

        if (bumpy) {
            add(out, "6.2", 2f, "Bodenfreiheit vorne fuer Bodenwellen/Curbs");
            add(out, "6.3", 2f, "Bodenfreiheit hinten fuer Bodenwellen/Curbs");
            for (int i = 0; i < 4; i++) {
                scale(out, "3[" + i + "].2", 0.95f, "Fast Bump fuer Bodenwellen weicher");
                scale(out, "3[" + i + "].4", 0.95f, "Fast Rebound fuer Bodenwellen weicher");
            }
        } else if (technical) {
            scale(out, "1.1[0]", 0.98f, "Etwas mehr mechanischer Frontgrip auf technischer Strecke");
            add(out, "1.4.3", -3f, "Etwas freiere Rotation in langsamen Kurven");
        }

        applyFineTune(out, fineTune);

        // Feste Sonderregel: Nur der Ford Mustang GT3 bekommt im STABLE+FAST-Profil TC1=1.
        if (mustang && profile == Profile.STABLE_FAST) {
            set(out, "5.1", 1f, "Mustang-Sonderprofil: STABLE + FAST mit TC1");
        }

        return out;
    }

    static String summary(String car, String track, Profile profile, BinarySetupEditor.EditResult result) {
        StringBuilder text = new StringBuilder();
        text.append(profile.title).append("\n")
                .append(car).append(" · ").append(track).append("\n")
                .append("Geschriebene Felder: ").append(result.applied).append(" / ").append(result.requested).append("\n");
        if (!result.skipped.isEmpty()) {
            text.append("Nicht verfuegbare Fahrzeugfelder wurden ausgelassen: ")
                    .append(String.join(", ", result.skipped)).append("\n");
        }
        text.append("Nur bestaetigte Float32-Felder werden in der .carsetup-Binaerstruktur veraendert.");
        return text.toString();
    }

    private static void applyFineTune(List<Change> out, int[] fine) {
        if (fine == null || fine.length < 8) return;

        switch (fine[0]) {
            case 1 -> { add(out, "4[0].1", -0.20f, "Fine: Vorderreifen etwas weniger Druck"); add(out, "4[1].1", -0.20f, "Fine: Vorderreifen etwas weniger Druck"); }
            case 2 -> { add(out, "4[2].1", -0.20f, "Fine: Hinterreifen etwas weniger Druck"); add(out, "4[3].1", -0.20f, "Fine: Hinterreifen etwas weniger Druck"); }
            case 3 -> { for (int i = 0; i < 4; i++) add(out, "4[" + i + "].1", -0.20f, "Fine: Reifendruck fuer langen Stint"); }
            default -> { }
        }
        switch (fine[1]) {
            case 1 -> set(out, "5.1", 1f, "Fine: TC1");
            case 2 -> set(out, "5.1", 2f, "Fine: TC2");
            case 3 -> set(out, "5.1", 3f, "Fine: TC3");
            default -> { }
        }
        switch (fine[2]) {
            case 1 -> add(out, "1.3.1", 0.50f, "Fine: Bremsbalance nach vorn");
            case 2 -> add(out, "1.3.1", -0.50f, "Fine: Bremsbalance nach hinten");
            default -> { }
        }
        switch (fine[3]) {
            case 1 -> add(out, "1.4.3", 10f, "Fine: mehr Diff-Vorspannung fuer Stabilitaet");
            case 2 -> add(out, "1.4.3", -10f, "Fine: weniger Diff-Vorspannung fuer Rotation");
            default -> { }
        }
        switch (fine[4]) {
            case 1 -> scale(out, "1.1[1]", 0.95f, "Fine: hinteren Stabilisator weicher");
            case 2 -> scale(out, "1.1[0]", 0.95f, "Fine: vorderen Stabilisator weicher");
            case 3 -> scale(out, "1.1[1]", 1.05f, "Fine: hinteren Stabilisator direkter");
            default -> { }
        }
        switch (fine[5]) {
            case 1 -> {
                scale(out, "2[2].1", 0.97f, "Fine: hintere Feder weicher");
                scale(out, "2[3].1", 0.97f, "Fine: hintere Feder weicher");
                scale(out, "3[2].1", 0.97f, "Fine: hinterer Slow Bump weicher");
                scale(out, "3[3].1", 0.97f, "Fine: hinterer Slow Bump weicher");
            }
            case 2 -> { for (int i = 0; i < 4; i++) { scale(out, "3[" + i + "].2", 0.92f, "Fine: Curbs Fast Bump weicher"); scale(out, "3[" + i + "].4", 0.92f, "Fine: Curbs Fast Rebound weicher"); } }
            case 3 -> { for (int i = 0; i < 4; i++) { scale(out, "3[" + i + "].1", 1.05f, "Fine: direkter Slow Bump"); scale(out, "3[" + i + "].3", 1.05f, "Fine: direkter Slow Rebound"); } }
            default -> { }
        }
        switch (fine[6]) {
            case 1 -> add(out, "6.3", 2f, "Fine: hinten hoeher");
            case 2 -> add(out, "6.2", 2f, "Fine: vorne hoeher");
            case 3 -> { add(out, "6.2", 2f, "Fine: vorne hoeher"); add(out, "6.3", 2f, "Fine: hinten hoeher"); }
            default -> { }
        }
        switch (fine[7]) {
            case 1 -> add(out, "6.5", 1f, "Fine: Heckfluegel +1");
            case 2 -> add(out, "6.5", 2f, "Fine: Heckfluegel +2");
            case 3 -> add(out, "6.5", -1f, "Fine: Heckfluegel -1");
            default -> { }
        }
    }

    private static void add(List<Change> out, String path, float value, String reason) {
        out.add(new Change(path, Operation.ADD, value, reason));
    }
    private static void scale(List<Change> out, String path, float value, String reason) {
        out.add(new Change(path, Operation.SCALE, value, reason));
    }
    private static void set(List<Change> out, String path, float value, String reason) {
        out.add(new Change(path, Operation.SET, value, reason));
    }
    private static boolean containsAny(String value, String... needles) {
        if (value == null) return false;
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private SetupEngine() {}
}
