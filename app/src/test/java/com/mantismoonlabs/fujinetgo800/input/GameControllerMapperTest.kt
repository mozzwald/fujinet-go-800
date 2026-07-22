package com.mantismoonlabs.fujinetgo800.input

import android.view.InputDevice
import android.view.KeyEvent
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.JoystickPort
import com.mantismoonlabs.fujinetgo800.settings.PortInputDevice
import com.mantismoonlabs.fujinetgo800.settings.withHardwareControllerFor
import com.mantismoonlabs.fujinetgo800.session.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameControllerMapperTest {
    private val mapper = GameControllerMapper()

    @Test
    fun leftStickMapsToJoystickAxes() {
        val calls = mutableListOf<ControllerDispatchCall>()

        val handled = mapper.handleMotion(
            motion = GameControllerMotion(
                source = InputDevice.SOURCE_JOYSTICK,
                axisX = 0.8f,
                axisY = -0.4f,
                hatX = 0f,
                hatY = 0f,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { port, x, y, fire -> calls += ControllerDispatchCall(port, x, y, fire) },
        )

        assertTrue(handled)
        assertEquals(listOf(ControllerDispatchCall(0, 0.8f, -0.4f, false)), calls)
    }

    @Test
    fun dpadSourceMapsToJoystickAxes() {
        val calls = mutableListOf<ControllerDispatchCall>()

        val handled = mapper.handleMotion(
            motion = GameControllerMotion(
                source = InputDevice.SOURCE_DPAD,
                axisX = 0f,
                axisY = 0f,
                hatX = -1f,
                hatY = 1f,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { port, x, y, fire -> calls += ControllerDispatchCall(port, x, y, fire) },
        )

        assertTrue(handled)
        assertEquals(listOf(ControllerDispatchCall(0, -1f, 1f, false)), calls)
    }

    @Test
    fun virtualDpadSourceIsNotTreatedAsExternalControllerMotion() {
        val calls = mutableListOf<ControllerDispatchCall>()

        val handled = mapper.handleMotion(
            motion = GameControllerMotion(
                source = InputDevice.SOURCE_DPAD,
                isVirtualDevice = true,
                axisX = 0f,
                axisY = 0f,
                hatX = -1f,
                hatY = 1f,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { port, x, y, fire -> calls += ControllerDispatchCall(port, x, y, fire) },
        )

        assertEquals(false, handled)
        assertEquals(emptyList<ControllerDispatchCall>(), calls)
    }

    @Test
    fun primaryButtonMapsToFire() {
        val calls = mutableListOf<ControllerDispatchCall>()

        mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { port, x, y, fire -> calls += ControllerDispatchCall(port, x, y, fire) },
        )
        mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                action = KeyEvent.ACTION_UP,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { port, x, y, fire -> calls += ControllerDispatchCall(port, x, y, fire) },
        )

        assertEquals(
            listOf(
                ControllerDispatchCall(0, 0f, 0f, true),
                ControllerDispatchCall(0, 0f, 0f, false),
            ),
            calls,
        )
    }

    @Test
    fun primaryButtonDoesNotMapToFireWhenControllerIsNotAssignedToHardwarePort() {
        val calls = mutableListOf<ControllerDispatchCall>()

        val handled = mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                controllerId = "unassigned-pad",
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = EmulatorSettings(),
            onJoystickState = { port, x, y, fire -> calls += ControllerDispatchCall(port, x, y, fire) },
        )

        assertEquals(false, handled)
        assertEquals(emptyList<ControllerDispatchCall>(), calls)
    }

    @Test
    fun dpadSourcePrimaryButtonMapsToFire() {
        val calls = mutableListOf<ControllerDispatchCall>()

        val handled = mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_DPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { port, x, y, fire -> calls += ControllerDispatchCall(port, x, y, fire) },
        )

        assertTrue(handled)
        assertEquals(listOf(ControllerDispatchCall(0, 0f, 0f, true)), calls)
    }

    @Test
    fun secondaryFaceButtonsMapToAtariKeysWhenControllerIsAssignedToHardwarePort() {
        val keyCalls = mutableListOf<Pair<Int, Boolean>>()

        val bHandled = mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_B,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { _, _, _, _ -> },
            onKeyState = { aKeyCode, pressed -> keyCalls += aKeyCode to pressed },
        )
        val xHandled = mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_X,
                action = KeyEvent.ACTION_UP,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { _, _, _, _ -> },
            onKeyState = { aKeyCode, pressed -> keyCalls += aKeyCode to pressed },
        )

        assertTrue(bHandled)
        assertTrue(xHandled)
        assertEquals(
            listOf(
                AtariKeyCode.AKEY_ESCAPE to true,
                AtariKeyCode.AKEY_RETURN to false,
            ),
            keyCalls,
        )
    }

    @Test
    fun controllerConsoleButtonsMapToAtariConsoleKeysWhenControllerIsAssignedToHardwarePort() {
        val consoleCalls = mutableListOf<Triple<Boolean, Boolean, Boolean>>()

        mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_Y,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { _, _, _, _ -> },
            onConsoleKeys = { start, select, option -> consoleCalls += Triple(start, select, option) },
        )
        mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_START,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { _, _, _, _ -> },
            onConsoleKeys = { start, select, option -> consoleCalls += Triple(start, select, option) },
        )
        mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_SELECT,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { _, _, _, _ -> },
            onConsoleKeys = { start, select, option -> consoleCalls += Triple(start, select, option) },
        )
        mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_Y,
                action = KeyEvent.ACTION_UP,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { _, _, _, _ -> },
            onConsoleKeys = { start, select, option -> consoleCalls += Triple(start, select, option) },
        )

        assertEquals(
            listOf(
                Triple(false, false, true),
                Triple(true, false, true),
                Triple(true, true, true),
                Triple(true, true, false),
            ),
            consoleCalls,
        )
    }

    @Test
    fun unmappedControllerButtonsAreConsumedWithoutAffectingEmulationWhenControllerIsAssigned() {
        val joystickCalls = mutableListOf<ControllerDispatchCall>()
        val keyCalls = mutableListOf<Pair<Int, Boolean>>()
        val consoleCalls = mutableListOf<Triple<Boolean, Boolean, Boolean>>()

        val handled = mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_L1,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { port, x, y, fire -> joystickCalls += ControllerDispatchCall(port, x, y, fire) },
            onKeyState = { aKeyCode, pressed -> keyCalls += aKeyCode to pressed },
            onConsoleKeys = { start, select, option -> consoleCalls += Triple(start, select, option) },
        )

        assertTrue(handled)
        assertEquals(emptyList<ControllerDispatchCall>(), joystickCalls)
        assertEquals(emptyList<Pair<Int, Boolean>>(), keyCalls)
        assertEquals(emptyList<Triple<Boolean, Boolean, Boolean>>(), consoleCalls)
    }

    @Test
    fun buttonModeIsNeverMapped() {
        val handled = mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                keyCode = KeyEvent.KEYCODE_BUTTON_MODE,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { _, _, _, _ -> },
        )

        assertEquals(false, handled)
    }

    @Test
    fun extraControllerButtonsDoNotMapWhenControllerIsNotAssignedToHardwarePort() {
        val keyCalls = mutableListOf<Pair<Int, Boolean>>()
        val consoleCalls = mutableListOf<Triple<Boolean, Boolean, Boolean>>()

        val handled = mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_GAMEPAD,
                controllerId = "unassigned-pad",
                keyCode = KeyEvent.KEYCODE_BUTTON_B,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = EmulatorSettings(),
            onJoystickState = { _, _, _, _ -> },
            onKeyState = { aKeyCode, pressed -> keyCalls += aKeyCode to pressed },
            onConsoleKeys = { start, select, option -> consoleCalls += Triple(start, select, option) },
        )

        assertEquals(false, handled)
        assertEquals(emptyList<Pair<Int, Boolean>>(), keyCalls)
        assertEquals(emptyList<Triple<Boolean, Boolean, Boolean>>(), consoleCalls)
    }

    @Test
    fun virtualDpadSourceIsNotTreatedAsExternalControllerButton() {
        val calls = mutableListOf<ControllerDispatchCall>()

        val handled = mapper.handleButton(
            event = GameControllerButtonEvent(
                source = InputDevice.SOURCE_DPAD,
                isVirtualDevice = true,
                keyCode = KeyEvent.KEYCODE_BUTTON_A,
                action = KeyEvent.ACTION_DOWN,
            ),
            sessionState = runningSession(),
            settings = hardwareJoystickSettings(),
            onJoystickState = { port, x, y, fire -> calls += ControllerDispatchCall(port, x, y, fire) },
        )

        assertEquals(false, handled)
        assertEquals(emptyList<ControllerDispatchCall>(), calls)
    }

    @Test
    fun assignedControllersRouteToTheirPorts() {
        val calls = mutableListOf<ControllerDispatchCall>()
        val settings = EmulatorSettings()
            .withHardwareControllerFor(JoystickPort.PORT_2, PortInputDevice.BLUETOOTH_JOYSTICK, "pad-a", "Pad A")
            .withHardwareControllerFor(JoystickPort.PORT_3, PortInputDevice.USB_JOYSTICK, "pad-b", "Pad B")

        mapper.handleMotion(
            motion = GameControllerMotion(
                source = InputDevice.SOURCE_JOYSTICK,
                controllerId = "pad-b",
                axisX = 0.5f,
                axisY = 0.25f,
                hatX = 0f,
                hatY = 0f,
            ),
            sessionState = runningSession(),
            settings = settings,
            onJoystickState = { port, x, y, fire -> calls += ControllerDispatchCall(port, x, y, fire) },
        )

        assertEquals(listOf(ControllerDispatchCall(2, 0.5f, 0.25f, false)), calls)
    }

    private fun runningSession(): SessionState.Running {
        return SessionState.Running(
            sessionToken = 1L,
            paused = false,
            surfaceAttached = true,
            launchMode = com.mantismoonlabs.fujinetgo800.settings.LaunchMode.LOCAL_ONLY,
        )
    }

    private fun hardwareJoystickSettings(): EmulatorSettings {
        return EmulatorSettings(port1InputDevice = PortInputDevice.BLUETOOTH_JOYSTICK)
    }
}

private data class ControllerDispatchCall(
    val port: Int,
    val x: Float,
    val y: Float,
    val fire: Boolean,
)
