package com.mantismoonlabs.fujinetgo800.session

import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer

interface SessionRepository {
    val state: StateFlow<SessionState>

    fun dispatch(command: SessionCommand)

    fun copyLatestFrame(target: ByteBuffer): Boolean = false

    fun refreshNotification() = Unit

    fun setKeyState(aKeyCode: Int, pressed: Boolean) {
        dispatch(SessionCommand.SetKeyState(aKeyCode = aKeyCode, pressed = pressed))
    }

    fun pasteText(text: String) {
        dispatch(SessionCommand.PasteText(text))
    }

    fun setConsoleKeys(start: Boolean, select: Boolean, option: Boolean) {
        dispatch(
            SessionCommand.SetConsoleKeys(
                start = start,
                select = select,
                option = option,
            ),
        )
    }

    fun setJoystickState(port: Int, x: Float, y: Float, fire: Boolean) {
        dispatch(
            SessionCommand.SetJoystickState(
                port = port,
                x = x,
                y = y,
                fire = fire,
            ),
        )
    }
}

class ServiceBackedSessionRepository(
    private val service: EmulatorSessionService,
) : SessionRepository {
    override val state: StateFlow<SessionState>
        get() = service.state

    override fun dispatch(command: SessionCommand) {
        service.dispatch(command)
    }

    override fun copyLatestFrame(target: ByteBuffer): Boolean = service.copyLatestFrame(target)

    override fun refreshNotification() {
        service.refreshNotification()
    }
}
