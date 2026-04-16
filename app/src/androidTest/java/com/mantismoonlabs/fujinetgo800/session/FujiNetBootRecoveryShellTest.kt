package com.mantismoonlabs.fujinetgo800.session

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.storage.LocalMediaViewModel
import com.mantismoonlabs.fujinetgo800.storage.MediaDocumentStore
import com.mantismoonlabs.fujinetgo800.storage.RuntimePaths
import com.mantismoonlabs.fujinetgo800.ui.EmulatorScreen
import com.mantismoonlabs.fujinetgo800.ui.theme.Fuji800ATheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FujiNetBootRecoveryShellTest {
    @get:Rule
    val notificationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun fujiNetDefaultStartShowsBootStatus() {
        val sessionRepository = FakeSessionRepository(
            initialState = SessionState.ReadyToLaunch(launchMode = LaunchMode.FUJINET_ENABLED),
            blockOnFujiNetStart = true,
        )
        showShell(sessionRepository)

        waitForText("Boot status")
        waitForText("Starting FujiNet")
        waitForText("Waiting for FujiNet readiness")
        sessionRepository.releaseFujiNetBarrier()
    }

    @Test
    fun fujiNetFailureOffersLocalRecovery() {
        val sessionRepository = FakeSessionRepository(
            initialState = SessionState.Failed(
                launchMode = LaunchMode.FUJINET_ENABLED,
                reason = FujiNetFailureReason.ReadinessTimeout,
                canRecoverLocally = true,
                message = FujiNetFailureReason.ReadinessTimeout.defaultMessage,
            ),
        )
        showShell(sessionRepository)

        waitForText("FujiNet Recovery")
        waitForText("FujiNet failed before the default boot could finish.")
        composeRule.onNodeWithText("Start Local Only").assertExists()
        composeRule.onNodeWithText("Start Local Only").performClick()

        waitForText("Preparing emulator")
    }

    @Test
    fun shellShowsCurrentFujiNetModeAndHealth() {
        val sessionRepository = FakeSessionRepository(
            initialState = SessionState.StartingFujiNet(launchMode = LaunchMode.FUJINET_ENABLED),
        )
        showShell(sessionRepository)

        waitForText("Starting FujiNet")
        waitForText("Waiting for FujiNet readiness")

        composeRule.runOnUiThread {
            sessionRepository.update(SessionState.Starting(launchMode = LaunchMode.LOCAL_ONLY))
        }

        waitForText("Preparing emulator")
    }

    private fun showShell(sessionRepository: FakeSessionRepository) {
        val settingsRepository = EmulatorSettingsRepository(composeRule.activity)
        runBlocking {
            settingsRepository.updateLaunchMode(LaunchMode.FUJINET_ENABLED)
        }
        val runtimePaths = RuntimePaths.fromFilesDirectory(composeRule.activity.filesDir)
        val localMediaViewModel = LocalMediaViewModel(
            runtimePaths = runtimePaths,
            documentStore = MediaDocumentStore(runtimePaths),
        )

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                Fuji800ATheme {
                    Surface {
                        EmulatorScreen(
                            settingsRepository = settingsRepository,
                            sessionRepository = sessionRepository,
                            localMediaViewModel = localMediaViewModel,
                            keyboardResetTrigger = 0,
                            onClearMediaSelection = {},
                            onPickSystemRom = {},
                            onClearSystemRom = {},
                        )
                    }
                }
            }
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

private class FakeSessionRepository(
    initialState: SessionState,
    private val blockOnFujiNetStart: Boolean = false,
) : SessionRepository {
    private val mutableState = MutableStateFlow(initialState)

    override val state: StateFlow<SessionState> = mutableState.asStateFlow()

    override fun dispatch(command: SessionCommand) {
        when (command) {
            is SessionCommand.StartSession -> {
                mutableState.value = SessionState.StartingFujiNet(launchMode = LaunchMode.FUJINET_ENABLED)
                if (!blockOnFujiNetStart) {
                    mutableState.value = SessionState.Starting(launchMode = LaunchMode.FUJINET_ENABLED)
                }
            }

            SessionCommand.RecoverLocalOnly -> {
                mutableState.value = SessionState.Starting(launchMode = LaunchMode.LOCAL_ONLY)
            }

            else -> Unit
        }
    }

    fun update(state: SessionState) {
        mutableState.value = state
    }

    fun releaseFujiNetBarrier() {
        if (blockOnFujiNetStart) {
            mutableState.value = SessionState.Starting(launchMode = LaunchMode.FUJINET_ENABLED)
        }
    }
}
