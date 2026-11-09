# MBQR

Android QR scanner that hands a VietQR transfer to the MB Bank app.

## What it does

1. Scans a VietQR (NAPAS EMVCo) code with CameraX and ZXing from the camera,
   a full-resolution capture, or a gallery image.
2. Builds the text that the MB transfer hub understands:
   `MB <account> <amount in thousands> [recipient name]`.
3. Copies that text to the clipboard and opens `mbbank://?ad_dp=transfer`.
4. Clears the clipboard after 60 seconds, or immediately when the app is
   removed from recents.

## Demo

Screenshot from demo mode, with the system status bar hidden:

<p>
  <img src="docs/scanner.png" alt="Scanner screen" width="320">
</p>

## Supported input

- VietQR payloads with the NAPAS provider (`A000000727`).
- VND currency only. Amounts below 5 VND are rejected.
- MB Bank BIN `970422`. Other BINs are shown as a raw payload with a copy
  button instead of being handed off.

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
- **Release smoke**: Actions -> `release` -> Run workflow with `publish`
  disabled. The signed APK and checksums are uploaded as artifacts without
  creating a release.
- Enabling `publish` on a manual run creates a release tagged
  `smoke-<run-number>`.

## Controls

- **Thông tin**: app credits, version, source, and license.
- **Đèn**: torch. **Ảnh**: scan an image from the gallery.
- **Arc dial**: CameraX zoom. Drags below the arc are mirrored. Haptics tick at
  minor marks and stronger at `1x/2x/4x/8x`.
- **Shutter**: full-resolution capture for distant codes.
- **Clipboard pill**: tap to copy the last text again and restart the 60-second
  timer; long-press to clear the app-managed entry immediately.

## Behavior notes

- Scanning starts as soon as camera permission is granted.
- Raw payloads that are not a supported VietQR can be copied, shared, or
  opened in a browser when the content is a plain HTTP(S) URL.
- Demo mode: tap the version line five times in the credits dialog. It hides
  the system status bar, shows the full controls over a static background, and
  loops the clipboard countdown. The gallery button only shows a demo message.
- The amount is rounded to two decimal places of the thousands value.
- After a successful handoff the camera stops and a clipboard countdown is
  shown; tap **Quét mã khác** to scan another code.
- Paste the copied text into the MB hub with a long-press. Transfer content is
  entered manually.
- The foreground service only runs while the clipboard entry is alive. It stops
  when the one-minute timer expires or when the task is removed from recents.
- No internet permission, no analytics, no background jobs.
