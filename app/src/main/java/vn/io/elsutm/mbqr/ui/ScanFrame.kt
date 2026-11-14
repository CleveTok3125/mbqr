package vn.io.elsutm.mbqr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.io.elsutm.mbqr.settings.ScanBoundaryStyle
import vn.io.elsutm.mbqr.settings.ScanFrameStyle
import kotlin.math.min

/** Scales the tightest free gap around the scan frame by a percentage. */
internal fun scanMargin(minGap: Dp, percent: Int): Dp =
    minGap * (percent.coerceIn(0, 100) / 100f)

/** Stroke and dash pattern shared by the preview and the scanner overlay. */
internal val ScanBoundaryStrokeWidth = 1.dp
internal val ScanBoundaryDashOn = 8.dp
internal val ScanBoundaryDashOff = 6.dp

/** Corner radius as a percentage of the short edge for the full-border styles. */
internal fun scanFrameCornerPercent(style: ScanFrameStyle): Int = when (style) {
    ScanFrameStyle.ROUNDED -> 11
    ScanFrameStyle.CORNERS, ScanFrameStyle.SQUARE -> 0
}

/** Corner radius of the dashed crop boundary. */
internal fun scanFrameBoundaryRadius(style: ScanBoundaryStyle): Dp = when (style) {
    ScanBoundaryStyle.ROUNDED -> 32.dp
    ScanBoundaryStyle.SQUARE -> 0.dp
}

/** Length of one corner bracket arm, capped on large frames. */
internal fun scanFrameArmLength(edgePx: Float, maxArmPx: Float): Float =
    (edgePx * 0.28f).coerceAtMost(maxArmPx)

/** Draws the scan frame outline for [style]. */
@Composable
internal fun ScanFrameOutline(
    style: ScanFrameStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    when (style) {
        ScanFrameStyle.ROUNDED -> Box(
            modifier = modifier.border(
                width = 3.dp,
                color = color,
                shape = RoundedCornerShape(percent = scanFrameCornerPercent(style)),
            ),
        )
        ScanFrameStyle.SQUARE -> Box(
            modifier = modifier.border(width = 3.dp, color = color),
        )
        ScanFrameStyle.CORNERS -> Canvas(modifier = modifier) {
            val arm = scanFrameArmLength(
                edgePx = min(size.width, size.height),
                maxArmPx = 32.dp.toPx(),
            )
            val stroke = 4.dp.toPx()
            val cap = StrokeCap.Round
            // Keep the stroke fully inside the frame bounds.
            val inset = stroke / 2f
            val left = inset
            val top = inset
            val right = size.width - inset
            val bottom = size.height - inset

            fun bracket(corner: Offset, horizontal: Float, vertical: Float) {
                drawLine(
                    color = color,
                    start = corner,
                    end = Offset(corner.x + horizontal * arm, corner.y),
                    strokeWidth = stroke,
                    cap = cap,
                )
                drawLine(
                    color = color,
                    start = corner,
                    end = Offset(corner.x, corner.y + vertical * arm),
                    strokeWidth = stroke,
                    cap = cap,
                )
            }

            bracket(Offset(left, top), 1f, 1f)
            bracket(Offset(right, top), -1f, 1f)
            bracket(Offset(left, bottom), 1f, -1f)
            bracket(Offset(right, bottom), -1f, -1f)
        }
    }
}
