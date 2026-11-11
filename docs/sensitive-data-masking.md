# Sensitive data masking

QR result sheets can hide selected values before display. Masking affects the
screen only. Copy, share, MB Bank handoff, and link actions use the real values.

## Levels

| Level | Name | Behavior |
| --- | --- | --- |
| `0` | Off | Show all parsed values |
| `1` | Light | Hide direct secrets and identifiers while names and context remain visible |
| `2` | Full | Hide direct secrets and identifiers plus personal context |

The app defaults to level `0`. Level `2` hides all sensitive fields. This
includes names, subjects, and links.

## Staircase summary

`●` means visible. `×` means masked.

| Field group | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Always visible | ● | ● | ● |
| Personal context | ● | ● | × |
| Direct secrets and identifiers | ● | × | × |

The `×` cells show the masking boundary for each level. Raw payload masking
depends on the parsed fields. Level `0` always shows raw payloads. Level `2`
always masks them. At level `1`, the app masks a raw payload only when it also
masks a parsed field.

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

Level `0` always shows raw payloads. Level `2` always masks them. At level `1`,
the app masks a raw payload only when it also masks a parsed field. Text-only
payloads have no parsed fields, so they stay visible at level `1`.

## Interactions

- Tap a masked value or its eye icon to toggle visibility.
- Long-press a field to copy its real value without changing visibility.
- When you open a result sheet again, masked values start hidden.
- Level `0` removes masks and eye icons.

## Limitations

- Masking is not encryption. Values still exist in the app process and in the
  parsed result.
- Clipboard writes, sharing, MB Bank launch, and link actions use the real value.
- Android 13 and later hide the clipboard preview for copied values.

## Related settings

The setting is `Mức ẩn dữ liệu nhạy cảm` with options `Tắt`, `Nhẹ`, and
`Toàn bộ`.
