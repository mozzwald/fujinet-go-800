package com.mantismoonlabs.fujinetgo800.ui

import android.Manifest
import android.content.Intent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.rule.GrantPermissionRule
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.mantismoonlabs.fujinetgo800.MainActivity
import com.mantismoonlabs.fujinetgo800.session.EmulatorSessionService
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EmulatorFoldLayoutTest {
    @get:Rule(order = 0)
    val notificationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val windowLayoutInfoPublisherRule = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeRule.activity.stopService(Intent(composeRule.activity, EmulatorSessionService::class.java))
    }

    @Test
    fun verticalSeparatingHingeKeepsEmulatorContentInOnePane() {
        val foldingFeature = FoldingFeature(
            activity = composeRule.activity,
            size = 40,
            state = FoldingFeature.State.HALF_OPENED,
            orientation = FoldingFeature.Orientation.VERTICAL,
        )
        windowLayoutInfoPublisherRule.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(listOf(foldingFeature)),
        )

        var contentBounds = Rect.Zero
        composeRule.waitUntil(timeoutMillis = 5_000) {
            contentBounds = composeRule.onNodeWithTag("emulator-safe-content")
                .fetchSemanticsNode().boundsInRoot
            contentBounds.right <= foldingFeature.bounds.left ||
                contentBounds.left >= foldingFeature.bounds.right
        }

        assertTrue(
            contentBounds.right <= foldingFeature.bounds.left ||
                contentBounds.left >= foldingFeature.bounds.right,
        )
    }

    /**
     * These two scenarios only exercise the new square-foldable layout when the test host's
     * actual window is large and near-square (e.g. an unfolded foldable emulator profile) --
     * the width/aspect thresholds in [EmulatorRuntimeLayout] are evaluated against the real
     * window size, which [WindowLayoutInfoPublisherRule] does not override.
     */
    @Test
    fun unfoldedSquareWindowShowsStackedJoystickControls() {
        windowLayoutInfoPublisherRule.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("square-emulator").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("square-emulator").assertExists()
        composeRule.onNodeWithTag("square-control-band").assertExists()
        composeRule.onNodeWithTag("square-port-status-strip").assertExists()
    }

    @Test
    fun unfoldedSquareWindowKeyboardModeHidesJoystickControls() {
        windowLayoutInfoPublisherRule.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("square-toggle-input-button").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("square-toggle-input-button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("square-keyboard").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("square-keyboard").assertExists()
        composeRule.onNodeWithTag("square-control-band").assertDoesNotExist()
    }
}
