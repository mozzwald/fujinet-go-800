package com.mantismoonlabs.fujinetgo800.settings

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostDisplaySettingsTest {
    @Test
    fun orientationModesMapToAndroidConstants() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER,
            requestedOrientationFor(OrientationMode.FOLLOW_SYSTEM),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT,
            requestedOrientationFor(OrientationMode.PORTRAIT),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE,
            requestedOrientationFor(OrientationMode.LANDSCAPE),
        )
    }

    @Test
    fun fitAndFillProduceDifferentDestinationRects() {
        val fitRect = destinationRectFor(
            scaleMode = ScaleMode.FIT,
            canvasWidth = 1000,
            canvasHeight = 1000,
            frameWidth = 320,
            frameHeight = 240,
        )
        val fillRect = destinationRectFor(
            scaleMode = ScaleMode.FILL,
            canvasWidth = 1000,
            canvasHeight = 1000,
            frameWidth = 320,
            frameHeight = 240,
        )
        val fitBounds = destinationRectBoundsFor(
            scaleMode = ScaleMode.FIT,
            canvasWidth = 1000,
            canvasHeight = 1000,
            frameWidth = 320,
            frameHeight = 240,
        )
        val fillBounds = destinationRectBoundsFor(
            scaleMode = ScaleMode.FILL,
            canvasWidth = 1000,
            canvasHeight = 1000,
            frameWidth = 320,
            frameHeight = 240,
        )

        assertEquals(
            DestinationRectBounds(left = 0, top = 125, right = 1000, bottom = 875),
            fitBounds,
        )
        assertEquals(
            DestinationRectBounds(left = -166, top = 0, right = 1167, bottom = 999),
            fillBounds,
        )
        assertTrue(fitBounds != fillBounds)
        assertEquals(0, fitRect.left)
        assertEquals(0, fillRect.left)
    }

    @Test
    fun fitAndIntegerRemainCenteredInUltraWideWindow() {
        assertEquals(
            DestinationRectBounds(left = 600, top = 0, right = 1800, bottom = 900),
            destinationRectBoundsFor(
                scaleMode = ScaleMode.FIT,
                canvasWidth = 2400,
                canvasHeight = 900,
                frameWidth = 320,
                frameHeight = 240,
            ),
        )
        assertEquals(
            DestinationRectBounds(left = 720, top = 90, right = 1680, bottom = 810),
            destinationRectBoundsFor(
                scaleMode = ScaleMode.INTEGER,
                canvasWidth = 2400,
                canvasHeight = 900,
                frameWidth = 320,
                frameHeight = 240,
            ),
        )
    }

    @Test
    fun fitRemainsCenteredInTallFoldPane() {
        assertEquals(
            DestinationRectBounds(left = 0, top = 540, right = 960, bottom = 1260),
            destinationRectBoundsFor(
                scaleMode = ScaleMode.FIT,
                canvasWidth = 960,
                canvasHeight = 1800,
                frameWidth = 320,
                frameHeight = 240,
            ),
        )
    }

    @Test
    fun normalizedInputRejectsLetterboxAndMapsFrameCorners() {
        val outside = normalizedDestinationPositionFor(
            scaleMode = ScaleMode.FIT,
            canvasWidth = 1000,
            canvasHeight = 1000,
            frameWidth = 320,
            frameHeight = 240,
            x = 500f,
            y = 50f,
            clampToBounds = false,
        )
        val topLeft = normalizedDestinationPositionFor(
            scaleMode = ScaleMode.FIT,
            canvasWidth = 1000,
            canvasHeight = 1000,
            frameWidth = 320,
            frameHeight = 240,
            x = 0f,
            y = 125f,
            clampToBounds = false,
        )
        val bottomRight = normalizedDestinationPositionFor(
            scaleMode = ScaleMode.FIT,
            canvasWidth = 1000,
            canvasHeight = 1000,
            frameWidth = 320,
            frameHeight = 240,
            x = 1000f,
            y = 875f,
            clampToBounds = false,
        )

        assertEquals(null, outside)
        assertEquals(NormalizedDestinationPosition(0f, 0f), topLeft)
        assertEquals(NormalizedDestinationPosition(1f, 1f), bottomRight)
    }

    @Test
    fun keepScreenOnStateIsAppliedOnTheHostView() {
        val appliedStates = mutableListOf<Boolean>()

        applyKeepScreenOn(keepScreenOn = true) { appliedStates.add(it) }
        applyKeepScreenOn(keepScreenOn = false) { appliedStates.add(it) }

        assertEquals(listOf(true, false), appliedStates)
    }
}
