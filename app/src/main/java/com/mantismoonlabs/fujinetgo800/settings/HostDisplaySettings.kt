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

internal fun applyKeepScreenOn(
    keepScreenOn: Boolean,
    setKeepScreenOn: (Boolean) -> Unit,
) {
    setKeepScreenOn(keepScreenOn)
}
