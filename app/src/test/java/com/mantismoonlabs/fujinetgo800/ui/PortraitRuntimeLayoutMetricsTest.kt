package com.mantismoonlabs.fujinetgo800.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortraitRuntimeLayoutMetricsTest {
    @Test
    fun defaultFractionKeepsSquareFoldableEmulatorFullWidth() {
        val metrics = calculatePortraitRuntimeLayoutMetrics(
            layoutAvailableHeight = 720.dp,
            contentWidth = 800.dp,
            fullWidthEmulatorHeight = 600.dp,
            sizeFraction = 1f,
        )

        assertDpEquals(800.dp, metrics.emulatorWidth)
        assertDpEquals(600.dp, metrics.emulatorHeight)
        assertDpEquals(120.dp, metrics.inputPanelMetrics.totalHeight)
    }

    @Test
    fun extendedFractionShrinksSquareFoldableEmulatorForControls() {
        val metrics = calculatePortraitRuntimeLayoutMetrics(
            layoutAvailableHeight = 720.dp,
            contentWidth = 800.dp,
            fullWidthEmulatorHeight = 600.dp,
            sizeFraction = 2f,
        )

        assertTrue(metrics.emulatorWidth < 800.dp)
        assertTrue(metrics.emulatorHeight < 600.dp)
        assertTrue(metrics.inputPanelMetrics.totalHeight > 120.dp)
        assertDpEquals(464.dp, metrics.emulatorWidth)
        assertDpEquals(348.dp, metrics.emulatorHeight)
    }

    @Test
    fun regularPortraitPhoneCanStillShrinkWithinMinimumWidth() {
        val metrics = calculatePortraitRuntimeLayoutMetrics(
            layoutAvailableHeight = 650.dp,
            contentWidth = 392.dp,
            fullWidthEmulatorHeight = 294.dp,
            sizeFraction = 2f,
        )

        assertTrue(metrics.emulatorWidth < 392.dp)
        assertTrue(metrics.emulatorWidth >= 320.dp)
        assertTrue(metrics.inputPanelMetrics.totalHeight > 356.dp)
    }

    private fun assertDpEquals(expected: Dp, actual: Dp) {
        assertEquals(expected.value, actual.value, 0.01f)
    }
}
