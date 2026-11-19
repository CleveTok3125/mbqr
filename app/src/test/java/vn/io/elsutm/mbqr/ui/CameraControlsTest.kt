package vn.io.elsutm.mbqr.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
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

    @Test
    fun `maps the bottom dial opening to the arc center`() {
        val pivot = Offset(100f, 200f)
        assertEquals(
            270f,
            dialAngle(Offset(100f, 100f), pivot, DialEdge.BOTTOM.angleOffset),
            0.01f,
        )
    }

    @Test
    fun `maps side dial openings to the arc center`() {
        val pivot = Offset(100f, 200f)
        assertEquals(
            270f,
            dialAngle(Offset(200f, 200f), pivot, DialEdge.LEFT.angleOffset),
            0.01f,
        )
        assertEquals(
            270f,
            dialAngle(Offset(0f, 200f), pivot, DialEdge.RIGHT.angleOffset),
            0.01f,
        )
    }

    @Test
    fun `mirrors touches on the outer side of the dial`() {
        val pivot = Offset(100f, 200f)
        assertEquals(
            270f,
            dialAngle(Offset(0f, 200f), pivot, DialEdge.LEFT.angleOffset),
            0.01f,
        )
        assertEquals(
            270f,
            dialAngle(Offset(200f, 200f), pivot, DialEdge.RIGHT.angleOffset),
            0.01f,
        )
        assertEquals(
            270f,
            dialAngle(Offset(100f, 300f), pivot, DialEdge.BOTTOM.angleOffset),
            0.01f,
        )
    }

    @Test
    fun `maps touches beside the opening to the arc ends`() {
        val pivot = Offset(100f, 200f)
        assertEquals(
            180f,
            dialAngle(Offset(100f, 100f), pivot, DialEdge.LEFT.angleOffset),
            0.01f,
        )
        assertEquals(
            180f,
            dialAngle(Offset(100f, 300f), pivot, DialEdge.RIGHT.angleOffset),
            0.01f,
        )
        assertEquals(
            0f,
            dialAngle(Offset(100f, 300f), pivot, DialEdge.LEFT.angleOffset),
            0.01f,
        )
        assertEquals(
            0f,
            dialAngle(Offset(100f, 100f), pivot, DialEdge.RIGHT.angleOffset),
            0.01f,
        )
    }
}
