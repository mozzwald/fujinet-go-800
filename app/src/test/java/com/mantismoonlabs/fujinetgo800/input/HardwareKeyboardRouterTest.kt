package com.mantismoonlabs.fujinetgo800.input

import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.mantismoonlabs.fujinetgo800.session.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareKeyboardRouterTest {
    private val router = HardwareKeyboardRouter(AndroidAtariKeyMapper())

    @Test
    fun filtersNonPhysicalKeys() {
        val handled = router.route(
            event = HardwareKeyEvent(
                keyCode = KeyEvent.KEYCODE_A,
                action = KeyEvent.ACTION_DOWN,
                source = InputDevice.SOURCE_TOUCHSCREEN,
                deviceId = KeyCharacterMap.VIRTUAL_KEYBOARD,
            ),
            sessionState = runningSession(),
            onKeyState = { _, _ -> },
            onConsoleKeys = { _, _, _ -> },
        )

        assertFalse(handled)
    }

    @Test
    fun routesKeyDownAndKeyUpWhenRunning() {
        val keyCalls = mutableListOf<Pair<Int, Boolean>>()

        val downHandled = router.route(
            event = HardwareKeyEvent(
                keyCode = KeyEvent.KEYCODE_A,
                action = KeyEvent.ACTION_DOWN,
                source = InputDevice.SOURCE_KEYBOARD,
                deviceId = 7,
            ),
            sessionState = runningSession(),
            onKeyState = { aKeyCode, pressed -> keyCalls += aKeyCode to pressed },
            onConsoleKeys = { _, _, _ -> },
        )
        val upHandled = router.route(
            event = HardwareKeyEvent(
                keyCode = KeyEvent.KEYCODE_A,
                action = KeyEvent.ACTION_UP,
                source = InputDevice.SOURCE_KEYBOARD,
                deviceId = 7,
            ),
            sessionState = runningSession(),
            onKeyState = { aKeyCode, pressed -> keyCalls += aKeyCode to pressed },
            onConsoleKeys = { _, _, _ -> },
        )

        assertTrue(downHandled)
        assertTrue(upHandled)
        assertEquals(
            listOf(
                AtariKeyCode.AKEY_a to true,
                AtariKeyCode.AKEY_a to false,
            ),
            keyCalls,
        )
    }

    @Test
    fun ignoresKeysWhenSessionNotRunning() {
        val keyCalls = mutableListOf<Pair<Int, Boolean>>()

        val handled = router.route(
            event = HardwareKeyEvent(
                keyCode = KeyEvent.KEYCODE_A,
                action = KeyEvent.ACTION_DOWN,
                source = InputDevice.SOURCE_KEYBOARD,
                deviceId = 3,
            ),
            sessionState = SessionState.ReadyToLaunch(launchMode = com.mantismoonlabs.fujinetgo800.settings.LaunchMode.LOCAL_ONLY),
            onKeyState = { aKeyCode, pressed -> keyCalls += aKeyCode to pressed },
            onConsoleKeys = { _, _, _ -> },
        )

        assertFalse(handled)
        assertTrue(keyCalls.isEmpty())
    }

    private fun runningSession(): SessionState.Running {
        return SessionState.Running(
            sessionToken = 1L,
            paused = false,
            surfaceAttached = true,
            launchMode = com.mantismoonlabs.fujinetgo800.settings.LaunchMode.LOCAL_ONLY,
        )
    }
}
