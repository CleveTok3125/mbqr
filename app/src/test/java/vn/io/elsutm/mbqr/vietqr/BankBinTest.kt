package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankBinTest {

    @Test
    fun `maps mb bank bin`() {
        assertEquals("MB Bank", BankBin.bankName("970422"))
    }

    @Test
    fun `returns null for unknown bin`() {
        assertNull(BankBin.bankName("999999"))
    }
}
