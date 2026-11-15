package vn.io.elsutm.mbqr.vietqr

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

sealed interface VietQrPayLinkResult {
    data class Success(val url: String) : VietQrPayLinkResult

    data class Ineligible(val reason: Reason) : VietQrPayLinkResult

    enum class Reason { MISSING_AMOUNT, MISSING_CONTENT, UNSUPPORTED_BANK }
}

/**
 * Builds a VietQR pay link that opens the VietQR gateway with the scanned
 * transfer prefilled.
 *
 * A link needs a transferable bank, a positive amount, and non-blank content.
 * Anything else is reported as [VietQrPayLinkResult.Ineligible] so the caller can
 * pick another handoff instead of sending an unusable link.
 *
 * The link omits the beneficiary name (`bn`): MB resolves the recipient from
 * the account and bank code in `ba`.
 */
object VietQrPayLinkBuilder {

    const val RETURN_URL = "https://127.0.0.1:0/"
    const val HOST = "dl.vietqr.io"
    const val PATH = "/pay"

    private const val SCHEME = "https"
    private const val ENCODED_AT = "%40"
    private const val AT = "@"
    private val BANK_CODE = Regex("^[a-z0-9-]+$")

    fun build(
        data: VietQrData,
        bank: BankInfo?,
        returnUrl: String = RETURN_URL,
    ): VietQrPayLinkResult {
        val bankCode = bank?.prefix?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (!BANK_CODE.matches(bankCode)) {
            return VietQrPayLinkResult.Ineligible(VietQrPayLinkResult.Reason.UNSUPPORTED_BANK)
        }

        val amount = data.amountVnd?.takeIf { it > 0 }
            ?: return VietQrPayLinkResult.Ineligible(VietQrPayLinkResult.Reason.MISSING_AMOUNT)

        val content = data.content?.trim()?.takeIf { it.isNotEmpty() }
            ?: return VietQrPayLinkResult.Ineligible(VietQrPayLinkResult.Reason.MISSING_CONTENT)

        val account = data.account.trim()
        if (account.isEmpty()) {
            return VietQrPayLinkResult.Ineligible(VietQrPayLinkResult.Reason.UNSUPPORTED_BANK)
        }

        val url = buildString {
            append(SCHEME).append("://").append(HOST).append(PATH)
            append("?app=mb")
            append("&ba=").append(encodeRecipient(account, bankCode))
            append("&am=").append(amount)
            append("&tn=").append(encode(content))
            append("&url=").append(returnUrl)
        }

        return VietQrPayLinkResult.Success(url)
    }

    /** Keeps the recipient `@` literal; the gateway rejects the encoded form. */
    private fun encodeRecipient(account: String, bankCode: String): String =
        encode("$account$AT$bankCode").replace(ENCODED_AT, AT)

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
