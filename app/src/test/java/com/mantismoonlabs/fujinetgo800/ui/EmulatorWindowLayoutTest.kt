package com.mantismoonlabs.fujinetgo800.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorWindowLayoutTest {
    @Test
    fun immersivePhoneUsesFullWindowWhenNoPhysicalObstructionIsPresent() {
        val layout = calculate(width = 1080, height = 2400)

        assertEquals(EmulatorWindowRect(0, 0, 1080, 2400), layout.contentBounds)
        assertEquals(EmulatorLayoutClass.CompactPortrait, layout.layoutClass)
    }

    @Test
    fun phonePortraitUsesSafeWindowBounds() {
        val layout = calculate(
            width = 1080,
            height = 2400,
            insets = EmulatorWindowInsets(left = 12, top = 72, right = 12, bottom = 120),
        )

        assertEquals(EmulatorWindowRect(12, 72, 1068, 2280), layout.contentBounds)
        assertEquals(EmulatorLayoutClass.CompactPortrait, layout.layoutClass)
        assertFalse(layout.isLandscape)
    }

    @Test
    fun shortLandscapeWindowUsesCurrentBoundsRatherThanPhysicalOrientation() {
        val layout = calculate(width = 1200, height = 540)

        assertEquals(EmulatorLayoutClass.CompactLandscape, layout.layoutClass)
        assertTrue(layout.isLandscape)
    }

    @Test
    fun expandedTabletUsesFullSafeArea() {
        val layout = calculate(width = 2560, height = 1600)

        assertEquals(EmulatorWindowRect(0, 0, 2560, 1600), layout.contentBounds)
        assertEquals(EmulatorLayoutClass.ExpandedLandscape, layout.layoutClass)
    }

    @Test
    fun verticalSeparatingHingeChoosesLargestUninterruptedPane() {
        val layout = calculate(
            width = 2200,
            height = 1800,
            fold = fold(
                bounds = EmulatorWindowRect(1000, 0, 1080, 1800),
                orientation = EmulatorFoldOrientation.Vertical,
            ),
        )

        assertEquals(EmulatorWindowRect(1080, 0, 2200, 1800), layout.contentBounds)
        assertEquals(EmulatorFoldPosture.Book, layout.foldPosture)
    }

    @Test
    fun equalBookPanesChooseLeadingPaneDeterministically() {
        val layout = calculate(
            width = 2080,
            height = 1800,
            fold = fold(
                bounds = EmulatorWindowRect(1000, 0, 1080, 1800),
                orientation = EmulatorFoldOrientation.Vertical,
            ),
        )

        assertEquals(EmulatorWindowRect(0, 0, 1000, 1800), layout.contentBounds)
    }

    @Test
    fun horizontalSeparatingHingeChoosesLargestUninterruptedPane() {
        val layout = calculate(
            width = 1800,
            height = 2200,
            fold = fold(
                bounds = EmulatorWindowRect(0, 900, 1800, 980),
                orientation = EmulatorFoldOrientation.Horizontal,
            ),
        )

        assertEquals(EmulatorWindowRect(0, 980, 1800, 2200), layout.contentBounds)
        assertEquals(EmulatorFoldPosture.Tabletop, layout.foldPosture)
    }

    @Test
    fun nonSeparatingFlatFoldKeepsFullWindow() {
        val layout = calculate(
            width = 2200,
            height = 1800,
            fold = fold(
                bounds = EmulatorWindowRect(1080, 0, 1080, 1800),
                orientation = EmulatorFoldOrientation.Vertical,
                separating = false,
                occluding = false,
            ),
        )

        assertEquals(EmulatorWindowRect(0, 0, 2200, 1800), layout.contentBounds)
        assertEquals(EmulatorFoldPosture.None, layout.foldPosture)
    }

    @Test
    fun occludingFoldSplitsWindowEvenWhenNotSeparating() {
        val layout = calculate(
            width = 2200,
            height = 1800,
            fold = fold(
                bounds = EmulatorWindowRect(1050, 0, 1150, 1800),
                orientation = EmulatorFoldOrientation.Vertical,
                separating = false,
                occluding = true,
            ),
        )

        assertEquals(EmulatorWindowRect(0, 0, 1050, 1800), layout.contentBounds)
        assertEquals(EmulatorFoldPosture.Book, layout.foldPosture)
    }

    @Test
    fun foldOutsideSafeBoundsDoesNotChangeContent() {
        val layout = calculate(
            width = 1200,
            height = 2000,
            insets = EmulatorWindowInsets(top = 100),
            fold = fold(
                bounds = EmulatorWindowRect(0, 20, 1200, 40),
                orientation = EmulatorFoldOrientation.Horizontal,
            ),
        )

        assertEquals(EmulatorWindowRect(0, 100, 1200, 2000), layout.contentBounds)
        assertEquals(EmulatorFoldPosture.None, layout.foldPosture)
    }

    @Test
    fun invalidInsetsFallBackToUsableWindow() {
        val layout = calculate(
            width = 800,
            height = 600,
            insets = EmulatorWindowInsets(left = 500, right = 500),
        )

        assertEquals(EmulatorWindowRect(0, 0, 800, 600), layout.contentBounds)
    }

    private fun calculate(
        width: Int,
        height: Int,
        insets: EmulatorWindowInsets = EmulatorWindowInsets(),
        fold: EmulatorFold? = null,
    ): EmulatorWindowLayout = calculateEmulatorWindowLayout(
        metrics = EmulatorWindowMetrics(
            widthPx = width,
            heightPx = height,
            safeInsets = insets,
            fold = fold,
        ),
        expandedWidthThresholdPx = 1400,
    )

    private fun fold(
        bounds: EmulatorWindowRect,
        orientation: EmulatorFoldOrientation,
        separating: Boolean = true,
        occluding: Boolean = false,
    ) = EmulatorFold(
        bounds = bounds,
        orientation = orientation,
        isSeparating = separating,
        isOccluding = occluding,
    )
}
