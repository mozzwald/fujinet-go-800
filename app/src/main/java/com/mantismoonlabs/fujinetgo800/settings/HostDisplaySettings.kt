package com.mantismoonlabs.fujinetgo800.settings

import android.content.pm.ActivityInfo
import android.graphics.Rect
import kotlin.math.floor

internal data class DestinationRectBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal data class NormalizedDestinationPosition(
    val x: Float,
    val y: Float,
)

fun requestedOrientationFor(mode: OrientationMode): Int = when (mode) {
    OrientationMode.FOLLOW_SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
    OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
}

fun destinationRectFor(
    scaleMode: ScaleMode,
    canvasWidth: Int,
    canvasHeight: Int,
    frameWidth: Int,
    frameHeight: Int,
): Rect {
    val bounds = destinationRectBoundsFor(
        scaleMode = scaleMode,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        frameWidth = frameWidth,
        frameHeight = frameHeight,
    )
    return Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)
}

internal fun destinationRectBoundsFor(
    scaleMode: ScaleMode,
    canvasWidth: Int,
    canvasHeight: Int,
    frameWidth: Int,
    frameHeight: Int,
): DestinationRectBounds {
    val safeCanvasWidth = canvasWidth.coerceAtLeast(1)
    val safeCanvasHeight = canvasHeight.coerceAtLeast(1)
    val safeFrameWidth = frameWidth.coerceAtLeast(1)
    val safeFrameHeight = frameHeight.coerceAtLeast(1)
    val widthScale = safeCanvasWidth.toFloat() / safeFrameWidth
    val heightScale = safeCanvasHeight.toFloat() / safeFrameHeight
    val scale = when (scaleMode) {
        ScaleMode.FIT -> minOf(widthScale, heightScale)
        ScaleMode.FILL -> maxOf(widthScale, heightScale)
        ScaleMode.INTEGER -> {
            val fitScale = minOf(widthScale, heightScale)
            if (fitScale < 1f) fitScale else floor(fitScale).coerceAtLeast(1f)
        }
    }
    val destinationWidth = (safeFrameWidth * scale).toInt().coerceAtLeast(1)
    val destinationHeight = (safeFrameHeight * scale).toInt().coerceAtLeast(1)
    val left = (safeCanvasWidth - destinationWidth) / 2
    val top = (safeCanvasHeight - destinationHeight) / 2
    return DestinationRectBounds(
        left = left,
        top = top,
        right = left + destinationWidth,
        bottom = top + destinationHeight,
    )
}

internal fun normalizedDestinationPositionFor(
    scaleMode: ScaleMode,
    canvasWidth: Int,
    canvasHeight: Int,
    frameWidth: Int,
    frameHeight: Int,
    x: Float,
    y: Float,
    clampToBounds: Boolean,
): NormalizedDestinationPosition? {
    val bounds = destinationRectBoundsFor(
        scaleMode = scaleMode,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        frameWidth = frameWidth,
        frameHeight = frameHeight,
    )
    val outsideBounds = x < bounds.left || x > bounds.right || y < bounds.top || y > bounds.bottom
    if (outsideBounds && !clampToBounds) {
        return null
    }
    return NormalizedDestinationPosition(
        x = ((x - bounds.left) / bounds.width.coerceAtLeast(1f)).coerceIn(0f, 1f),
        y = ((y - bounds.top) / bounds.height.coerceAtLeast(1f)).coerceIn(0f, 1f),
    )
}

private val DestinationRectBounds.width: Float
    get() = (right - left).toFloat()

private val DestinationRectBounds.height: Float
    get() = (bottom - top).toFloat()

internal fun applyKeepScreenOn(
    keepScreenOn: Boolean,
    setKeepScreenOn: (Boolean) -> Unit,
) {
    setKeepScreenOn(keepScreenOn)
}
