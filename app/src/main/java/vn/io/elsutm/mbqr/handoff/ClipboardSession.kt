package vn.io.elsutm.mbqr.handoff

/**
 * Process-scoped copy of the last handoff text, kept in memory only so the
 * countdown pill can restore it after an activity recreation.
 */
object ClipboardSession {
    @Volatile
    var text: String? = null
}
