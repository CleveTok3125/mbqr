package vn.io.elsutm.mbqr.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.io.elsutm.mbqr.qr.QrContent
import vn.io.elsutm.mbqr.qr.QrContentParser
import vn.io.elsutm.mbqr.vietqr.BankDirectory
import vn.io.elsutm.mbqr.vietqr.VietQrParseResult
import vn.io.elsutm.mbqr.vietqr.VietQrParser

class DemoScanCasesTest {

    @Test
    fun `demo cases cover transfer and generic qr results`() {
        assertTrue(DEMO_SCAN_CASES.size >= 7)
        assertTrue(QrContentParser.parse(case("Wi-Fi").raw) is QrContent.Details)
        assertTrue(QrContentParser.parse(case("Danh bạ").raw) is QrContent.Details)
        assertTrue(QrContentParser.parse(case("Liên kết").raw) is QrContent.Details)
        assertTrue(case("Xác nhận chuyển khoản").mbInstalled)
        assertFalse(case("Chưa cài MB Bank").mbInstalled)
    }

    @Test
    fun `handoff readiness and confirmation drive demo sheets`() {
        val ready = case("Xác nhận chuyển khoản")
        assertTrue(ready.handoffReady)
        assertTrue(ready.shouldShowDemoSheet(confirmBeforeOpeningMb = true))
        assertFalse(ready.shouldShowDemoSheet(confirmBeforeOpeningMb = false))

        val missingMb = case("Chưa cài MB Bank")
        assertFalse(missingMb.handoffReady)
        assertTrue(missingMb.shouldShowDemoSheet(confirmBeforeOpeningMb = false))
    }

    @Test
    fun `supported demo payload parses with synthetic fields`() {
        val data = VietQrParser.parse(
            case("Xác nhận chuyển khoản").raw,
        ) as VietQrParseResult.VietQr

        assertTrue(data.crcValid)
        assertEquals("970422", data.data.bankBin)
        assertEquals("00000000000001", data.data.account)
        assertEquals(10_000L, data.data.amountVnd)
        assertEquals("NGUYEN VAN MUOI", data.data.merchantName)
    }

    @Test
    fun `unsupported bank demo payload is valid but absent from the directory`() {
        val data = VietQrParser.parse(
            case("Chưa hỗ trợ ngân hàng").raw,
        ) as VietQrParseResult.VietQr

        assertTrue(data.crcValid)
        assertEquals("999999", data.data.bankBin)
        assertNull(BankDirectory.find(data.data.bankBin))
    }

    @Test
    fun `invalid crc demo payload parses but reports an invalid crc`() {
        val data = VietQrParser.parse(
            case("CRC không hợp lệ").raw,
        ) as VietQrParseResult.VietQr

        assertFalse(data.crcValid)
    }

    @Test
    fun `generic demo payloads expose parsed fields`() {
        val wifi = QrContentParser.parse(case("Wi-Fi").raw) as QrContent.Details
        assertEquals("MBQR Demo", wifi.field("Tên mạng"))
        assertEquals("demo1234", wifi.field("Mật khẩu"))

        val contact = QrContentParser.parse(case("Danh bạ").raw) as QrContent.Details
        assertEquals("NGUYEN VAN MUOI", contact.field("Tên"))

        val url = QrContentParser.parse(case("Liên kết").raw) as QrContent.Details
        assertEquals("https://example.com/demo", url.field("Liên kết"))
    }

    private fun QrContent.Details.field(label: String): String =
        fields.firstOrNull { it.label == label }?.value
            ?: throw AssertionError("missing field $label")

    private fun case(reason: String) = DEMO_SCAN_CASES.first { it.reason == reason }
}
