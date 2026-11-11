package vn.io.elsutm.mbqr.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrContentParserTest {

    @Test
    fun `vietqr wins over generic formats and preserves crc state`() {
        val content = QrContentParser.parse(SAMPLE) as QrContent.VietQr
        assertTrue(content.crcValid)
        assertEquals("970422", content.data.bankBin)

        val invalid = QrContentParser.parse(SAMPLE.dropLast(4) + "0000") as QrContent.VietQr
        assertFalse(invalid.crcValid)
        assertEquals("0941196908", invalid.data.account)
    }

    @Test
    fun `parses wifi fields and escaped values`() {
        val content = QrContentParser.parse(
            "WIFI:T:WPA;S:My\\;Net;P:pa\\:ss\\;word;H:true;;",
        ) as QrContent.Details

        assertEquals("Wi-Fi", content.title)
        assertEquals("My;Net", content.field("Tên mạng"))
        assertEquals("WPA/WPA2", content.field("Bảo mật"))
        assertEquals("pa:ss;word", content.field("Mật khẩu"))
        assertEquals("Có", content.field("Mạng ẩn"))
    }

    @Test
    fun `classifies fields for both masking levels`() {
        val wifi = QrContentParser.parse(
            "WIFI:T:WPA;S:Home;P:secret;H:true;;",
        ) as QrContent.Details
        assertEquals(QrFieldSensitivity.Personal, wifi.fieldSensitivity("Tên mạng"))
        assertEquals(QrFieldSensitivity.Direct, wifi.fieldSensitivity("Mật khẩu"))
        assertEquals(QrFieldSensitivity.None, wifi.fieldSensitivity("Bảo mật"))
        assertEquals(QrFieldSensitivity.None, wifi.fieldSensitivity("Mạng ẩn"))

        val contact = QrContentParser.parse(
            "BEGIN:VCARD\nVERSION:3.0\nFN:Nguyen Van A\n" +
                "TEL:0901234567\nEMAIL:a@example.com\nORG:Example\nEND:VCARD",
        ) as QrContent.Details
        assertEquals(QrFieldSensitivity.Personal, contact.fieldSensitivity("Tên"))
        assertEquals(QrFieldSensitivity.Direct, contact.fieldSensitivity("Điện thoại"))
        assertEquals(QrFieldSensitivity.Direct, contact.fieldSensitivity("Email"))
        assertEquals(QrFieldSensitivity.Personal, contact.fieldSensitivity("Tổ chức"))

        val email = QrContentParser.parse(
            "mailto:a@example.com?subject=Hello&body=World",
        ) as QrContent.Details
        assertEquals(QrFieldSensitivity.Direct, email.fieldSensitivity("Đến"))
        assertEquals(QrFieldSensitivity.Personal, email.fieldSensitivity("Tiêu đề"))
        assertEquals(QrFieldSensitivity.Direct, email.fieldSensitivity("Nội dung"))

        val url = QrContentParser.parse(
            "URLTO:Example:https://example.com/path",
        ) as QrContent.Details
        assertEquals(QrFieldSensitivity.Personal, url.fieldSensitivity("Tiêu đề"))
        assertEquals(QrFieldSensitivity.Personal, url.fieldSensitivity("Liên kết"))

        val isbn = QrContentParser.parse("ISBN:9780306406157") as QrContent.Details
        assertEquals(QrFieldSensitivity.None, isbn.fieldSensitivity("ISBN"))
    }

    @Test
    fun `wifi prefix is case insensitive and open networks omit password`() {
        val content = QrContentParser.parse(
            "wifi:T:nopass;S:Open;P:;H:false;;",
        ) as QrContent.Details

        assertEquals("Open", content.field("Tên mạng"))
        assertEquals("Không mật khẩu", content.field("Bảo mật"))
        assertEquals(null, content.fieldOrNull("Mật khẩu"))
    }

    @Test
    fun `malformed wifi payload falls back to text`() {
        assertTrue(QrContentParser.parse("WIFI:T:WPA;P:secret;;") is QrContent.Text)
    }

    @Test
    fun `parses contact formats through one rule`() {
        val vcard = QrContentParser.parse(
            "BEGIN:VCARD\nVERSION:3.0\nFN:Nguyen Van A\n" +
                "TEL:0901234567\nEMAIL:a@example.com\nORG:Example\nEND:VCARD",
        ) as QrContent.Details
        assertEquals("Danh bạ", vcard.title)
        assertEquals("Nguyen Van A", vcard.field("Tên"))
        assertEquals("0901234567", vcard.field("Điện thoại"))
        assertEquals("a@example.com", vcard.field("Email"))
        assertEquals("Example", vcard.field("Tổ chức"))

        val mecard = QrContentParser.parse(
            "MECARD:N:Nguyen Van B;TEL:0907654321;EMAIL:b@example.com;;",
        ) as QrContent.Details
        assertEquals("Nguyen Van B", mecard.field("Tên"))
        assertEquals("0907654321", mecard.field("Điện thoại"))
    }

    @Test
    fun `parses email phone sms geo isbn and url details`() {
        val email = QrContentParser.parse(
            "mailto:a@example.com?subject=Hello&body=World",
        ) as QrContent.Details
        assertEquals("Email", email.title)
        assertEquals("a@example.com", email.field("Đến"))
        assertEquals("Hello", email.field("Tiêu đề"))
        assertEquals("World", email.field("Nội dung"))

        val phone = QrContentParser.parse("tel:+84901234567") as QrContent.Details
        assertEquals("Điện thoại", phone.title)
        assertEquals("+84901234567", phone.field("Số điện thoại"))

        val sms = QrContentParser.parse("sms:+84901234567?body=Hello") as QrContent.Details
        assertEquals("Tin nhắn", sms.title)
        assertEquals("+84901234567", sms.field("Đến"))
        assertEquals("Hello", sms.field("Nội dung"))

        val geo = QrContentParser.parse("geo:10.762622,106.660172") as QrContent.Details
        assertEquals("Vị trí", geo.title)
        assertEquals("10.762622", geo.field("Vĩ độ"))
        assertEquals("106.660172", geo.field("Kinh độ"))

        val isbn = QrContentParser.parse("ISBN:9780306406157") as QrContent.Details
        assertEquals("ISBN", isbn.title)
        assertEquals("9780306406157", isbn.field("ISBN"))

        val url = QrContentParser.parse("https://example.com/path?q=1") as QrContent.Details
        assertEquals("Liên kết", url.title)
        assertEquals("https://example.com/path?q=1", url.field("Liên kết"))
    }

    @Test
    fun `specific prefixes are not stolen by broader url matching`() {
        assertTrue(QrContentParser.parse("mailto:a@example.com") is QrContent.Details)
        assertEquals(
            "Email",
            (QrContentParser.parse("mailto:a@example.com") as QrContent.Details).title,
        )
        assertEquals(
            "Tin nhắn",
            (QrContentParser.parse("sms:+84901234567") as QrContent.Details).title,
        )
        assertEquals(
            "Danh bạ",
            (QrContentParser.parse("MECARD:N:A;TEL:1;;") as QrContent.Details).title,
        )
    }

    @Test
    fun `urlto bookmark uses title and embedded url`() {
        val url = QrContentParser.parse(
            "URLTO:Example:https://example.com/path",
        ) as QrContent.Details

        assertEquals("https://example.com/path", url.raw)
        assertEquals("Example", url.field("Tiêu đề"))
        assertEquals("https://example.com/path", url.field("Liên kết"))
    }

    @Test
    fun `urlto direct url remains supported`() {
        val url = QrContentParser.parse(
            "URLTO:https://example.com/path",
        ) as QrContent.Details

        assertEquals("https://example.com/path", url.raw)
    }

    @Test
    fun `normalization removes markers and preserves line breaks`() {
        assertEquals(
            "hello\nworld",
            QrContentParser.normalized("\uFEFF  hello\nworld  \u200B"),
        )
    }

    @Test
    fun `invalid isbn falls back to text`() {
        assertTrue(QrContentParser.parse("ISBN:hello") is QrContent.Text)
    }

    @Test
    fun `invalid url and plain text use raw text content`() {
        assertTrue(QrContentParser.parse("http://") is QrContent.Text)
        assertTrue(QrContentParser.parse("hello world") is QrContent.Text)
    }

    @Test
    fun `manual test qr set parses as expected`() {
        MANUAL_CASES.forEach { case ->
            when (val expected = case.expected) {
                is Expected.Text -> assertTrue(
                    "${case.name}: expected text",
                    QrContentParser.parse(case.raw) is QrContent.Text,
                )

                is Expected.VietQr -> {
                    val content = QrContentParser.parse(case.raw) as? QrContent.VietQr
                        ?: throw AssertionError("${case.name}: expected VietQR")
                    assertEquals("${case.name}: CRC", expected.crcValid, content.crcValid)
                    assertEquals("${case.name}: bank", expected.bankBin, content.data.bankBin)
                    assertEquals("${case.name}: amount", expected.amountVnd, content.data.amountVnd)
                }

                is Expected.Details -> {
                    val content = QrContentParser.parse(case.raw) as? QrContent.Details
                        ?: throw AssertionError("${case.name}: expected details")
                    assertEquals("${case.name}: title", expected.title, content.title)
                    assertEquals(
                        "${case.name}: field",
                        expected.value,
                        content.field(expected.field),
                    )
                    expected.absentField?.let { absent ->
                        assertEquals(
                            "${case.name}: absent $absent",
                            null,
                            content.fieldOrNull(absent),
                        )
                    }
                }
            }
        }
    }

    private fun QrContent.Details.field(label: String): String =
        fieldOrNull(label) ?: throw AssertionError("missing field $label")

    private fun QrContent.Details.fieldOrNull(label: String): String? =
        fields.firstOrNull { it.label == label }?.value

    private fun QrContent.Details.fieldSensitivity(label: String): QrFieldSensitivity =
        fields.firstOrNull { it.label == label }?.sensitivity
            ?: throw AssertionError("missing field $label")

    private sealed interface Expected {
        data object Text : Expected

        data class VietQr(
            val crcValid: Boolean,
            val bankBin: String,
            val amountVnd: Long?,
        ) : Expected

        data class Details(
            val title: String,
            val field: String,
            val value: String,
            val absentField: String? = null,
        ) : Expected
    }

    private data class ManualCase(
        val name: String,
        val raw: String,
        val expected: Expected,
    )

    private companion object {
        const val SAMPLE =
            "00020101021138540010A0000007270124000697042201100941196908" +
                "0208QRIBFTTA53037045802VN6304a14c"

        const val VIETQR_VALID =
            "00020101021138580010A000000727012800069704220114000000000000010208" +
                "QRIBFTTA53037045802VN5405100005915NGUYEN VAN MUOI62080804DEMO6304B5C2"

        const val VIETQR_INVALID_CRC =
            "00020101021138580010A000000727012800069704220114000000000000010208" +
                "QRIBFTTA53037045802VN5405100005915NGUYEN VAN MUOI62080804DEMO6304B5C1"

        const val VIETQR_UNSUPPORTED_BANK =
            "00020101021138580010A000000727012800069999990114000000000000010208" +
                "QRIBFTTA53037045802VN5405100005915NGUYEN VAN MUOI62080804DEMO630440DC"

        val MANUAL_CASES = listOf(
            ManualCase(
                "01-vietqr-valid",
                VIETQR_VALID,
                Expected.VietQr(crcValid = true, bankBin = "970422", amountVnd = 10_000L),
            ),
            ManualCase(
                "02-vietqr-invalid-crc",
                VIETQR_INVALID_CRC,
                Expected.VietQr(crcValid = false, bankBin = "970422", amountVnd = 10_000L),
            ),
            ManualCase(
                "03-vietqr-unsupported-bank",
                VIETQR_UNSUPPORTED_BANK,
                Expected.VietQr(crcValid = true, bankBin = "999999", amountVnd = 10_000L),
            ),
            ManualCase(
                "04-wifi-wpa",
                "WIFI:T:WPA;S:MBQR-Test;P:test1234;H:false;;",
                Expected.Details("Wi-Fi", "Mật khẩu", "test1234"),
            ),
            ManualCase(
                "05-wifi-open",
                "WIFI:T:nopass;S:MBQR-Open;P:;H:false;;",
                Expected.Details(
                    title = "Wi-Fi",
                    field = "Bảo mật",
                    value = "Không mật khẩu",
                    absentField = "Mật khẩu",
                ),
            ),
            ManualCase(
                "06-wifi-hidden",
                "WIFI:T:WPA;S:MBQR-Hidden;P:hidden1234;H:true;;",
                Expected.Details("Wi-Fi", "Mạng ẩn", "Có"),
            ),
            ManualCase(
                "07-wifi-wep",
                "WIFI:T:WEP;S:MBQR-WEP;P:wep1234;H:false;;",
                Expected.Details("Wi-Fi", "Bảo mật", "WEP"),
            ),
            ManualCase(
                "08-vcard",
                "BEGIN:VCARD\nVERSION:3.0\nFN:NGUYEN VAN TEST\n" +
                    "TEL:0900000000\nEMAIL:test@example.com\nORG:MBQR Test\nEND:VCARD",
                Expected.Details("Danh bạ", "Tên", "NGUYEN VAN TEST"),
            ),
            ManualCase(
                "09-mecard",
                "MECARD:N:NGUYEN VAN TEST;TEL:0900000000;EMAIL:test@example.com;;",
                Expected.Details("Danh bạ", "Tên", "NGUYEN VAN TEST"),
            ),
            ManualCase(
                "10-email",
                "mailto:test@example.com?subject=MBQR%20test&body=Hello",
                Expected.Details("Email", "Đến", "test@example.com"),
            ),
            ManualCase(
                "11-phone",
                "tel:+84900000000",
                Expected.Details("Điện thoại", "Số điện thoại", "+84900000000"),
            ),
            ManualCase(
                "12-sms",
                "sms:+84900000000?body=Hello",
                Expected.Details("Tin nhắn", "Đến", "+84900000000"),
            ),
            ManualCase(
                "13-geo",
                "geo:10.762622,106.660172",
                Expected.Details("Vị trí", "Vĩ độ", "10.762622"),
            ),
            ManualCase(
                "14-isbn",
                "ISBN:9780306406157",
                Expected.Details("ISBN", "ISBN", "9780306406157"),
            ),
            ManualCase(
                "15-url",
                "https://example.com/test",
                Expected.Details("Liên kết", "Liên kết", "https://example.com/test"),
            ),
            ManualCase(
                "16-text",
                "MBQR test text",
                Expected.Text,
            ),
        )
    }
}
