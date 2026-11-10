package vn.io.elsutm.mbqr.settings

import android.content.Context
import androidx.core.content.edit

/** Persistent preferences for the MB Bank handoff. */
object AppSettings {

    const val MIN_CLIPBOARD_EXPIRY_SECONDS = 15
    const val MAX_CLIPBOARD_EXPIRY_SECONDS = 300
    const val CLIPBOARD_EXPIRY_STEP_SECONDS = 15
    const val DEFAULT_CLIPBOARD_EXPIRY_SECONDS = 60

    private const val PREFERENCES = "mbqr_settings"
    private const val CONFIRM_BEFORE_OPENING_MB = "confirm_before_opening_mb"
    private const val CLIPBOARD_EXPIRY_SECONDS = "clipboard_expiry_seconds"
    private const val DEFAULT_CONFIRM_BEFORE_OPENING_MB = true

    fun confirmBeforeOpeningMb(context: Context): Boolean =
        preferences(context).getBoolean(
            CONFIRM_BEFORE_OPENING_MB,
            DEFAULT_CONFIRM_BEFORE_OPENING_MB,
        )

    fun setConfirmBeforeOpeningMb(context: Context, enabled: Boolean) {
        preferences(context).edit { putBoolean(CONFIRM_BEFORE_OPENING_MB, enabled) }
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
        val bounded = seconds.coerceIn(
            MIN_CLIPBOARD_EXPIRY_SECONDS,
            MAX_CLIPBOARD_EXPIRY_SECONDS,
        )
        val step = CLIPBOARD_EXPIRY_STEP_SECONDS.coerceAtLeast(1)
        val offset = bounded - MIN_CLIPBOARD_EXPIRY_SECONDS
        return (MIN_CLIPBOARD_EXPIRY_SECONDS + ((offset + step / 2) / step) * step)
            .coerceIn(MIN_CLIPBOARD_EXPIRY_SECONDS, MAX_CLIPBOARD_EXPIRY_SECONDS)
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
