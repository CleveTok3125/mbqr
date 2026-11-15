package vn.io.elsutm.mbqr.handoff

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserLauncherTest {

    @Test
    fun `pay links are recognised`() {
        assertTrue(BrowserLauncher.isPayLink("https://dl.vietqr.io/pay?app=mb&ba=0123456789@mb"))
        assertTrue(BrowserLauncher.isPayLink("https://dl.vietqr.io/pay"))
    }

    @Test
    fun `other links are rejected`() {
        assertFalse(BrowserLauncher.isPayLink("http://dl.vietqr.io/pay"))
        assertFalse(BrowserLauncher.isPayLink("https://dl.vietqr.io/transfer"))
        assertFalse(BrowserLauncher.isPayLink("https://vietqr.io/pay"))
        assertFalse(BrowserLauncher.isPayLink("https://dl.vietqr.io:443.evil.com/pay"))
        assertFalse(BrowserLauncher.isPayLink("mbqr://return"))
        assertFalse(BrowserLauncher.isPayLink(""))
        assertFalse(BrowserLauncher.isPayLink("not a url"))
    }
}
