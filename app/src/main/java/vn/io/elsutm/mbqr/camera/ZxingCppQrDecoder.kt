package vn.io.elsutm.mbqr.camera

import android.graphics.Bitmap
import android.util.Log
import zxingcpp.BarcodeReader

/**
 * Native ZXing-C++ fallback for still images.
 *
 * The Java ZXing path stays in use for live camera frames. This decoder is
 * loaded lazily when a still image cannot be decoded by the Java path.
 */
internal object ZxingCppQrDecoder {

    private const val TAG = "MBQR"

    @Volatile
    private var reader: BarcodeReader? = null

    @Volatile
    private var unavailable = false

    @Synchronized
    fun decode(bitmap: Bitmap): String? {
        if (unavailable) return null

        val active = reader ?: try {
            createReader().also { reader = it }
        } catch (error: Throwable) {
            unavailable = true
            Log.w(TAG, "zxing-cpp load failed", error)
            null
        } ?: return null

        return try {
            active.read(bitmap)
                .firstNotNullOfOrNull { it.text?.takeIf(String::isNotBlank) }
        } catch (error: Throwable) {
            Log.w(TAG, "zxing-cpp decode failed", error)
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
