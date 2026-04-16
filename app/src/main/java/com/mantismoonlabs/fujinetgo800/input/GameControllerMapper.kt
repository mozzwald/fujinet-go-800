package com.mantismoonlabs.fujinetgo800.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
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
        sessionRepository: SessionRepository,
    ): Boolean {
        return handleMotion(
            motion = GameControllerMotion(
                source = event.source,
                axisX = event.getAxisValue(MotionEvent.AXIS_X),
                axisY = event.getAxisValue(MotionEvent.AXIS_Y),
                hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
                hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y),
            ),
            sessionState = sessionState,
            onJoystickState = sessionRepository::setJoystickState,
        )
    }

    fun handleButtonEvent(
        event: KeyEvent,
        sessionState: SessionState,
        sessionRepository: SessionRepository,
    ): Boolean {
        return handleButton(
            event = GameControllerButtonEvent(
                source = event.source,
                keyCode = event.keyCode,
                action = event.action,
            ),
            sessionState = sessionState,
            onJoystickState = sessionRepository::setJoystickState,
        )
    }

    internal fun handleMotion(
        motion: GameControllerMotion,
        sessionState: SessionState,
        onJoystickState: (Int, Float, Float, Boolean) -> Unit,
    ): Boolean {
        if (sessionState !is SessionState.Running || !motion.isFromController()) {
            return false
        }

        xAxis = selectAxis(primary = motion.axisX, fallback = motion.hatX)
        yAxis = selectAxis(primary = motion.axisY, fallback = motion.hatY)
        onJoystickState(0, xAxis, yAxis, firePressed)
        return true
    }

    internal fun handleButton(
        event: GameControllerButtonEvent,
        sessionState: SessionState,
        onJoystickState: (Int, Float, Float, Boolean) -> Unit,
    ): Boolean {
        if (sessionState !is SessionState.Running || !event.isFromController()) {
            return false
        }
        if (event.keyCode != KeyEvent.KEYCODE_BUTTON_A) {
            return false
        }

        firePressed = when (event.action) {
            KeyEvent.ACTION_DOWN -> true
            KeyEvent.ACTION_UP -> false
            else -> return false
        }
        onJoystickState(0, xAxis, yAxis, firePressed)
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

    private companion object {
        const val DEFAULT_DEADZONE = 0.2f
    }
}

internal data class GameControllerMotion(
    val source: Int,
    val axisX: Float,
    val axisY: Float,
    val hatX: Float,
    val hatY: Float,
)

internal data class GameControllerButtonEvent(
    val source: Int,
    val keyCode: Int,
    val action: Int,
)
