package vn.io.elsutm.mbqr.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import vn.io.elsutm.mbqr.settings.ScanBoundaryStyle
import vn.io.elsutm.mbqr.settings.ScanFrameStyle

class ScanFrameTest {

    @Test
    fun `rounds only the rounded frame style`() {
        assertEquals(11, scanFrameCornerPercent(ScanFrameStyle.ROUNDED))
        assertEquals(0, scanFrameCornerPercent(ScanFrameStyle.CORNERS))
        assertEquals(0, scanFrameCornerPercent(ScanFrameStyle.SQUARE))
    }

    @Test
    fun `keeps the crop boundary rounded only for the rounded style`() {
        assertEquals(32.dp, scanFrameBoundaryRadius(ScanBoundaryStyle.ROUNDED))
        assertEquals(0.dp, scanFrameBoundaryRadius(ScanBoundaryStyle.SQUARE))
    }

    @Test
    fun `caps corner arms on large frames`() {
        assertEquals(28f, scanFrameArmLength(edgePx = 100f, maxArmPx = 28f))
        assertEquals(14f, scanFrameArmLength(edgePx = 50f, maxArmPx = 28f))
    }

    @Test
    fun `scales the tightest gap by percentage`() {
        assertEquals(0.dp, scanMargin(minGap = 20.dp, percent = 0))
        assertEquals(10.dp, scanMargin(minGap = 20.dp, percent = 50))
        assertEquals(20.dp, scanMargin(minGap = 20.dp, percent = 100))
    }

    @Test
    fun `clamps margin percentages`() {
        assertEquals(0.dp, scanMargin(minGap = 20.dp, percent = -10))
        assertEquals(20.dp, scanMargin(minGap = 20.dp, percent = 150))
    }
}
