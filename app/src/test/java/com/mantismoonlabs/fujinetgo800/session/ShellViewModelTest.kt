package com.mantismoonlabs.fujinetgo800.session

import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun runningStateUsesPauseLabel() = runTest {
        val repository = FakeSessionRepository(
            SessionState.Running(
                sessionToken = 7,
                paused = false,
                surfaceAttached = true,
                launchMode = LaunchMode.FUJINET_ENABLED,
            )
        )
        val viewModel = ShellViewModel(repository)

        advanceUntilIdle()

        assertEquals("Pause", viewModel.uiState.value.pauseButtonLabel)
        assertEquals("Running", viewModel.uiState.value.statusLabel)
        assertEquals("Session #7", viewModel.uiState.value.sessionLabel)
        assertTrue(viewModel.uiState.value.isPauseEnabled)
    }

    @Test
    fun pausedStateUsesResumeLabel() = runTest {
        val repository = FakeSessionRepository(
            SessionState.Running(
                sessionToken = 7,
                paused = true,
                surfaceAttached = false,
                launchMode = LaunchMode.FUJINET_ENABLED,
            )
        )
        val viewModel = ShellViewModel(repository)

        advanceUntilIdle()

        assertEquals("Resume", viewModel.uiState.value.pauseButtonLabel)
        assertEquals("Paused", viewModel.uiState.value.statusLabel)
        assertEquals("Session #7", viewModel.uiState.value.sessionLabel)
        assertTrue(viewModel.uiState.value.isPauseEnabled)
    }

    @Test
    fun readyToLaunchStateUsesNonRunningLabels() = runTest {
        val repository = FakeSessionRepository(
            SessionState.ReadyToLaunch(
                LaunchMode.LOCAL_ONLY
            )
        )
        val viewModel = ShellViewModel(repository)

        advanceUntilIdle()

        assertEquals("Ready to launch", viewModel.uiState.value.sessionLabel)
        assertEquals("Local only", viewModel.uiState.value.statusLabel)
        assertEquals("Pause", viewModel.uiState.value.pauseButtonLabel)
        assertFalse(viewModel.uiState.value.isPauseEnabled)
    }

    @Test
    fun recoveringStateUsesRecoveryCopy() = runTest {
        val repository = FakeSessionRepository(
            SessionState.Recovering(
                launchMode = LaunchMode.FUJINET_ENABLED,
                reason = RuntimeRecoveryReason.RuntimeLost,
            )
        )
        val viewModel = ShellViewModel(repository)

        advanceUntilIdle()

        assertEquals("Recovering session", viewModel.uiState.value.sessionLabel)
        assertEquals("Recovering session", viewModel.uiState.value.statusLabel)
        assertEquals(
            "The previous runtime was lost while the app was in the background.",
            viewModel.uiState.value.detailLabel,
        )
        assertFalse(viewModel.uiState.value.isPauseEnabled)
    }

    @Test
    fun togglePauseDispatchesTogglePauseCommand() = runTest {
        val repository = FakeSessionRepository(SessionState.Idle)
        val viewModel = ShellViewModel(repository)

        viewModel.onPauseTogglePressed()

        assertEquals(listOf(SessionCommand.TogglePause), repository.dispatchedCommands)
    }

    @Test
    fun settingsVisibilityTogglesLocally() = runTest {
        val repository = FakeSessionRepository(
            SessionState.Running(
                sessionToken = 11,
                paused = false,
                surfaceAttached = true,
                launchMode = LaunchMode.FUJINET_ENABLED,
            )
        )
        val viewModel = ShellViewModel(repository)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settingsVisible)
        assertEquals(
            SessionState.Running(
                sessionToken = 11,
                paused = false,
                surfaceAttached = true,
                launchMode = LaunchMode.FUJINET_ENABLED,
            ),
            repository.state.value
        )

        viewModel.onSettingsPressed()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.settingsVisible)
        assertEquals(
            SessionState.Running(
                sessionToken = 11,
                paused = false,
                surfaceAttached = true,
                launchMode = LaunchMode.FUJINET_ENABLED,
            ),
            repository.state.value
        )

        viewModel.onSettingsDismissed()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settingsVisible)
        assertTrue(repository.dispatchedCommands.isEmpty())
    }
}

private class FakeSessionRepository(
    initialState: SessionState,
) : SessionRepository {
    private val mutableState = MutableStateFlow(initialState)
    val dispatchedCommands = mutableListOf<SessionCommand>()

    override val state: StateFlow<SessionState> = mutableState

    override fun dispatch(command: SessionCommand) {
        dispatchedCommands += command
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
