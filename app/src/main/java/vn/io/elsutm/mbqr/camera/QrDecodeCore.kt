package vn.io.elsutm.mbqr.camera

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

/**
 * Shared ZXing decode path for live camera frames and still images.
 *
 * Callers provide an 8-bit luminance plane. Keeping the source type identical
 * makes the camera and import paths differ only by how the luminance is
 * produced.
 */
internal object QrDecodeCore {

    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to "UTF-8",
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.ALSO_INVERTED to true,
    )

    fun decode(
        luminance: ByteArray,
        width: Int,
        height: Int,
        crop: PixelCrop? = null,
    ): String? {
        if (width <= 0 || height <= 0 || luminance.size < width * height) return null
        if (crop != null &&
            (crop.width <= 0 || crop.height <= 0 ||
                crop.left < 0 || crop.top < 0 ||
                crop.left + crop.width > width ||
                crop.top + crop.height > height)
        ) {
            return null
        }

        val source = if (crop == null) {
            PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        } else {
            PlanarYUVLuminanceSource(
                luminance,
                width,
                height,
                crop.left,
                crop.top,
                crop.width,
                crop.height,
                false,
            )
        }

        val reader = MultiFormatReader().apply { setHints(hints) }
        val binarizers = listOf(HybridBinarizer(source), GlobalHistogramBinarizer(source))
        for (binarizer in binarizers) {
            try {
                return reader.decodeWithState(BinaryBitmap(binarizer)).text
            } catch (_: ReaderException) {
                // Try the next binarizer.
            } finally {
                reader.reset()
            }
        }
        return null
    }
}
