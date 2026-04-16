package com.mantismoonlabs.fujinetgo800.session

import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.settings.VideoStandard
import org.junit.Assert.assertEquals
import org.junit.Test

class InputCommandRoutingTest {
    @Test
    fun keyAndJoystickCommandsRouteThroughTheService() {
        val harness = InputRoutingHarness()
        harness.coldStart()
        harness.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(launchMode = LaunchMode.LOCAL_ONLY),
                ),
            ),
        )

        harness.dispatch(SessionCommand.SetKeyState(aKeyCode = 123, pressed = true))
        harness.dispatch(
            SessionCommand.SetConsoleKeys(
                start = true,
                select = false,
                option = true,
            ),
        )
        harness.dispatch(
            SessionCommand.SetJoystickState(
                port = 0,
                x = 1f,
                y = -1f,
                fire = true,
            ),
        )

        assertEquals(listOf(123 to true), harness.keyStateCalls)
        assertEquals(listOf(Triple(true, false, true)), harness.consoleKeyCalls)
        assertEquals(listOf(JoystickStateCall(0, 1f, -1f, true)), harness.joystickStateCalls)
    }

    @Test
    fun inputCommandsAreIgnoredUntilSessionRunning() {
        val harness = InputRoutingHarness()
        harness.coldStart()

        harness.dispatch(SessionCommand.SetKeyState(aKeyCode = 55, pressed = true))
        harness.dispatch(
            SessionCommand.SetConsoleKeys(
                start = false,
                select = true,
                option = false,
            ),
        )
        harness.dispatch(
            SessionCommand.SetJoystickState(
                port = 0,
                x = 0.5f,
                y = 0.5f,
                fire = false,
            ),
        )

        assertEquals(emptyList<Pair<Int, Boolean>>(), harness.keyStateCalls)
        assertEquals(emptyList<Triple<Boolean, Boolean, Boolean>>(), harness.consoleKeyCalls)
        assertEquals(emptyList<JoystickStateCall>(), harness.joystickStateCalls)
    }
}

private data class JoystickStateCall(
    val port: Int,
    val x: Float,
    val y: Float,
    val fire: Boolean,
)

private class InputRoutingHarness {
    private val runtime = EmulatorSessionRuntime(
        ensureDirectories = {},
        startSession = {},
        getSessionToken = { 1L },
        pauseSession = {},
        resetSystem = { _ -> },
        warmResetSystem = {},
        attachSurface = { _, _, _ -> },
        detachSurface = {},
        mountDisk = { _, _ -> },
        ejectDisk = {},
        insertCartridge = {},
        removeCartridge = {},
        loadExecutable = {},
        applyCustomRom = {},
        clearCustomRom = {},
        applyBasicRom = {},
        clearBasicRom = {},
        applyAtari400800Rom = {},
        clearAtari400800Rom = {},
        setTurboEnabled = {},
        setVideoStandard = { _: VideoStandard -> },
        setKeyState = { aKeyCode, pressed -> keyStateCalls += aKeyCode to pressed },
        setConsoleKeys = { start, select, option -> consoleKeyCalls += Triple(start, select, option) },
        setJoystickState = { port, x, y, fire ->
            joystickStateCalls += JoystickStateCall(port, x, y, fire)
        },
        startAudio = {},
        resumeAudio = {},
        pauseAudio = {},
        setHostAudioMuted = {},
    )
    private val controller = EmulatorSessionController(runtime)

    val keyStateCalls = mutableListOf<Pair<Int, Boolean>>()
    val consoleKeyCalls = mutableListOf<Triple<Boolean, Boolean, Boolean>>()
    val joystickStateCalls = mutableListOf<JoystickStateCall>()

    fun coldStart(): SessionState = controller.onColdStart(LaunchMode.FUJINET_ENABLED)

    fun dispatch(command: SessionCommand): SessionState = controller.dispatch(command)
}
