package vn.io.elsutm.mbqr.ui

import vn.io.elsutm.mbqr.qr.QrFieldSensitivity
import vn.io.elsutm.mbqr.settings.AppSettings

internal const val MASKED_SENSITIVE_VALUE = "••••••••"

/**
 * Whether [sensitivity] fields are hidden at [maskingLevel].
 *
 * Direct fields hide from the light level; personal fields only at the full level.
 */
internal fun isMaskingActive(
    sensitivity: QrFieldSensitivity,
    maskingLevel: Int,
): Boolean = when (sensitivity) {
    QrFieldSensitivity.None -> false
    QrFieldSensitivity.Direct -> maskingLevel >= AppSettings.MASKING_LIGHT
    QrFieldSensitivity.Personal -> maskingLevel >= AppSettings.MASKING_FULL
}
