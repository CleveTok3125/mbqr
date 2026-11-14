package vn.io.elsutm.mbqr.qr

import java.util.Base64

/** Encodes QR payload text for the advanced copy actions. */
object PayloadEncoder {

    private const val HEX_DIGITS = "0123456789abcdef"

    fun toHex(text: String): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val hex = StringBuilder(bytes.size * 2)
        bytes.forEach { byte ->
            val value = byte.toInt() and 0xFF
            hex.append(HEX_DIGITS[value shr 4]).append(HEX_DIGITS[value and 0x0F])
        }
        return hex.toString()
    }

    fun toBase64(text: String): String =
        Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
}
