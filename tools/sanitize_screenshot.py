#!/usr/bin/env python3
"""Re-encode a screenshot as a metadata-free PNG.

Usage:
    python3 tools/sanitize_screenshot.py input.jpg output.png

The image is copied pixel for pixel into a fresh RGB PNG, so EXIF, XMP, ICC,
JFIF and similar metadata blocks are dropped. No visual processing is done.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    with Image.open(args.input) as source:
        source.load()
        image = source.convert("RGB")

    clean = Image.new("RGB", image.size)
    clean.paste(image)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    clean.save(args.output, format="PNG", optimize=True)
    print(f"wrote {args.output} ({clean.width}x{clean.height})")


if __name__ == "__main__":
    main()
