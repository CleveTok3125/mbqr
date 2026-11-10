package vn.io.elsutm.mbqr.camera

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlin.math.min

/**
 * Decodes QR codes from CameraX frames without Google Play services.
 *
 * Frames are throttled and duplicate results are ignored so a single QR does not
 * trigger the handoff repeatedly. The Y plane is used directly instead of
 * converting every frame to a bitmap, which keeps the analyzer responsive.
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
                DecodeHintType.ALSO_INVERTED to true,
            ),
        )
    }

    private var lastAttemptAt = 0L
    private val decodeGuard = DecodeGuard()
    private var loggedFirstFrame = false
    private var lastFailureLogAt = 0L

    override fun analyze(image: ImageProxy) {
        try {
            if (!enabled) return

            if (!loggedFirstFrame) {
                Log.d(
                    TAG,
                    "frame ${image.width}x${image.height} format=${image.format} " +
                        "rotation=${image.imageInfo.rotationDegrees}",
                )
                loggedFirstFrame = true
            }

            val now = SystemClock.elapsedRealtime()
            if (now - lastAttemptAt < THROTTLE_MILLIS) return
            lastAttemptAt = now

            val text = decodeGuard.accept(decode(image)) ?: return
            Log.d(TAG, "decoded payload length=${text.length}")
            onDecoded(text)
        } catch (error: Exception) {
            if (SystemClock.elapsedRealtime() - lastFailureLogAt > LOG_INTERVAL_MILLIS) {
                lastFailureLogAt = SystemClock.elapsedRealtime()
                Log.w(TAG, "frame analysis failed", error)
            }
        } finally {
            image.close()
        }
    }

    fun resetDuplicateGuard(requireFreshFrame: Boolean = false) {
        decodeGuard.reset(requireFreshFrame)
    }

    private fun decode(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null

        val luminance = extractLuminance(width, height, plane)
        val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)

        // QR detection is rotation independent; skipping the rotation copy keeps
        // analysis fast enough to run on every throttled frame.
        return decodeSource(source)
    }

    private fun decodeSource(source: LuminanceSource): String? {
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

    private fun extractLuminance(
        width: Int,
        height: Int,
        plane: ImageProxy.PlaneProxy,
    ): ByteArray {
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val luminance = ByteArray(width * height)

        if (pixelStride == 1 && rowStride == width) {
            buffer.rewind()
            buffer.get(luminance, 0, min(buffer.remaining(), luminance.size))
            return luminance
        }

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

        return luminance
    }

    private companion object {
        const val TAG = "MBQR"
        const val THROTTLE_MILLIS = 100L
        const val LOG_INTERVAL_MILLIS = 2_000L
    }
}
