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

/** Camera analysis size in pixels. */
data class AnalysisSize(
    val width: Int,
    val height: Int,
) {
    override fun toString(): String = "${width}×$height"
}
