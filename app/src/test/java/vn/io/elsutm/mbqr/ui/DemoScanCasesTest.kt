package vn.io.elsutm.mbqr.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.io.elsutm.mbqr.vietqr.BankDirectory
import vn.io.elsutm.mbqr.vietqr.VietQrParseResult
import vn.io.elsutm.mbqr.vietqr.VietQrParser

class DemoScanCasesTest {

    @Test
    fun `demo cases cover parsed and raw results`() {
        assertTrue(DEMO_SCAN_CASES.size >= 5)
        assertTrue(DEMO_SCAN_CASES.any { it.parsed })
        assertTrue(DEMO_SCAN_CASES.any { !it.parsed })
        assertTrue(case("Xác nhận chuyển khoản").mbInstalled)
        assertFalse(case("Chưa cài MB Bank").mbInstalled)
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
        assertEquals("NGUYEN VAN A", data.data.merchantName)
    }

    @Test
    fun `unsupported bank demo payload is valid but absent from the directory`() {
        val data = VietQrParser.parse(
            case("Chưa hỗ trợ ngân hàng BIN 999999").raw,
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

        assertEquals(false, data.crcValid)
    }

    @Test
    fun `url demo payload is not a vietqr`() {
        assertEquals(
            VietQrParseResult.NotVietQr,
            VietQrParser.parse(case("Không phải VietQR").raw),
        )
    }

    private fun case(reason: String) = DEMO_SCAN_CASES.first { it.reason == reason }
}
