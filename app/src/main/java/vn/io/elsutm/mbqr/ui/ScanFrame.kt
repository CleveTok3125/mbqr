package vn.io.elsutm.mbqr.ui

import androidx.compose.ui.unit.Dp

/** Scales the tightest free gap around the scan frame by a percentage. */
internal fun scanMargin(minGap: Dp, percent: Int): Dp =
    minGap * (percent.coerceIn(0, 100) / 100f)
