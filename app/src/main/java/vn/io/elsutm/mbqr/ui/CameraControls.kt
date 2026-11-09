package vn.io.elsutm.mbqr.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CameraControls(
    zoomRatio: Float,
    minZoom: Float,
    maxZoom: Float,
    isCapturing: Boolean,
    showCapture: Boolean,
    onZoomChange: (Float) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ZoomDial(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            zoomRatio = zoomRatio,
            minZoom = minZoom,
            maxZoom = maxZoom,
            onZoomChange = onZoomChange,
        )

        if (showCapture) {
            ShutterButton(
                modifier = Modifier.padding(bottom = 8.dp),
                isCapturing = isCapturing,
                onCapture = onCapture,
            )
        }
    }
}

@Composable
private fun ShutterButton(
    isCapturing: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .size(68.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(enabled = !isCapturing) {
                view.performHapticFeedback(HAPTIC_MAJOR)
                onCapture()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isCapturing) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun ZoomDial(
    zoomRatio: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = maxZoom > minZoom
    val trackColor = Color.White.copy(alpha = 0.35f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val view = LocalView.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(minZoom, maxZoom, enabled) {
                    if (!enabled) return@pointerInput

                    val pivotFromBottomPx = PIVOT_FROM_BOTTOM.toPx()
                    val ticks = dialTicks(minZoom, maxZoom)
                    val arcStart = ZoomMapping.START_ANGLE
                    val arcEnd = arcStart + ZoomMapping.SWEEP_ANGLE

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val downAngle = mappedAngle(size, down.position, pivotFromBottomPx)
                        var lastAngle: Float? = null

                        if (downAngle in arcStart..arcEnd) {
                            view.performHapticFeedback(HAPTIC_MAJOR)
                            lastAngle = downAngle
                        }
                        onZoomChange(ZoomMapping.ratioForAngle(downAngle, minZoom, maxZoom))

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            val angle = mappedAngle(size, change.position, pivotFromBottomPx)

                            if (angle !in arcStart..arcEnd) {
                                // Gaps beside the arc clamp the thumb but stay silent.
                                lastAngle = null
                                onZoomChange(
                                    ZoomMapping.ratioForAngle(angle, minZoom, maxZoom),
                                )
                                change.consume()
                                continue
                            }

                            val previous = lastAngle
                            if (previous == null) {
                                lastAngle = angle
                                onZoomChange(
                                    ZoomMapping.ratioForAngle(angle, minZoom, maxZoom),
                                )
                                change.consume()
                                continue
                            }

                            val crossed = strongestCrossedTick(previous, angle, ticks)
                            if (crossed != null) {
                                view.performHapticFeedback(
                                    if (crossed.major) HAPTIC_MAJOR else HAPTIC_MINOR,
                                )
                            }

                            lastAngle = angle
                            onZoomChange(ZoomMapping.ratioForAngle(angle, minZoom, maxZoom))
                            change.consume()
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {
            val center = Offset(size.width / 2f, size.height - PIVOT_FROM_BOTTOM.toPx())
            val radius = min(size.width / 2f - 24.dp.toPx(), 105.dp.toPx())
            val currentAngle = ZoomMapping.angleForRatio(zoomRatio, minZoom, maxZoom)
            val topLeft = Offset(center.x - radius, center.y - radius)

            drawArc(
                color = trackColor,
                startAngle = ZoomMapping.START_ANGLE,
                sweepAngle = ZoomMapping.SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = primaryColor,
                startAngle = ZoomMapping.START_ANGLE,
                sweepAngle = (currentAngle - ZoomMapping.START_ANGLE).coerceAtLeast(0f),
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )

            // Minor ticks every 10 degrees, radial so they point at the shutter center.
            var minorAngle = ZoomMapping.START_ANGLE + 7f
            while (minorAngle < ZoomMapping.START_ANGLE + ZoomMapping.SWEEP_ANGLE) {
                drawRadialTick(
                    center = center,
                    radius = radius,
                    angleDegrees = minorAngle,
                    innerOffset = 4.dp.toPx(),
                    outerOffset = 4.dp.toPx(),
                    strokeWidth = 1.dp.toPx(),
                    color = Color.White.copy(alpha = 0.45f),
                )
                minorAngle += 10f
            }

            zoomTicks(minZoom, maxZoom).forEach { ratio ->
                drawRadialTick(
                    center = center,
                    radius = radius,
                    angleDegrees = ZoomMapping.angleForRatio(ratio, minZoom, maxZoom),
                    innerOffset = 10.dp.toPx(),
                    outerOffset = 7.dp.toPx(),
                    strokeWidth = 2.dp.toPx(),
                    color = Color.White.copy(alpha = 0.9f),
                )
            }

            val radians = Math.toRadians(currentAngle.toDouble())
            val thumb = Offset(
                center.x + (radius * cos(radians)).toFloat(),
                center.y + (radius * sin(radians)).toFloat(),
            )
            drawCircle(color = Color.White, radius = 11.dp.toPx(), center = thumb)
            drawCircle(color = primaryColor, radius = 8.dp.toPx(), center = thumb)
        }

        Text(
            text = formatZoom(zoomRatio),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(50),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadialTick(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    innerOffset: Float,
    outerOffset: Float,
    strokeWidth: Float,
    color: Color,
) {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val unit = Offset(cos(radians).toFloat(), sin(radians).toFloat())
    drawLine(
        color = color,
        start = center + unit * (radius - innerOffset),
        end = center + unit * (radius + outerOffset),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun pointerAngle(
    size: IntSize,
    position: Offset,
    pivotFromBottomPx: Float,
): Float {
    val centerX = size.width / 2f
    val centerY = size.height - pivotFromBottomPx

    var degrees = Math.toDegrees(
        atan2(position.y - centerY, position.x - centerX).toDouble(),
    ).toFloat()
    if (degrees < 0f) degrees += 360f
    return degrees
}

private fun mappedAngle(
    size: IntSize,
    position: Offset,
    pivotFromBottomPx: Float,
): Float {
    val raw = pointerAngle(size, position, pivotFromBottomPx)
    val centerY = size.height - pivotFromBottomPx
    return if (position.y > centerY) 360f - raw else raw
}

private data class DialTick(val angle: Float, val major: Boolean)

private fun dialTicks(minZoom: Float, maxZoom: Float): List<DialTick> {
    val ticks = mutableListOf<DialTick>()
    var angle = ZoomMapping.START_ANGLE + MINOR_OFFSET_DEGREES
    val end = ZoomMapping.START_ANGLE + ZoomMapping.SWEEP_ANGLE
    while (angle < end) {
        ticks += DialTick(angle, major = false)
        angle += MINOR_STEP_DEGREES
    }
    zoomTicks(minZoom, maxZoom).forEach { ratio ->
        ticks += DialTick(
            angle = ZoomMapping.angleForRatio(ratio, minZoom, maxZoom),
            major = true,
        )
    }
    return ticks.sortedBy { it.angle }
}

private fun strongestCrossedTick(
    previous: Float,
    current: Float,
    ticks: List<DialTick>,
): DialTick? {
    val low = min(previous, current)
    val high = max(previous, current)
    return ticks
        .filter { it.angle > low && it.angle <= high }
        .maxByOrNull { if (it.major) 1 else 0 }
}

private fun zoomTicks(minZoom: Float, maxZoom: Float): List<Float> {
    val ticks = mutableListOf(minZoom, maxZoom)
    listOf(1f, 2f, 4f, 8f).forEach { ratio ->
        if (ratio > minZoom && ratio < maxZoom) ticks += ratio
    }
    return ticks.distinct().sorted()
}

private fun formatZoom(ratio: Float): String {
    val rounded = max(1, (ratio * 10).toInt())
    return if (rounded % 10 == 0) "${rounded / 10}x" else "${rounded / 10}.${rounded % 10}x"
}

private val PIVOT_FROM_BOTTOM = 42.dp
private val HAPTIC_MAJOR = HapticFeedbackConstants.CLOCK_TICK
private val HAPTIC_MINOR = HapticFeedbackConstants.KEYBOARD_TAP
private const val MINOR_STEP_DEGREES = 10f
private const val MINOR_OFFSET_DEGREES = 7f
