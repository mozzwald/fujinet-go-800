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
