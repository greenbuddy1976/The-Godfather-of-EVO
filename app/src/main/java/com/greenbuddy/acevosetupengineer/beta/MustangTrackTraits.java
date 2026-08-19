package com.greenbuddy.acevosetupengineer.beta;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact-ID table. Unknown or similar names are never accepted. */
public final class MustangTrackTraits {
    private static final Map<String, TrackTrait> TRAITS = build();
    private MustangTrackTraits() { }

    public static TrackTrait require(String exactLayoutId) {
        TrackTrait trait = TRAITS.get(exactLayoutId);
        if (trait == null) throw new IllegalArgumentException("Unknown exact layout: " + exactLayoutId);
        return trait;
    }

    public static Map<String, TrackTrait> all() { return TRAITS; }

    private static Map<String, TrackTrait> build() {
        Map<String, TrackTrait> map = new LinkedHashMap<>();
        add(map, "mount_panorama_gp", TrackTrait.HIGH_SPEED);
        add(map, "brands_hatch_gp", TrackTrait.BUMPY_KERB);
        add(map, "brands_hatch_indy", TrackTrait.TECHNICAL);
        add(map, "cota_gp", TrackTrait.BALANCED);
        add(map, "cota_national", TrackTrait.TECHNICAL);
        add(map, "donington_gp", TrackTrait.BUMPY_KERB);
        add(map, "donington_national", TrackTrait.TECHNICAL);
        add(map, "fuji_gp", TrackTrait.LOW_DOWNFORCE);
        add(map, "fuji_gp_short", TrackTrait.TECHNICAL);
        add(map, "imola_gp", TrackTrait.BUMPY_KERB);
        add(map, "kyalami_gp", TrackTrait.HIGH_SPEED);
        add(map, "laguna_seca_gp", TrackTrait.BUMPY_KERB);
        add(map, "monza_gp", TrackTrait.LOW_DOWNFORCE);
        add(map, "nurburgring_gp_strecke", TrackTrait.BALANCED);
        add(map, "nurburgring_sprint", TrackTrait.TECHNICAL);
        add(map, "nurburgring_24h", TrackTrait.BUMPY_KERB);
        add(map, "nurburgring_nordschleife", TrackTrait.BUMPY_KERB);
        add(map, "nurburgring_touristenfahrten", TrackTrait.BUMPY_KERB);
        add(map, "oulton_park_international", TrackTrait.BUMPY_KERB);
        add(map, "oulton_park_fosters", TrackTrait.TECHNICAL);
        add(map, "paul_ricard_1a_v2", TrackTrait.LOW_DOWNFORCE);
        add(map, "paul_ricard_1c_v2", TrackTrait.HIGH_SPEED);
        add(map, "paul_ricard_3a", TrackTrait.BALANCED);
        add(map, "paul_ricard_3c", TrackTrait.TECHNICAL);
        add(map, "red_bull_ring_gp", TrackTrait.HIGH_SPEED);
        add(map, "red_bull_ring_national", TrackTrait.TECHNICAL);
        add(map, "road_atlanta_gp", TrackTrait.BUMPY_KERB);
        add(map, "sebring_gp", TrackTrait.BUMPY_KERB);
        add(map, "spa_gp", TrackTrait.HIGH_SPEED);
        add(map, "suzuka_gp", TrackTrait.HIGH_SPEED);
        add(map, "suzuka_east", TrackTrait.TECHNICAL);
        add(map, "watkins_glen_gp", TrackTrait.HIGH_SPEED);
        add(map, "watkins_glen_gp_inner_loop", TrackTrait.BALANCED);
        add(map, "watkins_glen_short", TrackTrait.TECHNICAL);
        add(map, "watkins_glen_short_inner_loop", TrackTrait.BALANCED);
        if (map.size() != 35) throw new IllegalStateException("Expected 35 exact layout traits");
        return Collections.unmodifiableMap(map);
    }

    private static void add(Map<String, TrackTrait> map, String id, TrackTrait trait) {
        if (map.put(id, trait) != null) throw new IllegalStateException("Duplicate layout trait: " + id);
    }
}
