package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TlvParserTest {

    @Test
    fun `parses flat tlvs in order`() {
        val fields = TlvParser.parse("0002010102115303704")

        assertEquals(
            listOf(
                Tlv("00", "01"),
                Tlv("01", "11"),
                Tlv("53", "704"),
            ),
            fields,
        )
    }

    @Test
    fun `parses empty value`() {
        assertEquals(listOf(Tlv("62", "")), TlvParser.parse("6200"))
    }

    @Test
    fun `parses nested payload`() {
        val outer = TlvParser.parse("38540010A00000072701240006970422011009411969080208QRIBFTTA")
        val nested = TlvParser.parse(outer!!.single().value)

        assertEquals("A000000727", nested!![0].value)
        assertEquals("000697042201100941196908", nested[1].value)
    }

    @Test
    fun `rejects truncated length`() {
        assertNull(TlvParser.parse("0002"))
    }

    @Test
    fun `rejects value shorter than declared length`() {
        assertNull(TlvParser.parse("000501"))
    }

    @Test
    fun `rejects non numeric tag`() {
        assertNull(TlvParser.parse("XX0201"))
    }

    @Test
    fun `rejects non numeric length`() {
        assertNull(TlvParser.parse("00XX01"))
    }
}
