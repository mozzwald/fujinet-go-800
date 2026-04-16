package com.mantismoonlabs.fujinetgo800.input

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mantismoonlabs.fujinetgo800.MainActivity
import com.mantismoonlabs.fujinetgo800.settings.ControlMode
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.KeyboardInputMode
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.session.EmulatorSessionService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputControlsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeRule.activity.stopService(Intent(composeRule.activity, EmulatorSessionService::class.java))
    }

    @Test
    fun controlModeTogglePersistsAcrossRecreate() {
        resetInputSettings()
        waitForRunningSession()

        composeRule.onNodeWithText("HELP").assertExists()

        composeRule.onNodeWithTag("toggle-input-button").performClick()
        waitForJoystickMode()
        composeRule.onNodeWithText("ESC").assertExists()

        composeRule.activityRule.scenario.recreate()

        waitForControlsVisible()
        waitForJoystickMode()
        composeRule.onNodeWithText("ESC").assertExists()
    }

    @Test
    fun keyboardModeShowsFunctionBarAndImeProxy() {
        resetInputSettings()
        waitForRunningSession()

        composeRule.onNodeWithTag("input-hide-hint").assertExists()
        composeRule.onNodeWithTag("android-ime-proxy").assertExists()
        composeRule.onNodeWithText("HELP").assertExists()
        composeRule.onNodeWithText("START").assertExists()
        composeRule.onNodeWithText("SELECT").assertExists()
        composeRule.onNodeWithText("OPTION").assertExists()
        composeRule.onNodeWithText("ESC").assertExists()
        composeRule.onNodeWithText("SPACE").assertDoesNotExist()

        composeRule.onNodeWithTag("toggle-input-button").performClick()
        waitForJoystickMode()
    }

    @Test
    fun joystickModeHidesKeyboardAndShowsTouchControls() {
        resetInputSettings()
        waitForRunningSession()

        composeRule.onNodeWithTag("toggle-input-button").performClick()

        waitForJoystickMode()
        composeRule.onNodeWithTag("fire-button").assertExists()
        composeRule.onNodeWithTag("joystick-pad").assertExists()
        composeRule.onNodeWithText("ESC").assertExists()
    }

    @Test
    fun longPressTopToggleHidesAndShowsInputPanel() {
        resetInputSettings()
        waitForRunningSession()

        composeRule.onNodeWithTag("top-toggle-input-button").performTouchInput { longClick() }
        waitForInputPanelHidden()
        composeRule.onNodeWithTag("android-ime-proxy").assertDoesNotExist()
        composeRule.onNodeWithText("ESC").assertExists()

        composeRule.onNodeWithTag("top-toggle-input-button").performTouchInput { longClick() }
        waitForControlsVisible()
        composeRule.onNodeWithTag("android-ime-proxy").assertExists()
    }

    private fun waitForRunningSession() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("toggle-input-button").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForJoystickMode() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("fire-button").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForControlsVisible() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("toggle-input-button").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForInputPanelHidden() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("toggle-input-button").fetchSemanticsNodes().isEmpty()
        }
    }

    private fun resetInputSettings() {
        runBlocking {
            settingsRepository().updateLaunchMode(LaunchMode.LOCAL_ONLY)
            settingsRepository().updateControlMode(ControlMode.KEYBOARD)
            settingsRepository().updateInputPanelVisible(true)
            settingsRepository().updateInputHideHintSeen(false)
            settingsRepository().updateKeyboardInputMode(KeyboardInputMode.ANDROID)
        }
        composeRule.activityRule.scenario.recreate()
        waitForRunningSession()
    }

    private fun settingsRepository(): EmulatorSettingsRepository {
        val field = MainActivity::class.java.getDeclaredField("emulatorSettingsRepository")
        field.isAccessible = true
        return field.get(composeRule.activity) as EmulatorSettingsRepository
    }
}
