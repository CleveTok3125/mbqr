package vn.io.elsutm.mbqr.camera

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import vn.io.elsutm.mbqr.settings.QrDecodePreset

/**
 * Decodes QR codes from CameraX frames with ZXing-C++.
 *
 * Frames are throttled and duplicate results are ignored so a single QR does not
 * trigger the handoff repeatedly. ZXing-C++ reads the Y plane directly.
 */
class QrAnalyzer : ImageAnalysis.Analyzer {

    var onDecoded: (String) -> Unit = {}

    @Volatile
    var enabled: Boolean = true

    @Volatile
    var decodePreset: QrDecodePreset = QrDecodePreset.FAST

    @Volatile
    var targetFps: Int = 10

    @Volatile
    var onPipelineStatus: ((QrPipelineStatus) -> Unit)? = null

    @Volatile
    private var cropViewport = CropViewport(crop = null, viewWidth = 0, viewHeight = 0)

    private var lastAttemptAt = 0L
    private var lastFallbackAt = 0L
    private val decodeGuard = DecodeGuard()

    override fun analyze(image: ImageProxy) {
        try {
            if (!enabled) return

            val now = SystemClock.elapsedRealtime()
            val throttleMillis = (1_000L / targetFps.coerceAtLeast(1)).coerceAtLeast(1L)
            if (now - lastAttemptAt < throttleMillis) return
            lastAttemptAt = now

            val text = decodeGuard.accept(decode(image)) ?: return
            onDecoded(text)
        } catch (_: Exception) {
            // A bad frame must not stop the analyzer.
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
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null

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
        val startedAt = SystemClock.elapsedRealtime()
        val report = onPipelineStatus
        val brightness = if (report != null) QrBrightness.fromImage(image) else null
        fun emit(
            stage: QrPipelineStage,
            active: Boolean,
            success: Boolean?,
        ) {
            report?.invoke(
                QrPipelineStatus(
                    source = QrPipelineSource.LIVE,
                    preset = decodePreset,
                    stage = stage,
                    active = active,
                    success = success,
                    detail = "",
                    elapsedMs = SystemClock.elapsedRealtime() - startedAt,
                    brightness = brightness,
                ),
            )
        }

        emit(QrPipelineStage.DIRECT, active = true, success = null)
        val direct = ZxingCppQrDecoder.decode(image, pixelCrop)
        if (direct != null) {
            emit(QrPipelineStage.DIRECT, active = false, success = true)
            return direct
        }
        if (decodePreset == QrDecodePreset.FAST) {
            emit(QrPipelineStage.DIRECT, active = false, success = false)
            return null
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastFallbackAt < FALLBACK_INTERVAL_MILLIS) {
            emit(QrPipelineStage.DIRECT, active = false, success = false)
            return null
        }
        lastFallbackAt = now

        val bitmap = try {
            image.toBitmap()
        } catch (_: Exception) {
            null
        } ?: run {
            emit(QrPipelineStage.DIRECT, active = false, success = false)
            return null
        }

        return try {
            val text = ZxingCppQrDecoder.decodeWithFallback(
                bitmap,
                pixelCrop,
                decodePreset,
                onStage = { stage -> emit(stage, active = true, success = null) },
            )
            emit(QrPipelineStage.DONE, active = false, success = text != null)
            text
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val FALLBACK_INTERVAL_MILLIS = 500L
    }
}

private data class CropViewport(
    val crop: ScanCrop?,
    val viewWidth: Int,
    val viewHeight: Int,
)
