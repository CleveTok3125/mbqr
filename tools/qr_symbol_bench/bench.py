#!/usr/bin/env python3
"""Measure the QR symbol envelope of the zxing-cpp decoder used by the app.

The Android build bundles zxing-cpp 3.1.1, so this script pins the same version.
It generates QR symbols with the `qrcode` package, applies synthetic capture
degradations, and records decode results. Images are generated on the fly and
never committed.

The Python binding of 3.1.1 exposes try_rotate, try_downscale, and try_invert
but not try_harder or try_denoise, so the `maximum` preset here approximates the
app's white padding and 2x upscale without denoise.
"""

from __future__ import annotations

import argparse
import csv
import io
import random
import statistics
import time
from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np
import qrcode
import zxingcpp
from PIL import Image, ImageEnhance, ImageFilter, ImageOps
from qrcode.constants import ERROR_CORRECT_H, ERROR_CORRECT_L, ERROR_CORRECT_M, ERROR_CORRECT_Q
from qrcode.exceptions import DataOverflowError

EC_LEVELS = {
    "L": ERROR_CORRECT_L,
    "M": ERROR_CORRECT_M,
    "Q": ERROR_CORRECT_Q,
    "H": ERROR_CORRECT_H,
}

ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 :/-._"


def payload(length: int, seed: int = 7) -> str:
    rng = random.Random(seed)
    return "".join(rng.choice(ALPHABET) for _ in range(length))


def render(version: int, ec: str, module_px: int, quiet: int, text: str) -> Image.Image:
    code = qrcode.QRCode(
        version=version,
        error_correction=EC_LEVELS[ec],
        box_size=module_px,
        border=quiet,
    )
    code.add_data(text.encode("ascii"))
    code.make(fit=False)
    return code.make_image(fill_color="black", back_color="white").convert("L")


def capacity(version: int, ec: str) -> int:
    """Largest byte-mode payload that fits, found by binary search."""
    low, high = 0, 3000
    while low < high:
        middle = (low + high + 1) // 2
        try:
            render(version, ec, 1, 0, payload(middle))
        except DataOverflowError:
            high = middle - 1
        else:
            low = middle
    return low


@dataclass(frozen=True)
class Case:
    phase: str
    version: int
    ec: str
    module_px: int
    quiet: int
    fill: int
    transform: str
    preset: str


def degrade(image: Image.Image, transform: str) -> Image.Image:
    if transform == "none":
        return image
    if transform.startswith("rotate"):
        angle = float(transform.removeprefix("rotate"))
        return image.rotate(angle, expand=True, fillcolor=255, resample=Image.BICUBIC)
    if transform.startswith("perspective"):
        percent = float(transform.removeprefix("perspective")) / 100.0
        array = np.array(image)
        height, width = array.shape[:2]
        shift_x, shift_y = width * percent, height * percent
        source = np.float32([[0, 0], [width, 0], [width, height], [0, height]])
        target = np.float32(
            [
                [shift_x, shift_y],
                [width - shift_x, 0],
                [width, height - shift_y],
                [0, height],
            ],
        )
        matrix = cv2.getPerspectiveTransform(source, target)
        warped = cv2.warpPerspective(
            array,
            matrix,
            (width, height),
            borderValue=255,
            flags=cv2.INTER_LINEAR,
        )
        return Image.fromarray(warped)
    if transform.startswith("blur"):
        return image.filter(ImageFilter.GaussianBlur(float(transform.removeprefix("blur"))))
    if transform.startswith("jpeg"):
        quality = int(transform.removeprefix("jpeg"))
        buffer = io.BytesIO()
        image.save(buffer, format="JPEG", quality=quality)
        buffer.seek(0)
        return Image.open(buffer).convert("L")
    if transform.startswith("contrast"):
        factor = float(transform.removeprefix("contrast"))
        return ImageEnhance.Contrast(image).enhance(factor)
    if transform.startswith("occlude"):
        percent = int(transform.removeprefix("occlude"))
        array = np.array(image).copy()
        height, width = array.shape[:2]
        side = int((width * height * percent / 100.0) ** 0.5)
        left, top = (width - side) // 2, (height - side) // 2
        array[top : top + side, left : left + side] = 255
        return Image.fromarray(array)
    if transform.startswith("damage"):
        percent = int(transform.removeprefix("damage"))
        array = np.array(image).copy()
        height, width = array.shape[:2]
        rng = random.Random(11)
        budget = int(width * height * percent / 100.0)
        while budget > 0:
            side = rng.randint(max(2, width // 60), max(3, width // 25))
            left, top = rng.randint(0, width - side), rng.randint(0, height - side)
            array[top : top + side, left : left + side] = 0
            budget -= side * side
        return Image.fromarray(array)
    if transform == "invert":
        return ImageOps.invert(image)
    if transform.startswith("downscale"):
        factor = int(transform.removeprefix("downscale"))
        return image.resize(
            (max(1, image.width // factor), max(1, image.height // factor)),
            Image.LANCZOS,
        )
    raise ValueError(f"unknown transform: {transform}")


def pad_white(image: Image.Image) -> Image.Image:
    """Mirror the app's Tối đa padding: max(16px, 6% of the longest side)."""
    border = max(16, int(max(image.width, image.height) * 0.06))
    padded = Image.new("L", (image.width + border * 2, image.height + border * 2), 255)
    padded.paste(image, (border, border))
    return padded


def scale_up(image: Image.Image) -> Image.Image:
    """Mirror the app's 2x upscale capped at 2048 px."""
    longest = max(image.width, image.height)
    if longest >= 2048:
        return image
    target = min(longest * 2, 2048)
    if target / longest < 1.25:
        return image
    scale = target / longest
    return image.resize(
        (max(1, int(image.width * scale)), max(1, int(image.height * scale))),
        Image.BICUBIC,
    )


def decode(image: Image.Image, preset: str, expected: str) -> tuple[bool, float]:
    if preset == "maximum":
        image = scale_up(pad_white(image))

    started = time.perf_counter()
    found = zxingcpp.read_barcodes(
        image,
        formats=zxingcpp.BarcodeFormat.QRCode,
        try_rotate=True,
        try_invert=True,
        try_downscale=preset != "live",
    )
    elapsed_ms = (time.perf_counter() - started) * 1000.0

    text = found[0].text if found else None
    return text == expected, elapsed_ms


def build_cases() -> list[Case]:
    cases: list[Case] = []

    # Envelope: version x EC x module size at half payload.
    for version in (1, 2, 3, 5, 10, 15, 20, 25, 30, 35, 40):
        for ec in ("L", "M", "Q", "H"):
            for module_px in (1, 2, 3, 4, 6, 8):
                cases.append(Case("envelope", version, ec, module_px, 4, 50, "none", "fast"))

    # Quiet zone below the 4-module spec minimum.
    for version in (5, 15, 30, 40):
        for ec in ("M", "H"):
            for quiet in (0, 1, 2, 4):
                for transform in ("none", "blur1"):
                    cases.append(
                        Case("quiet-zone", version, ec, 3, quiet, 50, transform, "fast"),
                    )

    # Full-capacity payload: the hardest symbols per version.
    for version in (1, 5, 10, 20, 30, 40):
        for ec in ("L", "H"):
            for module_px in (1, 2, 3, 4):
                cases.append(Case("payload", version, ec, module_px, 4, 100, "none", "fast"))

    # Capture degradations on a mid-size symbol at camera-like module sizes.
    transforms = (
        "rotate90",
        "rotate15",
        "rotate6",
        "perspective5",
        "perspective10",
        "perspective15",
        "perspective20",
        "perspective25",
        "blur0.5",
        "blur1",
        "blur1.5",
        "blur2",
        "blur2.5",
        "jpeg60",
        "jpeg30",
        "contrast0.5",
        "contrast0.3",
        "contrast0.2",
        "contrast0.1",
        "occlude5",
        "occlude10",
        "occlude15",
        "occlude20",
        "occlude25",
        "damage5",
        "damage10",
        "damage15",
        "damage20",
        "invert",
        "downscale2",
        "downscale3",
        "downscale4",
    )
    for module_px in (2, 3):
        for transform in transforms:
            cases.append(Case("degradation", 10, "M", module_px, 4, 50, transform, "fast"))

    # Error-correction headroom: same damage across EC levels.
    for ec in ("L", "M", "Q", "H"):
        for transform in ("occlude10", "occlude15", "occlude20", "damage5", "blur1.5"):
            cases.append(Case("ecc", 10, ec, 3, 4, 50, transform, "fast"))

    # Blur tolerance by module size.
    for module_px in (2, 3, 4, 6):
        for blur in ("blur0.5", "blur1", "blur1.5", "blur2", "blur3"):
            cases.append(Case("blur-scale", 10, "M", module_px, 4, 50, blur, "fast"))

    # Preset comparison on hard combinations.
    for version in (30, 40):
        for ec in ("M", "H"):
            for transform in ("none", "blur1", "downscale2", "occlude15"):
                for preset in ("fast", "maximum"):
                    cases.append(
                        Case("preset", version, ec, 2, 4, 50, transform, preset),
                    )

    return cases


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--out",
        type=Path,
        default=Path(__file__).with_name("results.csv"),
    )
    parser.add_argument("--dump-dir", type=Path, default=None)
    args = parser.parse_args()

    cases = build_cases()
    sizes: dict[tuple[int, str], int] = {}
    rows: list[dict[str, object]] = []

    if args.dump_dir:
        args.dump_dir.mkdir(parents=True, exist_ok=True)

    for index, case in enumerate(cases, start=1):
        key = (case.version, case.ec)
        if key not in sizes:
            sizes[key] = capacity(*key)
        length = max(1, sizes[key] * case.fill // 100)
        text = payload(length)

        image = render(case.version, case.ec, case.module_px, case.quiet, text)
        image = degrade(image, case.transform)
        ok, elapsed_ms = decode(image, case.preset, text)

        rows.append(
            {
                "phase": case.phase,
                "version": case.version,
                "ec": case.ec,
                "module_px": case.module_px,
                "quiet": case.quiet,
                "fill": case.fill,
                "transform": case.transform,
                "preset": case.preset,
                "payload_bytes": length,
                "decoded": int(ok),
                "elapsed_ms": round(elapsed_ms, 2),
            },
        )

        if args.dump_dir and not ok:
            image.save(args.dump_dir / f"{index:03d}-{case.phase}-{case.version}{case.ec}.png")

    with args.out.open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)

    summary(rows, sizes)


def summary(rows: list[dict[str, object]], sizes: dict[tuple[int, str], int]) -> None:
    def rate(items: list[dict[str, object]]) -> str:
        ok = sum(int(item["decoded"]) for item in items)
        return f"{ok}/{len(items)}"

    print("zlib zxing-cpp 3.1.1 host benchmark")
    print(f"cases: {len(rows)}")

    print("\ndecode rate by version x module px (50% payload, EC all):")
    versions = sorted({int(row["version"]) for row in rows if row["phase"] == "envelope"})
    modules = sorted({int(row["module_px"]) for row in rows if row["phase"] == "envelope"})
    print("version " + " ".join(f"{m}px".rjust(8) for m in modules))
    for version in versions:
        cells = []
        for module_px in modules:
            subset = [
                row
                for row in rows
                if row["phase"] == "envelope"
                and row["version"] == version
                and row["module_px"] == module_px
            ]
            cells.append(rate(subset).rjust(8))
        print(f"{version:>7} " + " ".join(cells))

    print("\nquiet zone (spec minimum is 4 modules, 3px/module, blur1 included):")
    for quiet in (0, 1, 2, 4):
        subset = [row for row in rows if row["phase"] == "quiet-zone" and row["quiet"] == quiet]
        print(f"  quiet={quiet}: {rate(subset)}")

    print("\nfull-capacity payload (100% of byte-mode capacity):")
    for module_px in (1, 2, 3, 4):
        subset = [
            row
            for row in rows
            if row["phase"] == "payload" and row["module_px"] == module_px
        ]
        print(f"  module={module_px}px: {rate(subset)}")

    print("\ndegradations (version 10, EC M, 50% payload):")
    print(f"  {'transform':<14} {'2px':>8} {'3px':>8}")
    transforms = sorted(
        {str(row["transform"]) for row in rows if row["phase"] == "degradation"}
    )
    for transform in transforms:
        cells = []
        for module_px in (2, 3):
            subset = [
                row
                for row in rows
                if row["phase"] == "degradation"
                and row["transform"] == transform
                and row["module_px"] == module_px
            ]
            cells.append(rate(subset).rjust(8))
        print(f"  {transform:<14} " + " ".join(cells))

    print("\nEC headroom (version 10, 3px/module):")
    print(f"  {'transform':<14} {'L':>5} {'M':>5} {'Q':>5} {'H':>5}")
    for transform in ("occlude10", "occlude15", "occlude20", "damage5", "blur1.5"):
        cells = []
        for ec in ("L", "M", "Q", "H"):
            subset = [
                row
                for row in rows
                if row["phase"] == "ecc"
                and row["transform"] == transform
                and row["ec"] == ec
            ]
            cells.append(rate(subset).rjust(5))
        print(f"  {transform:<14} " + " ".join(cells))

    print("\nblur tolerance by module size (version 10, EC M):")
    print(f"  {'module':<8} " + " ".join(f"{b:>8}" for b in ("0.5", "1", "1.5", "2", "3")))
    for module_px in (2, 3, 4, 6):
        cells = []
        for blur in ("0.5", "1", "1.5", "2", "3"):
            subset = [
                row
                for row in rows
                if row["phase"] == "blur-scale"
                and row["module_px"] == module_px
                and row["transform"] == f"blur{blur}"
            ]
            cells.append(rate(subset).rjust(8))
        print(f"  {module_px}px     " + " ".join(cells))

    print("\nfast vs maximum preset (2px/module):")
    for transform in ("none", "blur1", "downscale2", "occlude15"):
        fast = [
            row
            for row in rows
            if row["phase"] == "preset" and row["transform"] == transform and row["preset"] == "fast"
        ]
        maximum = [
            row
            for row in rows
            if row["phase"] == "preset"
            and row["transform"] == transform
            and row["preset"] == "maximum"
        ]
        print(f"  {transform:<11} fast {rate(fast)}  maximum {rate(maximum)}")

    timings = [float(row["elapsed_ms"]) for row in rows]
    print(f"\ndecode time: median {statistics.median(timings):.1f} ms, max {max(timings):.1f} ms")
    print("\nbyte-mode capacity (bytes):")
    for version in sorted({version for version, _ in sizes}):
        parts = [f"{ec}={sizes[(version, ec)]}" for ec in ("L", "M", "Q", "H")]
        print(f"  v{version:<2} " + " ".join(parts))


if __name__ == "__main__":
    main()
