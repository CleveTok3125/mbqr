package vn.io.elsutm.mbqr.vietqr

/**
 * CRC-16/CCITT-FALSE as required by the EMV QR specification.
 */
object Crc16 {

    private const val POLYNOMIAL = 0x1021
    private const val INITIAL_VALUE = 0xFFFF

    fun ccittFalse(input: String): String {
        var crc = INITIAL_VALUE

        for (character in input) {
            crc = crc xor ((character.code and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) {
                    (crc shl 1) xor POLYNOMIAL
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }

        return "%04X".format(crc)
    }
}
