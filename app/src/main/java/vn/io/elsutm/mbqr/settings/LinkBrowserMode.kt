package vn.io.elsutm.mbqr.settings

/** Target used when a scanned VietQR link is opened outside the app. */
enum class LinkBrowserMode {
    CUSTOM_TAB,
    BROWSER_APP,
    ;

    companion object {
        fun fromName(value: String?): LinkBrowserMode =
            entries.firstOrNull { it.name == value } ?: CUSTOM_TAB
    }
}
