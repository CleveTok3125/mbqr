package vn.io.elsutm.mbqr.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import vn.io.elsutm.mbqr.settings.ControlsPlacement
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CameraControls(
    placement: ControlsPlacement,
    zoomRatio: Float,
    minZoom: Float,
    maxZoom: Float,
    isCapturing: Boolean,
    showCapture: Boolean,
    hapticsEnabled: Boolean,
    onZoomChange: (Float) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cluster: @Composable (DialEdge, Modifier) -> Unit = { edge, clusterModifier ->
        ControlsCluster(
            edge = edge,
            zoomRatio = zoomRatio,
            minZoom = minZoom,
            maxZoom = maxZoom,
            isCapturing = isCapturing,
            showCapture = showCapture,
            hapticsEnabled = hapticsEnabled,
            onZoomChange = onZoomChange,
            onCapture = onCapture,
            modifier = clusterModifier,
        )
    }

    Box(modifier) {
        when (placement) {
            ControlsPlacement.BOTTOM -> cluster(
                DialEdge.BOTTOM,
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(ControlsHeight),
            )

            ControlsPlacement.LEFT -> cluster(
                DialEdge.LEFT,
                Modifier
                    .align(PhysicalCenterLeft)
                    .width(ControlsWidth)
                    .fillMaxHeight(),
            )

            ControlsPlacement.RIGHT -> cluster(
                DialEdge.RIGHT,
                Modifier
                    .align(PhysicalCenterRight)
                    .width(ControlsWidth)
                    .fillMaxHeight(),
            )
        }
    }
}

/**
 * Center alignments resolved against physical left/right.
 *
 * The placement setting names physical sides, so it must keep its meaning under
 * a right-to-left layout direction.
 */
internal val PhysicalCenterLeft: Alignment = object : Alignment {
    override fun align(
        size: IntSize,
        space: IntSize,
        layoutDirection: LayoutDirection,
    ): IntOffset = IntOffset(0, (space.height - size.height) / 2)
}

internal val PhysicalCenterRight: Alignment = object : Alignment {
    override fun align(
        size: IntSize,
        space: IntSize,
        layoutDirection: LayoutDirection,
    ): IntOffset = IntOffset(space.width - size.width, (space.height - size.height) / 2)
}

@Composable
private fun ControlsCluster(
    edge: DialEdge,
    zoomRatio: Float,
    minZoom: Float,
    maxZoom: Float,
    isCapturing: Boolean,
    showCapture: Boolean,
    hapticsEnabled: Boolean,
    onZoomChange: (Float) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        ZoomDial(
            edge = edge,
            zoomRatio = zoomRatio,
            minZoom = minZoom,
            maxZoom = maxZoom,
            hapticsEnabled = hapticsEnabled,
            onZoomChange = onZoomChange,
            modifier = Modifier.fillMaxSize(),
        )

        when (edge) {
            DialEdge.BOTTOM -> {
                ZoomPill(
                    zoomRatio = zoomRatio,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                if (showCapture) {
                    ShutterButton(
                        isCapturing = isCapturing,
                        hapticsEnabled = hapticsEnabled,
                        onCapture = onCapture,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                    )
                }
            }

            DialEdge.LEFT, DialEdge.RIGHT -> {
                val density = LocalDensity.current
                var pillHeightPx by remember {
                    mutableIntStateOf(with(density) { PillHeight.roundToPx() })
                }
                Column(
                    modifier = Modifier
                        .align(
                            if (edge == DialEdge.LEFT) {
                                PhysicalCenterLeft
                            } else {
                                PhysicalCenterRight
                            },
                        )
                        .padding(horizontal = 8.dp)
                        .width(ShutterSize),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ZoomPill(
                        zoomRatio = zoomRatio,
                        modifier = Modifier.onSizeChanged { pillHeightPx = it.height },
                    )
                    if (showCapture) {
                        ShutterButton(
                            isCapturing = isCapturing,
                            hapticsEnabled = hapticsEnabled,
                            onCapture = onCapture,
                        )
                    }
                    // Mirrors the pill's measured height below the shutter so
                    // the shutter stays on the dial pivot at any font scale.
                    Spacer(Modifier.height(with(density) { pillHeightPx.toDp() }))
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(
    isCapturing: Boolean,
    hapticsEnabled: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .size(ShutterSize)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(enabled = !isCapturing) {
                if (hapticsEnabled) view.performHapticFeedback(HAPTIC_MAJOR)
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
private fun ZoomPill(
    zoomRatio: Float,
    modifier: Modifier = Modifier,
) {
    Text(
        text = formatZoom(zoomRatio),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun ZoomDial(
    edge: DialEdge,
    zoomRatio: Float,
    minZoom: Float,
    maxZoom: Float,
    hapticsEnabled: Boolean,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = maxZoom > minZoom
    val trackColor = Color.White.copy(alpha = 0.35f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val view = LocalView.current

    Canvas(
        modifier = modifier
            .pointerInput(minZoom, maxZoom, enabled, edge) {
                if (!enabled) return@pointerInput

                val pivot = dialPivot(
                    width = size.width.toFloat(),
                    height = size.height.toFloat(),
                    edge = edge,
                    pivotFromEdgePx = PIVOT_FROM_EDGE.toPx(),
                )
                val angleOffset = edge.angleOffset
                val ticks = dialTicks(minZoom, maxZoom)
                val arcStart = ZoomMapping.START_ANGLE
                val arcEnd = arcStart + ZoomMapping.SWEEP_ANGLE

                awaitEachGesture {
                    val down = awaitFirstDown()
                    val downAngle = dialAngle(down.position, pivot, angleOffset)
                    val downRatio = ZoomMapping.ratioForAngle(
                        downAngle,
                        minZoom,
                        maxZoom,
                    )
                    var lastAngle: Float? = null

                    if (downAngle in arcStart..arcEnd) {
                        lastAngle = downAngle
                    }
                    if (hapticsEnabled) {
                        view.performHapticFeedback(HAPTIC_MAJOR)
                    }
                    onZoomChange(downRatio)
                    var previousRatio = downRatio

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        val angle = dialAngle(change.position, pivot, angleOffset)
                        val ratio = ZoomMapping.ratioForAngle(angle, minZoom, maxZoom)

                        if (angle !in arcStart..arcEnd) {
                            // Gaps beside the arc clamp the thumb but still
                            // report the boundary when the clamp changes it.
                            if (crossedEndpoint(previousRatio, ratio, minZoom, maxZoom)) {
                                if (hapticsEnabled) {
                                    view.performHapticFeedback(HAPTIC_MAJOR)
                                }
                            }
                            lastAngle = null
                            previousRatio = ratio
                            onZoomChange(ratio)
                            change.consume()
                            continue
                        }

                        val previous = lastAngle
                        if (previous == null) {
                            lastAngle = angle
                            previousRatio = ratio
                            onZoomChange(ratio)
                            change.consume()
                            continue
                        }

                        if (crossedEndpoint(previousRatio, ratio, minZoom, maxZoom)) {
                            if (hapticsEnabled) {
                                view.performHapticFeedback(HAPTIC_MAJOR)
                            }
                        } else {
                            val crossed = strongestCrossedTick(previous, angle, ticks)
                            if (crossed != null && hapticsEnabled) {
                                view.performHapticFeedback(
                                    if (crossed.major) HAPTIC_MAJOR else HAPTIC_MINOR,
                                )
                            }
                        }

                        lastAngle = angle
                        previousRatio = ratio
                        onZoomChange(ratio)
                        change.consume()
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        val pivot = dialPivot(
            width = size.width,
            height = size.height,
            edge = edge,
            pivotFromEdgePx = PIVOT_FROM_EDGE.toPx(),
        )
        val radius = dialRadius(
            width = size.width,
            height = size.height,
            edge = edge,
            maxRadiusPx = DIAL_MAX_RADIUS.toPx(),
            marginPx = DIAL_EDGE_MARGIN.toPx(),
        )
        val angleOffset = edge.angleOffset
        val currentAngle = ZoomMapping.angleForRatio(zoomRatio, minZoom, maxZoom)
        val topLeft = Offset(pivot.x - radius, pivot.y - radius)

        drawArc(
            color = trackColor,
            startAngle = ZoomMapping.START_ANGLE + angleOffset,
            sweepAngle = ZoomMapping.SWEEP_ANGLE,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = primaryColor,
            startAngle = ZoomMapping.START_ANGLE + angleOffset,
            sweepAngle = (currentAngle - ZoomMapping.START_ANGLE).coerceAtLeast(0f),
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )

        // Minor ticks divide each major interval into equal log steps.
        val majors = ZoomTicks.majorTicks(minZoom, maxZoom)
        ZoomTicks.minorTicks(majors).forEach { ratio ->
            drawRadialTick(
                center = pivot,
                radius = radius,
                angleDegrees = ZoomMapping.angleForRatio(ratio, minZoom, maxZoom) +
                    angleOffset,
                innerOffset = 4.dp.toPx(),
                outerOffset = 4.dp.toPx(),
                strokeWidth = 1.dp.toPx(),
                color = Color.White.copy(alpha = 0.45f),
            )
        }

        majors.forEach { ratio ->
            drawRadialTick(
                center = pivot,
                radius = radius,
                angleDegrees = ZoomMapping.angleForRatio(ratio, minZoom, maxZoom) +
                    angleOffset,
                innerOffset = 10.dp.toPx(),
                outerOffset = 7.dp.toPx(),
                strokeWidth = 2.dp.toPx(),
                color = Color.White.copy(alpha = 0.9f),
            )
        }

        val radians = Math.toRadians((currentAngle + angleOffset).toDouble())
        val thumb = Offset(
            pivot.x + (radius * cos(radians)).toFloat(),
            pivot.y + (radius * sin(radians)).toFloat(),
        )
        drawCircle(color = Color.White, radius = 11.dp.toPx(), center = thumb)
        drawCircle(color = primaryColor, radius = 8.dp.toPx(), center = thumb)
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

internal enum class DialEdge(val angleOffset: Float) {
    BOTTOM(0f),
    LEFT(90f),
    RIGHT(270f),
}

private fun dialPivot(
    width: Float,
    height: Float,
    edge: DialEdge,
    pivotFromEdgePx: Float,
): Offset = when (edge) {
    DialEdge.BOTTOM -> Offset(width / 2f, height - pivotFromEdgePx)
    DialEdge.LEFT -> Offset(pivotFromEdgePx, height / 2f)
    DialEdge.RIGHT -> Offset(width - pivotFromEdgePx, height / 2f)
}

private fun dialRadius(
    width: Float,
    height: Float,
    edge: DialEdge,
    maxRadiusPx: Float,
    marginPx: Float,
): Float {
    val available = when (edge) {
        DialEdge.BOTTOM -> width / 2f - marginPx
        DialEdge.LEFT, DialEdge.RIGHT -> height / 2f - marginPx
    }
    return min(available, maxRadiusPx).coerceAtLeast(0f)
}

/**
 * Maps a pointer position to the arc angle of [edge], in the dial's local frame.
 *
 * The position is rotated back by the edge offset, then mirrored across the
 * pivot's horizontal axis so touches in the gap beside the arc still report a
 * boundary angle.
 */
internal fun dialAngle(position: Offset, pivot: Offset, angleOffset: Float): Float {
    val dx = position.x - pivot.x
    val dy = position.y - pivot.y
    val radians = Math.toRadians(-angleOffset.toDouble())
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()
    val localX = dx * cos - dy * sin
    val localY = dx * sin + dy * cos

    var degrees = Math.toDegrees(atan2(localY, localX).toDouble()).toFloat()
    if (degrees < 0f) degrees += 360f
    return if (localY > MIRROR_EPSILON) 360f - degrees else degrees
}

private data class DialTick(val angle: Float, val major: Boolean)

internal fun crossedEndpoint(
    previousRatio: Float,
    currentRatio: Float,
    minRatio: Float,
    maxRatio: Float,
): Boolean {
    if (currentRatio <= minRatio && previousRatio > minRatio) return true
    if (currentRatio >= maxRatio && previousRatio < maxRatio) return true
    return false
}

private fun dialTicks(minZoom: Float, maxZoom: Float): List<DialTick> {
    val majors = ZoomTicks.majorTicks(minZoom, maxZoom)
    val ticks = buildList {
        ZoomTicks.minorTicks(majors).forEach { ratio ->
            add(
                DialTick(
                    angle = ZoomMapping.angleForRatio(ratio, minZoom, maxZoom),
                    major = false,
                ),
            )
        }
        majors.forEach { ratio ->
            add(
                DialTick(
                    angle = ZoomMapping.angleForRatio(ratio, minZoom, maxZoom),
                    major = true,
                ),
            )
        }
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

private fun formatZoom(ratio: Float): String {
    val rounded = max(1, (ratio * 10).roundToInt())
    return if (rounded % 10 == 0) "${rounded / 10}x" else "${rounded / 10}.${rounded % 10}x"
}

private val ControlsHeight = 190.dp
private val ControlsWidth = 190.dp
private val ShutterSize = 68.dp
private val PillHeight = 24.dp
private val PIVOT_FROM_EDGE = 42.dp
private val DIAL_MAX_RADIUS = 105.dp
private val DIAL_EDGE_MARGIN = 24.dp
private const val MIRROR_EPSILON = 1e-3f
private val HAPTIC_MAJOR = HapticFeedbackConstants.CLOCK_TICK
private val HAPTIC_MINOR = HapticFeedbackConstants.KEYBOARD_TAP
