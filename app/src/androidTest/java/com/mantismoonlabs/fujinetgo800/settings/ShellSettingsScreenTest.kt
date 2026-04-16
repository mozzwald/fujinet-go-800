package com.mantismoonlabs.fujinetgo800.settings

import android.Manifest
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mantismoonlabs.fujinetgo800.MainActivity
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.session.EmulatorSessionService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShellSettingsScreenTest {
    @get:Rule
    val notificationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeRule.activity.stopService(Intent(composeRule.activity, EmulatorSessionService::class.java))
    }

    @Test
    fun tabbedSettingsShellShowsAppSections() {
        resetHostSettings()

        openSettings()
        composeRule.onNodeWithTag("settings-tab-machine").assertExists()
        composeRule.onNodeWithTag("settings-tab-fujinet").assertExists()
        composeRule.onNodeWithTag("settings-tab-app").assertExists()
        composeRule.onNodeWithTag("settings-tab-app").performClick()
        composeRule.onNodeWithText("Display settings").assertExists()
        composeRule.onNodeWithText("Scale mode").assertExists()
        composeRule.onNodeWithText("Rotate screen").assertExists()
        composeRule.onNodeWithText("Keyboard haptics").assertExists()
        composeRule.onNodeWithText("Power").assertExists()
        composeRule.onNodeWithText("Pause on app switch").assertExists()
        composeRule.onNodeWithText("Portrait").assertExists()
        composeRule.onNodeWithText("Landscape").assertExists()

        composeRule.onNodeWithText("Close Settings").performClick()
    }

    @Test
    fun machineAndFujiNetTabsExposeGroupedSettings() {
        resetHostSettings()

        openSettings()
        composeRule.onNodeWithTag("settings-tab-machine").performClick()
        composeRule.onNodeWithText("Machine type").assertExists()
        composeRule.onNodeWithText("5200").assertExists()
        composeRule.onNodeWithText("RAM size").assertExists()
        composeRule.onNodeWithText("Boot with BASIC").assertExists()
        composeRule.onNodeWithText("ROM & Firmware").assertExists()

        composeRule.onNodeWithTag("settings-tab-fujinet").performClick()
        composeRule.onNodeWithText("Runtime storage").assertExists()
        composeRule.onNodeWithText("Open FujiNet webUI").assertExists()
        composeRule.onNodeWithText("Printer").assertExists()
        composeRule.onNodeWithText("Use as virtual printer").assertExists()
        composeRule.onNodeWithText("HSIO Settings").assertExists()
        composeRule.onNodeWithText("HSIO Index").assertExists()
        composeRule.onNodeWithText("Boot Settings").assertExists()
        composeRule.onNodeWithText("Enable CONFIG boot disk").assertExists()
        composeRule.onNodeWithText("Debug").assertExists()
        composeRule.onNodeWithText("Close Settings").performClick()
    }

    private fun resetHostSettings() {
        runBlocking {
            settingsRepository().updateLaunchMode(LaunchMode.FUJINET_ENABLED)
            settingsRepository().updateScaleMode(ScaleMode.FIT)
            settingsRepository().updateKeepScreenOn(true)
            settingsRepository().updateOrientationMode(OrientationMode.FOLLOW_SYSTEM)
        }
        composeRule.activityRule.scenario.recreate()
        waitForText("Settings")
    }

    private fun openSettings() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun settingsRepository(): EmulatorSettingsRepository {
        val field = MainActivity::class.java.getDeclaredField("emulatorSettingsRepository")
        field.isAccessible = true
        return field.get(composeRule.activity) as EmulatorSettingsRepository
    }
}
