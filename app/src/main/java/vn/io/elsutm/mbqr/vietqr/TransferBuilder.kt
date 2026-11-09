package vn.io.elsutm.mbqr.vietqr

/**
 * Builds the plain text that the MB transfer hub parses after a long-press paste:
 * `<bank> <account> <amount in thousands> <recipient name>`.
 */
object TransferBuilder {

    fun build(data: VietQrData, bankPrefix: String): String = buildString {
        append(bankPrefix).append(' ').append(data.account)
        data.amountVnd?.let { append(' ').append(AmountFormatter.thousands(it)) }
        data.recipientName?.let { name ->
            append(' ').append(name.trim().replace(WHITESPACE, " "))
        }
    }

    private val WHITESPACE = Regex("\\s+")
}
