package vn.io.elsutm.mbqr.vietqr

import java.math.BigDecimal

/**
 * Converts a VND amount into the thousands notation accepted by the MB transfer
 * hub: `1000` becomes `1.0`, `12345` becomes `12.345`.
 */
object AmountFormatter {

    fun thousands(amountVnd: Long): String {
        val value = BigDecimal.valueOf(amountVnd, 3).stripTrailingZeros()
        val normalized = if (value.scale() < 1) value.setScale(1) else value
        return normalized.toPlainString()
    }
}
