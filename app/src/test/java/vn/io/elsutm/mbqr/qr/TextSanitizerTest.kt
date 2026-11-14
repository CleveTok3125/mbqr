package vn.io.elsutm.mbqr.qr

import org.junit.Assert.assertEquals
import org.junit.Test

class TextSanitizerTest {

    @Test
    fun `keeps plain text untouched`() {
        assertEquals(
            "STK 0941196908 - MB Bank",
            TextSanitizer.sanitize("STK 0941196908 - MB Bank"),
        )
    }

    @Test
    fun `keeps Vietnamese diacritics and emoji`() {
        assertEquals(
            "Nguyễn Văn Ánh 🚀",
            TextSanitizer.sanitize("Nguyễn Văn Ánh 🚀"),
        )
    }

    @Test
    fun `drops control characters`() {
        assertEquals(
            "abc",
            TextSanitizer.sanitize("a\u0000b\u0007c\u007F"),
        )
        assertEquals(
            "abc",
            TextSanitizer.sanitize("a\u0085b\u009Fc"),
        )
    }

    @Test
    fun `drops bidirectional and zero-width controls`() {
        assertEquals(
            "0941196908",
            TextSanitizer.sanitize("\u202E09411\u200B96908\u2066"),
        )
        assertEquals(
            "abc",
            TextSanitizer.sanitize("\u200Ea\u00ADb\u2060c\uFEFF"),
        )
    }

    @Test
    fun `keeps line breaks and tabs as printable separators`() {
        assertEquals("a\nb\tc", TextSanitizer.sanitize("a\r\nb\tc"))
        assertEquals("a\nb", TextSanitizer.sanitize("a\rb"))
        assertEquals("a\n\nb", TextSanitizer.sanitize("a\n\nb"))
    }

    @Test
    fun `drops unpaired surrogates but keeps emoji pairs`() {
        assertEquals("a", TextSanitizer.sanitize("a\uD83D"))
        assertEquals("a", TextSanitizer.sanitize("a\uDE80"))
        assertEquals("a🚀b", TextSanitizer.sanitize("a\uD83D\uDE80b"))
    }

    @Test
    fun `returns an empty string for invisible input`() {
        assertEquals("", TextSanitizer.sanitize("\u200B\uFEFF\u202A"))
        assertEquals("", TextSanitizer.sanitize(""))
    }
}
