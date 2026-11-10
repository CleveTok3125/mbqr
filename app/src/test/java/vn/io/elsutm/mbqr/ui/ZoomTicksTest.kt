package vn.io.elsutm.mbqr.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ZoomTicksTest {

    @Test
    fun `picks all powers of two plus the endpoints`() {
        assertEquals(
            listOf(1f, 2f, 4f, 8f, 10f),
            ZoomTicks.majorTicks(1f, 10f),
        )
        assertEquals(
            listOf(1f, 2f, 4f, 8f, 16f),
            ZoomTicks.majorTicks(1f, 16f),
        )
        assertEquals(
            listOf(1f, 2f, 4f, 8f, 16f, 30f),
            ZoomTicks.majorTicks(1f, 30f),
        )
        assertEquals(
            listOf(1f, 2f, 4f, 8f, 16f, 32f, 64f, 100f),
            ZoomTicks.majorTicks(1f, 100f),
        )
        assertEquals(
            listOf(
                1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f, 512f, 1000f,
            ),
            ZoomTicks.majorTicks(1f, 1000f),
        )
    }

    @Test
    fun `keeps interior majors as powers of two`() {
        val majors = ZoomTicks.majorTicks(1f, 100f)

        assertEquals(1f, majors.first(), 0.001f)
        assertEquals(100f, majors.last(), 0.001f)
        assertTrue(majors.contains(64f))
        assertTrue(
            majors.drop(1).dropLast(1).all(ZoomTicks::isPowerOfTwo),
        )
        assertTrue(ZoomTicks.majorTicks(1f, 1000f).contains(512f))
    }

    @Test
    fun `minor ticks divide a major gap into equal log steps`() {
        val minors = ZoomTicks.minorTicks(listOf(1f, 2f))

        assertEquals(4, minors.size)
        minors.forEachIndexed { index, ratio ->
            val expected = 2.0.pow((index + 1) / 5.0).toFloat()
            assertEquals(expected, ratio, 0.0001f)
        }
    }

    @Test
    fun `continues minor ticks into the tail using the next power`() {
        val ten = ZoomTicks.minorTicks(ZoomTicks.majorTicks(1f, 10f))
        val thirty = ZoomTicks.minorTicks(ZoomTicks.majorTicks(1f, 30f))
        val hundred = ZoomTicks.minorTicks(ZoomTicks.majorTicks(1f, 100f))
        val thousand = ZoomTicks.minorTicks(ZoomTicks.majorTicks(1f, 1000f))

        assertEquals(13, ten.size)
        assertEquals(20, thirty.size)
        assertEquals(27, hundred.size)
        assertEquals(40, thousand.size)

        assertTrue(ten.none { it > 10f })
        assertTrue(thirty.none { it > 30f })
        assertTrue(hundred.none { it > 100f })
        assertTrue(thousand.none { it > 1000f })
    }

    @Test
    fun `tail minors follow the octave grid and clip at max`() {
        val tenTail = ZoomTicks.minorTicks(ZoomTicks.majorTicks(1f, 10f))
            .filter { it > 8f }
        val thirtyTail = ZoomTicks.minorTicks(ZoomTicks.majorTicks(1f, 30f))
            .filter { it > 16f }
        val hundredTail = ZoomTicks.minorTicks(ZoomTicks.majorTicks(1f, 100f))
            .filter { it > 64f }

        assertEquals(1, tenTail.size)
        assertEquals(9.19f, tenTail.single(), 0.01f)

        assertEquals(4, thirtyTail.size)
        thirtyTail.forEachIndexed { index, ratio ->
            val expected = 16.0 * 2.0.pow((index + 1) / 5.0).toFloat()
            assertEquals(expected.toFloat(), ratio, 0.0001f)
        }

        assertEquals(3, hundredTail.size)
        hundredTail.forEachIndexed { index, ratio ->
            val expected = 64.0 * 2.0.pow((index + 1) / 5.0).toFloat()
            assertEquals(expected.toFloat(), ratio, 0.0001f)
        }
    }

    @Test
    fun `keeps minor ticks when the maximum is a power of two`() {
        val minors = ZoomTicks.minorTicks(ZoomTicks.majorTicks(1f, 16f))

        assertEquals(16, minors.size)
        assertEquals(minors.sorted(), minors)
        assertEquals(minors.distinct(), minors)
    }

    @Test
    fun `octave majors occupy equal arc lengths`() {
        val majors = ZoomTicks.majorTicks(1f, 30f)
        val angles = majors.map { ZoomMapping.angleForRatio(it, 1f, 30f) }
        val octaves = (0 until 4).map { index -> angles[index + 1] - angles[index] }

        octaves.forEach { span ->
            assertEquals(octaves.first(), span, 0.01f)
        }
        assertTrue(angles.last() - angles[angles.lastIndex - 1] < octaves.first())
    }

    @Test
    fun `fixed zoom range has no minor ticks`() {
        assertEquals(listOf(1f), ZoomTicks.majorTicks(1f, 1f))
        assertTrue(ZoomTicks.minorTicks(listOf(1f)).isEmpty())
    }
}
