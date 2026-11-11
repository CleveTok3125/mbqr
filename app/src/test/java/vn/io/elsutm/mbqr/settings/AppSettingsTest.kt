package vn.io.elsutm.mbqr.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `clamps clipboard expiry to the supported range`() {
        assertEquals(
            AppSettings.CLIPBOARD_TIMER_OFF_SECONDS,
            AppSettings.boundedExpirySeconds(0),
        )
        assertEquals(
            AppSettings.CLIPBOARD_TIMER_OFF_SECONDS,
            AppSettings.boundedExpirySeconds(-10),
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

    @Test
    fun `slider values below the first tick snap to off`() {
        assertEquals(0, AppSettings.snappedSliderExpirySeconds(0f))
        assertEquals(0, AppSettings.snappedSliderExpirySeconds(7.4f))
        assertEquals(15, AppSettings.snappedSliderExpirySeconds(7.6f))
        assertEquals(15, AppSettings.snappedSliderExpirySeconds(15f))
    }

    @Test
    fun `clamps and snaps scan frame margin percent`() {
        assertEquals(0, AppSettings.boundedScanFrameMarginPercent(0))
        assertEquals(0, AppSettings.boundedScanFrameMarginPercent(-10))
        assertEquals(50, AppSettings.boundedScanFrameMarginPercent(52))
        assertEquals(55, AppSettings.boundedScanFrameMarginPercent(53))
        assertEquals(100, AppSettings.boundedScanFrameMarginPercent(100))
        assertEquals(100, AppSettings.boundedScanFrameMarginPercent(101))
    }

    @Test
    fun `snaps scan frame margin slider values`() {
        assertEquals(0, AppSettings.snappedScanFrameMarginPercent(0f))
        assertEquals(5, AppSettings.snappedScanFrameMarginPercent(7.4f))
        assertEquals(10, AppSettings.snappedScanFrameMarginPercent(7.6f))
        assertEquals(50, AppSettings.snappedScanFrameMarginPercent(50f))
        assertEquals(100, AppSettings.snappedScanFrameMarginPercent(100f))
    }

    @Test
    fun `clamps masking level to the supported range`() {
        assertEquals(AppSettings.MASKING_OFF, AppSettings.boundedMaskingLevel(-1))
        assertEquals(AppSettings.MASKING_OFF, AppSettings.boundedMaskingLevel(0))
        assertEquals(AppSettings.MASKING_LIGHT, AppSettings.boundedMaskingLevel(1))
        assertEquals(AppSettings.MASKING_FULL, AppSettings.boundedMaskingLevel(2))
        assertEquals(AppSettings.MASKING_FULL, AppSettings.boundedMaskingLevel(3))
    }
}
