package vn.io.elsutm.mbqr.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `clamps clipboard expiry to the supported range`() {
        assertEquals(
            AppSettings.MIN_CLIPBOARD_EXPIRY_SECONDS,
            AppSettings.boundedExpirySeconds(0),
        )
        assertEquals(
            AppSettings.MAX_CLIPBOARD_EXPIRY_SECONDS,
            AppSettings.boundedExpirySeconds(10_000),
        )
    }

    @Test
    fun `snaps clipboard expiry to the slider steps`() {
        assertEquals(15, AppSettings.boundedExpirySeconds(16))
        assertEquals(30, AppSettings.boundedExpirySeconds(23))
        assertEquals(60, AppSettings.boundedExpirySeconds(61))
    }
}
