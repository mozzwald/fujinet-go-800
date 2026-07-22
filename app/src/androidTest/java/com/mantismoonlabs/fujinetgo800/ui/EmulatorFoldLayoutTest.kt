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
}
