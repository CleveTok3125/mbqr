# MBQR

Android QR scanner that hands a VietQR transfer to the MB Bank app.

## What it does

1. Scans a VietQR (NAPAS EMVCo) code with CameraX and ZXing from the camera,
   a full-resolution capture, or a gallery image.
2. Builds the text that the MB transfer hub understands:
   `<bank> <account> <amount in thousands> [merchant name]`.
3. Shows the transfer sheet by default. Confirming copies that text to the
   clipboard and opens `mbbank://?ad_dp=transfer`.
4. Clears the clipboard after the configured expiry, or immediately when the app is
   removed from recents.

## Demo

Screenshot from demo mode, with the system status bar hidden:

<p>
  <img src="docs/demo-scanner.jpg" alt="Scanner screen" width="260">
  <img src="docs/demo-confirm.jpg" alt="Transfer confirmation" width="260">
  <img src="docs/demo-missing-mb.jpg" alt="MB Bank missing" width="260">
</p>
<p>
  <img src="docs/demo-unsupported-bank.jpg" alt="Unsupported bank" width="260">
  <img src="docs/demo-invalid-crc.jpg" alt="Invalid CRC" width="260">
  <img src="docs/demo-not-vietqr.jpg" alt="Not a VietQR" width="260">
</p>

## Supported input

- VietQR payloads with the NAPAS provider (`A000000727`).
- VND currency only. Amounts below 5 VND are rejected.
- Any bank BIN in the bundled VietQR directory. The directory is generated
  from `https://api.vietqr.io/v2/banks`; the app never requests it at runtime.
- Raw payloads are shown only when the QR is not a supported VietQR, has an
  invalid CRC, or cannot be tied to a bank prefix.

## Bank directory

`BankDirectory.kt` is generated from the public VietQR bank list:

```sh
python3 tools/update_banks.py
```

The generated file is committed. Builds do not access the network, and the
app keeps no internet permission. The generator validates BIN shape, rejects
control/format characters and duplicate BINs, and refuses an unexpectedly
short bank list.

A scheduled workflow runs on the first day of each month and opens or updates
a pull request when the directory changes. The repository must allow GitHub
Actions to create pull requests. Pull requests opened with `GITHUB_TOKEN` may
not start `ci`; run it manually or push a follow-up commit when required.

## Build

```sh
./gradlew :app:assembleDebug
```

Release builds use `keystore.properties` when it exists:

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

The release workflow reads `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
and `KEY_PASSWORD` from CI secrets instead.

## Release

Push a tag:

```sh
git tag v0.1.0
git push origin v0.1.0
```

The workflow builds a signed `arm64-v8a` APK and publishes it with a
`SHA256SUMS` file.

Required repository secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## Manual runs

- **CI**: Actions -> `ci` -> Run workflow.
- **Update banks**: Actions -> `update-banks` -> Run workflow. It opens or
  updates the bank directory pull request if the API data changed.
- **Release smoke**: Actions -> `release` -> Run workflow with `publish`
  disabled. The signed APK and checksums are uploaded as artifacts without
  creating a release.
- Enabling `publish` on a manual run creates a release tagged
  `smoke-<run-number>`.

## Controls

- **Cài đặt**: confirmation switch for the MB handoff and clipboard expiry
  (15–300 seconds, default 60). **Thông tin** opens app credits, version,
  source, and license.
- **Đèn**: torch. **Ảnh**: scan an image from the gallery.
- **Arc dial**: CameraX zoom from `1x` to the camera maximum. Major ticks are
  powers of two plus the endpoints; minor ticks divide each interval. The final
  tail continues the same octave grid to the next power of two and clips at the
  maximum. Drags below the arc are mirrored. Haptics tick at minor marks and are
  stronger at major ticks, including the endpoints.
- **Shutter**: full-resolution capture for distant codes.
- **Clipboard pill**: tap to copy the last text again and restart the configured
  timer; long-press to clear the app-managed entry immediately.

## Behavior notes

- Scanning starts as soon as camera permission is granted.
- Raw payloads that are not a supported VietQR can be copied, shared, or
  opened in a browser when the content is a plain HTTP(S) URL.
- If MB Bank is not installed, a parsed transfer sheet is shown instead of a
  raw payload. It keeps the copy, share, and scan-again actions.
- Confirmation is enabled by default. The parsed transfer sheet is shown before
  the handoff. The clipboard is written and the configured timer starts only
  after **Mở MB Bank** is pressed, so the clipboard is ready before MB opens.
  Disable the switch to copy and open MB Bank immediately.
- The only name in the QR is the merchant name field. It may be the account
  holder name, a bank name, or absent. The app appends it as-is when present;
  MB resolves the actual recipient name separately.
- The copied text contains only the bank prefix, account, amount, and merchant
  name; content, city, MCC, and service code are not appended.
- Demo mode: tap the version line five times in the credits dialog. It hides
  the system status bar, shows the full controls over a static background, uses
  a `1x–25x` dial, and loops the clipboard countdown. The shutter and gallery
  buttons step through synthetic scan cases. Sheet actions are no-ops; **Quét
  tiếp**, swipe down, or back closes the sheet.
- The amount is rounded to two decimal places of the thousands value.
- After a successful handoff the camera stops and a clipboard countdown is
  shown; tap **Quét mã khác** to scan another code.
- Paste the copied text into the MB hub with a long-press. Transfer content is
  entered manually.
- The foreground service only runs while the clipboard entry is alive. It stops
  when the configured timer expires or when the task is removed from recents.
- No internet permission, no analytics, no background jobs.
