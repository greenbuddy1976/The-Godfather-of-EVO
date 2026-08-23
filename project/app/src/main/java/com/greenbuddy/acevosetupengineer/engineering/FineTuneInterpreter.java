package com.greenbuddy.acevosetupengineer.engineering;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FineTuneInterpreter {
    public FineTuneInterpretation interpret(String text) {
        String n = normalize(text);
        Set<HandlingIssue> issues = new LinkedHashSet<>();

        boolean entry = containsAny(n, "kurveneingang", "beim einlenken", "anbremsen", "bremsphase", "entry");
        boolean middle = containsAny(n, "kurvenmitte", "mitte der kurve", "mid corner", "midcorner");
        boolean exit = containsAny(n, "kurvenausgang", "beim rausbeschleunigen", "am ausgang", "exit", "auf gas");
        boolean understeer = containsAny(n, "untersteuert", "untersteuern", "schiebt uber die vorderachse",
                "schiebt vorne", "will nicht einlenken", "zu wenig rotation", "lenkt nicht ein");
        boolean oversteer = containsAny(n, "ubersteuert", "ubersteuern", "heck bricht aus", "heck kommt",
                "heck rutscht", "zu viel rotation");

        if (understeer) {
            if (entry) issues.add(HandlingIssue.ENTRY_UNDERSTEER);
            else if (exit) issues.add(HandlingIssue.EXIT_UNDERSTEER);
            else issues.add(HandlingIssue.MID_UNDERSTEER);
        }
        if (oversteer) {
            if (entry) issues.add(HandlingIssue.ENTRY_OVERSTEER);
            else if (exit) issues.add(HandlingIssue.EXIT_OVERSTEER);
            else issues.add(HandlingIssue.MID_OVERSTEER);
        }

        boolean rearNervous = containsAny(n, "heck nervos", "heck wird nervos", "heck ist nervos",
                "heck ist zu nervos", "nervoses heck", "heck unruhig", "hinten instabil",
                "heck zu lebendig");
        if (rearNervous) {
            issues.add(HandlingIssue.REAR_NERVOUS);
        }
        if (containsAny(n, "heck trage", "heck zu trage", "heck ist trage", "heck ist zu trage",
                "hinten trage", "dreht zu langsam", "zu stabil hinten")) {
            issues.add(HandlingIssue.REAR_SLUGGISH);
        }
        if (containsAny(n, "instabil beim bremsen", "beim bremsen unruhig", "heck beim bremsen",
                "bremsen instabil", "braking instability")) {
            issues.add(HandlingIssue.BRAKING_INSTABILITY);
        }
        if (rearNervous && entry && containsAny(n, "anbremsen", "bremsphase", "beim bremsen", "braking")) {
            issues.add(HandlingIssue.BRAKING_INSTABILITY);
        }
        if (containsAny(n, "keine traktion", "zu wenig traktion", "dreht durch", "wheelspin", "schlechte traktion")) {
            issues.add(HandlingIssue.POOR_TRACTION);
        }
        if (containsAny(n, "bodenwellen", "auf wellen", "uber bodenwellen", "bumps", "bump instabil")) {
            issues.add(HandlingIssue.BUMP_INSTABILITY);
        }
        if (containsAny(n, "curbs", "kerbs", "randsteine", "uber randsteine", "auf curbs")) {
            issues.add(HandlingIssue.CURB_INSTABILITY);
        }
        if (containsAny(n, "vorderreifen zu heiss", "vorderreifen uberhitzen", "vorne zu heiss")) {
            issues.add(HandlingIssue.FRONT_TYRE_OVERHEAT);
        }
        if (containsAny(n, "hinterreifen zu heiss", "hinterreifen uberhitzen", "hinten zu heiss")) {
            issues.add(HandlingIssue.REAR_TYRE_OVERHEAT);
        }
        if (containsAny(n, "lenkung zu nervos", "lenkung ist zu nervos", "lenkt zu scharf",
                "lenkung ist zu scharf", "zu direkt", "zu giftig beim einlenken")) {
            issues.add(HandlingIssue.STEERING_TOO_SHARP);
        }
        if (containsAny(n, "lenkung zu langsam", "lenkung ist zu langsam", "lenkt zu trage",
                "lenkung ist zu trage", "zu wenig direkt", "reagiert zu langsam")) {
            issues.add(HandlingIssue.STEERING_TOO_SLOW);
        }
        if (containsAny(n, "mehr heckflugel", "heckflugel erhohen", "heckflugel hoch",
                "mehr rear wing", "increase rear wing")) {
            issues.add(HandlingIssue.DIRECT_REAR_WING_MORE);
        }
        if (containsAny(n, "weniger heckflugel", "heckflugel verringern", "heckflugel runter",
                "weniger rear wing", "decrease rear wing")) {
            issues.add(HandlingIssue.DIRECT_REAR_WING_LESS);
        }

        return new FineTuneInterpretation(text == null ? "" : text, new ArrayList<>(issues));
    }

    private static String normalize(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.GERMAN)
                .replace('ß', 's')
                .replace("ä", "a")
                .replace("ö", "o")
                .replace("ü", "u");
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(normalize(phrase))) return true;
        }
        return false;
    }
}
