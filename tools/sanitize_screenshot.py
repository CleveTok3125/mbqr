#!/usr/bin/env python3
"""Replace a phone status bar with a neutral mock and strip image metadata.

Usage:
    python3 tools/sanitize_screenshot.py input.png output.png \\
        [--status-height PX] [--time 9:41]

The original pixels below the status bar are left untouched. The output is
re-encoded as RGB PNG so EXIF, XMP, ICC and similar metadata are dropped.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument(
        "--status-height",
        type=int,
        default=0,
        help="Height of the status bar in pixels (default: 8.4%% of width)",
    )
    parser.add_argument("--time", default="9:41")
    return parser.parse_args()


def status_height(width: int, requested: int) -> int:
    if requested > 0:
        return requested
    return max(48, round(width * 0.084))


def mock_bar(image: Image.Image, height: int, clock: str) -> Image.Image:
    width = image.width
    top = image.getpixel((0, 0))
    bottom = image.getpixel((0, min(height, image.height - 1)))

    bar = Image.new("RGB", (width, height))
    draw = ImageDraw.Draw(bar)
    for y in range(height):
        t = y / max(1, height - 1)
        draw.line(
            [(0, y), (width, y)],
            fill=tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3)),
        )

    inset = max(10, height // 5)
    text_size = max(12, int(height * 0.42))
    try:
        font = ImageFont.load_default(size=text_size)
    except TypeError:
        font = ImageFont.load_default()

    draw.text((inset, height // 2), clock, font=font, fill="white", anchor="lm")

    right = width - inset
    # Battery
    battery_w, battery_h = int(height * 0.58), int(height * 0.30)
    bx = right - battery_w
    by = (height - battery_h) // 2
    draw.rounded_rectangle(
        [bx, by, bx + battery_w, by + battery_h],
        radius=max(2, battery_h // 4),
        outline="white",
        width=max(2, height // 28),
    )
    draw.rectangle(
        [bx + battery_w + 2, by + battery_h // 3, bx + battery_w + 6, by + 2 * battery_h // 3],
        fill="white",
    )
    fill_w = int(battery_w * 0.72)
    draw.rectangle(
        [bx + battery_w // 8, by + battery_h // 4, bx + battery_w // 8 + fill_w, by + 3 * battery_h // 4],
        fill="white",
    )

    # Wi-Fi
    wifi_cx = bx - int(height * 0.42)
    wifi_cy = by + battery_h
    for i, radius in enumerate((int(height * 0.32), int(height * 0.22), int(height * 0.12))):
        draw.arc(
            [wifi_cx - radius, wifi_cy - radius, wifi_cx + radius, wifi_cy + radius],
            200,
            340,
            fill="white",
            width=max(2, height // 32),
        )
    draw.ellipse(
        [wifi_cx - 3, wifi_cy - 6, wifi_cx + 3, wifi_cy],
        fill="white",
    )

    # Signal bars
    signal_right = wifi_cx - int(height * 0.42)
    bar_w = max(4, height // 16)
    gap = max(3, height // 24)
    for i in range(4):
        h = int(height * (0.18 + i * 0.09))
        x1 = signal_right - (4 - i) * (bar_w + gap)
        draw.rectangle(
            [x1, height // 2 + int(height * 0.18) - h, x1 + bar_w, height // 2 + int(height * 0.18)],
            fill="white",
        )

    return bar


def main() -> None:
    args = parse_args()
    with Image.open(args.input) as source:
        source.load()
        image = source.convert("RGB")

    height = status_height(image.width, args.status_height)
    bar = mock_bar(image, height, args.time)
    image.paste(bar, (0, 0))

    # Re-encode a fresh RGB image so all metadata blocks are discarded.
    clean = Image.new("RGB", image.size)
    clean.paste(image)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    clean.save(args.output, format="PNG", optimize=True)
    print(f"wrote {args.output} ({clean.width}x{clean.height}, status bar {height}px)")


if __name__ == "__main__":
    main()
