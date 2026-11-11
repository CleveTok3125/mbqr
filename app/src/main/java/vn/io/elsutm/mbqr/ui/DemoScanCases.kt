package vn.io.elsutm.mbqr.ui

internal data class DemoScanCase(
    val reason: String,
    val raw: String,
    val mbInstalled: Boolean,
    val handoffReady: Boolean,
)

/**
 * Synthetic scan results for demo mode. The payloads use placeholder accounts
 * and the public example domain; none of them reference real recipients.
 */
internal val DEMO_SCAN_CASES = listOf(
    DemoScanCase(
        reason = "Xác nhận chuyển khoản",
        raw = "00020101021138580010A000000727012800069704220114000000000000010208" +
            "QRIBFTTA53037045802VN5405100005915NGUYEN VAN MUOI62080804DEMO6304B5C2",
        mbInstalled = true,
        handoffReady = true,
    ),
    DemoScanCase(
        reason = "Chưa cài MB Bank",
        raw = "00020101021138580010A000000727012800069704220114000000000000010208" +
            "QRIBFTTA53037045802VN5405100005915NGUYEN VAN MUOI62080804DEMO6304B5C2",
        mbInstalled = false,
        handoffReady = false,
    ),
    DemoScanCase(
        reason = "Chưa hỗ trợ ngân hàng",
        raw = "00020101021138580010A000000727012800069999990114000000000000010208" +
            "QRIBFTTA53037045802VN5405100005915NGUYEN VAN MUOI62080804DEMO630440DC",
        mbInstalled = false,
        handoffReady = false,
    ),
    DemoScanCase(
        reason = "CRC không hợp lệ",
        raw = "00020101021138580010A000000727012800069704220114000000000000010208" +
            "QRIBFTTA53037045802VN5405100005915NGUYEN VAN MUOI62080804DEMO6304B5C1",
        mbInstalled = false,
        handoffReady = false,
    ),
    DemoScanCase(
        reason = "Wi-Fi",
        raw = "WIFI:T:WPA;S:MBQR Demo;P:demo1234;H:false;;",
        mbInstalled = false,
        handoffReady = false,
    ),
    DemoScanCase(
        reason = "Danh bạ",
        raw = "BEGIN:VCARD\nVERSION:3.0\nFN:NGUYEN VAN MUOI\n" +
            "TEL:0900000000\nEMAIL:demo@example.com\nORG:MBQR\nEND:VCARD",
        mbInstalled = false,
        handoffReady = false,
    ),
    DemoScanCase(
        reason = "Liên kết",
        raw = "https://example.com/demo",
        mbInstalled = false,
        handoffReady = false,
    ),
)

internal fun DemoScanCase.shouldShowDemoSheet(confirmBeforeOpeningMb: Boolean): Boolean =
    confirmBeforeOpeningMb || !handoffReady
