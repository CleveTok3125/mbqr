package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VietQrParserTest {

    @Test
    fun `parses sample payload`() {
        val result = VietQrParser.parse(SAMPLE) as? VietQrParseResult.VietQr
            ?: throw AssertionError("expected a VietQR payload")
        val qr = result.data
        assertEquals("970422", qr.bankBin)
        assertEquals("0941196908", qr.account)
        assertNull(qr.amountVnd)
        assertNull(qr.recipientName)
        assertTrue(result.crcValid)
    }

    @Test
    fun `parses amount name and content`() {
        val payload = buildPayload(
            amount = "10500",
            name = "NGUYEN VAN A",
            content = "TRA SUA",
        )

        val data = (VietQrParser.parse(payload) as VietQrParseResult.VietQr).data

        assertEquals(10_500L, data.amountVnd)
        assertEquals("NGUYEN VAN A", data.recipientName)
        assertEquals("TRA SUA", data.content)
    }

    @Test
    fun `keeps data but flags invalid crc`() {
        val payload = buildPayload(amount = "1000").dropLast(4) + "0000"

        val result = VietQrParser.parse(payload) as VietQrParseResult.VietQr

        assertFalse(result.crcValid)
        assertEquals("0941196908", result.data.account)
        assertEquals(1_000L, result.data.amountVnd)
    }

    @Test
    fun `rejects text that is not a qr payload`() {
        assertEquals(VietQrParseResult.NotVietQr, VietQrParser.parse("hello world"))
    }

    @Test
    fun `rejects payload without napas field`() {
        val payload = withCrc(tlv("00", "01") + tlv("01", "11") + tlv("53", "704"))

        assertEquals(VietQrParseResult.NotVietQr, VietQrParser.parse(payload))
    }

    @Test
    fun `rejects unsupported provider guid`() {
        val payload = buildPayload(guid = "A000000999")

        assertEquals(VietQrParseResult.NotVietQr, VietQrParser.parse(payload))
    }

    @Test
    fun `rejects non vnd currency`() {
        val payload = buildPayload(currency = "840")

        assertEquals(VietQrParseResult.NotVietQr, VietQrParser.parse(payload))
    }

    @Test
    fun `trims surrounding whitespace before parsing`() {
        val result = VietQrParser.parse("  $SAMPLE\n")

        assertTrue(result is VietQrParseResult.VietQr)
    }

    @Test
    fun `treats malformed amount as not vietqr`() {
        val payload = buildPayload(amount = "10.5")

        assertEquals(VietQrParseResult.NotVietQr, VietQrParser.parse(payload))
    }

    @Test
    fun `rejects amount that rounds to zero`() {
        val payload = buildPayload(amount = "4")

        assertEquals(VietQrParseResult.NotVietQr, VietQrParser.parse(payload))
    }

    @Test
    fun `accepts smallest representable amount`() {
        val payload = buildPayload(amount = "5")
        val data = (VietQrParser.parse(payload) as VietQrParseResult.VietQr).data

        assertEquals(5L, data.amountVnd)
    }

    private fun buildPayload(
        guid: String = "A000000727",
        currency: String = "704",
        amount: String? = null,
        name: String? = null,
        content: String? = null,
    ): String {
        val provider = tlv("00", guid) +
            tlv("01", tlv("00", "970422") + tlv("01", "0941196908")) +
            tlv("02", "QRIBFTTA")

        val body = buildString {
            append(tlv("00", "01"))
            append(tlv("01", "11"))
            append(tlv("38", provider))
            append(tlv("53", currency))
            append(tlv("58", "VN"))
            amount?.let { append(tlv("54", it)) }
            name?.let { append(tlv("59", it)) }
            content?.let { append(tlv("62", tlv("08", it))) }
        }

        return withCrc(body)
    }

    private fun withCrc(body: String): String {
        val withoutCrc = body + "6304"
        return withoutCrc + Crc16.ccittFalse(withoutCrc)
    }

    private fun tlv(tag: String, value: String): String =
        tag + "%02d".format(value.length) + value

    private companion object {
        const val SAMPLE =
            "00020101021138540010A0000007270124000697042201100941196908" +
                "0208QRIBFTTA53037045802VN6304a14c"
    }
}
