package vn.io.elsutm.mbqr.ui

internal data class DemoScanCase(
    val reason: String,
    val raw: String,
    val parsed: Boolean,
    val mbInstalled: Boolean,
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
        parsed = true,
        mbInstalled = true,
    ),
    DemoScanCase(
        reason = "Chưa cài MB Bank",
        raw = "00020101021138580010A000000727012800069704220114000000000000010208" +
            "QRIBFTTA53037045802VN5405100005915NGUYEN VAN MUOI62080804DEMO6304B5C2",
        parsed = true,
        mbInstalled = false,
    ),
    DemoScanCase(
        reason = "Chưa hỗ trợ ngân hàng",
        raw = "00020101021138580010A000000727012800069999990114000000000000010208" +
            "QRIBFTTA53037045802VN5405100005915NGUYEN VAN MUOI62080804DEMO630440DC",
        parsed = true,
        mbInstalled = false,
    ),
    DemoScanCase(
        reason = "CRC không hợp lệ",
        raw = "00020101021138580010A000000727012800069704220114000000000000010208" +
            "QRIBFTTA53037045802VN5405100005915NGUYEN VAN MUOI62080804DEMO6304B5C1",
        parsed = false,
        mbInstalled = false,
    ),
    DemoScanCase(
        reason = "Không phải VietQR",
        raw = "https://example.com/demo",
        parsed = false,
        mbInstalled = false,
    ),
)
