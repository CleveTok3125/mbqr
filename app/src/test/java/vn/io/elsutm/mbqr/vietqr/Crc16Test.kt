package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Test

class Crc16Test {

    @Test
    fun `matches reference vector for 123456789`() {
        assertEquals("29B1", Crc16.ccittFalse("123456789"))
    }

    @Test
    fun `matches checksum of sample payload prefix`() {
        val payload = "00020101021138540010A00000072701240006970422011009411969080208QRIBFTTA53037045802VN6304"

        assertEquals("A14C", Crc16.ccittFalse(payload))
    }
}
