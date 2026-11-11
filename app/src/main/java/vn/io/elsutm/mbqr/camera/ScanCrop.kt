package vn.io.elsutm.mbqr.camera

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Normalized crop rectangle in PreviewView coordinates. */
data class ScanCrop(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** Pixel rectangle in the unrotated analysis image. */
internal data class PixelCrop(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

/**
 * Maps a crop in the preview view to the analysis image.
 *
 * PreviewView uses FILL_CENTER, so the displayed image is scaled to cover the
 * view and may be cropped on either axis. The analysis image is unrotated; the
 * rect is mapped back through the reported clockwise rotation.
 */
internal object ScanCropMapper {

    fun map(
        crop: ScanCrop,
        viewWidth: Int,
        viewHeight: Int,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
    ): PixelCrop? {
        if (viewWidth <= 0 || viewHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
            return null
        }

        val left = crop.left.coerceIn(0f, 1f)
        val top = crop.top.coerceIn(0f, 1f)
        val right = crop.right.coerceIn(0f, 1f)
        val bottom = crop.bottom.coerceIn(0f, 1f)
        if (right <= left || bottom <= top) return null

        val rotation = ((rotationDegrees % 360) + 360) % 360
        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
            return null
        }

        val displayedWidth = if (rotation == 90 || rotation == 270) imageHeight else imageWidth
        val displayedHeight = if (rotation == 90 || rotation == 270) imageWidth else imageHeight
        val scale = max(
            viewWidth.toFloat() / displayedWidth,
            viewHeight.toFloat() / displayedHeight,
        )
        if (scale <= 0f) return null

        val offsetX = (viewWidth - displayedWidth * scale) / 2f
        val offsetY = (viewHeight - displayedHeight * scale) / 2f

        var cropLeft = (left * viewWidth - offsetX) / scale
        var cropTop = (top * viewHeight - offsetY) / scale
        var cropRight = (right * viewWidth - offsetX) / scale
        var cropBottom = (bottom * viewHeight - offsetY) / scale

        cropLeft = cropLeft.coerceIn(0f, displayedWidth.toFloat())
        cropTop = cropTop.coerceIn(0f, displayedHeight.toFloat())
        cropRight = cropRight.coerceIn(0f, displayedWidth.toFloat())
        cropBottom = cropBottom.coerceIn(0f, displayedHeight.toFloat())
        if (cropRight <= cropLeft || cropBottom <= cropTop) return null

        val unrotatedLeft: Float
        val unrotatedTop: Float
        val unrotatedRight: Float
        val unrotatedBottom: Float
        when (rotation) {
            0 -> {
                unrotatedLeft = cropLeft
                unrotatedTop = cropTop
                unrotatedRight = cropRight
                unrotatedBottom = cropBottom
            }

            90 -> {
                unrotatedLeft = cropTop
                unrotatedTop = imageHeight - cropRight
                unrotatedRight = cropBottom
                unrotatedBottom = imageHeight - cropLeft
            }

            180 -> {
                unrotatedLeft = imageWidth - cropRight
                unrotatedTop = imageHeight - cropBottom
                unrotatedRight = imageWidth - cropLeft
                unrotatedBottom = imageHeight - cropTop
            }

            else -> {
                unrotatedLeft = imageWidth - cropBottom
                unrotatedTop = cropLeft
                unrotatedRight = imageWidth - cropTop
                unrotatedBottom = cropRight
            }
        }

        val leftPx = floor(unrotatedLeft.coerceIn(0f, imageWidth.toFloat()))
            .toInt()
            .coerceIn(0, imageWidth - 1)
        val topPx = floor(unrotatedTop.coerceIn(0f, imageHeight.toFloat()))
            .toInt()
            .coerceIn(0, imageHeight - 1)
        val rightPx = ceil(unrotatedRight.coerceIn(0f, imageWidth.toFloat()))
            .toInt()
            .coerceIn(leftPx + 1, imageWidth)
        val bottomPx = ceil(unrotatedBottom.coerceIn(0f, imageHeight.toFloat()))
            .toInt()
            .coerceIn(topPx + 1, imageHeight)

        return PixelCrop(
            left = leftPx,
            top = topPx,
            width = rightPx - leftPx,
            height = bottomPx - topPx,
        )
    }
}
