package vn.io.elsutm.mbqr.vietqr

/**
 * NAPAS bank BIN to display name. Only banks whose transfer string has been
 * verified with the MB hub belong here.
 */
object BankBin {

    private val names = mapOf(
        "970422" to "MB Bank",
    )

    fun bankName(bin: String): String? = names[bin]
}
