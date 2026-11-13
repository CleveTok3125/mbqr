package vn.io.elsutm.mbqr.settings

/** Decode pipeline for live camera frames or still images. */
enum class QrDecodePreset {
    FAST,
    BALANCED,
    MAXIMUM,
}

/** Requested CameraX analysis resolution. */
enum class AnalysisResolutionPreset {
    AUTO,
    P640,
    P720,
    P1080,
    CUSTOM,
}

/** Requested live analysis frame rate. */
enum class AnalysisFpsPreset(val fps: Int?) {
    FPS_10(10),
    FPS_24(24),
    FPS_30(30),
    FPS_60(60),
    CUSTOM(null),
    ;

    companion object {
        val fixed: List<AnalysisFpsPreset> = entries.filter { it.fps != null }

        fun fromFps(fps: Int): AnalysisFpsPreset =
            entries.firstOrNull { it.fps == fps } ?: CUSTOM
    }
}

/** Camera analysis size in pixels. */
data class AnalysisSize(
    val width: Int,
    val height: Int,
) {
    override fun toString(): String = "${width}×$height"
}
