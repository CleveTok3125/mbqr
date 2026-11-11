package vn.io.elsutm.mbqr.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanCropMapperTest {

    @Test
    fun `maps a crop without rotation`() {
        val crop = ScanCrop(0.2f, 0.2f, 0.8f, 0.8f)

        assertEquals(
            PixelCrop(left = 200, top = 100, width = 600, height = 300),
            ScanCropMapper.map(
                crop = crop,
                viewWidth = 1000,
                viewHeight = 500,
                imageWidth = 1000,
                imageHeight = 500,
                rotationDegrees = 0,
            ),
        )
    }

    @Test
    fun `maps the full view through portrait fill center`() {
        assertEquals(
            PixelCrop(left = 0, top = 72, width = 1280, height = 576),
            ScanCropMapper.map(
                crop = ScanCrop(0f, 0f, 1f, 1f),
                viewWidth = 1080,
                viewHeight = 2400,
                imageWidth = 1280,
                imageHeight = 720,
                rotationDegrees = 90,
            ),
        )
    }

    @Test
    fun `maps the left half of the view through rotation 90`() {
        assertEquals(
            PixelCrop(left = 0, top = 360, width = 1280, height = 288),
            ScanCropMapper.map(
                crop = ScanCrop(0f, 0f, 0.5f, 1f),
                viewWidth = 1080,
                viewHeight = 2400,
                imageWidth = 1280,
                imageHeight = 720,
                rotationDegrees = 90,
            ),
        )
    }

    @Test
    fun `maps the left half of the view through rotation 270`() {
        assertEquals(
            PixelCrop(left = 0, top = 72, width = 1280, height = 288),
            ScanCropMapper.map(
                crop = ScanCrop(0f, 0f, 0.5f, 1f),
                viewWidth = 1080,
                viewHeight = 2400,
                imageWidth = 1280,
                imageHeight = 720,
                rotationDegrees = 270,
            ),
        )
    }

    @Test
    fun `maps a full view through rotation 180`() {
        assertEquals(
            PixelCrop(left = 478, top = 0, width = 324, height = 720),
            ScanCropMapper.map(
                crop = ScanCrop(0f, 0f, 1f, 1f),
                viewWidth = 1080,
                viewHeight = 2400,
                imageWidth = 1280,
                imageHeight = 720,
                rotationDegrees = 180,
            ),
        )
    }

    @Test
    fun `accounts for a four by three image in a widescreen view`() {
        assertEquals(
            PixelCrop(left = 0, top = 60, width = 640, height = 360),
            ScanCropMapper.map(
                crop = ScanCrop(0f, 0f, 1f, 1f),
                viewWidth = 1600,
                viewHeight = 900,
                imageWidth = 640,
                imageHeight = 480,
                rotationDegrees = 0,
            ),
        )
    }

    @Test
    fun `rejects invalid viewports and rotations`() {
        assertNull(
            ScanCropMapper.map(
                crop = ScanCrop(0f, 0f, 1f, 1f),
                viewWidth = 0,
                viewHeight = 2400,
                imageWidth = 1280,
                imageHeight = 720,
                rotationDegrees = 90,
            ),
        )
        assertNull(
            ScanCropMapper.map(
                crop = ScanCrop(0.8f, 0f, 0.2f, 1f),
                viewWidth = 1080,
                viewHeight = 2400,
                imageWidth = 1280,
                imageHeight = 720,
                rotationDegrees = 90,
            ),
        )
        assertNull(
            ScanCropMapper.map(
                crop = ScanCrop(0f, 0f, 1f, 1f),
                viewWidth = 1080,
                viewHeight = 2400,
                imageWidth = 1280,
                imageHeight = 720,
                rotationDegrees = 45,
            ),
        )
    }
}
