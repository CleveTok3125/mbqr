package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountFormatterTest {

    @Test
    fun `formats whole thousand with one decimal`() {
        assertEquals("1.0", AmountFormatter.thousands(1_000))
    }

    @Test
    fun `formats ten thousand five hundred`() {
        assertEquals("10.5", AmountFormatter.thousands(10_500))
    }

    @Test
    fun `formats one hundred fifty thousand`() {
        assertEquals("150.0", AmountFormatter.thousands(150_000))
    }

    @Test
    fun `keeps three decimals for odd amount`() {
        assertEquals("12.345", AmountFormatter.thousands(12_345))
    }

    @Test
    fun `formats sub thousand amount`() {
        assertEquals("0.5", AmountFormatter.thousands(500))
        assertEquals("0.001", AmountFormatter.thousands(1))
        assertEquals("0.999", AmountFormatter.thousands(999))
    }

    @Test
    fun `formats millions`() {
        assertEquals("1000.0", AmountFormatter.thousands(1_000_000))
    }
}
