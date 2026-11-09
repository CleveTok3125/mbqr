package vn.io.elsutm.mbqr.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlin.math.max

/**
 * Decodes a QR from a full-resolution captured photo. The image is sampled down
 * to a bounded size first so a 50 MP capture does not exhaust the heap.
 */
object PhotoQrDecoder {

    private const val MAX_DIMENSION = 4096

    fun decodeJpeg(bytes: ByteArray): String? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sampleSize = 1
            while (max(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_DIMENSION) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
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

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        return decodeSource(source)
    }

    private fun decodeSource(source: LuminanceSource): String? {
        val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.CHARACTER_SET to "UTF-8",
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.ALSO_INVERTED to true,
                ),
            )
        }

        val binarizers = listOf(HybridBinarizer(source), GlobalHistogramBinarizer(source))
        for (binarizer in binarizers) {
            try {
                return reader.decodeWithState(BinaryBitmap(binarizer)).text
            } catch (_: NotFoundException) {
                // Try the next binarizer.
            } finally {
                reader.reset()
            }
        }
        return null
    }
}
