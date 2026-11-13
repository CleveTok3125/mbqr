package vn.io.elsutm.mbqr.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import kotlin.math.max

/** Coarse luminance sample used only by the optional pipeline status UI. */
internal object QrBrightness {

    private const val SAMPLE_STEP = 8

    fun fromBitmap(bitmap: Bitmap): Int? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null
        var sum = 0L
        var count = 0
        val step = max(1, SAMPLE_STEP)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val color = bitmap.getPixel(x, y)
                val red = (color shr 16) and 0xFF
                val green = (color shr 8) and 0xFF
                val blue = color and 0xFF
                sum += (red * 77 + green * 150 + blue * 29) shr 8
                count++
            }
        }
        return if (count == 0) null else (sum / count).toInt()
    }

    fun fromImage(image: ImageProxy): Int? {
        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null

        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        var sum = 0L
        var count = 0
        for (y in 0 until height step SAMPLE_STEP) {
            val row = y * rowStride
            for (x in 0 until width step SAMPLE_STEP) {
                val index = row + x * pixelStride
                if (index >= buffer.limit()) break
                sum += buffer.get(index).toInt() and 0xFF
                count++
            }
        }
        return if (count == 0) null else (sum / count).toInt()
    }
}
