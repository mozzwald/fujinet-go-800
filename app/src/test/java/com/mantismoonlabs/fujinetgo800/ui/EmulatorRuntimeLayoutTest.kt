package com.mantismoonlabs.fujinetgo800.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorRuntimeLayoutTest {
    @Test
    fun regularPortraitPhoneUsesStackedLayout() {
        val layout = calculate(width = 411f, height = 891f)

        assertEquals(EmulatorRuntimePlacement.Stacked, layout.placement)
        assertFalse(layout.showsConcurrentTouchControls)
    }

    @Test
    fun regularLandscapePhoneKeepsWideLayout() {
        val layout = calculate(width = 891f, height = 411f)

        assertEquals(EmulatorRuntimePlacement.Wide, layout.placement)
    }

    @Test
    fun expandedNearSquarePortraitUsesWideLayout() {
        val layout = calculate(width = 840f, height = 900f)

        assertTrue(layout.isWide)
    }

    @Test
    fun expandedNearSquareRequiresCompleteRailWidthBudget() {
        val below = calculate(
            width = MinimumWideRuntimeWidthDp - 1f,
            height = 800f,
        )
        val atThreshold = calculate(
            width = MinimumWideRuntimeWidthDp,
            height = 800f,
        )

        assertFalse(below.isWide)
        assertTrue(atThreshold.isWide)
    }

    @Test
    fun expandedNearSquareRequiresAspectRatioThreshold() {
        val width = 800f
        val justBelow = calculate(
            width = width,
            height = width / (MinimumExpandedWideAspectRatio - 0.001f),
        )
        val justAbove = calculate(
            width = width,
            height = width / (MinimumExpandedWideAspectRatio + 0.001f),
        )

        assertFalse(justBelow.isWide)
        assertTrue(justAbove.isWide)
    }

    @Test
    fun expandedTallPortraitRemainsStacked() {
        val layout = calculate(width = 700f, height = 1100f)

        assertEquals(EmulatorRuntimePlacement.Stacked, layout.placement)
    }

    @Test
    fun shortWideKeyboardUsesFunctionRails() {
        val layout = calculate(
            width = 891f,
            height = 411f,
            keyboardSelected = true,
            keyboardPanelHeight = 240f,
        )

        assertEquals(EmulatorWideKeyboardRails.FunctionKeys, layout.keyboardRails)
    }

    @Test
    fun largeNearSquareKeyboardUsesConcurrentTouchControls() {
        val layout = calculate(
            width = 840f,
            height = 900f,
            keyboardSelected = true,
            keyboardPanelHeight = 300f,
        )

        assertTrue(layout.showsConcurrentTouchControls)
    }

    @Test
    fun combinedControlsRequireMinimumWidth() {
        val below = calculate(
            width = MinimumCombinedControlsWidthDp - 1f,
            height = 800f,
            keyboardSelected = true,
            keyboardPanelHeight = 300f,
        )
        val atThreshold = calculate(
            width = MinimumCombinedControlsWidthDp,
            height = 800f,
            keyboardSelected = true,
            keyboardPanelHeight = 300f,
        )

        assertFalse(below.showsConcurrentTouchControls)
        assertTrue(atThreshold.showsConcurrentTouchControls)
    }

    @Test
    fun combinedControlsRequireMinimumTopRowHeight() {
        val below = calculate(
            width = 800f,
            height = 300f + 8f + MinimumCombinedControlsTopRowHeightDp - 1f,
            keyboardSelected = true,
            keyboardPanelHeight = 300f,
        )
        val atThreshold = calculate(
            width = 800f,
            height = 300f + 8f + MinimumCombinedControlsTopRowHeightDp,
            keyboardSelected = true,
            keyboardPanelHeight = 300f,
        )

        assertFalse(below.showsConcurrentTouchControls)
        assertTrue(atThreshold.showsConcurrentTouchControls)
    }

    @Test
    fun joystickSelectionNeverRequestsCombinedKeyboardRails() {
        val layout = calculate(
            width = 840f,
            height = 900f,
            keyboardSelected = false,
            keyboardPanelHeight = 300f,
        )

        assertTrue(layout.isWide)
        assertFalse(layout.showsConcurrentTouchControls)
    }

    @Test
    fun portraitHeightBudgetTradesViewportHeightForUsefulControls() {
        val budget = calculateEmulatorPortraitHeightBudget(
            totalHeightDp = 900f,
            fixedChromeHeightDp = 80f,
            desiredViewportHeightDp = 630f,
            usefulInputPanelHeightDp = 350f,
        )

        assertEquals(350f, budget.maxInputPanelHeightDp)
        assertEquals(369f, budget.minimumViewportHeightDp)
    }

    @Test
    fun portraitHeightBudgetPreservesMinimumViewportOnShortWindow() {
        val budget = calculateEmulatorPortraitHeightBudget(
            totalHeightDp = 500f,
            fixedChromeHeightDp = 80f,
            desiredViewportHeightDp = 300f,
            usefulInputPanelHeightDp = 350f,
        )

        assertEquals(189f, budget.minimumViewportHeightDp)
        assertEquals(231f, budget.maxInputPanelHeightDp)
    }

    @Test
    fun scalablePortraitControlsCanUseAllSpaceAboveMinimumViewport() {
        val budget = calculateEmulatorPortraitHeightBudget(
            totalHeightDp = 840f,
            fixedChromeHeightDp = 70f,
            desiredViewportHeightDp = 296f,
            usefulInputPanelHeightDp = 372f,
            allowInputPanelExpansion = true,
        )

        assertEquals(474f, budget.maxInputPanelHeightDp)
        assertEquals(296f, budget.minimumViewportHeightDp)
    }

    @Test
    fun normalPortraitKeyboardHeightCanUseAvailableSpaceWithoutShrinkingViewport() {
        val budget = calculateEmulatorPortraitHeightBudget(
            totalHeightDp = 840f,
            fixedChromeHeightDp = 70f,
            desiredViewportHeightDp = 296f,
            usefulInputPanelHeightDp = 482f,
        )

        assertEquals(474f, budget.maxInputPanelHeightDp)
        assertEquals(296f, budget.minimumViewportHeightDp)
    }

    private fun calculate(
        width: Float,
        height: Float,
        keyboardSelected: Boolean = false,
        keyboardPanelHeight: Float = 240f,
    ): EmulatorRuntimeLayout = calculateEmulatorRuntimeLayout(
        EmulatorRuntimeLayoutMetrics(
            widthDp = width,
            heightDp = height,
            keyboardPanelHeightDp = keyboardPanelHeight,
            keyboardSelected = keyboardSelected,
        ),
    )
}
