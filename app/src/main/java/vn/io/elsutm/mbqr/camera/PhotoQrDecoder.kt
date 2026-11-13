package vn.io.elsutm.mbqr.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import vn.io.elsutm.mbqr.settings.QrDecodePreset
import kotlin.math.max

/**
 * Decodes a QR from a captured photo, imported image, or shared image with
 * ZXing-C++.
 *
 * Downsampling is bounded so a large capture does not exhaust the heap.
 * Transparent images are composited onto white before decoding.
 */
object PhotoQrDecoder {

    private const val MAX_DIMENSION = 4096

    fun decodeImage(
        bytes: ByteArray,
        preset: QrDecodePreset,
        source: QrPipelineSource = QrPipelineSource.IMPORT,
        onPipelineStatus: ((QrPipelineStatus) -> Unit)? = null,
    ): String? = try {
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
                    decodeBitmap(it, preset, source, onPipelineStatus)
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

    fun decodeBitmap(
        bitmap: Bitmap,
        preset: QrDecodePreset,
        source: QrPipelineSource = QrPipelineSource.IMPORT,
        onPipelineStatus: ((QrPipelineStatus) -> Unit)? = null,
    ): String? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null

        val startedAt = SystemClock.elapsedRealtime()
        var brightness: Int? = null
        fun emit(
            stage: QrPipelineStage,
            active: Boolean,
            success: Boolean?,
        ) {
            onPipelineStatus?.invoke(
                QrPipelineStatus(
                    source = source,
                    preset = preset,
                    stage = stage,
                    active = active,
                    success = success,
                    detail = "",
                    elapsedMs = SystemClock.elapsedRealtime() - startedAt,
                    brightness = brightness,
                ),
            )
        }

        val readable = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        } ?: return null
        brightness = if (onPipelineStatus != null) QrBrightness.fromBitmap(readable) else null

        val decoderBitmap = try {
            if (readable.hasAlpha()) readable.flattenOnWhite() else readable
        } catch (_: Throwable) {
            if (readable !== bitmap) readable.recycle()
            return null
        }

        return try {
            emit(QrPipelineStage.DIRECT, active = true, success = null)
            val direct = ZxingCppQrDecoder.decode(decoderBitmap)
            if (direct != null) {
                emit(QrPipelineStage.DIRECT, active = false, success = true)
                return direct
            }

            if (preset == QrDecodePreset.FAST) {
                emit(QrPipelineStage.DIRECT, active = false, success = false)
                return null
            }

            val text = ZxingCppQrDecoder.decodeWithFallback(
                decoderBitmap,
                null,
                preset,
                onStage = { stage -> emit(stage, active = true, success = null) },
            )
            emit(QrPipelineStage.DONE, active = false, success = text != null)
            text
        } finally {
            if (decoderBitmap !== readable) decoderBitmap.recycle()
            if (readable !== bitmap) readable.recycle()
        }
    }

    private fun Bitmap.flattenOnWhite(): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(this, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
            return output
        } catch (error: Throwable) {
            output.recycle()
            throw error
        }
    }
}
