package vn.io.elsutm.mbqr.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanFrameTest {

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
