package vn.io.elsutm.mbqr.vietqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankDirectoryTest {

    @Test
    fun `maps mb bank bin to its api prefix`() {
        assertEquals("MB", BankDirectory.find("970422")?.prefix)
    }

    @Test
    fun `maps tcb bin to its api prefix`() {
        assertEquals("TCB", BankDirectory.find("970407")?.prefix)
    }

    @Test
    fun `keeps the api prefix exactly as returned`() {
        assertEquals("momo", BankDirectory.find("971025")?.prefix)
        assertEquals("Ubank", BankDirectory.find("546035")?.prefix)
    }

    @Test
    fun `reads transfer support from the directory`() {
        assertEquals(true, BankDirectory.find("970422")?.transferSupported)
        assertEquals(false, BankDirectory.find("458761")?.transferSupported)
    }

    @Test
    fun `returns null for unknown bin`() {
        assertNull(BankDirectory.find("999999"))
    }

    @Test
    fun `contains complete entries`() {
        val banks = BankDirectory.all()

        assertTrue(banks.size >= 50)
        assertEquals(banks.size, banks.map { it.bin }.distinct().size)
        assertTrue(banks.all { it.bin.matches(Regex("\\d{6}")) })
        assertTrue(banks.all { it.prefix.isNotBlank() })
        assertTrue(banks.all { it.shortName.isNotBlank() })
    }
}
