package vn.io.elsutm.mbqr.vietqr

data class Tlv(val tag: String, val value: String)

/**
 * Parses a flat sequence of EMV TLV fields (tag 2 digits, length 2 digits).
 *
 * Returns null when the input is malformed instead of skipping fields.
 */
object TlvParser {

    fun parse(input: String): List<Tlv>? {
        val fields = mutableListOf<Tlv>()
        var index = 0

        while (index < input.length) {
            if (index + 4 > input.length) return null

            val tag = input.substring(index, index + 2)
            val lengthText = input.substring(index + 2, index + 4)
            if (!tag.all(Char::isDigit) || !lengthText.all(Char::isDigit)) return null

            val length = lengthText.toIntOrNull() ?: return null
            index += 4
            if (index + length > input.length) return null

            fields += Tlv(tag, input.substring(index, index + length))
            index += length
        }

        return fields
    }
}
