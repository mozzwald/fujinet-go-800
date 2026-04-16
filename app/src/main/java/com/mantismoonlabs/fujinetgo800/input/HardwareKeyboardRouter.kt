package com.mantismoonlabs.fujinetgo800.input

import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.mantismoonlabs.fujinetgo800.session.SessionRepository
import com.mantismoonlabs.fujinetgo800.session.SessionState

class HardwareKeyboardRouter(
    private val keyMapper: AndroidAtariKeyMapper,
) {
    private val consoleKeysPressed = linkedSetOf<AtariConsoleKey>()

    fun route(event: KeyEvent, sessionState: SessionState, sessionRepository: SessionRepository): Boolean {
        return route(
            event = HardwareKeyEvent(
                keyCode = event.keyCode,
                action = event.action,
                source = event.source,
                deviceId = event.deviceId,
                shiftPressed = event.isShiftPressed,
                ctrlPressed = event.isCtrlPressed,
            ),
            sessionState = sessionState,
            onKeyState = sessionRepository::setKeyState,
            onConsoleKeys = sessionRepository::setConsoleKeys,
        )
    }

    internal fun route(
        event: HardwareKeyEvent,
        sessionState: SessionState,
        onKeyState: (Int, Boolean) -> Unit,
        onConsoleKeys: (Boolean, Boolean, Boolean) -> Unit,
    ): Boolean {
        if (sessionState !is SessionState.Running || !event.isPhysicalKeyboard()) {
            return false
        }

        val mapping = keyMapper.mapKeyEvent(
            AndroidKeyInput(
                keyCode = event.keyCode,
                shiftPressed = event.shiftPressed,
                ctrlPressed = event.ctrlPressed,
            ),
        ) ?: return false

        val pressed = when (event.action) {
            KeyEvent.ACTION_DOWN -> true
            KeyEvent.ACTION_UP -> false
            else -> return false
        }

        mapping.aKeyCode?.let { aKeyCode ->
            onKeyState(aKeyCode, pressed)
        }
        mapping.consoleKey?.let { consoleKey ->
            if (pressed) {
                consoleKeysPressed += consoleKey
            } else {
                consoleKeysPressed -= consoleKey
            }
            onConsoleKeys(
                AtariConsoleKey.START in consoleKeysPressed,
                AtariConsoleKey.SELECT in consoleKeysPressed,
                AtariConsoleKey.OPTION in consoleKeysPressed,
            )
        }
        return true
    }

    private fun HardwareKeyEvent.isPhysicalKeyboard(): Boolean {
        if (deviceId == KeyCharacterMap.VIRTUAL_KEYBOARD) {
            return false
        }
        return source and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD
    }
}

internal data class HardwareKeyEvent(
    val keyCode: Int,
    val action: Int,
    val source: Int,
    val deviceId: Int,
    val shiftPressed: Boolean = false,
    val ctrlPressed: Boolean = false,
)
