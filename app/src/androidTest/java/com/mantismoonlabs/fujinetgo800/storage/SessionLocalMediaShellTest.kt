package com.mantismoonlabs.fujinetgo800.storage

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mantismoonlabs.fujinetgo800.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionLocalMediaShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsShowLocalMediaControls() {
        composeRule.onNodeWithText("Session Settings").assertExists()
        composeRule.onNodeWithText("Session Settings").performClick()

        composeRule.onNodeWithText("Mount Disk").assertExists()
        composeRule.onNodeWithText("Select Cartridge").assertExists()
        composeRule.onNodeWithText("Select Executable").assertExists()
        composeRule.onNodeWithText("Select OS ROM").assertExists()
        composeRule.onNodeWithText("Default built-in Altirra ROM").assertExists()
    }

    @Test
    fun mountedMediaCanBeReplacedOrEjected() {
        composeRule.activity.runOnUiThread {
            localMediaViewModel().onDocumentPicked(
                role = MediaRole.DISK,
                uriString = "content://provider/disks/first.atr",
                displayName = "first.atr",
            )
        }

        openSettings()
        composeRule.onNodeWithText("first.atr").assertExists()
        composeRule.onNodeWithText("Eject Disk").assertExists()

        composeRule.activity.runOnUiThread {
            localMediaViewModel().onDocumentPicked(
                role = MediaRole.DISK,
                uriString = "content://provider/disks/second.atr",
                displayName = "second.atr",
            )
        }

        openSettings()
        composeRule.onNodeWithText("second.atr").assertExists()
        composeRule.onNodeWithText("Eject Disk").performClick()
        composeRule.onNodeWithText("No disk selected").assertExists()
    }

    private fun openSettings() {
        composeRule.onNode(hasText("Session Settings") and hasClickAction()).assertExists()
        composeRule.onNode(hasText("Session Settings") and hasClickAction()).performClick()
    }

    private fun localMediaViewModel(): LocalMediaViewModel {
        val field = MainActivity::class.java.getDeclaredField("localMediaViewModel")
        field.isAccessible = true
        return field.get(composeRule.activity) as LocalMediaViewModel
    }
}
