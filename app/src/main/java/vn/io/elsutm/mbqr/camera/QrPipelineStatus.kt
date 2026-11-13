package vn.io.elsutm.mbqr.camera

import vn.io.elsutm.mbqr.settings.QrDecodePreset

enum class QrPipelineSource {
    LIVE,
    IMPORT,
    SHUTTER,
    SHARED,
}

enum class QrPipelineStage {
    DIRECT,
    PAD,
    UPSCALE,
    DONE,
}

data class QrPipelineStatus(
    val source: QrPipelineSource,
    val preset: QrDecodePreset,
    val stage: QrPipelineStage,
    val active: Boolean,
    val success: Boolean?,
    val detail: String,
    val elapsedMs: Long,
    val brightness: Int?,
)
