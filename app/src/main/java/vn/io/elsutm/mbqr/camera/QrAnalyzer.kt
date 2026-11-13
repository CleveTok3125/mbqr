package vn.io.elsutm.mbqr.camera

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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

    @Volatile
    private var cropViewport = CropViewport(crop = null, viewWidth = 0, viewHeight = 0)

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

    fun setScanCrop(crop: ScanCrop?, viewWidth: Int, viewHeight: Int) {
        cropViewport = CropViewport(crop, viewWidth, viewHeight)
    }

    private fun decode(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null

        val luminance = extractLuminance(width, height, plane)
        val viewport = cropViewport
        val pixelCrop = viewport.crop?.let { crop ->
            ScanCropMapper.map(
                crop = crop,
                viewWidth = viewport.viewWidth,
                viewHeight = viewport.viewHeight,
                imageWidth = width,
                imageHeight = height,
                rotationDegrees = image.imageInfo.rotationDegrees,
            )
        }
        // QR detection is rotation independent; skipping the rotation copy keeps
        // analysis fast enough to run on every throttled frame.
        return QrDecodeCore.decode(luminance, width, height, pixelCrop)
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

private data class CropViewport(
    val crop: ScanCrop?,
    val viewWidth: Int,
    val viewHeight: Int,
)
