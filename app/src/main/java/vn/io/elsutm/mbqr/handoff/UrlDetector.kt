package vn.io.elsutm.mbqr.handoff

import java.net.URI
import java.net.URISyntaxException

/**
 * Decides whether raw QR content is safe to open with an external browser.
 * Only plain HTTP(S) URLs with a host are accepted so QR payloads cannot launch
 * arbitrary intents or local files.
 */
object UrlDetector {

    fun isHttpUrl(text: String): Boolean {
        val candidate = text.trim()
        if (candidate.isEmpty()) return false

        return try {
            val uri = URI(candidate)
            val scheme = uri.scheme ?: return false
            val isHttp = scheme.equals("http", ignoreCase = true) ||
                scheme.equals("https", ignoreCase = true)
            isHttp && !uri.host.isNullOrBlank()
        } catch (_: URISyntaxException) {
            false
        }
    }
}
