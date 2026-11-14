# QR symbol support

Scope: this page describes which **QR symbols** the app can decode, meaning the
ISO/IEC 18004 QR Code structure. Payload formats are listed in
[Supported input](../README.md#supported-input); they are a separate concern.

The app decodes with `io.github.zxing-cpp:android:3.1.1` and only enables its
`QR_CODE` reader. The numbers below come from the host benchmark in
[`tools/qr_symbol_bench/`](../tools/qr_symbol_bench/README.md), which pins the
same `zxing-cpp` version and decodes generated bitmaps.

## Support levels

| Level | Meaning |
|---|---|
| Supported | QR Code Model 2 that meets the symbol structure and the measured capture envelope. Parsed, displayed, copied, and handed off like any other scan. |
| Best-effort | QR-like symbols outside the envelope or outside Model 2. The decoder may still read them; success depends on the image and is not promised. |
| Unsupported | Not decoded at all: other barcode symbologies, and payloads that no image of the symbol can carry. |

The benchmark sends generated bitmaps, not camera frames. Camera focus,
exposure, motion, rolling shutter, and screen moiré narrow these limits.
On-device validation with the fixtures in [`test-qr/hard/`](../test-qr/hard) is
the final check; it is manual.

## Measured decoder envelope

512 cases, `zxing-cpp` 3.1.1, `try_rotate` and `try_invert` on, `try_downscale`
on (the app disables it for live frames). `try_harder` and `try_denoise` are not
exposed by the Python binding, so the app's own settings can shift these
thresholds.

### Versions and error correction

Every combination below decoded with 100% success on clean bitmaps: versions
`1, 2, 3, 5, 10, 15, 20, 25, 30, 35, 40` at EC levels `L`, `M`, `Q`, `H`, and
module sizes `1, 2, 3, 4, 6, 8` px with a 4-module quiet zone and 50% payload.

Full byte-mode capacity also decoded at every tested module size of 1–4 px for
versions `1, 5, 10, 20, 30, 40` at EC `L` and `H`.

Byte-mode capacity, matching the ISO/IEC 18004 limits:

| Version | L | M | Q | H |
|---|---|---|---|---|
| 1 | 17 | 14 | 11 | 7 |
| 2 | 32 | 26 | 20 | 14 |
| 3 | 53 | 42 | 32 | 24 |
| 5 | 106 | 84 | 60 | 44 |
| 10 | 271 | 213 | 151 | 119 |
| 15 | 520 | 412 | 292 | 220 |
| 20 | 858 | 666 | 482 | 382 |
| 25 | 1273 | 997 | 715 | 535 |
| 30 | 1732 | 1370 | 982 | 742 |
| 35 | 2303 | 1809 | 1283 | 983 |
| 40 | 2953 | 2331 | 1663 | 1273 |

Decoding itself is not the limiting factor: the same symbols pass at 1 px per
module. Capture quality is what removes headroom.

### Capture degradations

Version 10, EC `M`, 50% payload, 4-module quiet zone. Values are pass/fail per
module size.

| Degradation | 2 px/module | 3 px/module |
|---|---|---|
| Rotate 6°, 15°, 90° | pass | pass |
| Invert (light on dark) | pass | pass |
| JPEG q30, q60 | pass | pass |
| Contrast 0.1–0.5 | pass | pass |
| Perspective 5–15% | pass | pass |
| Perspective 20% | fail | pass |
| Perspective 25% | fail | fail |
| Downscale to about 1 px/module | pass (2x) | pass (3x) |
| Downscale below 1 px/module | fail (3x, 4x) | fail (4x) |
| Gaussian blur 0.5 px | pass | pass |
| Gaussian blur 1 px | fail | pass |
| Gaussian blur 1.5 px and above | fail | fail |
| Centered occlusion 5–10% | pass | pass |
| Centered occlusion 15% and above | fail | fail |
| Random black damage 5% and above | fail | fail |

Blur tolerance scales with module size, roughly half a module:

| Module size | Largest passing blur |
|---|---|
| 2 px | 0.5 px |
| 3 px | 1 px |
| 4 px | 1.5 px |
| 6 px | 2 px |

Error correction raises the occlusion ceiling but not the black-damage one:

| Degradation, version 10, 3 px/module | L | M | Q | H |
|---|---|---|---|---|
| Centered occlusion 10% | fail | pass | pass | pass |
| Centered occlusion 15% | fail | fail | pass | pass |
| Centered occlusion 20% | fail | fail | fail | fail |
| Random black damage 5% | fail | fail | fail | fail |

Random black blocks can cover finder, timing, or alignment patterns, which are
outside error correction. White occlusion in the data area is recoverable up to
the EC budget.

### Presets

Version 30 and 40, EC `M` and `H`, 2 px/module. `Tối đa` adds about 6% white
padding and a 2x upscale retry; the host benchmark cannot enable denoise.

| Case | `Nhanh` | `Tối đa` |
|---|---|---|
| Clean | 4/4 | 4/4 |
| Blur 1 px | 0/4 | 2/4 |
| Downscale 2x | 4/4 | 4/4 |
| 15% occlusion | 2/4 | 2/4 |

Decode time on the host: median 0.4 ms, maximum 5.8 ms per attempt.

## Symbol structure limits

| Case | Behaviour |
|---|---|
| QR Code Model 2, versions 1–40, all EC levels and masks | Supported |
| Micro QR | Unsupported; only `QR_CODE` is enabled |
| QR Code Model 1 | Best-effort; the reader targets Model 2 |
| Mirrored symbol | Best-effort; mirror search is not enabled |
| Missing or short quiet zone | Best-effort; clean 0–4 module cases pass, and `Tối đa` pads still images |
| Structured append, up to 16 symbols | Unsupported as a whole; `maxNumberOfSymbols = 1` reads one symbol and never merges parts |
| Several symbols in one frame or image | Only the first result is used |
| Stylized symbols: dots, line art, custom finder patterns, logos, hand-drawn | Best-effort; success depends on preserved finder/timing/alignment patterns and contrast |
| Symbols damaged beyond the limits above | Best-effort up to the EC budget and pattern survival |
| 1D barcodes, DataMatrix, Aztec, PDF417 | Unsupported; other formats are not enabled |

## Capture guidance

- Keep the symbol at 3 px per module or more and above 1 px per module after any
  downscale. A version 40 symbol then needs roughly 550 px on its long side.
- Hold steady: blur is the most common failure. Half a module of blur is the
  approximate ceiling at each module size.
- Stay under 15% occlusion and 20% perspective. Do not let anything cover the
  three finder patterns.
- Use `Tối đa` for still images; live scanning uses `Nhanh`. Raise the analysis
  resolution only when the symbol is small in the frame.
- `Chỉ quét trong khung` crops live frames, which helps focus and cost, but the
  symbol must still sit inside the frame plus margin.

## Pipeline

- Live frames: read `ImageProxy` directly, no downscale, one symbol, `Nhanh`
  preset.
- Still images, `Nhanh`: decode the bitmap directly.
- Still images, `Tối đa`: pad with white, then decode with denoise, then retry
  with a 2x upscale capped at 2048 px.
- Defaults: 1280×720 analysis at 24 FPS; the resolution and rate are
  configurable under **Nâng cao → Quét trực tiếp**.

## Verification

- Synthetic symbol fixtures: `test-qr/hard/` covers perspective, low contrast,
  blur and noise, tight crop, inverted, damaged, small-in-page, wobbly, dot,
  logo, curved, high-density, line art, finder styles, and alpha color.
- Payload fixtures: `test-qr/` covers VietQR, Wi-Fi, contacts, and links.
- Regenerate the benchmark:

  ```sh
  cd tools/qr_symbol_bench
  python3 -m venv --system-site-packages .venv
  .venv/bin/pip install -r requirements.txt
  .venv/bin/python bench.py
  ```

- Device check: import each fixture through **Ảnh**, once with `Nhanh` and once
  with `Tối đa`, from both a gallery image and a shared image.
