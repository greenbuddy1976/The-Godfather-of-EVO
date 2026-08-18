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
EXPECTED_MODES = 4
EXPECTED_RANGE_IDENTITIES = 68
EXPECTED_BINARY_IDENTITIES = 69
UNWRITABLE_BINARY_KEYS = {"frontARB", "rearARB", "brakePressure", "engineMap"}


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def unique(rows, key: str, label: str):
    values = [row[key] for row in rows]
    require(len(values) == len(set(values)), f"duplicate {label}")
    return set(values)


def require(condition: bool, message: str):
    if not condition:
        raise SystemExit(f"VERIFY FAILED: {message}")


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
    mapping_blocked_profiles = []
    self_calc_ready_keys = []
    for range_key, knobs in ranges.items():
        invalid = []
        for knob, value in knobs.items():
            if value is None:
                continue
            if (not isinstance(value, dict)
                    or not all(field in value for field in ("min", "max", "step"))
                    or not value["min"] < value["max"]
                    or not 0 < value["step"] <= value["max"] - value["min"]):
                invalid.append(knob)
        if invalid:
            invalid_range_profiles.append({"rangeKey": range_key, "fields": sorted(invalid)})
        elif any(knobs.get(key) is not None for key in UNWRITABLE_BINARY_KEYS):
            mapping_blocked_profiles.append(range_key)
        else:
            self_calc_ready_keys.append(range_key)
    require(len(invalid_range_profiles) == 3, "malformed range profile count changed")
    require(len(mapping_blocked_profiles) == 36, "unsafe binary mapping profile count changed")
    require(len(self_calc_ready_keys) == 29, "SELF CALC ready range profile count changed")

    matrix = len(vehicles) * len(layouts) * EXPECTED_MODES
    require(matrix == 6816, "N x M x 4 matrix size")
    report = {
        "status": "verified",
        "gameVersion": EXPECTED_VERSION,
        "vehicles": len(vehicles),
        "vehicleThumbnails": len(image_rows),
        "binaryVehicleIdentities": signatures,
        "rangeVehicleIdentities": range_identities,
        "selfCalcReadyRangeIdentities": len(self_calc_ready_keys),
        "selfCalcBlockedBinaryMappingIdentities": len(mapping_blocked_profiles),
        "invalidRangeProfiles": invalid_range_profiles,
        "layouts": len(layouts),
        "trackProfiles": len(track_rows),
        "modes": EXPECTED_MODES,
        "matrixCases": matrix,
        "rangeDatasetSha256": range_sha,
    }
    encoded = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(encoded, encoding="utf-8")
    print(encoded, end="")


if __name__ == "__main__":
    main()
