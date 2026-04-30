package com.mantismoonlabs.fujinetgo800.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EmulatorSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultsToFujiNetEnabledFitFollowSystem() = runTest {
        val repository = EmulatorSettingsRepository.createForTest(
            produceFile = { temporaryFolder.newFile("emulator-settings.preferences_pb") },
            scope = CoroutineScope(coroutineContext + Job()),
        )

        assertEquals(
            EmulatorSettings(),
            repository.settings.first(),
        )
    }

    @Test
    fun betaBuildClampsPersistedLaunchModeAndScaleMode() = runTest {
        val settingsFile = temporaryFolder.newFile("emulator-settings.preferences_pb")
        val firstScope = CoroutineScope(coroutineContext + Job())
        val firstRepository = EmulatorSettingsRepository.createForTest(
            produceFile = { settingsFile },
            scope = firstScope,
        )

        firstRepository.updateLaunchMode(LaunchMode.LOCAL_ONLY)
        firstRepository.updateScaleMode(ScaleMode.FILL)
        firstRepository.updateKeepScreenOn(false)
        firstRepository.updateOrientationMode(OrientationMode.LANDSCAPE)
        firstRepository.updateTurboEnabled(true)
        firstScope.coroutineContext[Job]?.cancel()

        val reloadedRepository = EmulatorSettingsRepository.createForTest(
            produceFile = { settingsFile },
            scope = CoroutineScope(coroutineContext + Job()),
        )

        assertEquals(
            EmulatorSettings(
                launchMode = LaunchMode.FUJINET_ENABLED,
                scaleMode = ScaleMode.FIT,
                keepScreenOn = false,
                orientationMode = OrientationMode.LANDSCAPE,
                turboEnabled = true,
            ),
            reloadedRepository.settings.first(),
        )
    }

    @Test
    fun fallsBackToDefaultsForMissingOrInvalidPreferences() = runTest {
        val settingsFile = temporaryFolder.newFile("invalid.preferences_pb")
        val seedScope = CoroutineScope(coroutineContext + Job())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = seedScope,
            produceFile = { settingsFile },
        )
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.launchMode] = "NOT_A_MODE"
            preferences[EmulatorSettingsPreferenceKeys.scaleMode] = "NOT_A_SCALE"
            preferences[EmulatorSettingsPreferenceKeys.orientationMode] = "NOT_AN_ORIENTATION"
            preferences[EmulatorSettingsPreferenceKeys.keepScreenOn] = true
        }
        seedScope.coroutineContext[Job]?.cancel()

        val repository = EmulatorSettingsRepository.createForTest(
            produceFile = { settingsFile },
            scope = CoroutineScope(coroutineContext + Job()),
        )

        assertEquals(
            EmulatorSettings(
                launchMode = LaunchMode.FUJINET_ENABLED,
                scaleMode = ScaleMode.FIT,
                keepScreenOn = true,
                orientationMode = OrientationMode.FOLLOW_SYSTEM,
                turboEnabled = false,
            ),
            repository.settings.first(),
        )
    }

    @Test
    fun persistsExpandedPhaseElevenSettingsAndForcesFitScaleMode() = runTest {
        val settingsFile = temporaryFolder.newFile("phase11.preferences_pb")
        val firstScope = CoroutineScope(coroutineContext + Job())
        val firstRepository = EmulatorSettingsRepository.createForTest(
            produceFile = { settingsFile },
            scope = firstScope,
        )

        firstRepository.updateScaleMode(ScaleMode.INTEGER)
        firstRepository.updatePauseOnAppSwitch(true)
        firstRepository.updateMachineType(AtariMachineType.ATARI_5200)
        firstRepository.updateMemoryProfile(MemoryProfile.RAM_16)
        firstRepository.updateSioPatchMode(SioPatchMode.NO_PATCH_ALL)
        firstRepository.updateArtifactingMode(ArtifactingMode.PAL_BLEND)
        firstRepository.updateNtscFilterPreset(NtscFilterPreset.RGB)
        firstRepository.updateNtscFilterSharpness(0.7f)
        firstRepository.updateNtscFilterResolution(0.6f)
        firstRepository.updateNtscFilterArtifacts(-0.4f)
        firstRepository.updateNtscFilterFringing(-0.3f)
        firstRepository.updateNtscFilterBleed(-0.2f)
        firstRepository.updateNtscFilterBurstPhase(0.5f)
        firstRepository.updateScanlinesEnabled(true)
        firstRepository.updateStereoPokeyEnabled(true)
        firstRepository.updateKeyboardHapticsEnabled(false)
        firstRepository.updateHDevicePath(1, "/storage/emulated/0/H1")
        firstScope.coroutineContext[Job]?.cancel()

        val reloadedRepository = EmulatorSettingsRepository.createForTest(
            produceFile = { settingsFile },
            scope = CoroutineScope(coroutineContext + Job()),
        )

        assertEquals(
            EmulatorSettings(
                scaleMode = ScaleMode.FIT,
                pauseOnAppSwitch = true,
                machineType = AtariMachineType.ATARI_5200,
                memoryProfile = MemoryProfile.RAM_16,
                sioPatchMode = SioPatchMode.NO_PATCH_ALL,
                artifactingMode = ArtifactingMode.PAL_BLEND,
                ntscFilter = NtscFilterSettings(
                    preset = NtscFilterPreset.RGB,
                    sharpness = 0.7f,
                    resolution = 0.6f,
                    artifacts = -0.4f,
                    fringing = -0.3f,
                    bleed = -0.2f,
                    burstPhase = 0.5f,
                ),
                scanlinesEnabled = true,
                stereoPokeyEnabled = true,
                hDevice1Path = "/storage/emulated/0/H1",
                keyboardHapticsEnabled = false,
            ),
            reloadedRepository.settings.first(),
        )
    }

    @Test
    fun persistsEmulatorVolumePercent() = runTest {
        val settingsFile = temporaryFolder.newFile("audio.preferences_pb")
        val firstScope = CoroutineScope(coroutineContext + Job())
        val firstRepository = EmulatorSettingsRepository.createForTest(
            produceFile = { settingsFile },
            scope = firstScope,
        )

        firstRepository.updateEmulatorVolumePercent(50)
        firstScope.coroutineContext[Job]?.cancel()

        val reloadedRepository = EmulatorSettingsRepository.createForTest(
            produceFile = { settingsFile },
            scope = CoroutineScope(coroutineContext + Job()),
        )

        assertEquals(
            50,
            reloadedRepository.settings.first().emulatorVolumePercent,
        )
    }

    @Test
    fun portraitInputPanelFractionAllowsExtendedEmulatorShrinkRange() = runTest {
        val repository = EmulatorSettingsRepository.createForTest(
            produceFile = { temporaryFolder.newFile("portrait-input.preferences_pb") },
            scope = CoroutineScope(coroutineContext + Job()),
        )

        repository.updatePortraitInputPanelSizeFraction(1.6f)
        assertEquals(1.6f, repository.settings.first().portraitInputPanelSizeFraction, 0.001f)

        repository.updatePortraitInputPanelSizeFraction(3f)
        assertEquals(2f, repository.settings.first().portraitInputPanelSizeFraction, 0.001f)
    }
}
