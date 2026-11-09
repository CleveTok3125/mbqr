package vn.io.elsutm.mbqr.vietqr

data class VietQrData(
    val bankBin: String,
    val account: String,
    val amountVnd: Long?,
    val recipientName: String?,
    val content: String?,
)

sealed interface VietQrParseResult {
    data class VietQr(val data: VietQrData, val crcValid: Boolean) : VietQrParseResult

    data object NotVietQr : VietQrParseResult
}

/**
 * Parses NAPAS VietQR payloads (EMVCo QR, point of initiation 11/12).
 *
 * Only VND transfers through the NAPAS provider are accepted. Other payloads are
 * reported as [VietQrParseResult.NotVietQr] so the caller can show the raw value.
 */
object VietQrParser {

    private const val NAPAS_GUID = "A000000727"
    private const val VND_CURRENCY = "704"
    private const val FORMAT_INDICATOR = "01"

    fun parse(raw: String): VietQrParseResult {
        val payload = normalize(raw)
        if (payload.length < 12) return VietQrParseResult.NotVietQr
        if (payload.substring(payload.length - 8, payload.length - 4) != "6304") {
            return VietQrParseResult.NotVietQr
        }

        val fields = TlvParser.parse(payload) ?: return VietQrParseResult.NotVietQr
        val crcField = fields.lastOrNull()?.takeIf { it.tag == CRC_TAG }
            ?: return VietQrParseResult.NotVietQr
        if (!crcField.value.matches(CRC_VALUE_PATTERN)) return VietQrParseResult.NotVietQr

        val crcValid = crcField.value.equals(
            Crc16.ccittFalse(payload.dropLast(CRC_LENGTH)),
            ignoreCase = true,
        )

        if (fields.value("00") != FORMAT_INDICATOR) return VietQrParseResult.NotVietQr
        if (fields.value("53") != VND_CURRENCY) return VietQrParseResult.NotVietQr

        val provider = fields.value("38")
            ?.let(TlvParser::parse)
            ?: return VietQrParseResult.NotVietQr
        if (provider.value("00") != NAPAS_GUID) return VietQrParseResult.NotVietQr

        val accountInfo = provider.value("01")
            ?.let(TlvParser::parse)
            ?: return VietQrParseResult.NotVietQr
        val bankBin = accountInfo.value("00")
            ?.takeIf { it.length == 6 && it.all(Char::isDigit) }
            ?: return VietQrParseResult.NotVietQr
        val account = accountInfo.value("01")
            ?.takeIf { it.isNotBlank() }
            ?: return VietQrParseResult.NotVietQr

        val amount = fields.value("54")?.let { text ->
            text.toLongOrNull()?.takeIf { it > 0 } ?: return VietQrParseResult.NotVietQr
        }

        val data = VietQrData(
            bankBin = bankBin,
            account = account,
            amountVnd = amount,
            recipientName = fields.value("59")?.takeIf { it.isNotBlank() },
            content = fields.value("62")
                ?.let(TlvParser::parse)
                ?.value("08")
                ?.takeIf { it.isNotBlank() },
        )

        return VietQrParseResult.VietQr(data = data, crcValid = crcValid)
    }

    /**
     * Returns the payload with surrounding whitespace and invisible characters
     * removed. Used when the raw value has to be shown or copied.
     */
    fun normalized(raw: String): String = normalize(raw)

    private fun normalize(raw: String): String =
        raw.trim().filter { !it.isISOControl() && it != '\uFEFF' && it != '\u200B' }

    private fun List<Tlv>.value(tag: String): String? =
        firstOrNull { it.tag == tag }?.value

    private const val CRC_TAG = "63"
    private const val CRC_LENGTH = 4
    private val CRC_VALUE_PATTERN = Regex("[0-9A-Fa-f]{4}")
}
