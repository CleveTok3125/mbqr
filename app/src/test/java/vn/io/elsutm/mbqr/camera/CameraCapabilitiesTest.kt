package vn.io.elsutm.mbqr.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraCapabilitiesTest {

    @Test
    fun `max fps is limited by the sensor and analysis size`() {
        assertEquals(30, CameraCapabilities.maxSupportedFps(60, 33_333_333L))
        assertEquals(30, CameraCapabilities.maxSupportedFps(30, 16_666_666L))
        assertEquals(60, CameraCapabilities.maxSupportedFps(60, 16_666_666L))
        assertEquals(60, CameraCapabilities.maxSupportedFps(60, 0L))
        assertEquals(60, CameraCapabilities.maxSupportedFps(null, 0L))
        assertEquals(30, CameraCapabilities.maxSupportedFps(null, 33_333_333L))
    }

    @Test
    fun `prefers exact then narrowest then closest supported range`() {
        val support = AnalysisFpsSupport(
            maxFps = 60,
            ranges = listOf(15..30, 30..30, 60..60),
            known = true,
        )

        assertEquals(30..30, CameraCapabilities.targetFpsRange(30, support))
        assertEquals(15..30, CameraCapabilities.targetFpsRange(24, support))
        assertEquals(15..30, CameraCapabilities.targetFpsRange(10, support))
        assertEquals(60..60, CameraCapabilities.targetFpsRange(60, support))
        assertEquals(60..60, CameraCapabilities.targetFpsRange(45, support))
    }

    @Test
    fun `clamps requested fps to the supported maximum`() {
        val support = AnalysisFpsSupport(
            maxFps = 30,
            ranges = listOf(15..30, 60..60),
            known = true,
        )

        assertEquals(15..30, CameraCapabilities.targetFpsRange(60, support))
    }

    @Test
    fun `returns no target range when the camera exposes none`() {
        val support = AnalysisFpsSupport(maxFps = 60, ranges = emptyList(), known = false)
        assertNull(CameraCapabilities.targetFpsRange(30, support))
    }
}
