package vn.io.elsutm.mbqr.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import vn.io.elsutm.mbqr.settings.QrDecodePreset
import zxingcpp.BarcodeReader
import kotlin.math.max
import kotlin.math.min

/**
 * ZXing-C++ decoder used by both live camera frames and still images.
 *
 * Live frames are read directly from [ImageProxy]. Still images and fallback
 * candidates use [QrDecodePreset] to choose a fixed, predictable pipeline.
 */
internal object ZxingCppQrDecoder {

    private enum class ReaderKind { Live, Still, Maximum }

    @Volatile
    private var liveReader: BarcodeReader? = null

    @Volatile
    private var stillReader: BarcodeReader? = null

    @Volatile
    private var maximumReader: BarcodeReader? = null

    @Volatile
    private var unavailable = false

    private val readerLock = Any()

    fun decode(image: ImageProxy, crop: PixelCrop?): String? {
        val reader = reader(ReaderKind.Live) ?: return null
        val previousCrop = if (crop != null) image.cropRect else null
        if (crop != null) {
            image.setCropRect(
                Rect(
                    crop.left,
                    crop.top,
                    crop.left + crop.width,
                    crop.top + crop.height,
                ),
            )
        }

        return try {
            reader.read(image)
                .firstNotNullOfOrNull { it.text?.takeIf(String::isNotBlank) }
        } catch (_: Throwable) {
            null
        } finally {
            if (previousCrop != null) image.setCropRect(previousCrop)
        }
    }

    fun decode(bitmap: Bitmap): String? {
        val reader = reader(ReaderKind.Still) ?: return null
        return decode(reader, bitmap)
    }

    fun decodeWithFallback(
        bitmap: Bitmap,
        crop: PixelCrop?,
        preset: QrDecodePreset,
        onStage: ((QrPipelineStage) -> Unit)? = null,
    ): String? {
        if (preset == QrDecodePreset.FAST) return null
        if (bitmap.width <= 0 || bitmap.height <= 0) return null

        val source = cropBitmap(bitmap, crop)
        return try {
            val kind = if (preset == QrDecodePreset.MAXIMUM) {
                ReaderKind.Maximum
            } else {
                ReaderKind.Still
            }
            onStage?.invoke(QrPipelineStage.PAD)
            val padded = padWithWhite(source)
            try {
                decodeCandidate(padded, kind)?.let { return it }

                if (preset == QrDecodePreset.MAXIMUM) {
                    onStage?.invoke(QrPipelineStage.UPSCALE)
                    val scaled = scaleUp(padded)
                    if (scaled !== padded) {
                        try {
                            decodeCandidate(scaled, ReaderKind.Maximum)?.let { return it }
                        } finally {
                            scaled.recycle()
                        }
                    }
                }
            } finally {
                padded.recycle()
            }
            null
        } finally {
            if (source !== bitmap) source.recycle()
        }
    }

    private fun decodeCandidate(bitmap: Bitmap, kind: ReaderKind): String? {
        val reader = reader(kind) ?: return null
        return decode(reader, bitmap)
    }

    private fun decode(reader: BarcodeReader, bitmap: Bitmap): String? = try {
        reader.read(bitmap)
            .firstNotNullOfOrNull { it.text?.takeIf(String::isNotBlank) }
    } catch (_: Throwable) {
        null
    }

    private fun reader(kind: ReaderKind): BarcodeReader? {
        if (unavailable) return null
        val current = when (kind) {
            ReaderKind.Live -> liveReader
            ReaderKind.Still -> stillReader
            ReaderKind.Maximum -> maximumReader
        }
        if (current != null) return current

        synchronized(readerLock) {
            if (unavailable) return null
            val existing = when (kind) {
                ReaderKind.Live -> liveReader
                ReaderKind.Still -> stillReader
                ReaderKind.Maximum -> maximumReader
            }
            if (existing != null) return existing

            val created = try {
                createReader(kind)
            } catch (_: Throwable) {
                unavailable = true
                return null
            }
            when (kind) {
                ReaderKind.Live -> liveReader = created
                ReaderKind.Still -> stillReader = created
                ReaderKind.Maximum -> maximumReader = created
            }
            return created
        }
    }

    private fun createReader(kind: ReaderKind): BarcodeReader = BarcodeReader(
        BarcodeReader.Options(
            formats = setOf(BarcodeReader.Format.QR_CODE),
            tryHarder = true,
            tryRotate = true,
            tryInvert = true,
            tryDownscale = kind != ReaderKind.Live,
            tryDenoise = kind == ReaderKind.Maximum,
            maxNumberOfSymbols = 1,
        ),
    )

    private fun cropBitmap(bitmap: Bitmap, crop: PixelCrop?): Bitmap {
        if (crop == null) return bitmap
        val left = crop.left.coerceIn(0, bitmap.width - 1)
        val top = crop.top.coerceIn(0, bitmap.height - 1)
        val right = (crop.left + crop.width).coerceIn(left + 1, bitmap.width)
        val bottom = (crop.top + crop.height).coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    private fun padWithWhite(source: Bitmap): Bitmap {
        val border = max(MIN_PADDING_PX, (max(source.width, source.height) * PADDING_FRACTION).toInt())
        val output = Bitmap.createBitmap(
            source.width + border * 2,
            source.height + border * 2,
            Bitmap.Config.ARGB_8888,
        )
        try {
            val canvas = Canvas(output)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(source, border.toFloat(), border.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
            return output
        } catch (error: Throwable) {
            output.recycle()
            throw error
        }
    }

    private fun scaleUp(source: Bitmap): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= 0 || longest >= MAX_FALLBACK_DIMENSION) return source
        val target = min(longest * 2, MAX_FALLBACK_DIMENSION)
        if (target <= longest || target / longest < 1.25f) return source
        val scale = target.toFloat() / longest
        return Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private const val MIN_PADDING_PX = 16
    private const val PADDING_FRACTION = 0.06f
    private const val MAX_FALLBACK_DIMENSION = 2048
}
