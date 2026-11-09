package vn.io.elsutm.mbqr.vietqr

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Converts a VND amount into the thousands notation accepted by the MB transfer
 * hub: `1000` becomes `1.0`, `12345` becomes `12.35`.
 *
 * The hub only reads two decimal places, so amounts are rounded to the nearest
 * 10 VND before building the transfer text.
 */
object AmountFormatter {

    private const val SCALE = 2

    fun thousands(amountVnd: Long): String {
        val value = BigDecimal.valueOf(amountVnd, 3)
            .setScale(SCALE, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        val normalized = if (value.scale() < 1) value.setScale(1) else value
        return normalized.toPlainString()
    }
}
