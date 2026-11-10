package vn.io.elsutm.mbqr.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraControlsTest {

    @Test
    fun `detects endpoint crossings`() {
        assertTrue(crossedEndpoint(2f, 1f, 1f, 8f))
        assertTrue(crossedEndpoint(7f, 8f, 1f, 8f))
        assertFalse(crossedEndpoint(1f, 1f, 1f, 8f))
        assertFalse(crossedEndpoint(8f, 8f, 1f, 8f))
        assertFalse(crossedEndpoint(4f, 5f, 1f, 8f))
        assertFalse(crossedEndpoint(5f, 4f, 1f, 8f))
    }
}
