package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankBinTest {

    @Test
    fun `maps mb bank bin to transfer prefix`() {
        assertEquals("MB", BankBin.transferPrefix("970422"))
    }

    @Test
    fun `returns null for unknown bin`() {
        assertNull(BankBin.transferPrefix("999999"))
    }
}
