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

Check that long-press on each parsed field copies its value. On
`01-vietqr-valid.png`, long-press `Số tiền` and confirm the clipboard contains
`10000`, not `10.000 ₫`.

Automated parser tests use the same payloads. Update both when changing a
fixture.
