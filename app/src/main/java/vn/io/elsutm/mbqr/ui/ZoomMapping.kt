package vn.io.elsutm.mbqr.ui

import kotlin.math.exp
import kotlin.math.ln

/**
 * Maps the zoom arc angle to a CameraX zoom ratio on a logarithmic scale, so
 * equal drag distances feel like equal zoom changes.
 */
object ZoomMapping {

    const val START_ANGLE = 200f
    const val SWEEP_ANGLE = 140f

    fun ratioForAngle(
        angle: Float,
        minRatio: Float,
        maxRatio: Float,
        startAngle: Float = START_ANGLE,
        sweepAngle: Float = SWEEP_ANGLE,
    ): Float {
        if (minRatio <= 0f || maxRatio <= minRatio || sweepAngle <= 0f) {
            return minRatio.coerceAtLeast(0f)
        }

        val clamped = angle.coerceIn(startAngle, startAngle + sweepAngle)
        val fraction = (clamped - startAngle) / sweepAngle
        return exp(ln(minRatio) + fraction * (ln(maxRatio) - ln(minRatio)))
    }

    fun angleForRatio(
        ratio: Float,
        minRatio: Float,
        maxRatio: Float,
        startAngle: Float = START_ANGLE,
        sweepAngle: Float = SWEEP_ANGLE,
    ): Float {
        if (minRatio <= 0f || maxRatio <= minRatio || sweepAngle <= 0f) {
            return startAngle
        }

        val clamped = ratio.coerceIn(minRatio, maxRatio)
        val fraction = ln(clamped / minRatio) / ln(maxRatio / minRatio)
        return startAngle + fraction * sweepAngle
    }
}
