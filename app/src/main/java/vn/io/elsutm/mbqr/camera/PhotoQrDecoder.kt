package vn.io.elsutm.mbqr.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.max

/**
 * Decodes a QR from a captured photo or an imported image.
 *
 * The image is converted to the same 8-bit luminance plane used by the live
 * camera, then passed to [QrDecodeCore]. Downsampling is bounded so a large
 * capture does not exhaust the heap.
 */
object PhotoQrDecoder {

    private const val MAX_DIMENSION = 4096

    fun decodeImage(bytes: ByteArray): String? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sampleSize = 1
            while (max(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_DIMENSION) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inPremultiplied = false
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            bitmap?.let {
                try {
                    decodeBitmap(it)
                } finally {
                    it.recycle()
                }
            }
        }
    } catch (_: OutOfMemoryError) {
        null
    } catch (_: Exception) {
        null
    }

    fun decodeBitmap(bitmap: Bitmap): String? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null

        val readable = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        } ?: return null

        return try {
            val luminance = readable.toLuminance() ?: return null
            QrDecodeCore.decode(luminance, readable.width, readable.height)
        } finally {
            if (readable !== bitmap) readable.recycle()
        }
    }

    private fun Bitmap.toLuminance(): ByteArray? {
        val pixels = IntArray(width * height)
        return try {
            getPixels(pixels, 0, width, 0, 0, width, height)
            val luminance = ByteArray(pixels.size)
            for (index in pixels.indices) {
                val color = pixels[index]
                val alpha = color ushr 24
                var red = (color shr 16) and 0xFF
                var green = (color shr 8) and 0xFF
                var blue = color and 0xFF

                // Transparent PNG backgrounds are commonly stored as black;
                // composite them onto white before calculating luminance.
                if (alpha != 0xFF) {
                    val inverse = 255 - alpha
                    red = (red * alpha + 255 * inverse + 127) / 255
                    green = (green * alpha + 255 * inverse + 127) / 255
                    blue = (blue * alpha + 255 * inverse + 127) / 255
                }

                luminance[index] = ((red * 77 + green * 150 + blue * 29) shr 8).toByte()
            }
            luminance
        } catch (_: Exception) {
            null
        }
    }
}
