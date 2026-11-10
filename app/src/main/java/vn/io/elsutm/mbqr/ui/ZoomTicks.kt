package vn.io.elsutm.mbqr.ui

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Builds the arc-dial ticks from the active CameraX zoom range.
 *
 * Major ticks always include the range endpoints. Every power of two in the
 * range is also a major tick, so each octave occupies the same arc length.
 * Minor ticks divide each major interval into five equal logarithmic steps.
 * The tail continues that octave grid to the next power of two, clipped at the
 * maximum.
 */
object ZoomTicks {

    const val MINOR_TICKS_PER_GAP = 4

    fun majorTicks(minRatio: Float, maxRatio: Float): List<Float> {
        if (!minRatio.isFinite() || !maxRatio.isFinite()) {
            return listOf(minRatio.coerceAtLeast(0f))
        }
        if (minRatio <= 0f || maxRatio <= minRatio) {
            return listOf(minRatio.coerceAtLeast(0f))
        }

        return buildList {
            add(minRatio)
            powersOfTwo(minRatio, maxRatio).forEach { power ->
                if (power > minRatio && power < maxRatio) add(power)
            }
            add(maxRatio)
        }.distinct().sorted()
    }

    fun minorTicks(majors: List<Float>): List<Float> {
        if (majors.size < 2) return emptyList()

        val ticks = mutableListOf<Float>()
        for (index in 0 until majors.lastIndex) {
            val lower = majors[index]
            val upper = majors[index + 1]
            if (lower <= 0f || upper <= lower) continue

            // The tail uses the next power of two as its log grid so the tick
            // rhythm continues past the last power-of-two major, then clips at max.
            val isTail = index == majors.lastIndex - 1 && !isPowerOfTwo(upper)
            val gridUpper = if (isTail) nextPowerOfTwoAbove(upper) else upper
            if (gridUpper <= lower) continue

            val logLower = ln(lower.toDouble())
            val logSpan = ln(gridUpper.toDouble()) - logLower
            for (step in 1..MINOR_TICKS_PER_GAP) {
                val fraction = step.toDouble() / (MINOR_TICKS_PER_GAP + 1)
                val ratio = exp(logLower + logSpan * fraction).toFloat()
                if (ratio < upper) ticks += ratio
            }
        }
        return ticks
    }

    internal fun isPowerOfTwo(value: Float): Boolean {
        if (!value.isFinite() || value <= 0f) return false
        val rounded = value.roundToInt()
        return value == rounded.toFloat() &&
            rounded > 0 &&
            (rounded and (rounded - 1)) == 0
    }

    private fun powersOfTwo(minRatio: Float, maxRatio: Float): List<Float> {
        val powers = mutableListOf<Float>()
        var power = 1f
        while (power < minRatio && power.isFinite()) power *= 2f
        while (power <= maxRatio && power.isFinite() && powers.size < MAX_POWER_CANDIDATES) {
            powers += power
            power *= 2f
        }
        return powers
    }

    private fun nextPowerOfTwoAbove(value: Float): Float {
        var power = 1f
        while (power <= value && power.isFinite()) power *= 2f
        return power
    }

    private const val MAX_POWER_CANDIDATES = 32
}
