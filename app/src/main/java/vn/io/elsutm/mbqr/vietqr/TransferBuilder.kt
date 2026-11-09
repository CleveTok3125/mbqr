package vn.io.elsutm.mbqr.vietqr

/**
 * Builds the plain text that the MB transfer hub parses after a long-press paste:
 * `STK <account> <amount in thousands> - <bank> [recipient name]`.
 */
object TransferBuilder {

    fun build(data: VietQrData, bankName: String): String = buildString {
        append("STK ").append(data.account)
        data.amountVnd?.let { append(' ').append(AmountFormatter.thousands(it)) }
        append(" - ").append(bankName)
        data.recipientName?.let { name ->
            append(' ').append(name.trim().replace(WHITESPACE, " "))
        }
    }

    private val WHITESPACE = Regex("\\s+")
}
