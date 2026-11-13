package vn.io.elsutm.mbqr.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import vn.io.elsutm.mbqr.settings.AnalysisResolutionPreset
import vn.io.elsutm.mbqr.settings.AnalysisSize
import kotlin.math.abs

/** Camera analysis resolutions supported by the back camera. */
object CameraCapabilities {

    val fixedPresets = listOf(
        AnalysisResolutionPreset.P640 to AnalysisSize(640, 480),
        AnalysisResolutionPreset.P720 to AnalysisSize(1280, 720),
        AnalysisResolutionPreset.P1080 to AnalysisSize(1920, 1080),
    )

    fun supportedSizes(context: Context): List<AnalysisSize> = try {
        val manager = context.getSystemService(CameraManager::class.java) ?: return emptyList()
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: manager.cameraIdList.firstOrNull() ?: return emptyList()

        val characteristics = manager.getCameraCharacteristics(cameraId)
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

    fun contains(sizes: List<AnalysisSize>, size: AnalysisSize): Boolean =
        sizes.any { it.width == size.width && it.height == size.height }

    fun resolve(
        preset: AnalysisResolutionPreset,
        custom: AnalysisSize,
        supported: List<AnalysisSize>,
    ): AnalysisSize {
        if (supported.isEmpty()) return AnalysisSize(1280, 720)

        fixedPresets.firstOrNull { (candidate, size) ->
            candidate == preset && contains(supported, size)
        }?.let { return it.second }

        if (preset == AnalysisResolutionPreset.CUSTOM && contains(supported, custom)) {
            return custom
        }

        return autoSize(supported)
    }

    fun autoSize(supported: List<AnalysisSize>): AnalysisSize {
        if (supported.isEmpty()) return AnalysisSize(1280, 720)

        val preferred = listOf(
            AnalysisSize(1920, 1080),
            AnalysisSize(1280, 720),
            AnalysisSize(640, 480),
        )
        preferred.firstOrNull { contains(supported, it) }?.let { return it }

        return supported.minBy { size ->
            abs(size.width - 1280) + abs(size.height - 720) +
                if (size.width * 9 == size.height * 16) 0 else 1_000
        }
    }
}
