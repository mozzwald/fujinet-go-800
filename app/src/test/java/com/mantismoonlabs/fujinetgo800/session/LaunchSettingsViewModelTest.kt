package com.mantismoonlabs.fujinetgo800.session

import com.mantismoonlabs.fujinetgo800.settings.AtariMachineType
import com.mantismoonlabs.fujinetgo800.settings.ArtifactingMode
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.settings.MemoryProfile
import com.mantismoonlabs.fujinetgo800.settings.NtscFilterPreset
import com.mantismoonlabs.fujinetgo800.settings.NtscFilterSettings
import com.mantismoonlabs.fujinetgo800.settings.OrientationMode
import com.mantismoonlabs.fujinetgo800.settings.ScaleMode
import com.mantismoonlabs.fujinetgo800.settings.SioPatchMode
import com.mantismoonlabs.fujinetgo800.settings.SystemRomKind
import com.mantismoonlabs.fujinetgo800.settings.VideoStandard
import com.mantismoonlabs.fujinetgo800.storage.RuntimePaths
import com.mantismoonlabs.fujinetgo800.storage.SystemRomDocumentStore
import com.mantismoonlabs.fujinetgo800.storage.SystemRomSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun betaBuildAlwaysShowsFujiNetLaunchLabel() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "labels.preferences_pb",
        )
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = RecordingSessionRepository(),
        )

        advanceUntilIdle()
        assertEquals("FujiNet enabled", viewModel.uiState.value.launchModeLabel)

        settingsRepository.updateLaunchMode(LaunchMode.LOCAL_ONLY)

        advanceUntilIdle()
        assertEquals("FujiNet enabled", viewModel.uiState.value.launchModeLabel)
    }

    @Test
    fun selectingLocalOnlyKeepsFujiNetLaunchModeWithoutStarting() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "local-only.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository()
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )

        viewModel.onLaunchModeSelected(LaunchMode.LOCAL_ONLY)

        advanceUntilIdle()

        assertEquals(LaunchMode.FUJINET_ENABLED, viewModel.uiState.value.settings.launchMode)
        assertTrue(sessionRepository.dispatchedCommands.isEmpty())
    }

    @Test
    fun startRequestedDispatchesLatestPersistedLaunchConfig() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "start-request.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository()
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )
        val expectedSettings = EmulatorSettings(
            launchMode = LaunchMode.FUJINET_ENABLED,
            scaleMode = ScaleMode.FILL,
            keepScreenOn = false,
            orientationMode = OrientationMode.LANDSCAPE,
            turboEnabled = true,
            videoStandard = VideoStandard.PAL,
        )

        viewModel.onLaunchModeSelected(expectedSettings.launchMode)
        viewModel.onScaleModeSelected(expectedSettings.scaleMode)
        viewModel.onKeepScreenOnChanged(expectedSettings.keepScreenOn)
        viewModel.onOrientationModeSelected(expectedSettings.orientationMode)
        viewModel.onTurboModeChanged(expectedSettings.turboEnabled)
        viewModel.onVideoStandardSelected(expectedSettings.videoStandard)
        advanceUntilIdle()

        viewModel.onStartRequested()

        assertEquals(
            listOf(
                SessionCommand.StartSession(
                    SessionLaunchConfig(settings = expectedSettings),
                )
            ),
            sessionRepository.dispatchedCommands,
        )
    }

    @Test
    fun turboModeChangeDispatchesRuntimeSettingsWhenSessionIsRunning() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "running-turbo.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository().apply {
            setState(
                SessionState.Running(
                    sessionToken = 42L,
                    paused = false,
                    surfaceAttached = false,
                    launchMode = LaunchMode.FUJINET_ENABLED,
                ),
            )
        }
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )

        advanceUntilIdle()
        viewModel.onTurboModeChanged(true)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SessionCommand.ApplyRuntimeSettings(
                    EmulatorSettings(turboEnabled = true),
                ),
            ),
            sessionRepository.dispatchedCommands,
        )
    }

    @Test
    fun videoStandardChangeDispatchesRuntimeSettingsWhenSessionIsRunning() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "running-video.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository().apply {
            setState(
                SessionState.Running(
                    sessionToken = 42L,
                    paused = false,
                    surfaceAttached = false,
                    launchMode = LaunchMode.FUJINET_ENABLED,
                ),
            )
        }
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )

        advanceUntilIdle()
        viewModel.onVideoStandardSelected(VideoStandard.PAL)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SessionCommand.ApplyRuntimeSettings(
                    EmulatorSettings(videoStandard = VideoStandard.PAL),
                ),
            ),
            sessionRepository.dispatchedCommands,
        )
    }

    @Test
    fun artifactingChangeDispatchesRuntimeSettingsWhenSessionIsRunning() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "running-artifact.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository().apply {
            setState(
                SessionState.Running(
                    sessionToken = 42L,
                    paused = false,
                    surfaceAttached = false,
                    launchMode = LaunchMode.LOCAL_ONLY,
                ),
            )
        }
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )

        advanceUntilIdle()
        viewModel.onArtifactingModeSelected(ArtifactingMode.NTSC_NEW)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SessionCommand.ApplyRuntimeSettings(
                    EmulatorSettings(artifactingMode = ArtifactingMode.NTSC_NEW),
                ),
            ),
            sessionRepository.dispatchedCommands,
        )
    }

    @Test
    fun ntscFilterPresetChangeDispatchesRuntimeSettingsWhenSessionIsRunning() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "running-ntsc-preset.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository().apply {
            setState(
                SessionState.Running(
                    sessionToken = 42L,
                    paused = false,
                    surfaceAttached = false,
                    launchMode = LaunchMode.LOCAL_ONLY,
                ),
            )
        }
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )

        advanceUntilIdle()
        viewModel.onNtscFilterPresetSelected(NtscFilterPreset.RGB)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SessionCommand.ApplyRuntimeSettings(
                    EmulatorSettings(
                        ntscFilter = NtscFilterSettings(
                            preset = NtscFilterPreset.RGB,
                            sharpness = -0.3f,
                            resolution = 0.7f,
                            artifacts = -1f,
                            fringing = -1f,
                            bleed = -1f,
                            burstPhase = 0f,
                        ),
                    ),
                ),
            ),
            sessionRepository.dispatchedCommands,
        )
    }

    @Test
    fun ntscFilterSliderChangeMarksPresetCustomAndDispatchesRuntimeSettings() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "running-ntsc-custom.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository().apply {
            setState(
                SessionState.Running(
                    sessionToken = 42L,
                    paused = false,
                    surfaceAttached = false,
                    launchMode = LaunchMode.LOCAL_ONLY,
                ),
            )
        }
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )

        advanceUntilIdle()
        viewModel.onNtscFilterSharpnessChanged(0.25f)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SessionCommand.ApplyRuntimeSettings(
                    EmulatorSettings(
                        ntscFilter = NtscFilterSettings(
                            preset = NtscFilterPreset.CUSTOM,
                            sharpness = 0.25f,
                            resolution = -0.1f,
                            artifacts = 0f,
                            fringing = 0f,
                            bleed = 0f,
                            burstPhase = 0f,
                        ),
                    ),
                ),
            ),
            sessionRepository.dispatchedCommands,
        )
    }

    @Test
    fun closingSettingsDoesNotRestartWhenLaunchModeSelectionStaysFujiNet() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "close-settings.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository().apply {
            setState(
                SessionState.Running(
                    sessionToken = 7L,
                    paused = false,
                    surfaceAttached = false,
                    launchMode = LaunchMode.FUJINET_ENABLED,
                ),
            )
        }
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )

        advanceUntilIdle()
        viewModel.onLaunchModeSelected(LaunchMode.LOCAL_ONLY)
        advanceUntilIdle()
        sessionRepository.dispatchedCommands.clear()

        viewModel.onSettingsClosed(sessionRepository.state.value)

        assertTrue(sessionRepository.dispatchedCommands.isEmpty())
    }

    @Test
    fun closingSettingsRestartsRunningEmulatorWhenMachineConfigChanges() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "machine-restart.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository().apply {
            setState(
                SessionState.Running(
                    sessionToken = 9L,
                    paused = false,
                    surfaceAttached = false,
                    launchMode = LaunchMode.FUJINET_ENABLED,
                ),
            )
        }
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )

        advanceUntilIdle()
        viewModel.onSettingsOpened()
        advanceUntilIdle()
        viewModel.onMachineTypeSelected(AtariMachineType.ATARI_130XE)
        viewModel.onMemoryProfileSelected(MemoryProfile.RAM_128)
        advanceUntilIdle()
        sessionRepository.dispatchedCommands.clear()

        viewModel.onSettingsClosed(sessionRepository.state.value)

        assertEquals(
            listOf(
                SessionCommand.ReturnToLaunch,
                SessionCommand.StartSession(
                    SessionLaunchConfig(
                        settings = EmulatorSettings(
                            machineType = AtariMachineType.ATARI_130XE,
                            memoryProfile = MemoryProfile.RAM_128,
                        ),
                    ),
                ),
            ),
            sessionRepository.dispatchedCommands,
        )
    }

    @Test
    fun importingSystemRomPersistsPathAndDispatchesRuntimeSettingsWhenRunning() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "system-rom.preferences_pb",
        )
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("system-rom-runtime"))
        val systemRomDocumentStore = SystemRomDocumentStore(runtimePaths)
        val sessionRepository = RecordingSessionRepository().apply {
            setState(
                SessionState.Running(
                    sessionToken = 11L,
                    paused = false,
                    surfaceAttached = false,
                    launchMode = LaunchMode.LOCAL_ONLY,
                ),
            )
        }
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
            runtimePaths = runtimePaths,
            systemRomDocumentStore = systemRomDocumentStore,
        )

        advanceUntilIdle()
        viewModel.onSystemRomImported(
            SystemRomKind.BASIC,
            SystemRomSelection(
                uriString = "content://provider/roms/basic.rom",
                importedPath = "/tmp/basic.rom",
                displayName = "Cloud BASIC.rom",
                lastUpdatedEpochMillis = 1234L,
            ),
        )
        advanceUntilIdle()

        assertEquals("/tmp/basic.rom", settingsRepository.currentSettings().basicRomPath)
        assertEquals(
            "content://provider/roms/basic.rom",
            systemRomDocumentStore.loadSelection(SystemRomKind.BASIC)?.uriString,
        )
        assertEquals(
            listOf(
                SessionCommand.ApplyRuntimeSettings(
                    EmulatorSettings(basicRomPath = "/tmp/basic.rom"),
                ),
            ),
            sessionRepository.dispatchedCommands,
        )
        assertEquals("Cloud BASIC.rom", viewModel.uiState.value.basicRomLabel)
    }

    @Test
    fun emulatorVolumeChangeDispatchesRuntimeUpdateWhileRunning() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "volume-running.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository().apply {
            setState(
                SessionState.Running(
                    sessionToken = 15L,
                    paused = false,
                    surfaceAttached = false,
                    launchMode = LaunchMode.FUJINET_ENABLED,
                ),
            )
        }
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )

        advanceUntilIdle()
        viewModel.onEmulatorVolumePreviewChanged(50)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SessionCommand.SetAudioVolume(50),
            ),
            sessionRepository.dispatchedCommands,
        )
    }

    @Test
    fun emulatorVolumeChangeFinishedPersistsSelection() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "volume-persist.preferences_pb",
        )
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = RecordingSessionRepository(),
        )

        advanceUntilIdle()
        viewModel.onEmulatorVolumePreviewChanged(50)
        viewModel.onEmulatorVolumeChangeFinished()
        advanceUntilIdle()

        assertEquals(50, settingsRepository.currentSettings().emulatorVolumePercent)
    }

    @Test
    fun resetToDefaultsRestoresDefaultSettings() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "reset-defaults.preferences_pb",
        )
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = RecordingSessionRepository(),
        )

        viewModel.onLaunchModeSelected(LaunchMode.LOCAL_ONLY)
        viewModel.onScaleModeSelected(ScaleMode.FILL)
        viewModel.onMachineTypeSelected(AtariMachineType.ATARI_130XE)
        viewModel.onMemoryProfileSelected(MemoryProfile.RAM_128)
        advanceUntilIdle()

        viewModel.onResetToDefaults()
        advanceUntilIdle()

        assertEquals(EmulatorSettings(), settingsRepository.currentSettings())
        assertEquals(EmulatorSettings(), viewModel.uiState.value.settings)
        assertEquals(SettingsTab.APP, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun selectingExpandedMachineTypeCoercesInvalidMemoryProfile() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "machine-memory-coercion.preferences_pb",
        )
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = RecordingSessionRepository(),
        )

        viewModel.onMemoryProfileSelected(MemoryProfile.RAM_1088)
        advanceUntilIdle()

        viewModel.onMachineTypeSelected(AtariMachineType.ATARI_1200XL)
        advanceUntilIdle()

        assertEquals(AtariMachineType.ATARI_1200XL, viewModel.uiState.value.settings.machineType)
        assertEquals(MemoryProfile.RAM_64, viewModel.uiState.value.settings.memoryProfile)
        assertEquals(MemoryProfile.RAM_64, settingsRepository.currentSettings().memoryProfile)
    }

    @Test
    fun invalidPersistedMemoryProfileIsNormalizedOnLoad() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "machine-memory-800xl.preferences_pb",
        )

        settingsRepository.updateMachineType(AtariMachineType.ATARI_800XL)
        settingsRepository.updateMemoryProfile(MemoryProfile.RAM_576)

        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = RecordingSessionRepository(),
        )

        advanceUntilIdle()

        assertEquals(AtariMachineType.ATARI_800XL, viewModel.uiState.value.settings.machineType)
        assertEquals(MemoryProfile.RAM_64, viewModel.uiState.value.settings.memoryProfile)
        assertEquals(MemoryProfile.RAM_64, settingsRepository.currentSettings().memoryProfile)
    }

    @Test
    fun closingSettingsRestartsWhenPatchModeChanges() = runTest {
        val settingsRepository = createSettingsRepository(
            scope = CoroutineScope(coroutineContext + Job()),
            fileName = "patch-mode-restart.preferences_pb",
        )
        val sessionRepository = RecordingSessionRepository().apply {
            setState(
                SessionState.Running(
                    sessionToken = 15L,
                    paused = false,
                    surfaceAttached = false,
                    launchMode = LaunchMode.LOCAL_ONLY,
                ),
            )
        }
        val viewModel = LaunchSettingsViewModel(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        )

        advanceUntilIdle()
        viewModel.onLaunchModeSelected(LaunchMode.LOCAL_ONLY)
        advanceUntilIdle()
        viewModel.onSettingsOpened()
        viewModel.onSioPatchModeSelected(SioPatchMode.NO_PATCH_ALL)
        advanceUntilIdle()
        sessionRepository.dispatchedCommands.clear()

        viewModel.onSettingsClosed(sessionRepository.state.value)

        assertEquals(
            listOf(
                SessionCommand.ReturnToLaunch,
                SessionCommand.StartSession(
                    SessionLaunchConfig(
                        settings = EmulatorSettings(
                            sioPatchMode = SioPatchMode.NO_PATCH_ALL,
                        ),
                    ),
                ),
            ),
            sessionRepository.dispatchedCommands,
        )
    }

    private fun createSettingsRepository(
        scope: CoroutineScope,
        fileName: String,
    ): EmulatorSettingsRepository {
        return EmulatorSettingsRepository.createForTest(
            produceFile = { temporaryFolder.newFile(fileName) },
            scope = scope,
        )
    }
}

private class RecordingSessionRepository : SessionRepository {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Idle)

    val dispatchedCommands = mutableListOf<SessionCommand>()

    override val state: StateFlow<SessionState> = mutableState

    override fun dispatch(command: SessionCommand) {
        dispatchedCommands += command
    }

    fun setState(state: SessionState) {
        mutableState.value = state
    }
}
