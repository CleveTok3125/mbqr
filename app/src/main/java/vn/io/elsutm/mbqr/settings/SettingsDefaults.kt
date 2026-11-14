package vn.io.elsutm.mbqr.settings

/** Factory values used by the settings screen reset actions. */
data class SettingsDefaults(
    val confirmBeforeOpeningMb: Boolean,
    val clipboardExpirySeconds: Int,
    val scanInsideFrame: Boolean,
    val scanFrameMarginPercent: Int,
    val sensitiveMaskingLevel: Int,
    val liveDecodePreset: QrDecodePreset,
    val stillDecodePreset: QrDecodePreset,
    val pipelineStatusEnabled: Boolean,
    val advancedCopyEnabled: Boolean,
    val sanitizeResults: Boolean,
    val hapticsEnabled: Boolean,
    val scanFrameStyle: ScanFrameStyle,
    val scanBoundaryStyle: ScanBoundaryStyle,
    val analysisResolutionPreset: AnalysisResolutionPreset,
    val analysisFpsPreset: AnalysisFpsPreset,
)
