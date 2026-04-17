package com.mantismoonlabs.fujinetgo800.ui.input

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mantismoonlabs.fujinetgo800.input.AndroidAtariKeyMapper
import com.mantismoonlabs.fujinetgo800.input.AtariConsoleKey
import com.mantismoonlabs.fujinetgo800.input.AtariKeyCode
import com.mantismoonlabs.fujinetgo800.input.AtariKeyMapping
import com.mantismoonlabs.fujinetgo800.settings.ControlMode
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.JoystickInputStyle
import com.mantismoonlabs.fujinetgo800.session.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InputControlsUiState(
    val controlMode: ControlMode = ControlMode.KEYBOARD,
    val controlModeLabel: String = "Keyboard",
    val isKeyboardVisible: Boolean = true,
    val isInputPanelVisible: Boolean = true,
    val inputHideHintSeen: Boolean = false,
    val portraitInputPanelSizeFraction: Float = 1f,
    val joystickInputStyle: JoystickInputStyle = JoystickInputStyle.STICK_8_WAY,
    val joystickHapticsEnabled: Boolean = true,
    val keyboardHapticsEnabled: Boolean = true,
)

class InputControlsViewModel(
    private val settingsRepository: EmulatorSettingsRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val imeKeyMapper = AndroidAtariKeyMapper()
    val uiState: StateFlow<InputControlsUiState> = settingsRepository.settings
        .map { settings -> settings.toInputControlsUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, InputControlsUiState())

    private val pressedAtByMapping = mutableMapOf<AtariKeyMapping, Long>()
    private val pendingReleaseJobs = mutableMapOf<AtariKeyMapping, Job>()
    private val consoleKeysPressed = linkedSetOf<AtariConsoleKey>()
    private val joystickDispatcher = TouchJoystickDispatcher(
        onDispatch = { port, x, y, fire ->
            sessionRepository.setJoystickState(port = port, x = x, y = y, fire = fire)
        },
    )

    fun onControlModeSelected(controlMode: ControlMode) {
        if (controlMode != ControlMode.JOYSTICK) {
            joystickDispatcher.reset()
        }
        viewModelScope.launch {
            settingsRepository.updateControlMode(controlMode)
        }
    }

    fun toggleControlMode() {
        val nextMode = if (uiState.value.controlMode == ControlMode.KEYBOARD) {
            ControlMode.JOYSTICK
        } else {
            ControlMode.KEYBOARD
        }
        onControlModeSelected(nextMode)
    }

    fun hideInputPanel() {
        setInputPanelVisible(false)
    }

    fun showInputPanel() {
        setInputPanelVisible(true)
    }

    fun toggleInputPanelVisibility() {
        markInputHideHintSeen()
        setInputPanelVisible(!uiState.value.isInputPanelVisible)
    }

    fun markInputHideHintSeen() {
        if (uiState.value.inputHideHintSeen) {
            return
        }
        viewModelScope.launch {
            settingsRepository.updateInputHideHintSeen(true)
        }
    }

    fun setPortraitInputPanelSizeFraction(fraction: Float) {
        viewModelScope.launch {
            settingsRepository.updatePortraitInputPanelSizeFraction(fraction)
        }
    }

    fun resetPortraitInputPanelSize() {
        setPortraitInputPanelSizeFraction(1f)
    }

    fun onKeyPressed(mapping: AtariKeyMapping) {
        if (uiState.value.controlMode != ControlMode.KEYBOARD) {
            return
        }
        pendingReleaseJobs.remove(mapping)?.cancel()
        pressedAtByMapping[mapping] = SystemClock.elapsedRealtime()
        dispatchKeyMapping(mapping, pressed = true)
    }

    fun onKeyReleased(mapping: AtariKeyMapping) {
        if (uiState.value.controlMode != ControlMode.KEYBOARD) {
            return
        }
        val pressedAt = pressedAtByMapping.remove(mapping) ?: return
        val remainingHoldMs = (MINIMUM_KEY_HOLD_MS - (SystemClock.elapsedRealtime() - pressedAt))
            .coerceAtLeast(0L)

        pendingReleaseJobs[mapping] = viewModelScope.launch {
            if (remainingHoldMs > 0L) {
                delay(remainingHoldMs)
            }
            dispatchKeyMapping(mapping, pressed = false)
            pendingReleaseJobs.remove(mapping)
        }
    }

    fun onImeTextChanged(previousText: String, newText: String) {
        if (uiState.value.controlMode != ControlMode.KEYBOARD) {
            return
        }
        val prefixLength = previousText.commonPrefixWith(newText).length
        val deletedCharacters = (previousText.length - prefixLength).coerceAtLeast(0)
        repeat(deletedCharacters) {
            dispatchMomentaryKey(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BACKSPACE))
        }
        newText.substring(prefixLength).forEach { character ->
            imeKeyMapper.mapCharacter(character)?.let(::dispatchMomentaryKey)
        }
    }

    fun onImeBackspacePressed() {
        if (uiState.value.controlMode != ControlMode.KEYBOARD) {
            return
        }
        dispatchMomentaryKey(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BACKSPACE))
    }

    fun onImeEnterPressed() {
        if (uiState.value.controlMode != ControlMode.KEYBOARD) {
            return
        }
        dispatchMomentaryKey(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_RETURN))
    }

    fun onFunctionKeyPressed(mapping: AtariKeyMapping) {
        dispatchKeyMapping(mapping, pressed = true)
    }

    fun onFunctionKeyReleased(mapping: AtariKeyMapping) {
        dispatchKeyMapping(mapping, pressed = false)
    }

    fun onJoystickMoved(x: Float, y: Float) {
        if (uiState.value.controlMode != ControlMode.JOYSTICK) {
            return
        }
        joystickDispatcher.move(x, y)
    }

    fun onJoystickReleased() {
        if (uiState.value.controlMode != ControlMode.JOYSTICK) {
            return
        }
        joystickDispatcher.release()
    }

    fun onFirePressed() {
        if (uiState.value.controlMode != ControlMode.JOYSTICK) {
            return
        }
        joystickDispatcher.pressFire()
    }

    fun onFireReleased() {
        if (uiState.value.controlMode != ControlMode.JOYSTICK) {
            return
        }
        joystickDispatcher.releaseFire()
    }

    override fun onCleared() {
        pendingReleaseJobs.values.forEach(Job::cancel)
        pendingReleaseJobs.clear()
        consoleKeysPressed.clear()
        joystickDispatcher.reset()
        super.onCleared()
    }

    private fun dispatchKeyMapping(mapping: AtariKeyMapping, pressed: Boolean) {
        mapping.aKeyCode?.let { aKeyCode ->
            sessionRepository.setKeyState(aKeyCode = aKeyCode, pressed = pressed)
        }
        mapping.consoleKey?.let { consoleKey ->
            if (pressed) {
                consoleKeysPressed += consoleKey
            } else {
                consoleKeysPressed -= consoleKey
            }
            sessionRepository.setConsoleKeys(
                start = AtariConsoleKey.START in consoleKeysPressed,
                select = AtariConsoleKey.SELECT in consoleKeysPressed,
                option = AtariConsoleKey.OPTION in consoleKeysPressed,
            )
        }
    }

    private fun dispatchMomentaryKey(mapping: AtariKeyMapping) {
        pendingReleaseJobs.remove(mapping)?.cancel()
        dispatchKeyMapping(mapping, pressed = true)
        pendingReleaseJobs[mapping] = viewModelScope.launch {
            delay(MINIMUM_KEY_HOLD_MS)
            dispatchKeyMapping(mapping, pressed = false)
            pendingReleaseJobs.remove(mapping)
        }
    }

    private fun EmulatorSettings.toInputControlsUiState(): InputControlsUiState {
        return InputControlsUiState(
            controlMode = controlMode,
            controlModeLabel = controlMode.toLabel(),
            isKeyboardVisible = controlMode == ControlMode.KEYBOARD,
            isInputPanelVisible = inputPanelVisible,
            inputHideHintSeen = inputHideHintSeen,
            portraitInputPanelSizeFraction = portraitInputPanelSizeFraction,
            joystickInputStyle = joystickInputStyle,
            joystickHapticsEnabled = joystickHapticsEnabled,
            keyboardHapticsEnabled = keyboardHapticsEnabled,
        )
    }

    private fun setInputPanelVisible(visible: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateInputPanelVisible(visible)
        }
    }

    private fun ControlMode.toLabel(): String = when (this) {
        ControlMode.KEYBOARD -> "Keyboard"
        ControlMode.JOYSTICK -> "Joystick"
    }

    companion object {
        private const val MINIMUM_KEY_HOLD_MS = 75L

        fun provideFactory(
            settingsRepository: EmulatorSettingsRepository,
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                InputControlsViewModel(
                    settingsRepository = settingsRepository,
                    sessionRepository = sessionRepository,
                )
            }
        }
    }
}

internal class TouchJoystickDispatcher(
    private val onDispatch: (port: Int, x: Float, y: Float, fire: Boolean) -> Unit,
    private val port: Int = 0,
) {
    private var xAxis = 0f
    private var yAxis = 0f
    private var firePressed = false

    fun move(x: Float, y: Float) {
        xAxis = x.coerceIn(-1f, 1f)
        yAxis = y.coerceIn(-1f, 1f)
        dispatch()
    }

    fun release() {
        xAxis = 0f
        yAxis = 0f
        dispatch()
    }

    fun pressFire() {
        firePressed = true
        dispatch()
    }

    fun releaseFire() {
        firePressed = false
        dispatch()
    }

    fun reset() {
        xAxis = 0f
        yAxis = 0f
        firePressed = false
        dispatch()
    }

    private fun dispatch() {
        onDispatch(port, xAxis, yAxis, firePressed)
    }
}
