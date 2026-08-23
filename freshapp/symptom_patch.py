from pathlib import Path
import re

main_path = Path('app/src/main/java/com/greenbuddy/godfatherlive/MainActivity.java')
s = main_path.read_text(encoding='utf-8')

if 'FINE_TUNE_PROBLEMS' not in s:
    problems = '''    private static final String[] FINE_TUNE_PROBLEMS = {
            "Kein Problem ausgewählt",
            "Untersteuern – Auto schiebt über die Vorderachse",
            "Übersteuern – Heck bricht aus",
            "Heck nervös / instabil",
            "Auto zu träge beim Einlenken",
            "Auto reagiert zu aggressiv / zu nervös",
            "Auto instabil beim Bremsen",
            "Schlechte Traktion beim Herausbeschleunigen",
            "Auto unruhig über Curbs / Bodenwellen",
            "Mehr Heckstabilität / mehr Abtrieb gewünscht",
            "Mehr Topspeed / weniger Abtrieb gewünscht"
    };'''
    s, n = re.subn(r'    private static final String\[\]\[\] FINE_TUNE = \{.*?\n    \};', problems, s, count=1, flags=re.S)
    assert n == 1
    s = s.replace('private final Spinner[] fineSpinners = new Spinner[8];', 'private final Spinner[] fineSpinners = new Spinner[4];')
    s = s.replace('VERSION 2.1.0 · LIVE + GENERATOR', 'VERSION 2.1.1 · LIVE + GENERATOR')
    s = s.replace('FEINTUNING (8 BEREICHE)', 'FEINTUNING (BIS ZU 4 PROBLEME)')

    old_loop = '''        for (int i = 0; i < FINE_TUNE.length; i++) {
            fineSpinners[i] = spinner(FINE_TUNE[i]);
            finePanel.addView(fineSpinners[i], margins(0, 4, 0, 4));
        }'''
    new_loop = '''        TextView fineHelp = label("Wähle bis zu 4 verschiedene Fahrverhaltens-Probleme. Die App entscheidet selbst, welche Setupwerte geändert werden.", 12, MUTED, false);
        finePanel.addView(fineHelp, margins(0, 2, 0, 8));
        for (int i = 0; i < fineSpinners.length; i++) {
            finePanel.addView(label("Problem " + (i + 1), 12, YELLOW, true), margins(0, 4, 0, 2));
            fineSpinners[i] = spinner(FINE_TUNE_PROBLEMS);
            finePanel.addView(fineSpinners[i], margins(0, 2, 0, 4));
        }'''
    assert old_loop in s
    s = s.replace(old_loop, new_loop)

    needle = '        final int[] fine = fineSelections();\n'
    insert = '''        final int[] fine = fineSelections();
        if (!validFineSelections(fine)) {
            status.setText("Bitte jedes Fahrverhaltens-Problem nur einmal auswählen. Maximal 4 verschiedene Probleme gleichzeitig.");
            return;
        }
'''
    assert needle in s
    s = s.replace(needle, insert, 1)

    anchor = '    private String fingerprint(String car, String track, int[] fine) {'
    validation = '''    private boolean validFineSelections(int[] fine) {
        boolean[] used = new boolean[FINE_TUNE_PROBLEMS.length];
        for (int value : fine) {
            if (value <= 0) continue;
            if (value >= used.length || used[value]) return false;
            used[value] = true;
        }
        return true;
    }

'''
    assert anchor in s
    s = s.replace(anchor, validation + anchor, 1)
    main_path.write_text(s, encoding='utf-8')

setup_path = Path('app/src/main/java/com/greenbuddy/godfatherlive/SetupEngine.java')
e = setup_path.read_text(encoding='utf-8')
if 'Problem Untersteuern: Front mechanisch entlasten' not in e:
    new_method = '''    private static void applyFineTune(List<Change> out, int[] fine) {
        if (fine == null) return;
        for (int problem : fine) {
            switch (problem) {
                case 1 -> {
                    scale(out, "1.1[0]", 0.97f, "Problem Untersteuern: Front mechanisch entlasten");
                    add(out, "1.4.3", -5f, "Problem Untersteuern: freiere Rotation");
                    add(out, "4[0].2", -0.05f, "Problem Untersteuern: Frontgrip links");
                    add(out, "4[1].2", -0.05f, "Problem Untersteuern: Frontgrip rechts");
                }
                case 2 -> {
                    scale(out, "1.1[1]", 0.95f, "Problem Übersteuern: Hinterachse beruhigen");
                    add(out, "1.4.3", 8f, "Problem Übersteuern: Diff-Übergang stabilisieren");
                    add(out, "4[2].3", 0.01f, "Problem Übersteuern: mehr Hinterachs-Vorspur links");
                    add(out, "4[3].3", 0.01f, "Problem Übersteuern: mehr Hinterachs-Vorspur rechts");
                    add(out, "6.5", 1f, "Problem Übersteuern: mehr Heckstabilität");
                }
                case 3 -> {
                    scale(out, "1.1[1]", 0.95f, "Nervöses Heck: hinteren Stabilisator beruhigen");
                    scale(out, "2[2].1", 0.97f, "Nervöses Heck: Hinterachse links nachgiebiger");
                    scale(out, "2[3].1", 0.97f, "Nervöses Heck: Hinterachse rechts nachgiebiger");
                    add(out, "1.4.3", 10f, "Nervöses Heck: Diff stabilisieren");
                    add(out, "6.5", 1f, "Nervöses Heck: Aero-Stabilität erhöhen");
                }
                case 4 -> {
                    scale(out, "1.1[1]", 1.03f, "Träges Einlenken: mehr Rotation über Hinterachse");
                    add(out, "1.4.3", -5f, "Träges Einlenken: freiere Rotation");
                    add(out, "1.3.1", -0.20f, "Träges Einlenken: etwas mehr Rotation beim Anbremsen");
                }
                case 5 -> {
                    scale(out, "1.1[1]", 0.97f, "Zu aggressiv: Hinterachse beruhigen");
                    add(out, "1.4.3", 7f, "Zu aggressiv: Übergänge glätten");
                    add(out, "1.3.1", 0.20f, "Zu aggressiv: Bremsphase stabilisieren");
                }
                case 6 -> {
                    add(out, "1.3.1", 0.50f, "Instabil beim Bremsen: Bremsbalance stabilisieren");
                    add(out, "1.4.3", 5f, "Instabil beim Bremsen: Diff-Übergang beruhigen");
                    add(out, "4[2].3", 0.01f, "Instabil beim Bremsen: Hinterachse links stabilisieren");
                    add(out, "4[3].3", 0.01f, "Instabil beim Bremsen: Hinterachse rechts stabilisieren");
                }
                case 7 -> {
                    scale(out, "1.1[1]", 0.95f, "Schlechte Traktion: hinteren Stabilisator weicher");
                    scale(out, "2[2].1", 0.97f, "Schlechte Traktion: Hinterachse links weicher");
                    scale(out, "2[3].1", 0.97f, "Schlechte Traktion: Hinterachse rechts weicher");
                    add(out, "6.5", 1f, "Schlechte Traktion: mehr Hecklast über Aero");
                }
                case 8 -> {
                    add(out, "6.2", 2f, "Curbs/Bodenwellen: vorne mehr Bodenfreiheit");
                    add(out, "6.3", 2f, "Curbs/Bodenwellen: hinten mehr Bodenfreiheit");
                    for (int i = 0; i < 4; i++) {
                        scale(out, "3[" + i + "].2", 0.92f, "Curbs/Bodenwellen: Fast Bump weicher");
                        scale(out, "3[" + i + "].4", 0.92f, "Curbs/Bodenwellen: Fast Rebound weicher");
                    }
                }
                case 9 -> add(out, "6.5", 1f, "Mehr Heckstabilität: Heckflügel erhöhen");
                case 10 -> add(out, "6.5", -1f, "Mehr Topspeed: Heckflügel reduzieren");
                default -> { }
            }
        }
    }
'''
    pattern = r'    private static void applyFineTune\(List<Change> out, int\[] fine\) \{.*?\n    \}\n\n    private static void add'
    e, n = re.subn(pattern, new_method + '\n    private static void add', e, count=1, flags=re.S)
    assert n == 1
    setup_path.write_text(e, encoding='utf-8')
