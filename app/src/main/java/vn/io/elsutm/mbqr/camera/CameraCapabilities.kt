package vn.io.elsutm.mbqr.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import vn.io.elsutm.mbqr.settings.AnalysisResolutionPreset
import vn.io.elsutm.mbqr.settings.AnalysisSize
import kotlin.math.roundToInt

/** Frame-rate capability for one camera and analysis size. */
data class AnalysisFpsSupport(
    val maxFps: Int,
    val ranges: List<IntRange>,
    val known: Boolean,
)

/** Camera analysis resolutions supported by the back camera. */
object CameraCapabilities {

    private const val DEFAULT_MAX_FPS = 60

    val fixedPresets = listOf(
        AnalysisResolutionPreset.P640 to AnalysisSize(640, 480),
        AnalysisResolutionPreset.P720 to AnalysisSize(1280, 720),
        AnalysisResolutionPreset.P1080 to AnalysisSize(1920, 1080),
    )

    fun supportedSizes(context: Context): List<AnalysisSize> = try {
        val characteristics = backCameraCharacteristics(context) ?: return emptyList()
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return emptyList()
        map.getOutputSizes(ImageFormat.YUV_420_888)
            ?.map { AnalysisSize(it.width, it.height) }
            ?.distinct()
            ?.sortedWith(compareBy({ it.width * it.height }, { it.width }))
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    /**
     * Reads the back camera's AE frame-rate ranges and the maximum frame rate
     * allowed at [size].
     *
     * [AnalysisFpsSupport.known] is false when the camera has no usable
     * capability data, so callers can avoid downgrading a stored preset.
     */
    fun fpsSupport(context: Context, size: AnalysisSize): AnalysisFpsSupport = try {
        val characteristics = backCameraCharacteristics(context)
            ?: return AnalysisFpsSupport(DEFAULT_MAX_FPS, emptyList(), known = false)
        val ranges = characteristics
            .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.mapNotNull { range ->
                val lower = range.lower
                val upper = range.upper
                if (lower > 0 && upper >= lower) lower..upper else null
            }
            ?.distinct()
            ?.sortedWith(compareBy({ it.first }, { it.last }))
            ?: emptyList()
        val minFrameDurationNanos = characteristics
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputMinFrameDuration(
                ImageFormat.YUV_420_888,
                android.util.Size(size.width, size.height),
            )
            ?: 0L
        val known = ranges.isNotEmpty() || minFrameDurationNanos > 0L
        val maxFps = maxSupportedFps(
            aeMaxFps = ranges.maxOfOrNull { it.last },
            minFrameDurationNanos = minFrameDurationNanos,
        )
        AnalysisFpsSupport(
            maxFps = if (known) maxFps else DEFAULT_MAX_FPS,
            ranges = ranges,
            known = known,
        )
    } catch (_: Exception) {
        AnalysisFpsSupport(DEFAULT_MAX_FPS, emptyList(), known = false)
    }

    /**
     * Picks a supported AE range for [fps].
     *
     * Exact ranges win, then the narrowest containing range, then the lowest
     * range that can run at least [fps]. Returns null when the camera exposes
     * no usable range.
     */
    fun targetFpsRange(fps: Int, support: AnalysisFpsSupport): IntRange? {
        val requested = fps.coerceIn(1, support.maxFps)
        val usable = support.ranges.filter { it.last <= support.maxFps }
        if (usable.isEmpty()) return null

        return usable.firstOrNull { it.first == requested && it.last == requested }
            ?: usable
                .filter { requested in it }
                .minWithOrNull(compareBy({ it.last - it.first }, { -it.first }))
            ?: usable
                .filter { it.last >= requested }
                .minByOrNull { it.last }
            ?: usable.maxByOrNull { it.last }
    }

    internal fun maxSupportedFps(
        aeMaxFps: Int?,
        minFrameDurationNanos: Long,
    ): Int {
        val sizeMax = if (minFrameDurationNanos > 0L) {
            (1_000_000_000.0 / minFrameDurationNanos)
                .roundToInt()
                .coerceAtLeast(1)
        } else {
            null
        }
        return listOfNotNull(aeMaxFps, sizeMax)
            .minOrNull()
            ?.coerceAtLeast(1)
            ?: DEFAULT_MAX_FPS
    }

    private fun backCameraCharacteristics(context: Context): CameraCharacteristics? {
        val manager = context.getSystemService(CameraManager::class.java) ?: return null
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: manager.cameraIdList.firstOrNull() ?: return null
        return manager.getCameraCharacteristics(cameraId)
    }

    fun contains(sizes: List<AnalysisSize>, size: AnalysisSize): Boolean =
        sizes.any { it.width == size.width && it.height == size.height }

    fun resolve(
        preset: AnalysisResolutionPreset,
        custom: AnalysisSize,
    ): AnalysisSize =
        fixedPresets.firstOrNull { (candidate, _) -> candidate == preset }?.second
            ?: custom
}
