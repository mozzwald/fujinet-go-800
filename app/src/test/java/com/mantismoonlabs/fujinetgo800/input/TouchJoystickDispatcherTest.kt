package com.mantismoonlabs.fujinetgo800.input

import com.mantismoonlabs.fujinetgo800.ui.input.TouchJoystickDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test

class TouchJoystickDispatcherTest {
    @Test
    fun releaseRecentersJoystick() {
        val calls = mutableListOf<JoystickDispatchCall>()
        val dispatcher = TouchJoystickDispatcher(onDispatch = { port, x, y, fire ->
            calls += JoystickDispatchCall(port, x, y, fire)
        })

        dispatcher.move(0.75f, -0.5f)
        dispatcher.release()

        assertEquals(
            listOf(
                JoystickDispatchCall(port = 0, x = 0.75f, y = -0.5f, fire = false),
                JoystickDispatchCall(port = 0, x = 0f, y = 0f, fire = false),
            ),
            calls,
        )
    }

    @Test
    fun fireButtonDispatchesPressedAndReleased() {
        val calls = mutableListOf<JoystickDispatchCall>()
        val dispatcher = TouchJoystickDispatcher(onDispatch = { port, x, y, fire ->
            calls += JoystickDispatchCall(port, x, y, fire)
        })

        dispatcher.pressFire()
        dispatcher.releaseFire()

        assertEquals(
            listOf(
                JoystickDispatchCall(port = 0, x = 0f, y = 0f, fire = true),
                JoystickDispatchCall(port = 0, x = 0f, y = 0f, fire = false),
            ),
            calls,
        )
    }

    @Test
    fun changingPortsNeutralizesPreviousPortBeforeDispatchingNewPort() {
        val calls = mutableListOf<JoystickDispatchCall>()
        var selectedPort = 0
        val dispatcher = TouchJoystickDispatcher(
            portProvider = { selectedPort },
            onDispatch = { port, x, y, fire ->
                calls += JoystickDispatchCall(port, x, y, fire)
            },
        )

        dispatcher.move(1f, 0f)
        selectedPort = 1
        dispatcher.pressFire()

        assertEquals(
            listOf(
                JoystickDispatchCall(port = 0, x = 1f, y = 0f, fire = false),
                JoystickDispatchCall(port = 0, x = 0f, y = 0f, fire = false),
                JoystickDispatchCall(port = 1, x = 1f, y = 0f, fire = true),
            ),
            calls,
        )
    }
}

private data class JoystickDispatchCall(
    val port: Int,
    val x: Float,
    val y: Float,
    val fire: Boolean,
)
