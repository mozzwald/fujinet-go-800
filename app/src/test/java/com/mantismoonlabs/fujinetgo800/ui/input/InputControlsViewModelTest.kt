package com.mantismoonlabs.fujinetgo800.ui.input

import com.mantismoonlabs.fujinetgo800.input.AtariConsoleKey
import com.mantismoonlabs.fujinetgo800.input.AtariKeyCode
import com.mantismoonlabs.fujinetgo800.input.AtariKeyMapping
import com.mantismoonlabs.fujinetgo800.session.MainDispatcherRule
import com.mantismoonlabs.fujinetgo800.session.SessionCommand
import com.mantismoonlabs.fujinetgo800.session.SessionRepository
import com.mantismoonlabs.fujinetgo800.session.SessionState
import com.mantismoonlabs.fujinetgo800.settings.ControlMode
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class InputControlsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun functionKeyPressAndReleaseDispatchesHeldState() = runTest {
        val settingsRepository = createSettingsRepository(backgroundScope)
        val sessionRepository = FakeSessionRepository()
        val viewModel = InputControlsViewModel(settingsRepository, sessionRepository)

        advanceUntilIdle()
        viewModel.onFunctionKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_F1))
        viewModel.onFunctionKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_F1))
        advanceUntilIdle()

        assertEquals(
            listOf(
                SessionCommand.SetKeyState(AtariKeyCode.AKEY_F1, true),
                SessionCommand.SetKeyState(AtariKeyCode.AKEY_F1, false),
            ),
            sessionRepository.commands,
        )
    }

    @Test
    fun imeTextChangeDispatchesMappedKeysAndBackspace() = runTest {
        val settingsRepository = createSettingsRepository(backgroundScope)
        val sessionRepository = FakeSessionRepository()
        val viewModel = InputControlsViewModel(settingsRepository, sessionRepository)

        advanceUntilIdle()
        viewModel.onImeTextChanged("", "A")
        advanceTimeBy(75)
        advanceUntilIdle()
        viewModel.onImeTextChanged("A", "")
        advanceTimeBy(75)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SessionCommand.SetKeyState(AtariKeyCode.AKEY_A, true),
                SessionCommand.SetKeyState(AtariKeyCode.AKEY_A, false),
                SessionCommand.SetKeyState(AtariKeyCode.AKEY_BACKSPACE, true),
                SessionCommand.SetKeyState(AtariKeyCode.AKEY_BACKSPACE, false),
            ),
            sessionRepository.commands,
        )
    }

    @Test
    fun consoleFunctionKeyDispatchesHeldConsoleState() = runTest {
        val settingsRepository = createSettingsRepository(backgroundScope)
        val sessionRepository = FakeSessionRepository()
        val viewModel = InputControlsViewModel(settingsRepository, sessionRepository)

        advanceUntilIdle()
        viewModel.onFunctionKeyPressed(AtariKeyMapping(consoleKey = AtariConsoleKey.START))
        viewModel.onFunctionKeyReleased(AtariKeyMapping(consoleKey = AtariConsoleKey.START))
        advanceUntilIdle()

        assertEquals(
            listOf(
                SessionCommand.SetConsoleKeys(start = true, select = false, option = false),
                SessionCommand.SetConsoleKeys(start = false, select = false, option = false),
            ),
            sessionRepository.commands,
        )
    }

    @Test
    fun toggleInputPanelVisibilityPersistsWithoutChangingControlMode() = runTest {
        val settingsRepository = createSettingsRepository(backgroundScope)
        val sessionRepository = FakeSessionRepository()
        val viewModel = InputControlsViewModel(settingsRepository, sessionRepository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isKeyboardVisible)
        assertTrue(viewModel.uiState.value.isInputPanelVisible)

        viewModel.toggleInputPanelVisibility()
        val persistedSettings = settingsRepository.settings.first { !it.inputPanelVisible && it.inputHideHintSeen }

        assertTrue(viewModel.uiState.value.isKeyboardVisible)
        assertFalse(persistedSettings.inputPanelVisible)
        assertTrue(persistedSettings.inputHideHintSeen)
    }

    @Test
    fun markInputHideHintSeenPersistsFlag() = runTest {
        val settingsRepository = createSettingsRepository(backgroundScope)
        val sessionRepository = FakeSessionRepository()
        val viewModel = InputControlsViewModel(settingsRepository, sessionRepository)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.inputHideHintSeen)

        viewModel.markInputHideHintSeen()
        val persistedSettings = settingsRepository.settings.first { it.inputHideHintSeen }

        assertTrue(persistedSettings.inputHideHintSeen)
    }

    private fun createSettingsRepository(scope: CoroutineScope): EmulatorSettingsRepository {
        val file = Files.createTempFile("input-controls", ".preferences_pb").toFile()
        return EmulatorSettingsRepository.createForTest(
            produceFile = { file },
            scope = scope,
        )
    }
}

private class FakeSessionRepository : SessionRepository {
    private val mutableState = MutableStateFlow<SessionState>(
        SessionState.Running(
            sessionToken = 1L,
            paused = false,
            surfaceAttached = true,
            launchMode = LaunchMode.LOCAL_ONLY,
        )
    )
    val commands = mutableListOf<SessionCommand>()

    override val state: StateFlow<SessionState> = mutableState

    override fun dispatch(command: SessionCommand) {
        commands += command
    }
}
