package com.mantismoonlabs.fujinetgo800.session

import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.settings.VideoStandard
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EmulatorSessionServiceTest {
    @Test
    fun normalizePasteTextConvertsWebPunctuationToTypeableAscii() {
        assertEquals(
            "\"HELLO\" 'THERE' - OK... <= >= !=",
            "\u201CHELLO\u201D \u2018THERE\u2019 \u2014 OK\u2026 \u2264 \u2265 \u2260".normalizePasteText(),
        )
    }

    @Test
    fun normalizePasteTextNormalizesLineEndingsAndAccents() {
        assertEquals(
            "10 PRINT \"cafe\"\n20 GOTO 10\nRUN",
            "10 PRINT \u201Ccaf\u00E9\u201D\r\n20 GOTO 10\rRUN".normalizePasteText(),
        )
    }

    @Test
    fun coldStartPublishesReadyToLaunch() {
        val harness = SessionServiceHarness()

        val state = harness.coldStart(
            persistedLaunchMode = LaunchMode.LOCAL_ONLY,
        )

        assertEquals(SessionState.ReadyToLaunch(launchMode = LaunchMode.LOCAL_ONLY), state)
        assertEquals(0, harness.startSessionCalls.size)
    }

    @Test
    fun coldStartIgnoresOldRecoveryModel() {
        val harness = SessionServiceHarness()

        val state = harness.coldStart(
            persistedLaunchMode = LaunchMode.FUJINET_ENABLED,
        )

        assertEquals(
            SessionState.ReadyToLaunch(launchMode = LaunchMode.FUJINET_ENABLED),
            state,
        )
        assertEquals(0, harness.startSessionCalls.size)
    }

    @Test
    fun startSessionIsIdempotentWhileRunning() {
        val harness = SessionServiceHarness()
        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)

        val launchConfig = SessionLaunchConfig(
            settings = EmulatorSettings(launchMode = LaunchMode.LOCAL_ONLY),
        )

        val firstState = harness.dispatch(SessionCommand.StartSession(launchConfig))
        val secondState = harness.dispatch(SessionCommand.StartSession(launchConfig))

        assertEquals(
            SessionState.Running(
                sessionToken = 101L,
                paused = false,
                surfaceAttached = false,
                launchMode = LaunchMode.LOCAL_ONLY,
            ),
            firstState,
        )
        assertEquals(firstState, secondState)
        assertEquals(listOf(launchConfig), harness.startSessionCalls)
        assertEquals(1, harness.resetSystemCalls)
    }

    @Test
    fun hostLifecycleCommandsIgnoreReadyStateAndPreserveRunningSession() {
        val harness = SessionServiceHarness()

        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)
        assertEquals(
            SessionState.ReadyToLaunch(launchMode = LaunchMode.FUJINET_ENABLED),
            harness.dispatch(SessionCommand.HostStarted),
        )
        assertEquals(
            SessionState.ReadyToLaunch(launchMode = LaunchMode.FUJINET_ENABLED),
            harness.dispatch(SessionCommand.HostStopped),
        )

        val runningState = harness.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(launchMode = LaunchMode.LOCAL_ONLY),
                ),
            ),
        )

        assertEquals(
            SessionState.Running(
                sessionToken = 101L,
                paused = false,
                surfaceAttached = false,
                launchMode = LaunchMode.LOCAL_ONLY,
            ),
            runningState,
        )
        assertEquals(
            SessionState.Running(
                sessionToken = 101L,
                paused = false,
                surfaceAttached = false,
                launchMode = LaunchMode.LOCAL_ONLY,
            ),
            harness.dispatch(SessionCommand.HostStarted),
        )
        assertEquals(
            SessionState.Running(
                sessionToken = 101L,
                paused = false,
                surfaceAttached = false,
                launchMode = LaunchMode.LOCAL_ONLY,
            ),
            harness.dispatch(SessionCommand.HostStopped),
        )
    }

    @Test
    fun hostStartedNoLongerAttemptsRecovery() {
        val harness = SessionServiceHarness(
            getSessionToken = { serviceHarness ->
                serviceHarness.currentSessionToken ?: 0L
            },
            isSessionAlive = { serviceHarness, sessionToken ->
                serviceHarness.currentSessionToken == sessionToken
            },
        )
        harness.coldStart(persistedLaunchMode = LaunchMode.LOCAL_ONLY)
        harness.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(launchMode = LaunchMode.LOCAL_ONLY),
                ),
            ),
        )

        harness.dispatch(SessionCommand.HostStopped)
        harness.currentSessionToken = null
        harness.clearAudioTracking()

        val state = harness.dispatch(SessionCommand.HostStarted)

        assertEquals(
            SessionState.Running(
                launchMode = LaunchMode.LOCAL_ONLY,
                sessionToken = 101L,
                paused = false,
                surfaceAttached = false,
            ),
            state,
        )
        assertEquals(listOf(false), harness.hostMutedCalls)
    }

    @Test
    fun hostStartedDoesNotRestartFujiNetWhenUiReturns() = runTest {
        val harness = SessionServiceHarness(
            getSessionToken = { serviceHarness ->
                serviceHarness.currentSessionToken ?: 0L
            },
            isFujiNetHealthy = { false },
        )
        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)
        harness.startSession(
            SessionLaunchConfig(
                settings = EmulatorSettings(launchMode = LaunchMode.FUJINET_ENABLED),
            ),
        )

        harness.dispatch(SessionCommand.HostStopped)
        harness.currentSessionToken = null
        harness.clearAudioTracking()

        val state = harness.dispatch(SessionCommand.HostStarted)

        assertEquals(
            SessionState.Running(
                launchMode = LaunchMode.FUJINET_ENABLED,
                sessionToken = 101L,
                paused = false,
                surfaceAttached = false,
            ),
            state,
        )
        assertEquals(listOf(false), harness.hostMutedCalls)
    }

    @Test
    fun hostStoppedMutesAudioWhenBackgroundAudioDisabled() {
        val harness = SessionServiceHarness()
        harness.coldStart(persistedLaunchMode = LaunchMode.LOCAL_ONLY)
        harness.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(launchMode = LaunchMode.LOCAL_ONLY),
                ),
            ),
        )

        harness.clearAudioTracking()
        harness.dispatch(SessionCommand.HostStopped)

        assertEquals(listOf(true), harness.hostMutedCalls)
        assertEquals(0, harness.pauseAudioCalls)
        assertTrue(harness.resumeAudioCalled)
    }

    @Test
    fun hostStoppedKeepsAudioUnmutedWhenBackgroundAudioEnabled() {
        val harness = SessionServiceHarness()
        harness.coldStart(persistedLaunchMode = LaunchMode.LOCAL_ONLY)
        harness.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(
                        launchMode = LaunchMode.LOCAL_ONLY,
                        backgroundAudioEnabled = true,
                    ),
                ),
            ),
        )

        harness.clearAudioTracking()
        harness.dispatch(SessionCommand.HostStopped)

        assertEquals(listOf(false), harness.hostMutedCalls)
        assertEquals(0, harness.pauseAudioCalls)
        assertTrue(harness.resumeAudioCalled)
    }

    @Test
    fun endSessionReturnsToLaunchModeSelection() {
        val harness = SessionServiceHarness()
        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)

        harness.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(launchMode = LaunchMode.LOCAL_ONLY),
                ),
            ),
        )

        val state = harness.dispatch(SessionCommand.ReturnToLaunch)

        assertEquals(
            SessionState.ReadyToLaunch(launchMode = LaunchMode.LOCAL_ONLY),
            state,
        )
        assertEquals(listOf(true), harness.pauseSessionCalls)
        assertEquals(1, harness.pauseAudioCalls)
    }

    @Test
    fun videoStandardIsAppliedOnStartAndRuntimeUpdate() {
        val harness = SessionServiceHarness()
        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)

        harness.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(
                        launchMode = LaunchMode.LOCAL_ONLY,
                        videoStandard = VideoStandard.PAL,
                    ),
                ),
            ),
        )
        harness.dispatch(
            SessionCommand.ApplyRuntimeSettings(
                EmulatorSettings(videoStandard = VideoStandard.NTSC),
            ),
        )

        assertEquals(
            listOf(VideoStandard.PAL, VideoStandard.PAL, VideoStandard.NTSC),
            harness.videoStandardCalls,
        )
    }

    @Test
    fun fujiNetStartWaitsForReadinessBeforeRunning() = runTest {
        val readyGate = CompletableDeferred<Unit>()
        val harness = SessionServiceHarness(
            ensureFujiNetReady = { serviceHarness ->
                serviceHarness.harnessReadyCalls += 1
                readyGate.await()
            },
        )
        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)

        val launchConfig = SessionLaunchConfig(
            settings = EmulatorSettings(launchMode = LaunchMode.FUJINET_ENABLED),
        )

        val runningState = async { harness.startSession(launchConfig) }
        advanceUntilIdle()

        assertEquals(
            listOf(SessionState.StartingFujiNet(launchMode = LaunchMode.FUJINET_ENABLED)),
            harness.stateTransitions,
        )
        assertTrue(harness.startSessionCalls.isEmpty())

        readyGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(
            SessionState.Running(
                sessionToken = 101L,
                paused = false,
                surfaceAttached = false,
                launchMode = LaunchMode.FUJINET_ENABLED,
            ),
            runningState.await(),
        )
        assertEquals(1, harness.harnessReadyCalls)
    }

    @Test
    fun playbackRequestedDoesNotForceFocusMute() {
        val policy = ServiceAudioPolicy()

        assertEquals(
            ServiceAudioPolicy.Decision(
                playbackRequested = true,
                focusMuted = false,
                ducked = false,
            ),
            policy.onPlaybackRequestedChanged(true),
        )
    }

    @Test
    fun hostMuteDoesNotChangeServiceFocusDecision() {
        val policy = ServiceAudioPolicy()
        assertEquals(
            ServiceAudioPolicy.Decision(
                playbackRequested = true,
                focusMuted = false,
                ducked = false,
            ),
            policy.onPlaybackRequestedChanged(true),
        )
        assertEquals(
            ServiceAudioPolicy.Decision(
                playbackRequested = true,
                focusMuted = false,
                ducked = false,
            ),
            policy.onHostMutedChanged(true),
        )
    }

    @Test
    fun foregroundInteractionPreservesLocalOnlyPlaybackDecision() {
        val policy = ServiceAudioPolicy()
        assertEquals(
            ServiceAudioPolicy.Decision(
                playbackRequested = true,
                focusMuted = false,
                ducked = false,
            ),
            policy.onPlaybackRequestedChanged(true),
        )
        assertEquals(
            ServiceAudioPolicy.Decision(
                playbackRequested = true,
                focusMuted = false,
                ducked = false,
            ),
            policy.onForegroundInteraction(),
        )
    }
}

private class SessionServiceHarness {
    constructor(
        ensureFujiNetReady: suspend (SessionServiceHarness) -> Unit = { harness ->
            harness.harnessReadyCalls += 1
        },
        getSessionToken: (SessionServiceHarness) -> Long = { harness ->
            harness.nextToken++
        },
        isSessionAlive: (SessionServiceHarness, Long) -> Boolean = { _, token ->
            token != 0L
        },
        isFujiNetHealthy: suspend (SessionServiceHarness) -> Boolean = { true },
    ) {
        val ensureReady: suspend () -> Unit = {
            ensureFujiNetReady(this@SessionServiceHarness)
        }
        val healthCheck: suspend () -> Boolean = {
            isFujiNetHealthy(this@SessionServiceHarness)
        }
        runtime = EmulatorSessionRuntime(
            ensureDirectories = {},
            startSession = { config -> startSessionCalls += config },
            getSessionToken = { getSessionToken(this@SessionServiceHarness) },
            isSessionAlive = { sessionToken ->
                isSessionAlive(this@SessionServiceHarness, sessionToken)
            },
            pauseSession = { paused -> pauseSessionCalls += paused },
            resetSystem = { _ -> resetSystemCalls += 1 },
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
            setVideoStandard = { videoStandard -> videoStandardCalls += videoStandard },
            setKeyState = { _, _ -> },
            setConsoleKeys = { _, _, _ -> },
            setJoystickState = { _, _, _, _ -> },
            startAudio = {},
            resumeAudio = { resumeAudioCalled = true },
            pauseAudio = { pauseAudioCalls += 1 },
            setHostAudioMuted = { muted -> hostMutedCalls += muted },
            ensureFujiNetReady = ensureReady,
            isFujiNetHealthy = healthCheck,
        )
        controller = EmulatorSessionController(runtime)
    }

    var currentSessionToken: Long? = 101L
    var nextToken = 101L
    var harnessReadyCalls = 0

    val startSessionCalls = mutableListOf<SessionLaunchConfig>()
    var resetSystemCalls = 0
        private set
    val pauseSessionCalls = mutableListOf<Boolean>()
    val videoStandardCalls = mutableListOf<VideoStandard>()
    val stateTransitions = mutableListOf<SessionState>()
    var pauseAudioCalls = 0
        private set
    var resumeAudioCalled = false
    val hostMutedCalls = mutableListOf<Boolean>()
    private val runtime: EmulatorSessionRuntime
    private val controller: EmulatorSessionController

    fun coldStart(
        persistedLaunchMode: LaunchMode,
    ): SessionState = controller.onColdStart(
        persistedLaunchMode = persistedLaunchMode,
    )

    fun dispatch(command: SessionCommand): SessionState =
        controller.dispatch(command)

    suspend fun startSession(config: SessionLaunchConfig): SessionState =
        controller.startSession(SessionCommand.StartSession(config)) { state ->
            stateTransitions += state
        }

    fun clearAudioTracking() {
        pauseAudioCalls = 0
        resumeAudioCalled = false
        hostMutedCalls.clear()
    }
}
