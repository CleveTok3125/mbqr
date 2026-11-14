package vn.io.elsutm.mbqr.qr

import org.junit.Assert.assertEquals
import org.junit.Test

class PayloadEncoderTest {

    @Test
    fun `encodes payload text as lowercase utf8 hex`() {
        assertEquals("", PayloadEncoder.toHex(""))
        assertEquals("4142", PayloadEncoder.toHex("AB"))
        assertEquals(
            "303030323031303130323131",
            PayloadEncoder.toHex("000201010211"),
        )
    }

    @Test
    fun `encodes payload text as base64`() {
        assertEquals("", PayloadEncoder.toBase64(""))
        assertEquals("QUI=", PayloadEncoder.toBase64("AB"))
        assertEquals("SGk=", PayloadEncoder.toBase64("Hi"))
    }

    @Test
    fun `encodes multi-byte characters from their utf8 bytes`() {
        assertEquals("c6af", PayloadEncoder.toHex("Ư"))
        assertEquals("4oKs", PayloadEncoder.toBase64("€"))
    }
}
