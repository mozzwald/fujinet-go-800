package com.mantismoonlabs.fujinetgo800.session

import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.settings.NtscFilterSettings
import com.mantismoonlabs.fujinetgo800.settings.VideoStandard
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeSettingsRoutingTest {
    @Test
    fun turboSettingRoutesThroughTheService() {
        val harness = RuntimeSettingsHarness()
        val launchConfig = SessionLaunchConfig(
            settings = EmulatorSettings(
                launchMode = LaunchMode.LOCAL_ONLY,
                turboEnabled = true,
            ),
        )

        harness.coldStart(persistedLaunchMode = launchConfig.settings.launchMode)

        harness.dispatch(SessionCommand.StartSession(launchConfig))

        assertEquals(listOf(true), harness.turboEnabledCalls)
    }

    @Test
    fun runningSessionReceivesUpdatedTurboSetting() {
        val harness = RuntimeSettingsHarness()

        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)
        harness.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(launchMode = LaunchMode.LOCAL_ONLY),
                ),
            ),
        )
        harness.dispatch(
            SessionCommand.ApplyRuntimeSettings(
                EmulatorSettings(
                    launchMode = LaunchMode.LOCAL_ONLY,
                    turboEnabled = true,
                ),
            ),
        )
        harness.dispatch(
            SessionCommand.ApplyRuntimeSettings(
                EmulatorSettings(
                    launchMode = LaunchMode.LOCAL_ONLY,
                    turboEnabled = false,
                ),
            ),
        )

        assertEquals(listOf(false, true, false), harness.turboEnabledCalls)
    }

    @Test
    fun runningSessionReceivesUpdatedVideoStandard() {
        val harness = RuntimeSettingsHarness()

        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)
        harness.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(launchMode = LaunchMode.LOCAL_ONLY),
                ),
            ),
        )
        harness.dispatch(
            SessionCommand.ApplyRuntimeSettings(
                EmulatorSettings(videoStandard = VideoStandard.PAL),
            ),
        )
        harness.dispatch(
            SessionCommand.ApplyRuntimeSettings(
                EmulatorSettings(videoStandard = VideoStandard.NTSC),
            ),
        )

        assertEquals(
            listOf(VideoStandard.NTSC, VideoStandard.NTSC, VideoStandard.PAL, VideoStandard.NTSC),
            harness.videoStandardCalls,
        )
    }

    @Test
    fun runningSessionReceivesUpdatedNtscFilterConfig() {
        val harness = RuntimeSettingsHarness()

        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)
        harness.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(launchMode = LaunchMode.LOCAL_ONLY),
                ),
            ),
        )
        harness.dispatch(
            SessionCommand.ApplyRuntimeSettings(
                EmulatorSettings(
                    ntscFilter = NtscFilterSettings(sharpness = 0.5f),
                ),
            ),
        )

        assertEquals(
            listOf(
                NtscFilterSettings(),
                NtscFilterSettings(),
                NtscFilterSettings(sharpness = 0.5f),
            ),
            harness.ntscFilterSettingsCalls,
        )
    }
}

private class RuntimeSettingsHarness {
    private var nextToken = 200L

    val turboEnabledCalls = mutableListOf<Boolean>()
    val videoStandardCalls = mutableListOf<VideoStandard>()
    val ntscFilterSettingsCalls = mutableListOf<NtscFilterSettings>()
    private val runtime = EmulatorSessionRuntime(
        ensureDirectories = {},
        startSession = {},
        getSessionToken = { nextToken++ },
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
        setTurboEnabled = { enabled -> turboEnabledCalls += enabled },
        setVideoStandard = { videoStandard -> videoStandardCalls += videoStandard },
        setKeyState = { _, _ -> },
        setNtscFilterConfig = { settings -> ntscFilterSettingsCalls += settings },
        setConsoleKeys = { _, _, _ -> },
        setJoystickState = { _, _, _, _ -> },
        startAudio = {},
        resumeAudio = {},
        pauseAudio = {},
        setHostAudioMuted = {},
    )
    private val controller = EmulatorSessionController(runtime)

    fun coldStart(persistedLaunchMode: LaunchMode): SessionState =
        controller.onColdStart(persistedLaunchMode)

    fun dispatch(command: SessionCommand): SessionState =
        controller.dispatch(command)
}
