package vn.io.elsutm.mbqr.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.io.elsutm.mbqr.qr.QrFieldSensitivity
import vn.io.elsutm.mbqr.settings.AppSettings

class MaskingPolicyTest {

    @Test
    fun `never hides insensitive fields`() {
        listOf(
            AppSettings.MASKING_OFF,
            AppSettings.MASKING_LIGHT,
            AppSettings.MASKING_FULL,
        ).forEach { level ->
            assertFalse(isMaskingActive(QrFieldSensitivity.None, level))
        }
    }

    @Test
    fun `hides direct fields from the light level`() {
        assertFalse(isMaskingActive(QrFieldSensitivity.Direct, AppSettings.MASKING_OFF))
        assertTrue(isMaskingActive(QrFieldSensitivity.Direct, AppSettings.MASKING_LIGHT))
        assertTrue(isMaskingActive(QrFieldSensitivity.Direct, AppSettings.MASKING_FULL))
    }

    @Test
    fun `hides personal fields only at the full level`() {
        assertFalse(isMaskingActive(QrFieldSensitivity.Personal, AppSettings.MASKING_OFF))
        assertFalse(isMaskingActive(QrFieldSensitivity.Personal, AppSettings.MASKING_LIGHT))
        assertTrue(isMaskingActive(QrFieldSensitivity.Personal, AppSettings.MASKING_FULL))
    }
}
