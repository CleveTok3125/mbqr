package vn.io.elsutm.mbqr.camera

/**
 * Suppresses duplicate decodes while allowing the same payload again only after
 * the camera has seen a frame without a QR code.
 */
internal class DecodeGuard {

    private var lastDecoded: String? = null
    private var requireFreshFrame = false

    fun reset(requireFreshFrame: Boolean = false) {
        if (!requireFreshFrame) lastDecoded = null
        this.requireFreshFrame = requireFreshFrame
    }

    fun accept(payload: String?): String? {
        if (payload == null) {
            requireFreshFrame = false
            lastDecoded = null
            return null
        }

        if (requireFreshFrame) {
            if (payload == lastDecoded) return null
            requireFreshFrame = false
        }

        if (payload == lastDecoded) return null
        lastDecoded = payload
        return payload
    }
}
