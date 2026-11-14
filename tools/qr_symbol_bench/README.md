# QR symbol benchmark

Measures the readable QR envelope of `zxing-cpp` 3.1.1, the decoder bundled in
the app. Symbols are generated on the fly; no image fixtures are stored.

```sh
python3 -m venv --system-site-packages .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python bench.py
```

`bench.py` writes `results.csv` next to the script and prints a summary.
Pass `--dump-dir <path>` to keep the symbols that failed.

Requires `qrcode`, `Pillow`, `numpy`, and `opencv-python` from the host Python
(`--system-site-packages`), plus the pinned `zxing-cpp` wheel.

Scope and limits:

- The Python binding of 3.1.1 has no `try_harder` or `try_denoise` switch, so
  the `maximum` preset approximates the app's white padding and 2x upscale
  without denoise.
- The benchmark decodes generated bitmaps, not camera frames, so focus,
  exposure, motion blur, and screen moiré are not represented.
