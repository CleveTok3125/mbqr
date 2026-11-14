package vn.io.elsutm.mbqr.qr

/**
 * Removes invisible characters that can spoof or corrupt QR text.
 *
 * Visible characters, Vietnamese diacritics, and emoji are kept. Carriage
 * returns become line feeds; other control characters, bidirectional controls,
 * zero-width characters, and unpaired surrogates are dropped.
 */
object TextSanitizer {

    fun sanitize(text: String): String {
        if (text.isEmpty()) return text

        val cleaned = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '\r' -> {
                    // Collapse CRLF and lone CR to LF.
                    if (index + 1 >= text.length || text[index + 1] != '\n') {
                        cleaned.append('\n')
                    }
                }

                char == '\n' || char == '\t' -> cleaned.append(char)
                char.isHighSurrogate() -> {
                    val next = text.getOrNull(index + 1)
                    if (next != null && next.isLowSurrogate()) {
                        cleaned.append(char).append(next)
                        index += 1
                    }
                }

                char.isLowSurrogate() -> Unit
                isDropped(char) -> Unit
                else -> cleaned.append(char)
            }
            index += 1
        }
        return cleaned.toString()
    }

    private fun isDropped(char: Char): Boolean {
        // C0 and C1 controls, excluding the line breaks and tabs handled above.
        if (char.isISOControl()) return true

        return when (char) {
            '\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF', '\u00AD',
            '\u061C', '\u200E', '\u200F',
            in '\u202A'..'\u202E',
            in '\u2066'..'\u2069',
            -> true

            else -> false
        }
    }
}
