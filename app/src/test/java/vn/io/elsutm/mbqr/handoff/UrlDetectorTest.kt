package vn.io.elsutm.mbqr.handoff

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlDetectorTest {

    @Test
    fun `accepts http and https urls`() {
        assertTrue(UrlDetector.isHttpUrl("https://example.com"))
        assertTrue(UrlDetector.isHttpUrl("http://example.com/path?q=1"))
        assertTrue(UrlDetector.isHttpUrl("HTTPS://EXAMPLE.COM"))
        assertTrue(UrlDetector.isHttpUrl("  https://example.com  "))
    }

    @Test
    fun `rejects plain text`() {
        assertFalse(UrlDetector.isHttpUrl(""))
        assertFalse(UrlDetector.isHttpUrl("   "))
        assertFalse(UrlDetector.isHttpUrl("example.com"))
        assertFalse(UrlDetector.isHttpUrl("hello world"))
    }

    @Test
    fun `rejects unsafe or unsupported schemes`() {
        assertFalse(UrlDetector.isHttpUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(UrlDetector.isHttpUrl("javascript:alert(1)"))
        assertFalse(UrlDetector.isHttpUrl("file:///etc/passwd"))
        assertFalse(UrlDetector.isHttpUrl("ftp://example.com"))
        assertFalse(UrlDetector.isHttpUrl("tel:+84941196908"))
    }

    @Test
    fun `rejects malformed http urls`() {
        assertFalse(UrlDetector.isHttpUrl("https://"))
        assertFalse(UrlDetector.isHttpUrl("https://exa mple.com"))
    }
}
