package vn.io.elsutm.mbqr.vietqr

/**
 * Builds the plain text that the MB transfer hub parses after a long-press paste:
 * `<bank> <account> <amount in thousands> <merchant name>`.
 *
 * The hub resolves the recipient name itself. The merchant name is metadata and
 * may be ignored; it is not treated as the account holder name. The app appends
 * only the merchant name; content, city, MCC, and service code are not appended.
 */
object TransferBuilder {

    fun build(data: VietQrData, bankPrefix: String): String = buildString {
        append(bankPrefix).append(' ').append(data.account)
        data.amountVnd?.let { append(' ').append(AmountFormatter.thousands(it)) }
        data.merchantName?.let { merchantName ->
            append(' ').append(merchantName.trim().replace(WHITESPACE, " "))
        }
    }

    private val WHITESPACE = Regex("\\s+")
}
