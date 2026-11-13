package vn.io.elsutm.mbqr.camera

import android.graphics.Bitmap
import zxingcpp.BarcodeReader

/**
 * Native ZXing-C++ fallback for still images.
 *
 * The Java ZXing path stays in use for live camera frames. This decoder is
 * loaded lazily when a still image cannot be decoded by the Java path.
 */
internal object ZxingCppQrDecoder {

    @Volatile
    private var reader: BarcodeReader? = null

    @Volatile
    private var unavailable = false

    @Synchronized
    fun decode(bitmap: Bitmap): String? {
        if (unavailable) return null

        val active = reader ?: try {
            createReader().also { reader = it }
        } catch (_: Throwable) {
            unavailable = true
            null
        } ?: return null

        return try {
            active.read(bitmap)
                .firstNotNullOfOrNull { it.text?.takeIf(String::isNotBlank) }
        } catch (_: Throwable) {
            null
        }
    }

    private fun createReader(): BarcodeReader = BarcodeReader(
        BarcodeReader.Options(
            formats = setOf(BarcodeReader.Format.QR_CODE),
            tryHarder = true,
            tryRotate = true,
            tryInvert = true,
            tryDownscale = true,
            maxNumberOfSymbols = 1,
        ),
    )
}
