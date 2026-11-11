package vn.io.elsutm.mbqr.qr

import vn.io.elsutm.mbqr.vietqr.VietQrData

/**
 * One displayable value from a parsed QR payload.
 *
 * [copyValue] can differ from [value] when the displayed form is not the value
 * users need on the clipboard, such as a formatted amount.
 */
data class QrField(
    val label: String,
    val value: String,
    val copyValue: String = value,
)

/** Parsed QR content before it is mapped to a result sheet. */
sealed interface QrContent {
    val raw: String

    data class VietQr(
        override val raw: String,
        val data: VietQrData,
        val crcValid: Boolean,
    ) : QrContent

    data class Details(
        override val raw: String,
        val title: String,
        val fields: List<QrField>,
    ) : QrContent

    data class Text(override val raw: String) : QrContent
}
