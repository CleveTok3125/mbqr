# Sensitive data masking

QR result sheets can hide selected values before display. Masking affects the
screen only: copy, share, MB Bank handoff, and link actions use the real values.

## Levels

| Level | Name | Behavior |
| --- | --- | --- |
| `0` | Off | Show all parsed values |
| `1` | Light | Hide direct secrets and identifiers; names and context stay visible |
| `2` | Full | Hide direct secrets, identifiers, and personal context |

Default is level `0`.

## Field groups

### Always visible

- `Ngân hàng`
- Wi-Fi `Bảo mật`, `Mạng ẩn`, `EAP`, `Phase 2`
- `ISBN`

### Personal context: masked at level 2

- VietQR `Tên đơn vị`
- Wi-Fi `Tên mạng`
- Contact `Tên`, `Biệt danh`, `Tổ chức`, `Chức danh`, `URL`
- Email and SMS `Tiêu đề`
- Phone `Tên`
- Location `Độ cao`, `Truy vấn`
- URL `Tiêu đề`, `Liên kết`

### Direct secrets and identifiers: masked from level 1

- VietQR `Tài khoản`, `Số tiền`, `Nội dung`
- Transfer command block `Lệnh chuyển khoản`
- Wi-Fi `Mật khẩu`, `Danh tính`, `Danh tính ẩn`
- Contact `Điện thoại`, `Email`, `Địa chỉ`, `Ghi chú`, `Sinh nhật`, `Nhắn tin`, `Vị trí`
- Email `Đến`, `CC`, `BCC`, `Nội dung`
- Phone `Số điện thoại`
- SMS `Đến`, `Nội dung`
- Location `Vĩ độ`, `Kinh độ`

### Raw payload

Raw payloads follow the parsed fields: visible at level `0`, masked at level
`2`, and at level `1` masked only when a parsed field is also masked. Text-only
payloads have no parsed fields, so they stay visible at level `1`.

## Interactions

- Tap a masked value or its eye icon to toggle visibility.
- Long-press a field to copy its real value without changing visibility.
- Sheets reopen with masked values hidden.
- Level `0` removes masks and eye icons.

## Limitations

- Masking is not encryption. Values still exist in the app process and in the
  parsed result.
- Clipboard writes, sharing, MB Bank launch, and link actions use the real value.
- Android 13 and later hide the clipboard preview for copied values.

## Related settings

The setting is `Mức ẩn dữ liệu nhạy cảm` with options `Tắt`, `Nhẹ`, and
`Toàn bộ`.
