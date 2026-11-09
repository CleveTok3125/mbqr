package vn.io.elsutm.mbqr.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class ZoomMappingTest {

    @Test
    fun `start angle maps to minimum ratio`() {
        assertEquals(1f, ZoomMapping.ratioForAngle(200f, 1f, 8f), 0.001f)
    }

    @Test
    fun `end angle maps to maximum ratio`() {
        assertEquals(8f, ZoomMapping.ratioForAngle(340f, 1f, 8f), 0.001f)
    }

    @Test
    fun `middle angle maps to geometric mean`() {
        assertEquals(sqrt(8f), ZoomMapping.ratioForAngle(270f, 1f, 8f), 0.001f)
    }

    @Test
    fun `ratio maps back to angle`() {
        assertEquals(270f, ZoomMapping.angleForRatio(sqrt(8f), 1f, 8f), 0.001f)
    }

    @Test
    fun `angles outside the arc are clamped`() {
        assertEquals(1f, ZoomMapping.ratioForAngle(120f, 1f, 8f), 0.001f)
        assertEquals(8f, ZoomMapping.ratioForAngle(400f, 1f, 8f), 0.001f)
    }

    @Test
    fun `degenerate zoom range returns minimum`() {
        assertEquals(1f, ZoomMapping.ratioForAngle(270f, 1f, 1f), 0.001f)
        assertEquals(2f, ZoomMapping.ratioForAngle(270f, 2f, 1f), 0.001f)
    }
}
