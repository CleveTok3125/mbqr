package vn.io.elsutm.mbqr.settings

/** Placement of the shutter and zoom dial when the screen is in landscape. */
enum class ControlsPlacement {
    BOTTOM,
    LEFT,
    RIGHT,
    ;

    companion object {
        fun fromName(value: String?): ControlsPlacement =
            entries.firstOrNull { it.name == value } ?: RIGHT
    }
}
