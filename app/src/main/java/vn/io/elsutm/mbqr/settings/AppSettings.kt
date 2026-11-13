package vn.io.elsutm.mbqr.settings

import android.content.Context
import androidx.core.content.edit
import kotlin.math.roundToInt

/** Persistent preferences for the MB Bank handoff. */
object AppSettings {

    const val MIN_CLIPBOARD_EXPIRY_SECONDS = 15
    const val MAX_CLIPBOARD_EXPIRY_SECONDS = 300
    const val CLIPBOARD_EXPIRY_STEP_SECONDS = 15
    const val CLIPBOARD_TIMER_OFF_SECONDS = 0
    const val DEFAULT_CLIPBOARD_EXPIRY_SECONDS = 60

    const val MIN_SCAN_FRAME_MARGIN_PERCENT = 0
    const val MAX_SCAN_FRAME_MARGIN_PERCENT = 100
    const val SCAN_FRAME_MARGIN_STEP_PERCENT = 5
    const val DEFAULT_SCAN_FRAME_MARGIN_PERCENT = 50

    const val MASKING_OFF = 0
    const val MASKING_LIGHT = 1
    const val MASKING_FULL = 2
    const val DEFAULT_MASKING_LEVEL = MASKING_OFF

    const val DEFAULT_ANALYSIS_FPS = 10

    private const val PREFERENCES = "mbqr_settings"
    private const val CONFIRM_BEFORE_OPENING_MB = "confirm_before_opening_mb"
    private const val CLIPBOARD_EXPIRY_SECONDS = "clipboard_expiry_seconds"
    private const val SCAN_INSIDE_FRAME = "scan_inside_frame"
    private const val SCAN_FRAME_MARGIN_PERCENT_KEY = "scan_frame_margin_percent"
    private const val SENSITIVE_MASKING_LEVEL = "sensitive_masking_level"
    private const val LEGACY_HIDE_SENSITIVE_FIELDS = "hide_sensitive_fields"
    private const val DEFAULT_CONFIRM_BEFORE_OPENING_MB = true
    private const val DEFAULT_SCAN_INSIDE_FRAME = false
    private const val LIVE_DECODE_PRESET = "live_decode_preset"
    private const val STILL_DECODE_PRESET = "still_decode_preset"
    private const val ANALYSIS_RESOLUTION_PRESET = "analysis_resolution_preset"
    private const val CUSTOM_ANALYSIS_WIDTH = "custom_analysis_width"
    private const val CUSTOM_ANALYSIS_HEIGHT = "custom_analysis_height"
    private const val PIPELINE_STATUS_ENABLED = "pipeline_status_enabled"
    private const val ANALYSIS_FPS = "analysis_fps"
    private const val ANALYSIS_FPS_PRESET = "analysis_fps_preset"
    private const val CUSTOM_ANALYSIS_FPS = "custom_analysis_fps"
    private const val DEFAULT_LIVE_DECODE_PRESET = "FAST"
    private const val DEFAULT_STILL_DECODE_PRESET = "BALANCED"
    private const val DEFAULT_ANALYSIS_RESOLUTION_PRESET = "AUTO"
    private const val DEFAULT_CUSTOM_ANALYSIS_WIDTH = 1280
    private const val DEFAULT_CUSTOM_ANALYSIS_HEIGHT = 720

    fun confirmBeforeOpeningMb(context: Context): Boolean =
        preferences(context).getBoolean(
            CONFIRM_BEFORE_OPENING_MB,
            DEFAULT_CONFIRM_BEFORE_OPENING_MB,
        )

    fun setConfirmBeforeOpeningMb(context: Context, enabled: Boolean) {
        preferences(context).edit { putBoolean(CONFIRM_BEFORE_OPENING_MB, enabled) }
    }

    fun scanInsideFrame(context: Context): Boolean =
        preferences(context).getBoolean(SCAN_INSIDE_FRAME, DEFAULT_SCAN_INSIDE_FRAME)

    fun setScanInsideFrame(context: Context, enabled: Boolean) {
        preferences(context).edit { putBoolean(SCAN_INSIDE_FRAME, enabled) }
    }

    fun sensitiveMaskingLevel(context: Context): Int {
        val prefs = preferences(context)
        if (prefs.contains(SENSITIVE_MASKING_LEVEL)) {
            return boundedMaskingLevel(
                prefs.getInt(SENSITIVE_MASKING_LEVEL, DEFAULT_MASKING_LEVEL),
            )
        }
        return if (prefs.getBoolean(LEGACY_HIDE_SENSITIVE_FIELDS, false)) {
            MASKING_FULL
        } else {
            MASKING_OFF
        }
    }

    fun setSensitiveMaskingLevel(context: Context, level: Int) {
        preferences(context).edit {
            putInt(SENSITIVE_MASKING_LEVEL, boundedMaskingLevel(level))
        }
    }

    internal fun boundedMaskingLevel(level: Int): Int =
        level.coerceIn(MASKING_OFF, MASKING_FULL)

    fun scanFrameMarginPercent(context: Context): Int =
        boundedScanFrameMarginPercent(
            preferences(context).getInt(
                SCAN_FRAME_MARGIN_PERCENT_KEY,
                DEFAULT_SCAN_FRAME_MARGIN_PERCENT,
            ),
        )

    fun setScanFrameMarginPercent(context: Context, percent: Int) {
        preferences(context).edit {
            putInt(SCAN_FRAME_MARGIN_PERCENT_KEY, boundedScanFrameMarginPercent(percent))
        }
    }

    fun liveDecodePreset(context: Context): QrDecodePreset =
        decodePreset(
            preferences(context).getString(
                LIVE_DECODE_PRESET,
                DEFAULT_LIVE_DECODE_PRESET,
            ),
            QrDecodePreset.FAST,
        )

    fun setLiveDecodePreset(context: Context, preset: QrDecodePreset) {
        preferences(context).edit { putString(LIVE_DECODE_PRESET, preset.name) }
    }

    fun stillDecodePreset(context: Context): QrDecodePreset =
        decodePreset(
            preferences(context).getString(
                STILL_DECODE_PRESET,
                DEFAULT_STILL_DECODE_PRESET,
            ),
            QrDecodePreset.BALANCED,
        )

    fun setStillDecodePreset(context: Context, preset: QrDecodePreset) {
        preferences(context).edit { putString(STILL_DECODE_PRESET, preset.name) }
    }

    fun analysisResolutionPreset(context: Context): AnalysisResolutionPreset =
        analysisResolutionPreset(
            preferences(context).getString(
                ANALYSIS_RESOLUTION_PRESET,
                DEFAULT_ANALYSIS_RESOLUTION_PRESET,
            ),
        )

    fun setAnalysisResolutionPreset(context: Context, preset: AnalysisResolutionPreset) {
        preferences(context).edit {
            putString(ANALYSIS_RESOLUTION_PRESET, preset.name)
        }
    }

    fun pipelineStatusEnabled(context: Context): Boolean =
        preferences(context).getBoolean(PIPELINE_STATUS_ENABLED, false)

    fun setPipelineStatusEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit { putBoolean(PIPELINE_STATUS_ENABLED, enabled) }
    }

    fun analysisFpsPreset(context: Context): AnalysisFpsPreset {
        val prefs = preferences(context)
        val legacyFps = prefs.getInt(ANALYSIS_FPS, DEFAULT_ANALYSIS_FPS)
        return analysisFpsPreset(
            prefs.getString(ANALYSIS_FPS_PRESET, null),
            legacyFps,
        )
    }

    fun setAnalysisFpsPreset(context: Context, preset: AnalysisFpsPreset) {
        preferences(context).edit {
            putString(ANALYSIS_FPS_PRESET, preset.name)
            preset.fps?.let { putInt(ANALYSIS_FPS, it) }
        }
    }

    fun customAnalysisFps(context: Context): Int {
        val prefs = preferences(context)
        return boundedAnalysisFps(
            prefs.getInt(
                CUSTOM_ANALYSIS_FPS,
                prefs.getInt(ANALYSIS_FPS, DEFAULT_ANALYSIS_FPS),
            ),
        )
    }

    fun setCustomAnalysisFps(context: Context, fps: Int) {
        val bounded = boundedAnalysisFps(fps)
        preferences(context).edit {
            putInt(CUSTOM_ANALYSIS_FPS, bounded)
            putInt(ANALYSIS_FPS, bounded)
        }
    }

    fun analysisFps(context: Context): Int =
        analysisFpsPreset(context).fps ?: customAnalysisFps(context)

    internal fun boundedAnalysisFps(fps: Int): Int =
        fps.coerceAtLeast(1)

    fun customAnalysisSize(context: Context): AnalysisSize {
        val prefs = preferences(context)
        return AnalysisSize(
            prefs.getInt(CUSTOM_ANALYSIS_WIDTH, DEFAULT_CUSTOM_ANALYSIS_WIDTH)
                .coerceAtLeast(1),
            prefs.getInt(CUSTOM_ANALYSIS_HEIGHT, DEFAULT_CUSTOM_ANALYSIS_HEIGHT)
                .coerceAtLeast(1),
        )
    }

    fun setCustomAnalysisSize(context: Context, size: AnalysisSize) {
        preferences(context).edit {
            putInt(CUSTOM_ANALYSIS_WIDTH, size.width.coerceAtLeast(1))
            putInt(CUSTOM_ANALYSIS_HEIGHT, size.height.coerceAtLeast(1))
        }
    }

    private fun decodePreset(value: String?, fallback: QrDecodePreset): QrDecodePreset =
        QrDecodePreset.entries.firstOrNull { it.name == value } ?: fallback

    private fun analysisResolutionPreset(value: String?): AnalysisResolutionPreset =
        AnalysisResolutionPreset.entries.firstOrNull { it.name == value }
            ?: AnalysisResolutionPreset.AUTO

    private fun analysisFpsPreset(value: String?, legacyFps: Int): AnalysisFpsPreset =
        AnalysisFpsPreset.entries.firstOrNull { it.name == value }
            ?: AnalysisFpsPreset.fromFps(legacyFps)

    fun clipboardExpirySeconds(context: Context): Int =
        boundedExpirySeconds(
            preferences(context)
                .getInt(CLIPBOARD_EXPIRY_SECONDS, DEFAULT_CLIPBOARD_EXPIRY_SECONDS),
        )

    fun setClipboardExpirySeconds(context: Context, seconds: Int) {
        preferences(context).edit {
            putInt(CLIPBOARD_EXPIRY_SECONDS, boundedExpirySeconds(seconds))
        }
    }

    fun clipboardExpiryMillis(context: Context): Long =
        clipboardExpirySeconds(context) * 1_000L

    internal fun boundedExpirySeconds(seconds: Int): Int {
        if (seconds <= CLIPBOARD_TIMER_OFF_SECONDS) return CLIPBOARD_TIMER_OFF_SECONDS

        val bounded = seconds.coerceIn(
            MIN_CLIPBOARD_EXPIRY_SECONDS,
            MAX_CLIPBOARD_EXPIRY_SECONDS,
        )
        val step = CLIPBOARD_EXPIRY_STEP_SECONDS.coerceAtLeast(1)
        val offset = bounded - MIN_CLIPBOARD_EXPIRY_SECONDS
        return (MIN_CLIPBOARD_EXPIRY_SECONDS + ((offset + step / 2) / step) * step)
            .coerceIn(MIN_CLIPBOARD_EXPIRY_SECONDS, MAX_CLIPBOARD_EXPIRY_SECONDS)
    }

    internal fun snappedSliderExpirySeconds(value: Float): Int {
        val step = CLIPBOARD_EXPIRY_STEP_SECONDS.coerceAtLeast(1)
        val snapped = (value / step).roundToInt() * step
        return snapped.coerceIn(CLIPBOARD_TIMER_OFF_SECONDS, MAX_CLIPBOARD_EXPIRY_SECONDS)
    }

    internal fun boundedScanFrameMarginPercent(percent: Int): Int {
        if (percent <= MIN_SCAN_FRAME_MARGIN_PERCENT) {
            return MIN_SCAN_FRAME_MARGIN_PERCENT
        }
        val step = SCAN_FRAME_MARGIN_STEP_PERCENT.coerceAtLeast(1)
        return ((percent + step / 2) / step * step)
            .coerceIn(MIN_SCAN_FRAME_MARGIN_PERCENT, MAX_SCAN_FRAME_MARGIN_PERCENT)
    }

    internal fun snappedScanFrameMarginPercent(value: Float): Int {
        val step = SCAN_FRAME_MARGIN_STEP_PERCENT.coerceAtLeast(1)
        val snapped = (value / step).roundToInt() * step
        return snapped.coerceIn(MIN_SCAN_FRAME_MARGIN_PERCENT, MAX_SCAN_FRAME_MARGIN_PERCENT)
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
