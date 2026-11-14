package vn.io.elsutm.mbqr.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.io.elsutm.mbqr.settings.AnalysisResolutionPreset
import vn.io.elsutm.mbqr.settings.AnalysisSize
import vn.io.elsutm.mbqr.settings.ScanBoundaryStyle
import vn.io.elsutm.mbqr.settings.ScanFrameStyle

class SettingsFormatTest {

    @Test
    fun `formats clipboard expiry in minutes and seconds`() {
        assertEquals("Không tự xoá", clipboardExpiryLabel(0))
        assertEquals("15 giây", clipboardExpiryLabel(15))
        assertEquals("1 phút", clipboardExpiryLabel(60))
        assertEquals("1 phút 30 giây", clipboardExpiryLabel(90))
        assertEquals("5 phút", clipboardExpiryLabel(300))
    }

    @Test
    fun `labels fixed resolutions by height`() {
        val custom = AnalysisSize(1280, 720)
        assertEquals(
            "480p",
            resolutionLabel(AnalysisResolutionPreset.P640, custom),
        )
        assertEquals(
            "720p",
            resolutionLabel(AnalysisResolutionPreset.P720, custom),
        )
        assertEquals(
            "1080p",
            resolutionLabel(AnalysisResolutionPreset.P1080, custom),
        )
    }

    @Test
    fun `labels the custom resolution with its exact size`() {
        assertEquals(
            "1080×1920",
            resolutionLabel(
                AnalysisResolutionPreset.CUSTOM,
                AnalysisSize(1080, 1920),
            ),
        )
    }

    @Test
    fun `labels the scan frame styles`() {
        assertEquals("Bo góc", ScanFrameStyle.ROUNDED.label())
        assertEquals("Góc", ScanFrameStyle.CORNERS.label())
        assertEquals("Vuông", ScanFrameStyle.SQUARE.label())
    }

    @Test
    fun `labels the scan boundary styles`() {
        assertEquals("Bo góc", ScanBoundaryStyle.ROUNDED.label())
        assertEquals("Vuông", ScanBoundaryStyle.SQUARE.label())
    }

    @Test
    fun `summarises the live analysis settings`() {
        assertEquals(
            "720p · 24 FPS",
            liveSummary(
                AnalysisResolutionPreset.P720,
                AnalysisSize(1280, 720),
                24,
            ),
        )
        assertEquals(
            "1920×1080 · 10 FPS",
            liveSummary(
                AnalysisResolutionPreset.CUSTOM,
                AnalysisSize(1920, 1080),
                10,
            ),
        )
    }
}
