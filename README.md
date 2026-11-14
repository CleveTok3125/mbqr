# MBQR

Android QR scanner that hands a VietQR transfer to the MB Bank app and parses
common QR formats.

## What it does

1. Scans a QR code from the camera, a full-resolution capture, a gallery image,
   or one image shared from another app.
2. Builds `<bank> <account> <amount in thousands> [merchant name]` for VietQR.
3. Shows the transfer sheet by default. Confirming copies the text and opens
   `mbbank://?ad_dp=transfer`.
4. Shows parsed details for Wi-Fi, contacts, email, phone, SMS, location, ISBN,
   and links.
5. Clears the clipboard after the configured timer, or on task removal while the
   timer is active.

## Demo

<p>
  <img src="docs/demo-scanner.jpg" alt="Scanner screen" width="260">
  <img src="docs/demo-confirm.jpg" alt="Transfer confirmation" width="260">
  <img src="docs/demo-missing-mb.jpg" alt="MB Bank missing" width="260">
</p>
<p>
  <img src="docs/demo-unsupported-bank.jpg" alt="Unsupported bank" width="260">
  <img src="docs/demo-invalid-crc.jpg" alt="Invalid CRC" width="260">
  <img src="docs/demo-not-vietqr.jpg" alt="Link QR" width="260">
</p>

## Supported input

Only QR Code Model 2 symbols are decoded. Measured symbol limits, capture
guidance, and out-of-spec behaviour are in
[QR symbol support](docs/qr-symbol-support.md).

- VietQR with the NAPAS provider (`A000000727`).
- Wi-Fi (`WIFI:`), vCard/MECARD/BIZCARD, email, phone, SMS/MMS, location, ISBN,
  and HTTP(S) links.
- VND only; amounts below 5 VND are rejected.
- Any BIN in the bundled directory. The directory is generated from
  `https://api.vietqr.io/v2/banks`; the app never requests it at runtime.
- Formats are detected from specific to generic, so `mailto:` and `sms:` are not
  treated as links and `WIFI:` is not treated as text.
- Wi-Fi, contacts, email, phone, SMS, location, and ISBN are parsed and shown
  only; the app does not connect to networks or launch other apps.
- Invalid-CRC VietQR and plain text open a raw sheet that can copy/share.
  HTTP(S) payloads open a link sheet with a browser action. An unsupported bank
  keeps the parsed sheet and previews the raw payload for copy.

## Bank directory

`BankDirectory.kt` is generated from the public VietQR list:

```sh
python3 tools/update_banks.py
```

The file is committed. Builds do not access the network. The generator validates
BIN shape, control characters, duplicates, and minimum list size. A monthly
workflow updates it; the repository must allow Actions to open PRs, and PRs
opened with `GITHUB_TOKEN` may not start CI.

## Build

```sh
./gradlew :app:assembleDebug
```

Release builds use `keystore.properties` when present:

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

The release workflow reads `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
and `KEY_PASSWORD` from CI secrets instead.

## Testing

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Manual QR fixtures with expected results are in [`test-qr/`](test-qr/);
symbol-level cases are in [`test-qr/hard/`](test-qr/hard). The host decoder
benchmark lives in [`tools/qr_symbol_bench/`](tools/qr_symbol_bench/README.md).

## Release

```sh
git tag v0.1.0
git push origin v0.1.0
```

The workflow builds a signed `arm64-v8a` APK and publishes it with
`SHA256SUMS`.

Required repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`.

## Manual runs

- **CI**: Actions -> `ci` -> Run workflow.
- **Update banks**: Actions -> `update-banks` -> Run workflow; updates the
  directory PR when API data changes.
- **Release smoke**: Actions -> `release` -> Run workflow with `publish`
  disabled; uploads the signed APK and checksums without a release.
- `publish` on a manual run creates a release tagged `smoke-<run-number>`.

## Controls

- **Cài đặt**: six groups: Giao diện, Quét, Tương tác, Tích hợp, Riêng tư, Nâng
  cao. Tap a header to collapse it; Nâng cao starts collapsed.
- **Giao diện**: one shared frame/margin preview. Sets the frame outline (`Bo
  góc` default, `Góc`, `Vuông`), scan margin `0–100%` (default `50`), and margin
  outline (`Bo góc` default, `Vuông`). Margin controls need **Chỉ quét trong
  khung**.
- **Quét**: **Chỉ quét trong khung** (off by default).
- **Tương tác**: **Rung** (default on).
- **Tích hợp**: **Hỏi trước khi mở MB Bank** (default on).
- **Riêng tư**: masking (`Tắt` / `Nhẹ` / `Toàn bộ`, default `Tắt`) and clipboard
  expiry (`0` = off, otherwise `15–300s` in 15s steps, default `60`).
- **Nâng cao**: **Lọc ký tự không an toàn** (default on), **Bật copy nâng cao**
  (default off, adds **Hex** / **Base64** actions), **Hiện thông tin xử lý**
  (decode overlay, default off), **Quét trực tiếp** (decoding, resolution, frame
  rate), and **Quét ảnh** (still decoding).
- **Preset giải mã**: independent `Nhanh` / `Tối đa`; live defaults to `Nhanh`,
  still images to `Tối đa`.
- **Độ phân giải**: default `1280×720`; `480p` / `720p` / `1080p`, plus `Khác`
  with an exact supported size. Unsupported sizes use the closest available one.
- **Tốc độ**: default `24` FPS; `10 / 24 / 30 / 60` plus `Khác` (`1–max`).
  Options above the sensor limit are disabled.
- **Camera**: torch, gallery/shared-image scan, arc zoom dial (`1x` to max with
  haptics when **Rung** is on), and a full-resolution shutter.
- **Clipboard pill**: tap to copy again and restart the timer; long-press to
  clear.
- Rows that differ from the factory value expose **Mặc định**. The overflow menu
  holds **Thu gọn tất cả**, **Mở rộng tất cả**, **Thông tin**, and **Khôi phục
  mặc định**.

## Behavior

### Scanning

- **Chỉ quét trong khung** is off by default. When on, live frames are cropped to
  the frame plus margin; shutter, gallery, and shared images scan the full image.
- **Kiểu khung** and **Kiểu lề** change outlines only. The crop region and
  decoded area stay the same.
- **Rung** covers the zoom dial, shutter, and clipboard copy, restore, and clear.
- Shared images use the sender's URI once; no gallery or storage permission is
  requested.
- The merchant name is appended only when present; MB resolves the recipient.

### Sanitizing

- Runs after parsing: TLV lengths and the VietQR CRC still use the original
  payload, then displayed values and the raw block are cleaned.
- Removes control characters (except line feeds and tabs), bidi controls,
  zero-width characters, BOM, soft hyphens, and unpaired surrogates; keeps
  visible characters, Vietnamese diacritics, and emoji.
- Copy, share, and handoff use cleaned text. Raw payloads can change because
  characters are removed; **Hex** and **Base64** encode the payload exactly as
  decoded, before normalization, sanitizing, or masking.

### Sensitive fields

- Three masking levels: `0` off, `1` light, `2` full; default off.
- Level `1` hides direct secrets and identifiers; level `2` also hides personal
  context. Raw payload masking follows the parsed fields.
- Tap a masked value or eye icon to toggle; long-press copies the real value.
  Sheets reopen with masked values hidden.
- See [Sensitive data masking](docs/sensitive-data-masking.md) for the field
  matrix.

### MB handoff and clipboard

- Without MB Bank, the parsed sheet still offers copy, share, and scan again.
- Confirmation is on by default. The clipboard is written before MB opens; a
  non-zero timer starts then. With confirmation off, copy and MB open
  immediately.
- If MB does not foreground within 5 seconds, the paused screen offers **Mở MB
  Bank**; a second failure reopens the transfer sheet via **Mở xác nhận chuyển
  khoản**.
- Supported transfers copy only the bank prefix, account, amount, and merchant
  name. Amounts round to two decimals of the thousands value. Paste manually with
  a long-press; transfer content is manual.
- Timer `0` disables auto-clear and the foreground service. A non-zero timer
  clears on expiry or task removal. After handoff, the camera stops and shows the
  countdown; **Quét mã khác** restarts scanning.

### Demo

- Five taps on the version line in credits enable demo. It reflects clipboard
  and confirmation settings and draws the frame boundary, but cases are not
  filtered by the frame.
- Cycles transfer, missing-MB, unsupported-bank, invalid-CRC, Wi-Fi, contact, and
  link cases. With confirmation off, handoff-ready cases show only a toast and
  actions are no-ops. **Quét tiếp**, swipe down, or back closes.

### Privacy

- No internet permission, analytics, or background work beyond the clipboard
  timer. Parsing is local; links open only on tap.

## License

GPL-3.0. See `LICENSE`. ZXing-C++ is used under Apache-2.0.
