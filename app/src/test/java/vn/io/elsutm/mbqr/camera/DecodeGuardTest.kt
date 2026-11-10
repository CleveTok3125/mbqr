package vn.io.elsutm.mbqr.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecodeGuardTest {

    @Test
    fun `suppresses repeated payloads`() {
        val guard = DecodeGuard()

        assertEquals("a", guard.accept("a"))
        assertNull(guard.accept("a"))
        assertNull(guard.accept("a"))
    }

    @Test
    fun `requires a blank frame before repeating after resume`() {
        val guard = DecodeGuard()
        assertEquals("a", guard.accept("a"))

        guard.reset(requireFreshFrame = true)
        assertNull(guard.accept("a"))
        assertNull(guard.accept(null))
        assertEquals("a", guard.accept("a"))
    }

    @Test
    fun `allows a different payload while waiting for a blank frame`() {
        val guard = DecodeGuard()
        assertEquals("a", guard.accept("a"))

        guard.reset(requireFreshFrame = true)
        assertEquals("b", guard.accept("b"))
        assertNull(guard.accept("b"))
    }

    @Test
    fun `reset without fresh frame clears the last payload`() {
        val guard = DecodeGuard()
        assertEquals("a", guard.accept("a"))

        guard.reset()
        assertEquals("a", guard.accept("a"))
    }
}
