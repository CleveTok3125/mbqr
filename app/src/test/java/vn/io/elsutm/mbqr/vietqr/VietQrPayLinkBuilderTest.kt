package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VietQrPayLinkBuilderTest {

    private val mbBank = BankInfo("970422", "MB", "MBBank", true)

    private fun data(
        account: String = "0123456789",
        amountVnd: Long? = 2_000,
        merchantName: String? = "NGUYEN VAN A",
        content: String? = "tra tien",
    ) = VietQrData(
        bankBin = "970422",
        account = account,
        amountVnd = amountVnd,
        merchantName = merchantName,
        content = content,
    )

    private fun successUrl(
        data: VietQrData = data(),
        bank: BankInfo? = mbBank,
    ): String = (VietQrPayLinkBuilder.build(data, bank) as VietQrPayLinkResult.Success).url

    private fun reason(data: VietQrData, bank: BankInfo? = mbBank) =
        (VietQrPayLinkBuilder.build(data, bank) as VietQrPayLinkResult.Ineligible).reason

    @Test
    fun `builds the full pay link`() {
        assertEquals(
            "https://dl.vietqr.io/pay?app=mb&ba=0123456789@mb&am=2000" +
                "&tn=tra+tien&url=https://127.0.0.1:0/",
            successUrl(),
        )
    }

    @Test
    fun `amount uses dong without thousands separators`() {
        val url = successUrl(data(amountVnd = 12_345))
        assertTrue(url, url.contains("&am=12345&"))
    }

    @Test
    fun `bank code is lowercased and keeps the at sign`() {
        val url = successUrl(bank = BankInfo("970436", "VCB", "Vietcombank", true))
        assertTrue(url, url.contains("&ba=0123456789@vcb&"))
        assertFalse(url, url.contains("%40"))
    }

    @Test
    fun `merchant name is never added`() {
        val url = successUrl(data(merchantName = "NGUYEN VAN A"))
        assertFalse(url, url.contains("&bn="))
        assertFalse(url, url.contains("NGUYEN"))
        assertFalse(successUrl(data(merchantName = null)).contains("&bn="))
        assertFalse(successUrl(data(merchantName = "   ")).contains("&bn="))
    }

    @Test
    fun `content is url encoded`() {
        val url = successUrl(data(content = "Trả tiền & phở"))
        assertTrue(url, url.contains("&tn=Tr%E1%BA%A3+ti%E1%BB%81n+%26+ph%E1%BB%9F&"))
    }

    @Test
    fun `missing amount is ineligible`() {
        assertEquals(VietQrPayLinkResult.Reason.MISSING_AMOUNT, reason(data(amountVnd = null)))
        assertEquals(VietQrPayLinkResult.Reason.MISSING_AMOUNT, reason(data(amountVnd = 0)))
        assertEquals(VietQrPayLinkResult.Reason.MISSING_AMOUNT, reason(data(amountVnd = -1)))
    }

    @Test
    fun `missing content is ineligible`() {
        assertEquals(VietQrPayLinkResult.Reason.MISSING_CONTENT, reason(data(content = null)))
        assertEquals(VietQrPayLinkResult.Reason.MISSING_CONTENT, reason(data(content = "  ")))
    }

    @Test
    fun `unsupported bank is ineligible`() {
        assertEquals(VietQrPayLinkResult.Reason.UNSUPPORTED_BANK, reason(data(), bank = null))
        assertEquals(
            VietQrPayLinkResult.Reason.UNSUPPORTED_BANK,
            reason(data(), bank = BankInfo("970422", "  ", "MBBank", true)),
        )
        assertEquals(
            VietQrPayLinkResult.Reason.UNSUPPORTED_BANK,
            reason(data(), bank = BankInfo("970422", "MB BANK", "MBBank", true)),
        )
        assertEquals(VietQrPayLinkResult.Reason.UNSUPPORTED_BANK, reason(data(account = " ")))
    }

    @Test
    fun `return url is appended raw`() {
        val url = successUrl()
        assertTrue(url, url.endsWith("&url=https://127.0.0.1:0/"))
        assertFalse(url, url.contains("%3A"))
        assertFalse(url, url.contains("%2F"))
        assertEquals(VietQrPayLinkBuilder.RETURN_URL, "https://127.0.0.1:0/")
    }
}
