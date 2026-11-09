package vn.io.elsutm.mbqr.vietqr

/**
 * NAPAS bank BIN to the prefix used by the MB transfer hub. Only banks whose
 * transfer syntax has been verified belong here.
 */
object BankBin {

    private val transferPrefixes = mapOf(
        "970422" to "MB",
    )

    fun transferPrefix(bin: String): String? = transferPrefixes[bin]
}
