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

    private const val PREFERENCES = "mbqr_settings"
    private const val CONFIRM_BEFORE_OPENING_MB = "confirm_before_opening_mb"
    private const val CLIPBOARD_EXPIRY_SECONDS = "clipboard_expiry_seconds"
    private const val SCAN_INSIDE_FRAME = "scan_inside_frame"
    private const val SCAN_FRAME_MARGIN_PERCENT_KEY = "scan_frame_margin_percent"
    private const val SENSITIVE_MASKING_LEVEL = "sensitive_masking_level"
    private const val LEGACY_HIDE_SENSITIVE_FIELDS = "hide_sensitive_fields"
    private const val DEFAULT_CONFIRM_BEFORE_OPENING_MB = true
    private const val DEFAULT_SCAN_INSIDE_FRAME = false

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
