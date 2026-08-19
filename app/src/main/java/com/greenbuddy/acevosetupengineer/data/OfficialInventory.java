package com.greenbuddy.acevosetupengineer.data;

import com.greenbuddy.acevosetupengineer.model.CarIdentity;
import com.greenbuddy.acevosetupengineer.model.TrackLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Version-bound display inventory. Stable IDs are application IDs, not claimed game
 * folder IDs. A verified release provider must map and verify each identity exactly.
 */
public final class OfficialInventory {
    public static final String GAME_VERSION = "0.8.1";
    public static final String CAR_SOURCE = "https://assettocorsa.gg/assetto-corsa-evo/";
    public static final String TRACK_SOURCE = "https://assettocorsa.gg/assetto-corsa-evo/";

    private static final List<CarIdentity> CARS = buildCars();
    private static final List<TrackLayout> LAYOUTS = buildLayouts();

    private OfficialInventory() { }

    public static List<CarIdentity> cars() { return CARS; }
    public static List<TrackLayout> layouts() { return LAYOUTS; }

    public static CarIdentity requireCar(String id) {
        for (CarIdentity car : CARS) if (car.getId().equals(id)) return car;
        throw new IllegalArgumentException("Unknown exact car id: " + id);
    }

    public static TrackLayout requireLayout(String id) {
        for (TrackLayout layout : LAYOUTS) if (layout.getId().equals(id)) return layout;
        throw new IllegalArgumentException("Unknown exact layout id: " + id);
    }

    private static List<CarIdentity> buildCars() {
        List<CarIdentity> cars = new ArrayList<>();
        addCar(cars, "abarth_695_biposto", "Abarth 695 Biposto (2022)");
        addCar(cars, "alfa_75_turbo_evoluzione", "Alfa Romeo 75 1.8 Turbo Evoluzione (1987)");
        addCar(cars, "alfa_giulia_gtam", "Alfa Romeo Giulia GTAm (2021)");
        addCar(cars, "alfa_junior_elettrica_veloce", "Alfa Romeo Junior Elettrica Veloce (2024)");
        addCar(cars, "alpine_a110_s", "Alpine A110 S (2020)");
        addCar(cars, "alpine_a290_beta", "Alpine A290_β (2023)");
        addCar(cars, "audi_rs3_sportback", "Audi RS 3 Sportback (2022)");
        addCar(cars, "audi_sport_quattro", "Audi Sport quattro (1983)");
        addCar(cars, "audi_rs6_avant", "Audi RS 6 Avant");
        addCar(cars, "bmw_m2_coupe_g87", "BMW M2 Coupe (G87)");
        addCar(cars, "bmw_m3_e46_csl", "BMW M3 E46 CSL (2003)");
        addCar(cars, "bmw_m3_e30_sport_evo", "BMW M3 Sport Evo (E30)");
        addCar(cars, "bmw_m4_csl_g82", "BMW M4 CSL G82 (2022)");
        addCar(cars, "bmw_m8_competition", "BMW M8 Competition (2022)");
        addCar(cars, "caterham_485_csr", "Caterham 485 CSR");
        addCar(cars, "chevrolet_camaro_zl1", "Chevrolet Camaro ZL1 (2022)");
        addCar(cars, "dallara_exp", "Dallara EXP");
        addCar(cars, "dallara_stradale", "Dallara Stradale");
        addCar(cars, "ferrari_288_gto", "Ferrari 288 GTO");
        addCar(cars, "ferrari_296_gtb", "Ferrari 296 GTB (2021)");
        addCar(cars, "ferrari_daytona_sp3", "Ferrari Daytona SP3 (2021)");
        addCar(cars, "ford_escort_rs_cosworth", "Ford Escort RS Cosworth (1994)");
        addCar(cars, "honda_nsx_r", "Honda NSX-R (1992)");
        addCar(cars, "honda_s2000", "Honda S-2000 (2003)");
        addCar(cars, "hyundai_i30_n", "Hyundai i30 N Hatchback (2021)");
        addCar(cars, "lamborghini_countach", "Lamborghini Countach");
        addCar(cars, "lamborghini_huracan_sto", "Lamborghini Huracán STO (2021)");
        addCar(cars, "lancia_delta_hf_evo2", "Lancia Delta HF Integrale Evoluzione 2 (1993)");
        addCar(cars, "lotus_emira", "Lotus Emira (2021)");
        addCar(cars, "lotus_exige_v6_cup", "Lotus Exige V6 Cup (2014)");
        addCar(cars, "mazda_mx5_na", "Mazda MX-5 NA (1994)");
        addCar(cars, "mini_john_cooper_s_mk6", "Mini John Cooper S Mk VI (1990)");
        addCar(cars, "mercedes_190e_evo2", "Mercedes-Benz 190E AMG 2.5-16 Evo II");
        addCar(cars, "nissan_datsun_240z", "Nissan Datsun 240Z (S30)");
        addCar(cars, "nissan_datsun_240z_tuned", "Nissan Datsun 240Z (S30) – Tuned");
        addCar(cars, "porsche_718_cayman_gt4_rs", "Porsche 718 Cayman GT4 RS (2024)");
        addCar(cars, "porsche_911_turbo_964", "Porsche 911 Turbo 3.6 [964] (1993)");
        addCar(cars, "porsche_911_992_gt3_rs", "Porsche 911 (992) GT3 RS");
        addCar(cars, "peugeot_205_t16", "Peugeot 205 T16 (1984)");
        addCar(cars, "renault_5_gt_turbo", "Renault 5 GT Turbo (1990)");
        addCar(cars, "toyota_ae86", "Toyota AE86");
        addCar(cars, "toyota_ae86_tuned", "Toyota AE86 Tuned");
        addCar(cars, "toyota_gr86", "Toyota GR86 (2024)");
        addCar(cars, "toyota_supra_rz", "Toyota Supra Turbo RZ (1998)");
        addCar(cars, "toyota_supra_rz_drift", "Toyota Supra Turbo RZ V2 DRIFT");
        addCar(cars, "volkswagen_golf_8_gti", "Volkswagen Golf 8 GTI Clubsport (2022)");
        addCar(cars, "volkswagen_golf_8_r", "Volkswagen Golf 8 R");
        addCar(cars, "volkswagen_golf_gti_mk1", "Volkswagen Golf GTI MK1");

        addCar(cars, "alfa_giulia_sprint_gta", "Alfa Romeo Giulia Sprint GTA (1965)");
        addCar(cars, "audi_r8_lms_gt3_evo2", "Audi R8 LMS GT3 Evo II");
        addCar(cars, "audi_r8_lms_gt4_evo", "Audi R8 LMS GT4 Evo");
        addCar(cars, "bmw_m4_gt3_evo", "BMW M4 GT3 EVO");
        addCar(cars, "bmw_m2_cs_racing", "BMW M2 CS Racing (2020)");
        addCar(cars, "caterham_seven_academy", "Caterham Seven Academy Racer");
        addCar(cars, "ferrari_296_gt3", "Ferrari 296 GT3");
        addCar(cars, "ferrari_488_challenge_evo", "Ferrari 488 Challenge Evo (2021)");
        addCar(cars, "ferrari_f40_lm", "Ferrari F40 LM (1993)");
        addCar(cars, "ferrari_f2004", "Ferrari F2004");
        addCar(cars, "ferrari_sf25", "Ferrari SF-25 (2025)");
        addCar(cars, "ford_mustang_gt3", "Ford Mustang GT3");
        addCar(cars, "ktm_xbow_gt2", "KTM X-BOW GT2");
        addCar(cars, "ktm_xbow_gt4", "KTM X-BOW GT4");
        addCar(cars, "lamborghini_huracan_st_evo2", "Lamborghini Huracán Super Trofeo EVO2");
        addCar(cars, "maserati_gt2", "Maserati GT2 (2023)");
        addCar(cars, "mazda_mx5_nd_cup", "Mazda MX5 ND Cup (2017)");
        addCar(cars, "mercedes_amg_gt2", "Mercedes-AMG GT2 (2023)");
        addCar(cars, "porsche_718_cayman_gt4_clubsport", "Porsche 718 Cayman GT4 Clubsport");
        addCar(cars, "porsche_935", "Porsche 935");
        addCar(cars, "porsche_911_gt3_cup_992", "Porsche 911 GT3 Cup [992] (2021)");
        addCar(cars, "porsche_991ii_gt2_rs_clubsport_evo", "Porsche 991II GT2 RS Clubsport Evo");
        addCar(cars, "porsche_992_gt3_r_rennsport", "Porsche 992 GT3 R Rennsport");

        assertUnique(cars, 71, "vehicle");
        return Collections.unmodifiableList(cars);
    }

    private static List<TrackLayout> buildLayouts() {
        List<TrackLayout> layouts = new ArrayList<>();
        addLayout(layouts, "mount_panorama_gp", "Mount Panorama", "GP");
        addLayout(layouts, "brands_hatch_gp", "Brands Hatch", "GP");
        addLayout(layouts, "brands_hatch_indy", "Brands Hatch", "Indy");
        addLayout(layouts, "cota_gp", "Circuit Of The Americas", "GP");
        addLayout(layouts, "cota_national", "Circuit Of The Americas", "National");
        addLayout(layouts, "donington_gp", "Donington Park", "GP");
        addLayout(layouts, "donington_national", "Donington Park", "National");
        addLayout(layouts, "fuji_gp", "Fuji Speedway", "GP");
        addLayout(layouts, "fuji_gp_short", "Fuji Speedway", "GP Short");
        addLayout(layouts, "imola_gp", "Imola", "GP");
        addLayout(layouts, "kyalami_gp", "Kyalami", "GP");
        addLayout(layouts, "laguna_seca_gp", "Laguna Seca", "GP");
        addLayout(layouts, "monza_gp", "Monza", "GP");
        addLayout(layouts, "nurburgring_gp_strecke", "Nurburgring", "Gp Strecke");
        addLayout(layouts, "nurburgring_sprint", "Nurburgring", "Sprint");
        addLayout(layouts, "nurburgring_24h", "Nurburgring", "24h");
        addLayout(layouts, "nurburgring_nordschleife", "Nurburgring", "Nordschleife");
        addLayout(layouts, "nurburgring_touristenfahrten", "Nurburgring", "Touristenfahrten");
        addLayout(layouts, "oulton_park_international", "Oulton Park", "International");
        addLayout(layouts, "oulton_park_fosters", "Oulton Park", "Fosters");
        addLayout(layouts, "paul_ricard_1a_v2", "Paul Ricard", "1A-V2");
        addLayout(layouts, "paul_ricard_1c_v2", "Paul Ricard", "1C-V2");
        addLayout(layouts, "paul_ricard_3a", "Paul Ricard", "3A");
        addLayout(layouts, "paul_ricard_3c", "Paul Ricard", "3C");
        addLayout(layouts, "red_bull_ring_gp", "Red Bull Ring", "GP");
        addLayout(layouts, "red_bull_ring_national", "Red Bull Ring", "National");
        addLayout(layouts, "road_atlanta_gp", "Road Atlanta", "GP");
        addLayout(layouts, "sebring_gp", "Sebring International Raceway", "GP");
        addLayout(layouts, "spa_gp", "Circuit de Spa Francorchamps", "GP");
        addLayout(layouts, "suzuka_gp", "Suzuka", "GP");
        addLayout(layouts, "suzuka_east", "Suzuka", "East");
        addLayout(layouts, "watkins_glen_gp", "Watkins Glen International", "GP");
        addLayout(layouts, "watkins_glen_gp_inner_loop", "Watkins Glen International", "GP Inner Loop");
        addLayout(layouts, "watkins_glen_short", "Watkins Glen International", "Short");
        addLayout(layouts, "watkins_glen_short_inner_loop", "Watkins Glen International", "Short Inner Loop");

        assertUnique(layouts, 35, "layout");
        return Collections.unmodifiableList(layouts);
    }

    private static void addCar(List<CarIdentity> cars, String id, String name) {
        cars.add(new CarIdentity(id, name, GAME_VERSION, CAR_SOURCE));
    }

    private static void addLayout(List<TrackLayout> layouts, String id, String track, String layout) {
        layouts.add(new TrackLayout(id, track, layout, GAME_VERSION, TRACK_SOURCE));
    }

    private static void assertUnique(List<?> items, int expected, String type) {
        Set<String> ids = new HashSet<>();
        for (Object item : items) {
            String id = item instanceof CarIdentity
                    ? ((CarIdentity) item).getId() : ((TrackLayout) item).getId();
            if (!ids.add(id)) throw new IllegalStateException("Duplicate " + type + ": " + id);
        }
        if (items.size() != expected) {
            throw new IllegalStateException("Expected " + expected + " " + type + " entries, got " + items.size());
        }
    }
}
