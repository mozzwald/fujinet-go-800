package com.mantismoonlabs.fujinetgo800.session

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import com.mantismoonlabs.fujinetgo800.MainActivity
import com.mantismoonlabs.fujinetgo800.settings.ControlMode
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionShellLifecycleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeRule.activity.stopService(Intent(composeRule.activity, EmulatorSessionService::class.java))
    }

    @Test
    fun launchCreatesSession() {
        resetLaunchDefaults()

        composeRule.onNodeWithContentDescription("Settings").assertExists()
        composeRule.onNodeWithContentDescription("Reset emulator").assertExists()
        waitForRunningSession()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Local media and ROM selection").assertExists()
        composeRule.onNodeWithText("Mount Disk").assertExists()
        composeRule.onNodeWithText("Close Settings").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Local media and ROM selection").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun backgroundResumeKeepsSession() {
        resetLaunchDefaults()
        waitForRunningSession()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        waitForRunningSession()
        composeRule.onNodeWithText("Launch Session").assertDoesNotExist()
        composeRule.onNodeWithText("HELP").assertExists()
    }

    @Test
    fun backgroundResumeRecoversFromLostRuntime() {
        resetLaunchDefaults()
        waitForRunningSession()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activity.stopService(Intent(composeRule.activity, EmulatorSessionService::class.java))
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Recovering session").assertDoesNotExist()
        composeRule.onNodeWithText("Launch Session").assertDoesNotExist()
    }

    private fun resetLaunchDefaults() {
        runBlocking {
            settingsRepository().updateLaunchMode(LaunchMode.LOCAL_ONLY)
            settingsRepository().updateControlMode(ControlMode.KEYBOARD)
            SessionRecoveryStore(composeRule.activity).clear()
        }
        composeRule.activity.stopService(Intent(composeRule.activity, EmulatorSessionService::class.java))
        composeRule.activityRule.scenario.recreate()
        waitForRunningSession()
    }

    private fun waitForRunningSession() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("HELP").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun settingsRepository(): EmulatorSettingsRepository {
        val field = MainActivity::class.java.getDeclaredField("emulatorSettingsRepository")
        field.isAccessible = true
        return field.get(composeRule.activity) as EmulatorSettingsRepository
    }
}
