package vn.io.elsutm.mbqr.camera

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlin.math.min

/**
 * Decodes QR codes from CameraX frames without Google Play services.
 *
 * Frames are throttled and duplicate results are ignored so a single QR does not
 * trigger the handoff repeatedly.
 */
class QrAnalyzer : ImageAnalysis.Analyzer {

    var onDecoded: (String) -> Unit = {}

    @Volatile
    var enabled: Boolean = true

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.CHARACTER_SET to "UTF-8",
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }

    private var lastAttemptAt = 0L
    private var lastDecoded: String? = null

    override fun analyze(image: ImageProxy) {
        try {
            if (!enabled) return

            val now = SystemClock.elapsedRealtime()
            if (now - lastAttemptAt < THROTTLE_MILLIS) return
            lastAttemptAt = now

            val text = decode(image) ?: return
            if (text == lastDecoded) return
            lastDecoded = text
            onDecoded(text)
        } catch (_: Exception) {
            // A single bad frame must not stop the analyzer.
        } finally {
            image.close()
        }
    }

    fun resetDuplicateGuard() {
        lastDecoded = null
    }

    private fun decode(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null

        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val luminance = ByteArray(width * height)

        if (pixelStride == 1 && rowStride == width) {
            buffer.rewind()
            buffer.get(luminance, 0, min(buffer.remaining(), luminance.size))
        } else {
            val row = ByteArray(rowStride)
            for (rowIndex in 0 until height) {
                val offset = rowIndex * rowStride
                val remaining = buffer.limit() - offset
                if (remaining <= 0) break

                buffer.position(offset)
                val count = min(rowStride, remaining)
                buffer.get(row, 0, count)

                val targetOffset = rowIndex * width
                for (column in 0 until width) {
                    val sourceIndex = column * pixelStride
                    if (sourceIndex >= count) break
                    luminance[targetOffset + column] = row[sourceIndex]
                }
            }
        }

        var source: LuminanceSource =
            PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        source = when (image.imageInfo.rotationDegrees) {
            90 -> source.rotateCounterClockwise()
            180 -> source.rotateCounterClockwise().rotateCounterClockwise()
            270 -> source.rotateCounterClockwise().rotateCounterClockwise().rotateCounterClockwise()
            else -> source
        }

        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            reader.decodeWithState(bitmap).text
        } finally {
            reader.reset()
        }
    }

    private companion object {
        const val THROTTLE_MILLIS = 100L
    }
}
