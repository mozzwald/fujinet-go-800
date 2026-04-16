package com.mantismoonlabs.fujinetgo800.session

import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.settings.VideoStandard
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FujiNetSessionControllerTest {
    @Test
    fun fujiNetStartWaitsForNativeReadyBarrier() = runTest {
        val readyBarrier = CompletableDeferred<Unit>()
        val harness = FujiNetSessionHarness(
            ensureFujiNetReady = {
                readyBarrier.await()
            },
        )
        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)

        val start = async {
            harness.startSession(
                SessionLaunchConfig(
                    settings = EmulatorSettings(launchMode = LaunchMode.FUJINET_ENABLED),
                ),
            )
        }
        advanceUntilIdle()

        assertEquals(
            listOf(SessionState.StartingFujiNet(launchMode = LaunchMode.FUJINET_ENABLED)),
            harness.stateTransitions,
        )
        assertTrue(harness.startSessionCalls.isEmpty())

        readyBarrier.complete(Unit)
        advanceUntilIdle()

        assertEquals(
            SessionState.Running(
                sessionToken = 101L,
                paused = false,
                surfaceAttached = false,
                launchMode = LaunchMode.FUJINET_ENABLED,
            ),
            start.await(),
        )
    }

    @Test
    fun fujiNetFailurePublishesNonRecoverableState() = runTest {
        val harness = FujiNetSessionHarness(
            ensureFujiNetReady = {
                throw IOException("Timed out waiting for FujiNet readiness")
            },
        )
        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)

        val state = harness.startSession(
            SessionLaunchConfig(
                settings = EmulatorSettings(launchMode = LaunchMode.FUJINET_ENABLED),
            ),
        )

        assertEquals(
            SessionState.Failed(
                launchMode = LaunchMode.FUJINET_ENABLED,
                reason = FujiNetFailureReason.ReadinessTimeout,
                canRecoverLocally = false,
            ),
            state,
        )
        assertEquals(
            listOf(
                SessionState.StartingFujiNet(launchMode = LaunchMode.FUJINET_ENABLED),
                SessionState.Failed(
                    launchMode = LaunchMode.FUJINET_ENABLED,
                    reason = FujiNetFailureReason.ReadinessTimeout,
                    canRecoverLocally = false,
                ),
            ),
            harness.stateTransitions,
        )
        assertTrue(harness.startSessionCalls.isEmpty())
    }

    @Test
    fun recoverLocalOnlyIsIgnoredWhenFailureIsNotRecoverable() = runTest {
        val harness = FujiNetSessionHarness(
            ensureFujiNetReady = {
                throw IOException("FujiNet assets failed to stage")
            },
        )
        harness.coldStart(persistedLaunchMode = LaunchMode.FUJINET_ENABLED)
        harness.startSession(
            SessionLaunchConfig(
                settings = EmulatorSettings(launchMode = LaunchMode.FUJINET_ENABLED),
            ),
        )

        val recovered = harness.dispatch(SessionCommand.RecoverLocalOnly)

        assertEquals(
            SessionState.Failed(
                launchMode = LaunchMode.FUJINET_ENABLED,
                reason = FujiNetFailureReason.AssetInitializationFailed,
                canRecoverLocally = false,
                message = "FujiNet assets failed to stage",
            ),
            recovered,
        )
        assertTrue(harness.startSessionCalls.isEmpty())
        assertEquals(
            SessionState.ReadyToLaunch(launchMode = LaunchMode.FUJINET_ENABLED),
            harness.dispatch(SessionCommand.ReturnToLaunch),
        )
    }
}

private class FujiNetSessionHarness(
    ensureFujiNetReady: suspend () -> Unit,
) {
    private var nextToken = 101L
    val startSessionCalls = mutableListOf<SessionLaunchConfig>()
    val stateTransitions = mutableListOf<SessionState>()

    private val controller = EmulatorSessionController(
        EmulatorSessionRuntime(
            ensureDirectories = {},
            startSession = { config -> startSessionCalls += config },
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
            setTurboEnabled = {},
            setVideoStandard = { _: VideoStandard -> },
            setKeyState = { _, _ -> },
            setConsoleKeys = { _, _, _ -> },
            setJoystickState = { _, _, _, _ -> },
            startAudio = {},
            resumeAudio = {},
            pauseAudio = {},
            setHostAudioMuted = {},
            ensureFujiNetReady = ensureFujiNetReady,
        ),
    )

    fun coldStart(persistedLaunchMode: LaunchMode): SessionState =
        controller.onColdStart(persistedLaunchMode)

    suspend fun startSession(config: SessionLaunchConfig): SessionState =
        controller.startSession(SessionCommand.StartSession(config)) { state ->
            stateTransitions += state
        }

    fun dispatch(command: SessionCommand): SessionState =
        controller.dispatch(command)
}
