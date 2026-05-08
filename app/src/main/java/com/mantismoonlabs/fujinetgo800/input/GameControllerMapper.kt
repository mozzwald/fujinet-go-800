package com.mantismoonlabs.fujinetgo800.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.JoystickPort
import com.mantismoonlabs.fujinetgo800.settings.PortInputDevice
import com.mantismoonlabs.fujinetgo800.settings.hardwareControllerIdFor
import com.mantismoonlabs.fujinetgo800.settings.hardwareJoystickPort
import com.mantismoonlabs.fujinetgo800.settings.inputDeviceFor
import com.mantismoonlabs.fujinetgo800.session.SessionRepository
import com.mantismoonlabs.fujinetgo800.session.SessionState
import kotlin.math.abs

class GameControllerMapper(
    private val deadzone: Float = DEFAULT_DEADZONE,
) {
    private var xAxis = 0f
    private var yAxis = 0f
    private var firePressed = false

    fun handleMotionEvent(
        event: MotionEvent,
        sessionState: SessionState,
        settings: EmulatorSettings,
        sessionRepository: SessionRepository,
    ): Boolean {
        return handleMotion(
            motion = GameControllerMotion(
                source = event.source,
                controllerId = event.controllerId(),
                axisX = event.getAxisValue(MotionEvent.AXIS_X),
                axisY = event.getAxisValue(MotionEvent.AXIS_Y),
                hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
                hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y),
            ),
            sessionState = sessionState,
            settings = settings,
            onJoystickState = sessionRepository::setJoystickState,
        )
    }

    fun handleButtonEvent(
        event: KeyEvent,
        sessionState: SessionState,
        settings: EmulatorSettings,
        sessionRepository: SessionRepository,
    ): Boolean {
        return handleButton(
            event = GameControllerButtonEvent(
                source = event.source,
                controllerId = event.controllerId(),
                keyCode = event.keyCode,
                action = event.action,
            ),
            sessionState = sessionState,
            settings = settings,
            onJoystickState = sessionRepository::setJoystickState,
        )
    }

    internal fun handleMotion(
        motion: GameControllerMotion,
        sessionState: SessionState,
        settings: EmulatorSettings = EmulatorSettings(),
        onJoystickState: (Int, Float, Float, Boolean) -> Unit,
    ): Boolean {
        if (sessionState !is SessionState.Running || !motion.isFromController()) {
            return false
        }
        val port = settings.hardwareJoystickPortForController(motion.controllerId)?.index ?: return false

        xAxis = selectAxis(primary = motion.axisX, fallback = motion.hatX)
        yAxis = selectAxis(primary = motion.axisY, fallback = motion.hatY)
        onJoystickState(port, xAxis, yAxis, firePressed)
        return true
    }

    internal fun handleButton(
        event: GameControllerButtonEvent,
        sessionState: SessionState,
        settings: EmulatorSettings = EmulatorSettings(),
        onJoystickState: (Int, Float, Float, Boolean) -> Unit,
    ): Boolean {
        if (sessionState !is SessionState.Running || !event.isFromController()) {
            return false
        }
        val port = settings.hardwareJoystickPortForController(event.controllerId)?.index ?: return false
        if (event.keyCode != KeyEvent.KEYCODE_BUTTON_A) {
            return false
        }

        firePressed = when (event.action) {
            KeyEvent.ACTION_DOWN -> true
            KeyEvent.ACTION_UP -> false
            else -> return false
        }
        onJoystickState(port, xAxis, yAxis, firePressed)
        return true
    }

    private fun selectAxis(primary: Float, fallback: Float): Float {
        val normalizedPrimary = normalize(primary)
        if (normalizedPrimary != 0f) {
            return normalizedPrimary
        }
        return normalize(fallback)
    }

    private fun normalize(value: Float): Float {
        return if (abs(value) < deadzone) 0f else value.coerceIn(-1f, 1f)
    }

    private fun GameControllerMotion.isFromController(): Boolean {
        return source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK ||
            source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
    }

    private fun GameControllerButtonEvent.isFromController(): Boolean {
        return source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
    }

    private fun MotionEvent.controllerId(): String {
        return device?.descriptor?.takeIf { it.isNotBlank() } ?: "device:$deviceId"
    }

    private fun KeyEvent.controllerId(): String {
        return device?.descriptor?.takeIf { it.isNotBlank() } ?: "device:$deviceId"
    }

    private fun EmulatorSettings.hardwareJoystickPortForController(controllerId: String): JoystickPort? {
        val assignedPort = JoystickPort.entries.firstOrNull { port ->
            val device = inputDeviceFor(port)
            (device == PortInputDevice.BLUETOOTH_JOYSTICK || device == PortInputDevice.USB_JOYSTICK) &&
                hardwareControllerIdFor(port) == controllerId
        }
        if (assignedPort != null) {
            return assignedPort
        }
        val hasSpecificAssignments = JoystickPort.entries.any { port ->
            val device = inputDeviceFor(port)
            (device == PortInputDevice.BLUETOOTH_JOYSTICK || device == PortInputDevice.USB_JOYSTICK) &&
                hardwareControllerIdFor(port) != null
        }
        return if (hasSpecificAssignments) null else hardwareJoystickPort()
    }

    private companion object {
        const val DEFAULT_DEADZONE = 0.2f
    }
}

internal data class GameControllerMotion(
    val source: Int,
    val controllerId: String = "",
    val axisX: Float,
    val axisY: Float,
    val hatX: Float,
    val hatY: Float,
)

internal data class GameControllerButtonEvent(
    val source: Int,
    val controllerId: String = "",
    val keyCode: Int,
    val action: Int,
)
