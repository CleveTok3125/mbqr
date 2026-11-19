package vn.io.elsutm.mbqr.settings

/** Factory values used by the settings screen reset actions. */
data class SettingsDefaults(
    val confirmBeforeOpeningMb: Boolean,
    val allowScreenshots: Boolean,
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
    val controlsPlacement: ControlsPlacement,
    val analysisResolutionPreset: AnalysisResolutionPreset,
    val analysisFpsPreset: AnalysisFpsPreset,
    val preferVietQrLink: Boolean,
    val linkBrowserMode: LinkBrowserMode,
    val browserPackage: String?,
)
