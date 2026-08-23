#!/usr/bin/env python3
"""Fail-closed source/data verification; emits a machine-readable report."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path


EXPECTED_RANGE_SHA256 = "62b277050cf60544fcb1c3ceb833fbcbd96ba5f36ae86f666ab01f8cad6c791c"
EXPECTED_VERSION = "0.8.1"
EXPECTED_VEHICLES = 71
EXPECTED_LAYOUTS = 24
EXPECTED_MODES = 5
EXPECTED_RANGE_IDENTITIES = 68
EXPECTED_BINARY_IDENTITIES = 69
EXPECTED_SELF_CALC_RANGE_IDENTITIES = 68
EXPECTED_BUNDLED_CARRIERS = 5

MODE_FACTORS = {
    "FAST_CONTROL": (0.78, 0.72, 0.20),
    "FAST_ATTACK": (1.00, 0.28, 0.00),
    "FAST_STABLE": (0.65, 0.88, 0.25),
    "FAST_SAFE": (0.45, 1.00, 0.35),
    "FAST_LONG_RUN": (0.68, 0.82, 1.00),
}


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def unique(rows, key: str, label: str):
    values = [row[key] for row in rows]
    require(len(values) == len(set(values)), f"duplicate {label}")
    return set(values)


def require(condition: bool, message: str):
    if not condition:
        raise SystemExit(f"VERIFY FAILED: {message}")


def read_varint(data: bytes, offset: int) -> tuple[int, int]:
    value = 0
    shift = 0
    while offset < len(data) and shift <= 63:
        byte = data[offset]
        offset += 1
        value |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return value, offset
        shift += 7
    raise ValueError("invalid varint")


def top_level_signature(data: bytes) -> str | None:
    offset = 0
    while offset < len(data):
        tag, offset = read_varint(data, offset)
        field, wire = tag >> 3, tag & 7
        if wire == 0:
            _, offset = read_varint(data, offset)
        elif wire == 1:
            offset += 8
        elif wire == 5:
            offset += 4
        elif wire == 2:
            length, offset = read_varint(data, offset)
            payload = data[offset:offset + length]
            require(len(payload) == length, "truncated structure carrier")
            offset += length
            if field == 9:
                return payload.decode("utf-8")
        else:
            raise ValueError("unsupported wire type")
    return None


def valid_range(value) -> bool:
    return (isinstance(value, dict)
            and all(field in value for field in ("min", "max", "step"))
            and all(isinstance(value[field], (int, float)) for field in ("min", "max", "step"))
            and value["min"] < value["max"]
            and 0 < value["step"] <= value["max"] - value["min"])


def aligned(value: float, origin: float, step: float) -> bool:
    position = (value - origin) / step
    return abs(position - round(position)) < 1e-6


def axle_range(knobs, left_key: str, right_key: str):
    left = knobs.get(left_key)
    right = knobs.get(right_key)
    if not valid_range(left) or not valid_range(right):
        return None
    if abs(left["step"] - right["step"]) > 1e-9:
        return None
    minimum = max(left["min"], right["min"])
    maximum = min(left["max"], right["max"])
    if (not minimum < maximum
            or not aligned(minimum, left["min"], left["step"])
            or not aligned(minimum, right["min"], right["step"])):
        return None
    return {"min": minimum, "max": maximum, "step": left["step"]}


def usable_parameters(knobs):
    result = {}
    direct = {
        "steerRatio": "STEERING_RATIO",
        "brakeBias": "BRAKE_BIAS",
        "diffPower": "DIFFERENTIAL_POWER",
        "diffCoast": "DIFFERENTIAL_COAST",
        "diffPreload": "DIFFERENTIAL_PRELOAD",
        "frontSpringRate": "SPRING_FRONT",
        "rearSpringRate": "SPRING_REAR",
        "frontBump": "SLOW_BUMP_FRONT",
        "rearBump": "SLOW_BUMP_REAR",
        "frontRebound": "SLOW_REBOUND_FRONT",
        "rearRebound": "SLOW_REBOUND_REAR",
        "frontCamber": "CAMBER_FRONT",
        "rearCamber": "CAMBER_REAR",
        "frontToe": "TOE_FRONT",
        "rearToe": "TOE_REAR",
        "tc": "TRACTION_CONTROL",
        "tc2": "TRACTION_CONTROL_2",
        "abs": "ABS",
        "frontRideHeight": "RIDE_HEIGHT_FRONT",
        "rearRideHeight": "RIDE_HEIGHT_REAR",
        "frontWing": "FRONT_AERO",
        "rearWing": "REAR_WING",
        "fuel": "FUEL",
    }
    for source_key, parameter_key in direct.items():
        value = knobs.get(source_key)
        if valid_range(value):
            result[parameter_key] = value
    for source_key, parameter_key in (
            ("frontARB", "ANTI_ROLL_BAR_FRONT"),
            ("rearARB", "ANTI_ROLL_BAR_REAR")):
        value = knobs.get(source_key)
        if valid_range(value) and value["min"] >= 1_000 and value["max"] >= 1_000:
            result[parameter_key] = value
    front_pressure = axle_range(knobs, "frontLeftTyrePressure", "frontRightTyrePressure")
    rear_pressure = axle_range(knobs, "rearLeftTyrePressure", "rearRightTyrePressure")
    if front_pressure:
        result["TYRE_PRESSURE_FRONT"] = front_pressure
    if rear_pressure:
        result["TYRE_PRESSURE_REAR"] = rear_pressure
    return result


def clamp(value: float, minimum: float, maximum: float) -> float:
    return max(minimum, min(maximum, value))


def fraction_for(key: str, mode: str, track: dict) -> float:
    pace, stability, endurance = MODE_FACTORS[mode]
    speed = track["speedDemand"]
    traction = track["tractionDemand"]
    braking = track["brakingDemand"]
    long_track = clamp((track["lengthMeters"] - 3_000.0) / 18_000.0, 0, 1)
    values = {
        "TYRE_PRESSURE_FRONT": 0.34 - 0.05 * speed + 0.02 * braking - 0.04 * endurance,
        "TYRE_PRESSURE_REAR": 0.33 - 0.04 * speed + 0.03 * traction - 0.04 * endurance,
        "CAMBER_FRONT": 0.44 - 0.14 * pace + 0.10 * endurance - 0.04 * speed,
        "CAMBER_REAR": 0.48 - 0.12 * pace + 0.12 * endurance - 0.03 * speed + 0.03 * stability,
        "TOE_FRONT": 0.45 - 0.12 * pace + 0.05 * stability + 0.02 * speed,
        "TOE_REAR": 0.50 + 0.15 * stability - 0.05 * pace + 0.05 * traction,
        "ABS": 0.26 + 0.28 * stability - 0.10 * pace + 0.12 * braking,
        "TRACTION_CONTROL": 0.12 + 0.42 * stability - 0.14 * pace + 0.16 * traction,
        "TRACTION_CONTROL_2": 0.12 + 0.42 * stability - 0.14 * pace + 0.16 * traction,
        "ENGINE_MAP": 0.70 + 0.18 * pace - 0.08 * endurance,
        "BRAKE_BIAS": 0.45 + 0.14 * stability - 0.06 * pace + 0.08 * braking,
        "BRAKE_PRESSURE": 0.76 + 0.08 * pace - 0.03 * braking,
        "FUEL": 0.08 + 0.12 * endurance + 0.25 * long_track,
        "DIFFERENTIAL_PRELOAD": 0.38 + 0.08 * pace + 0.06 * stability - 0.08 * traction,
        "DIFFERENTIAL_POWER": 0.36 + 0.08 * pace - 0.14 * stability - 0.08 * traction,
        "DIFFERENTIAL_COAST": 0.44 - 0.08 * pace + 0.14 * stability + 0.08 * braking,
        "STEERING_RATIO": 0.42 - 0.10 * pace + 0.10 * stability + 0.03 * speed,
        "ANTI_ROLL_BAR_FRONT": 0.38 + 0.08 * speed - 0.05 * traction + 0.04 * stability,
        "ANTI_ROLL_BAR_REAR": 0.36 + 0.08 * pace - 0.16 * stability + 0.04 * speed - 0.04 * traction,
        "SPRING_FRONT": 0.33 + 0.08 * speed - 0.04 * traction + 0.02 * pace,
        "SPRING_REAR": 0.31 + 0.06 * speed - 0.10 * traction + 0.04 * pace - 0.08 * stability,
        "RIDE_HEIGHT_FRONT": 0.25 - 0.08 * speed - 0.03 * pace + 0.06 * stability,
        "RIDE_HEIGHT_REAR": 0.28 - 0.07 * speed - 0.02 * pace + 0.08 * stability,
        "SLOW_BUMP_FRONT": 0.34 + 0.05 * speed - 0.04 * braking + 0.02 * stability,
        "SLOW_BUMP_REAR": 0.31 + 0.04 * speed - 0.07 * traction - 0.06 * stability,
        "SLOW_REBOUND_FRONT": 0.41 + 0.05 * speed + 0.03 * braking + 0.02 * stability,
        "SLOW_REBOUND_REAR": 0.38 + 0.04 * speed + 0.04 * traction - 0.06 * stability,
        "FRONT_AERO": 0.48 - 0.14 * speed + 0.03 * pace + 0.02 * stability,
        "REAR_WING": 0.45 - 0.16 * speed - 0.05 * pace + 0.22 * stability + 0.05 * traction,
    }
    return clamp(values[key], 0.08, 0.92)


def generated_value(definition: dict, fraction: float) -> float:
    minimum = definition["min"]
    maximum = definition["max"]
    step = definition["step"]
    raw = minimum + fraction * (maximum - minimum)
    clamped = clamp(raw, minimum, maximum)
    value = minimum + round((clamped - minimum) / step) * step
    return clamp(value, minimum, maximum)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    root = args.root.resolve()
    assets = root / "project/app/src/main/assets"

    catalog = load(assets / "catalog-0.8.1.json")
    thumbnails = load(assets / "vehicle-thumbnails-0.8.1.json")
    tracks = load(assets / "track-engineering-profiles-0.8.1.json")
    ranges_path = assets / "evo-carsetuplimits-0.8.1.json"
    ranges = load(ranges_path)
    carriers = load(assets / "structure-carriers-0.8.1.json")

    vehicles = catalog["vehicles"]
    layouts = catalog["layouts"]
    require(catalog["gameVersion"] == EXPECTED_VERSION, "catalog version")
    require(len(vehicles) == EXPECTED_VEHICLES, "vehicle count")
    require(len(layouts) == EXPECTED_LAYOUTS, "layout count")
    vehicle_ids = unique(vehicles, "id", "vehicle id")
    layout_ids = unique(layouts, "id", "layout id")
    require(all(row["exactLayoutVerified"] for row in layouts), "unverified exact layout identity")

    image_rows = thumbnails["vehicles"]
    require(len(image_rows) == EXPECTED_VEHICLES, "thumbnail count")
    require(unique(image_rows, "vehicleId", "thumbnail vehicle id") == vehicle_ids,
            "thumbnail/vehicle coverage")
    catalog_names = {row["id"]: row["name"] for row in vehicles}
    for row in image_rows:
        require(row["vehicleName"] == catalog_names[row["vehicleId"]],
                f"thumbnail name mismatch {row['vehicleId']}")
        require(row["thumbnailUrl"].startswith("https://assettocorsa.gg/wp-content/uploads/"),
                f"thumbnail host {row['vehicleId']}")
        require(row["sourceUrl"].startswith("https://assettocorsa.gg/wp-json/wp/v2/media/"),
                f"thumbnail source {row['vehicleId']}")

    track_rows = tracks["profiles"]
    require(tracks["gameVersion"] == EXPECTED_VERSION, "track profile version")
    require(len(track_rows) == EXPECTED_LAYOUTS, "track profile count")
    require(unique(track_rows, "id", "track profile id") == layout_ids,
            "track profile/layout coverage")
    for row in track_rows:
        require(row["lengthMeters"] > 0, f"track length {row['id']}")
        for key in ("speedDemand", "bumpDemand", "tractionDemand", "brakingDemand"):
            require(-1 <= row[key] <= 1, f"track demand {row['id']}/{key}")
        require(row["source"].startswith("https://"), f"track source {row['id']}")

    range_sha = hashlib.sha256(ranges_path.read_bytes()).hexdigest()
    require(range_sha == EXPECTED_RANGE_SHA256, "range dataset SHA-256")
    require(len(ranges) == EXPECTED_RANGE_IDENTITIES, "range identity count")
    range_identities = sum(bool(row.get("rangeKey")) for row in vehicles)
    signatures = sum(bool(row.get("signaturePrefix")) for row in vehicles)
    require(range_identities == EXPECTED_RANGE_IDENTITIES, "catalog range identity count")
    require(signatures == EXPECTED_BINARY_IDENTITIES, "catalog binary identity count")
    require(all(not row.get("rangeKey") or row["rangeKey"] in ranges for row in vehicles),
            "catalog range key missing in pinned dataset")

    invalid_range_profiles = []
    self_calc_ready_keys = []
    omitted_unverified_fields = []
    usable_by_range_key = {}
    for range_key, knobs in ranges.items():
        invalid = []
        for knob, value in knobs.items():
            if value is None:
                continue
            if not valid_range(value):
                invalid.append(knob)
        if invalid:
            invalid_range_profiles.append({"rangeKey": range_key, "fields": sorted(invalid)})
        usable = usable_parameters(knobs)
        usable_by_range_key[range_key] = usable
        if (len(usable) >= 4
                and "TYRE_PRESSURE_FRONT" in usable
                and "TYRE_PRESSURE_REAR" in usable):
            self_calc_ready_keys.append(range_key)
        omitted = list(invalid)
        for key in ("brakePressure", "engineMap"):
            if knobs.get(key) is not None:
                omitted.append(key)
        for key in ("frontARB", "rearARB"):
            value = knobs.get(key)
            if valid_range(value) and value["min"] < 1000:
                omitted.append(key)
        if omitted:
            omitted_unverified_fields.append({"rangeKey": range_key, "fields": sorted(set(omitted))})
    require(len(invalid_range_profiles) == 3, "malformed range profile count changed")
    require(len(self_calc_ready_keys) == EXPECTED_SELF_CALC_RANGE_IDENTITIES,
            "SELF CALC range profile count changed")

    require(carriers["schema"] == 1, "structure carrier schema")
    require(carriers["gameVersion"] == EXPECTED_VERSION, "structure carrier version")
    carrier_rows = carriers["vehicles"]
    require(len(carrier_rows) == EXPECTED_BUNDLED_CARRIERS, "bundled carrier count")
    unique(carrier_rows, "vehicleId", "carrier vehicle id")
    vehicle_by_id = {row["id"]: row for row in vehicles}
    for row in carrier_rows:
        require(row["vehicleId"] in vehicle_by_id, f"unknown carrier vehicle {row['vehicleId']}")
        require(row["asset"].startswith("structure-carriers/") and ".." not in row["asset"],
                f"unsafe carrier path {row['vehicleId']}")
        carrier_path = assets / row["asset"]
        require(carrier_path.is_file(), f"missing carrier {row['vehicleId']}")
        carrier_bytes = carrier_path.read_bytes()
        require(hashlib.sha256(carrier_bytes).hexdigest() == row["sha256"],
                f"carrier hash {row['vehicleId']}")
        signature = top_level_signature(carrier_bytes)
        require(signature == row["signature"], f"carrier signature manifest {row['vehicleId']}")
        require(signature.startswith(vehicle_by_id[row["vehicleId"]]["signaturePrefix"]),
                f"carrier vehicle identity {row['vehicleId']}")

    live_source = (root / "project/app/src/main/java/com/greenbuddy/acevosetupengineer/core/LiveSearchCoordinator.java")
    require("REQUIRED_ROUNDS = 2" in live_source.read_text(encoding="utf-8"),
            "LIVE round count is not exactly two")

    setup_mode_source = (root / "project/app/src/main/java/com/greenbuddy/acevosetupengineer/model/SetupMode.java")
    setup_mode_text = setup_mode_source.read_text(encoding="utf-8")
    require(all(mode in setup_mode_text for mode in MODE_FACTORS), "five-mode source contract")

    matrix = len(vehicles) * len(layouts) * EXPECTED_MODES
    require(matrix == 8520, "N x M x 5 request matrix size")
    self_calc_matrix = 0
    generated_value_checks = 0
    stability_comparisons = 0
    vehicle_by_range = {row.get("rangeKey"): row for row in vehicles if row.get("rangeKey")}
    for range_key in self_calc_ready_keys:
        parameters = usable_by_range_key[range_key]
        vehicle = vehicle_by_range[range_key]
        for track in track_rows:
            mode_values = {}
            for mode in MODE_FACTORS:
                generated = {}
                for parameter, definition in parameters.items():
                    value = generated_value(definition, fraction_for(parameter, mode, track))
                    require(definition["min"] - 1e-9 <= value <= definition["max"] + 1e-9,
                            f"generated range {range_key}/{track['id']}/{mode}/{parameter}")
                    require(aligned(value, definition["min"], definition["step"]),
                            f"generated step {range_key}/{track['id']}/{mode}/{parameter}")
                    generated[parameter] = value
                    generated_value_checks += 1
                if vehicle["id"] == "ford-mustang-gt3" and mode == "FAST_ATTACK":
                    require("TRACTION_CONTROL" in parameters, "Mustang TC range missing")
                    require(parameters["TRACTION_CONTROL"]["min"] <= 1
                            <= parameters["TRACTION_CONTROL"]["max"], "Mustang TC=1 outside range")
                    generated["TRACTION_CONTROL"] = 1.0
                    require(generated["TRACTION_CONTROL"] == 1.0, "Mustang FAST ATTACK TC policy")
                mode_values[mode] = generated
                self_calc_matrix += 1

            attack = mode_values["FAST_ATTACK"]
            control = mode_values["FAST_CONTROL"]
            for parameter, relation in (
                    ("REAR_WING", lambda stable, fast: stable >= fast),
                    ("TRACTION_CONTROL", lambda stable, fast: stable >= fast),
                    ("BRAKE_BIAS", lambda stable, fast: stable >= fast),
                    ("ANTI_ROLL_BAR_REAR", lambda stable, fast: stable <= fast)):
                if parameter in attack and parameter in control:
                    require(relation(control[parameter], attack[parameter]),
                            f"FAST CONTROL stability {range_key}/{track['id']}/{parameter}")
                    stability_comparisons += 1

    require(self_calc_matrix == EXPECTED_SELF_CALC_RANGE_IDENTITIES
            * EXPECTED_LAYOUTS * EXPECTED_MODES, "SELF CALC matrix size")
    exact_only_matrix = matrix - self_calc_matrix
    report = {
        "status": "verified",
        "gameVersion": EXPECTED_VERSION,
        "vehicles": len(vehicles),
        "vehicleThumbnails": len(image_rows),
        "binaryVehicleIdentities": signatures,
        "rangeVehicleIdentities": range_identities,
        "selfCalcReadyRangeIdentities": len(self_calc_ready_keys),
        "profilesWithExplicitlyOmittedUnverifiedFields": len(omitted_unverified_fields),
        "bundledStructureCarriers": len(carrier_rows),
        "invalidRangeProfiles": invalid_range_profiles,
        "layouts": len(layouts),
        "trackProfiles": len(track_rows),
        "modes": EXPECTED_MODES,
        "matrixCases": matrix,
        "selfCalcMatrixCases": self_calc_matrix,
        "exactOnlyMatrixCases": exact_only_matrix,
        "generatedValueChecks": generated_value_checks,
        "fastControlStabilityComparisons": stability_comparisons,
        "mustangFastAttackTc": 1,
        "rangeDatasetSha256": range_sha,
    }
    encoded = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(encoded, encoding="utf-8")
    print(encoded, end="")


if __name__ == "__main__":
    main()
