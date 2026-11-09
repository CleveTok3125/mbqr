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
    fun `rounds odd amount to two decimals`() {
        assertEquals("12.35", AmountFormatter.thousands(12_345))
        assertEquals("12.34", AmountFormatter.thousands(12_344))
        assertEquals("12.36", AmountFormatter.thousands(12_355))
    }

    @Test
    fun `formats sub thousand amount`() {
        assertEquals("0.5", AmountFormatter.thousands(500))
        assertEquals("0.1", AmountFormatter.thousands(100))
    }

    @Test
    fun `rounds smallest amount to ten vnd`() {
        assertEquals("0.01", AmountFormatter.thousands(5))
    }

    @Test
    fun `formats millions`() {
        assertEquals("1000.0", AmountFormatter.thousands(1_000_000))
    }
}
