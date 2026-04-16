package com.mantismoonlabs.fujinetgo800.session

import android.Manifest
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.mantismoonlabs.fujinetgo800.MainActivity
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionLaunchModesShellTest {
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
    fun canStartLocalOnlySession() {
        resetLaunchMode(LaunchMode.LOCAL_ONLY)
        waitForSessionStarted()
        composeRule.onNodeWithText("HELP").assertExists()
    }

    @Test
    fun persistedLaunchModeAutoStartsAfterRecreate() {
        resetLaunchMode(LaunchMode.LOCAL_ONLY)
        waitForSessionStarted()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("FujiNet").assertExists()
        composeRule.onNodeWithText("Current mode: Local only").assertExists()
        composeRule.onNodeWithText("Close Settings").performScrollTo().performClick()

        composeRule.activityRule.scenario.recreate()

        waitForSessionStarted()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        waitForText("Current mode: Local only")
    }

    @Test
    fun changingLaunchModeInSettingsRestartsIntoSelectedMode() {
        resetLaunchMode(LaunchMode.FUJINET_ENABLED)

        waitForSessionStarted()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("FujiNet").assertExists()
        composeRule.onNodeWithText("Use Local Only").performScrollTo().performClick()
        composeRule.onNodeWithText("Close Settings").performScrollTo().performClick()

        waitForSessionStarted()
        composeRule.onNodeWithText("Settings").performClick()
        waitForText("Current mode: Local only")
    }

    private fun resetLaunchMode(launchMode: LaunchMode) {
        runBlocking {
            settingsRepository().updateLaunchMode(launchMode)
        }
        composeRule.activityRule.scenario.recreate()
    }

    private fun waitForSessionStarted() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText("Launch").fetchSemanticsNodes().isEmpty()
        }
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
