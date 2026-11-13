# Test QR codes

Synthetic QR codes for manual testing.

| File | Content | Expected result |
| --- | --- | --- |
| [`01-vietqr-valid.png`](01-vietqr-valid.png) | VietQR, amount 10,000 VND | Transfer sheet, or `Chưa cài MB Bank` if MB is absent |
| [`02-vietqr-invalid-crc.png`](02-vietqr-invalid-crc.png) | VietQR with bad CRC | Raw sheet, `CRC không hợp lệ` |
| [`03-vietqr-unsupported-bank.png`](03-vietqr-unsupported-bank.png) | VietQR with BIN `999999` | Parsed sheet, `Chưa hỗ trợ ngân hàng` |
| [`04-wifi-wpa.png`](04-wifi-wpa.png) | Wi-Fi WPA | Wi-Fi details |
| [`05-wifi-open.png`](05-wifi-open.png) | Wi-Fi open | Wi-Fi details, no password |
| [`06-wifi-hidden.png`](06-wifi-hidden.png) | Wi-Fi hidden WPA | Wi-Fi details, `Mạng ẩn: Có` |
| [`07-wifi-wep.png`](07-wifi-wep.png) | Wi-Fi WEP | Wi-Fi details, security `WEP` |
| [`08-vcard.png`](08-vcard.png) | vCard | Contact details |
| [`09-mecard.png`](09-mecard.png) | MECARD | Contact details |
| [`10-email.png`](10-email.png) | `mailto:` | Email details |
| [`11-phone.png`](11-phone.png) | `tel:` | Phone details |
| [`12-sms.png`](12-sms.png) | `sms:` | SMS details |
| [`13-geo.png`](13-geo.png) | `geo:` | Location details |
| [`14-isbn.png`](14-isbn.png) | `ISBN:` | ISBN details |
| [`15-url.png`](15-url.png) | HTTPS URL | Link details with `Mở liên kết` |
| [`16-text.png`](16-text.png) | Plain text | Raw sheet |

At masking level `1`, direct identifiers and the transfer command show
`••••••••`. Names, subjects, and links stay visible. The app masks a raw payload
at level `1` only when it also masks a parsed field. At level `2`, the app also
masks personal context and all raw payloads.

Tap a value or its eye icon to toggle visibility. Long-press a field to copy its
real value without revealing the value. At level `0`, all fields stay visible.
See [`../docs/sensitive-data-masking.md`](../docs/sensitive-data-masking.md) for
the full field matrix.

Check that long-press on each parsed field copies its value. On
`01-vietqr-valid.png`, long-press `Số tiền` and confirm the clipboard contains
`10000`, not `10.000 ₫`.

## Hard still-image cases

These fixtures are synthetic and intentionally harder to detect. They exercise
the ZXing-C++ decoder used for live frames, imported images, captures, and
shared images.

| File | Content | Expected result |
| --- | --- | --- |
| [`hard/01-perspective.png`](hard/01-perspective.png) | `MBQR hard 01` | Raw sheet |
| [`hard/02-low-contrast.png`](hard/02-low-contrast.png) | `MBQR hard 02` | Raw sheet |
| [`hard/03-blur-noise.png`](hard/03-blur-noise.png) | `MBQR hard 03` | Raw sheet |
| [`hard/04-tight-crop.png`](hard/04-tight-crop.png) | `MBQR hard 04` | Raw sheet when the background differs from the finder-pattern color; undecodable when both match |
| [`hard/05-inverted.png`](hard/05-inverted.png) | `MBQR hard 05` | Raw sheet |
| [`hard/06-damaged.png`](hard/06-damaged.png) | `MBQR hard 06` | Raw sheet |
| [`hard/07-small-in-page.png`](hard/07-small-in-page.png) | `MBQR hard 07` | Raw sheet |
| [`hard/08-wobbly.png`](hard/08-wobbly.png) | `MBQR hard 08` | Raw sheet; wobbly hand-drawn modules; decodes with the `Tối đa` preset |
| [`hard/09-url-perspective.png`](hard/09-url-perspective.png) | HTTPS URL | Link sheet with `Mở liên kết` |
| [`hard/10-dots.png`](hard/10-dots.png) | `MBQR hard 10 dots` | Raw sheet; dot-style modules |
| [`hard/11-logo-center.png`](hard/11-logo-center.png) | `MBQR hard 11 logo center` | Raw sheet; centered logo |
| [`hard/12-logo-part.png`](hard/12-logo-part.png) | `MBQR hard 12 logo part` | Raw sheet; logo occludes part of the data area |
| [`hard/13-curved.png`](hard/13-curved.png) | `MBQR hard 13 curved` | Raw sheet; curved/shaded surface; unreliable from the live camera, decodes easily when imported |
| [`hard/14-high-density.png`](hard/14-high-density.png) | Long text payload | Raw sheet; dense QR on a tilted page |
| [`hard/15-very-low-contrast.png`](hard/15-very-low-contrast.png) | `MBQR hard 15 low contrast` | Raw sheet; gradient illumination |
| [`hard/16-line-horizontal.png`](hard/16-line-horizontal.png) | `MBQR hard 16 line horizontal` | Raw sheet; horizontal line style |
| [`hard/17-line-vertical.png`](hard/17-line-vertical.png) | `MBQR hard 17 line vertical` | Raw sheet; vertical line style |
| [`hard/18-finder-styles.png`](hard/18-finder-styles.png) | `MBQR hard 18 finder styles` | Raw sheet; rounded, circle, and diamond finder patterns |
| [`hard/19-alpha-colorful.png`](hard/19-alpha-colorful.png) | `MBQR hard 19 alpha overlay` | Raw sheet; QR under a semi-transparent multicolor overlay |

`hard/04-tight-crop.png` has no quiet zone. It only fails when the display
background matches the finder-pattern color; with a different background it is
decodable. This is a limitation of the fixture, not the decode pipeline.

`hard/13-curved.png` is unreliable from the live camera but decodes easily when
the same image is imported. This is a current limitation of the live pipeline;
making live decoding reliable for this case requires a proprietary barcode SDK.

Automated parser tests use the same payloads. Update both when changing a
fixture.
