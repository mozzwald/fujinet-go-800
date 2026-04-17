package com.mantismoonlabs.fujinetgo800.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mantismoonlabs.fujinetgo800.core.FujiNetNative
import com.mantismoonlabs.fujinetgo800.fujinet.FujiNetBootMode
import com.mantismoonlabs.fujinetgo800.fujinet.FujiNetPrinterModel
import com.mantismoonlabs.fujinetgo800.fujinet.FujiNetSettingsBridge
import com.mantismoonlabs.fujinetgo800.fujinet.FujiNetSettingsState
import com.mantismoonlabs.fujinetgo800.settings.AtariMachineType
import com.mantismoonlabs.fujinetgo800.settings.ArtifactingMode
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.JoystickInputStyle
import com.mantismoonlabs.fujinetgo800.settings.KeyboardInputMode
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.settings.MemoryProfile
import com.mantismoonlabs.fujinetgo800.settings.NtscFilterPreset
import com.mantismoonlabs.fujinetgo800.settings.NtscFilterSettings
import com.mantismoonlabs.fujinetgo800.settings.OrientationMode
import com.mantismoonlabs.fujinetgo800.settings.ScaleMode
import com.mantismoonlabs.fujinetgo800.settings.SioPatchMode
import com.mantismoonlabs.fujinetgo800.settings.SystemRomKind
import com.mantismoonlabs.fujinetgo800.settings.VideoStandard
import com.mantismoonlabs.fujinetgo800.settings.defaultMemoryProfile
import com.mantismoonlabs.fujinetgo800.settings.isValidFor
import com.mantismoonlabs.fujinetgo800.settings.normalizedMachineMemory
import com.mantismoonlabs.fujinetgo800.storage.RuntimePaths
import com.mantismoonlabs.fujinetgo800.storage.SystemRomDocumentStore
import com.mantismoonlabs.fujinetgo800.storage.SystemRomSelection
import java.io.File
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsTab {
    MACHINE,
    FUJINET,
    APP,
}

data class LaunchSettingsUiState(
    val selectedTab: SettingsTab = SettingsTab.FUJINET,
    val launchModeLabel: String = "FujiNet enabled",
    val scaleModeLabel: String = "Fit",
    val emulatorVolumeLabel: String = "35%",
    val keepScreenOnLabel: String = "On",
    val keepScreenOnActionLabel: String = "Turn off",
    val backgroundAudioLabel: String = "Off",
    val backgroundAudioActionLabel: String = "Turn on",
    val pauseOnAppSwitchLabel: String = "Off",
    val orientationModeLabel: String = "Follow system",
    val keyboardInputModeLabel: String = "Android keyboard",
    val keyboardHapticsLabel: String = "On",
    val stickyKeyboardShiftLabel: String = "Off",
    val stickyKeyboardCtrlLabel: String = "Off",
    val stickyKeyboardFnLabel: String = "Off",
    val joystickInputStyleLabel: String = "8-way stick",
    val joystickHapticsLabel: String = "On",
    val joystickHapticsActionLabel: String = "Turn off",
    val videoStandardLabel: String = "NTSC",
    val machineTypeLabel: String = "Atari 800XL",
    val memoryProfileLabel: String = "64 KB",
    val basicBootLabel: String = "Enabled",
    val sioPatchModeLabel: String = "Enhanced",
    val artifactingModeLabel: String = "Off",
    val scanlinesLabel: String = "Off",
    val stereoPokeyLabel: String = "Off",
    val ntscFilterPresetLabel: String = "Composite",
    val ntscFilterSharpnessLabel: String = "-0.50",
    val ntscFilterResolutionLabel: String = "-0.10",
    val ntscFilterArtifactsLabel: String = "0.00",
    val ntscFilterFringingLabel: String = "0.00",
    val ntscFilterBleedLabel: String = "0.00",
    val ntscFilterBurstPhaseLabel: String = "0.00",
    val ntscFilterControlsVisible: Boolean = false,
    val hDevice1Label: String = "Not set",
    val hDevice2Label: String = "Not set",
    val hDevice3Label: String = "Not set",
    val hDevice4Label: String = "Not set",
    val restartRequiredVisible: Boolean = false,
    val restartRequiredLabel: String = "",
    val xlxeRomLabel: String = "Built-in Altirra XL/XE ROM",
    val basicRomLabel: String = "Built-in Altirra BASIC ROM",
    val atari400800RomLabel: String = "Built-in Altirra 400/800 ROM",
    val fujiNetStorageLabel: String = "",
    val fujiNetStorageModeLabel: String = "Private app storage fallback",
    val fujiNetConsoleLogPathLabel: String = "",
    val fujiNetRecentLogLabel: String = "",
    val fujiNetSettingsState: FujiNetSettingsState = FujiNetSettingsState(),
    val settings: EmulatorSettings = EmulatorSettings(),
)

class LaunchSettingsViewModel(
    private val settingsRepository: EmulatorSettingsRepository,
    private val sessionRepository: SessionRepository,
    private val runtimePaths: RuntimePaths = RuntimePaths(File(".")),
    private val fujiNetSettingsBridge: FujiNetSettingsBridge = FujiNetSettingsBridge(runtimePaths),
    private val systemRomDocumentStore: SystemRomDocumentStore? = null,
    private val recentLogProvider: () -> String = {
        runCatching { FujiNetNative.recentLog() }.getOrDefault("")
    },
) : ViewModel() {
    private val persistedSettings: StateFlow<EmulatorSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, EmulatorSettings())
    private val editableSettings = MutableStateFlow(EmulatorSettings())
    private val recentFujiNetLog = MutableStateFlow("")
    private val fujiNetSettings = MutableStateFlow(
        FujiNetSettingsState(runtimeStoragePath = runtimePaths.fujiNetStorageDisplayPath),
    )
    private val systemRomSelections = MutableStateFlow<Map<SystemRomKind, SystemRomSelection>>(emptyMap())
    private val selectedTab = MutableStateFlow(SettingsTab.FUJINET)
    private val settingsOpenedSnapshot = MutableStateFlow<EmulatorSettings?>(null)

    init {
        viewModelScope.launch {
            persistedSettings.collect { settings ->
                val normalized = settings.normalizedMachineMemory()
                editableSettings.value = normalized
                if (normalized != settings) {
                    settingsRepository.updateMemoryProfile(normalized.memoryProfile)
                }
            }
        }
        refreshSystemRomSelections()
        refreshFujiNetLog()
        refreshFujiNetSettings()
    }

    val uiState: StateFlow<LaunchSettingsUiState> = combine(
            combine(
                editableSettings,
                recentFujiNetLog,
                fujiNetSettings,
                systemRomSelections,
                selectedTab,
            ) { settings, logTail, fujiNet, romSelections, tab ->
                UiInputs(
                    settings = settings,
                    logTail = logTail,
                    fujiNet = fujiNet,
                    romSelections = romSelections,
                    tab = tab,
                )
            },
            settingsOpenedSnapshot,
        ) { inputs, openedSnapshot ->
            val settings = inputs.settings
            val logTail = inputs.logTail
            val fujiNet = inputs.fujiNet
            val romSelections = inputs.romSelections
            val tab = inputs.tab
            val restartRequired = openedSnapshot?.let { requiresRestart(it, settings) } ?: false
            LaunchSettingsUiState(
                selectedTab = tab,
                launchModeLabel = settings.launchMode.toLabel(),
                scaleModeLabel = settings.scaleMode.toLabel(),
                emulatorVolumeLabel = settings.emulatorVolumePercent.toPercentLabel(),
                keepScreenOnLabel = settings.keepScreenOn.toWakeLabel(),
                keepScreenOnActionLabel = settings.keepScreenOn.toWakeActionLabel(),
                backgroundAudioLabel = settings.backgroundAudioEnabled.toWakeLabel(),
                backgroundAudioActionLabel = settings.backgroundAudioEnabled.toWakeActionLabel(),
                pauseOnAppSwitchLabel = settings.pauseOnAppSwitch.toWakeLabel(),
                orientationModeLabel = settings.orientationMode.toLabel(),
                keyboardInputModeLabel = settings.keyboardInputMode.toLabel(),
                keyboardHapticsLabel = settings.keyboardHapticsEnabled.toWakeLabel(),
                stickyKeyboardShiftLabel = settings.stickyKeyboardShiftEnabled.toWakeLabel(),
                stickyKeyboardCtrlLabel = settings.stickyKeyboardCtrlEnabled.toWakeLabel(),
                stickyKeyboardFnLabel = settings.stickyKeyboardFnEnabled.toWakeLabel(),
                joystickInputStyleLabel = settings.joystickInputStyle.toLabel(),
                joystickHapticsLabel = settings.joystickHapticsEnabled.toWakeLabel(),
                joystickHapticsActionLabel = settings.joystickHapticsEnabled.toWakeActionLabel(),
                videoStandardLabel = settings.videoStandard.toLabel(),
                machineTypeLabel = settings.machineType.toLabel(),
                memoryProfileLabel = settings.memoryProfile.toLabel(),
                basicBootLabel = settings.basicEnabled.toEnabledLabel(),
                sioPatchModeLabel = settings.sioPatchMode.toLabel(),
                artifactingModeLabel = settings.artifactingMode.toLabel(),
                scanlinesLabel = settings.scanlinesEnabled.toWakeLabel(),
                stereoPokeyLabel = settings.stereoPokeyEnabled.toWakeLabel(),
                ntscFilterPresetLabel = settings.ntscFilter.preset.toLabel(),
                ntscFilterSharpnessLabel = settings.ntscFilter.sharpness.toSliderLabel(),
                ntscFilterResolutionLabel = settings.ntscFilter.resolution.toSliderLabel(),
                ntscFilterArtifactsLabel = settings.ntscFilter.artifacts.toSliderLabel(),
                ntscFilterFringingLabel = settings.ntscFilter.fringing.toSliderLabel(),
                ntscFilterBleedLabel = settings.ntscFilter.bleed.toSliderLabel(),
                ntscFilterBurstPhaseLabel = settings.ntscFilter.burstPhase.toSliderLabel(),
                ntscFilterControlsVisible = settings.videoStandard == VideoStandard.NTSC &&
                    settings.artifactingMode == ArtifactingMode.NTSC_FULL,
                hDevice1Label = settings.hDevice1Path.toFolderLabel(),
                hDevice2Label = settings.hDevice2Path.toFolderLabel(),
                hDevice3Label = settings.hDevice3Path.toFolderLabel(),
                hDevice4Label = settings.hDevice4Path.toFolderLabel(),
                restartRequiredVisible = restartRequired,
                restartRequiredLabel = if (restartRequired) {
                    "Close settings to restart with machine changes."
                } else {
                    ""
                },
                xlxeRomLabel = romSelections[SystemRomKind.XL_XE]
                    ?.displayName
                    .toRomLabel(settings.xlxeRomPath, "Built-in Altirra XL/XE ROM"),
                basicRomLabel = romSelections[SystemRomKind.BASIC]
                    ?.displayName
                    .toRomLabel(settings.basicRomPath, "Built-in Altirra BASIC ROM"),
                atari400800RomLabel = romSelections[SystemRomKind.ATARI_400_800]
                    ?.displayName
                    .toRomLabel(settings.atari400800RomPath, "Built-in Altirra 400/800 ROM"),
                fujiNetStorageLabel = runtimePaths.fujiNetStorageDisplayPath,
                fujiNetStorageModeLabel = runtimePaths.toFujiNetStorageModeLabel(),
                fujiNetConsoleLogPathLabel = runtimePaths.fujiNetConsoleLogFile.absolutePath,
                fujiNetRecentLogLabel = logTail,
                fujiNetSettingsState = fujiNet,
                settings = settings,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LaunchSettingsUiState())

    private data class UiInputs(
        val settings: EmulatorSettings,
        val logTail: String,
        val fujiNet: FujiNetSettingsState,
        val romSelections: Map<SystemRomKind, SystemRomSelection>,
        val tab: SettingsTab,
    )

    fun onLaunchModeSelected(launchMode: LaunchMode) {
        editableSettings.update { settings -> settings.copy(launchMode = LaunchMode.FUJINET_ENABLED) }
        persistChange {
            settingsRepository.updateLaunchMode(LaunchMode.FUJINET_ENABLED)
        }
    }

    fun onScaleModeSelected(scaleMode: ScaleMode) {
        editableSettings.update { settings -> settings.copy(scaleMode = scaleMode) }
        persistChange {
            settingsRepository.updateScaleMode(scaleMode)
        }
    }

    fun onKeepScreenOnChanged(keepScreenOn: Boolean) {
        editableSettings.update { settings -> settings.copy(keepScreenOn = keepScreenOn) }
        persistChange {
            settingsRepository.updateKeepScreenOn(keepScreenOn)
        }
    }

    fun onEmulatorVolumePreviewChanged(volumePercent: Int) {
        val normalizedVolume = volumePercent.coerceIn(0, 100)
        editableSettings.update { settings -> settings.copy(emulatorVolumePercent = normalizedVolume) }
        if (sessionRepository.state.value is SessionState.Running) {
            sessionRepository.dispatch(SessionCommand.SetAudioVolume(normalizedVolume))
        }
    }

    fun onEmulatorVolumeChangeFinished() {
        val normalizedVolume = editableSettings.value.emulatorVolumePercent.coerceIn(0, 100)
        persistChange {
            settingsRepository.updateEmulatorVolumePercent(normalizedVolume)
        }
    }

    fun onBackgroundAudioChanged(backgroundAudioEnabled: Boolean) {
        editableSettings.update { settings -> settings.copy(backgroundAudioEnabled = backgroundAudioEnabled) }
        dispatchRuntimeSettingsIfRunning()
        persistChange {
            settingsRepository.updateBackgroundAudioEnabled(backgroundAudioEnabled)
        }
    }

    fun onOrientationModeSelected(orientationMode: OrientationMode) {
        editableSettings.update { settings -> settings.copy(orientationMode = orientationMode) }
        persistChange {
            settingsRepository.updateOrientationMode(orientationMode)
        }
    }

    fun onTurboModeChanged(turboEnabled: Boolean) {
        editableSettings.update { settings -> settings.copy(turboEnabled = turboEnabled) }
        dispatchRuntimeSettingsIfRunning()
        persistChange {
            settingsRepository.updateTurboEnabled(turboEnabled)
        }
    }

    fun onMachineTypeSelected(machineType: AtariMachineType) {
        editableSettings.update { settings ->
            val defaultMemory = machineType.defaultMemoryProfile()
            settings.copy(
                machineType = machineType,
                memoryProfile = if (settings.memoryProfile.isValidFor(machineType)) settings.memoryProfile else defaultMemory,
            )
        }
        persistChange {
            val settings = editableSettings.value
            settingsRepository.updateMachineType(settings.machineType)
            settingsRepository.updateMemoryProfile(settings.memoryProfile)
        }
    }

    fun onMemoryProfileSelected(memoryProfile: MemoryProfile) {
        editableSettings.update { settings -> settings.copy(memoryProfile = memoryProfile) }
        persistChange {
            settingsRepository.updateMemoryProfile(memoryProfile)
        }
    }

    fun onBasicEnabledChanged(enabled: Boolean) {
        editableSettings.update { settings -> settings.copy(basicEnabled = enabled) }
        persistChange {
            settingsRepository.updateBasicEnabled(enabled)
        }
    }

    fun onSioPatchModeSelected(sioPatchMode: SioPatchMode) {
        editableSettings.update { settings -> settings.copy(sioPatchMode = sioPatchMode) }
        persistChange {
            settingsRepository.updateSioPatchMode(sioPatchMode)
        }
    }

    fun onArtifactingModeSelected(artifactingMode: ArtifactingMode) {
        editableSettings.update { settings -> settings.copy(artifactingMode = artifactingMode) }
        dispatchRuntimeSettingsIfRunning()
        persistChange {
            settingsRepository.updateArtifactingMode(artifactingMode)
        }
    }

    fun onNtscFilterPresetSelected(preset: NtscFilterPreset) {
        val filter = preset.toSettings()
        editableSettings.update { settings -> settings.copy(ntscFilter = filter) }
        dispatchRuntimeSettingsIfRunning()
        persistChange {
            settingsRepository.updateNtscFilterPreset(filter.preset)
            settingsRepository.updateNtscFilterSharpness(filter.sharpness)
            settingsRepository.updateNtscFilterResolution(filter.resolution)
            settingsRepository.updateNtscFilterArtifacts(filter.artifacts)
            settingsRepository.updateNtscFilterFringing(filter.fringing)
            settingsRepository.updateNtscFilterBleed(filter.bleed)
            settingsRepository.updateNtscFilterBurstPhase(filter.burstPhase)
        }
    }

    fun onNtscFilterSharpnessChanged(value: Float) = updateNtscFilterCustom {
        copy(sharpness = value.coerceToNtscFilterRange())
    }

    fun onNtscFilterResolutionChanged(value: Float) = updateNtscFilterCustom {
        copy(resolution = value.coerceToNtscFilterRange())
    }

    fun onNtscFilterArtifactsChanged(value: Float) = updateNtscFilterCustom {
        copy(artifacts = value.coerceToNtscFilterRange())
    }

    fun onNtscFilterFringingChanged(value: Float) = updateNtscFilterCustom {
        copy(fringing = value.coerceToNtscFilterRange())
    }

    fun onNtscFilterBleedChanged(value: Float) = updateNtscFilterCustom {
        copy(bleed = value.coerceToNtscFilterRange())
    }

    fun onNtscFilterBurstPhaseChanged(value: Float) = updateNtscFilterCustom {
        copy(burstPhase = value.coerceToNtscFilterRange())
    }

    fun onScanlinesChanged(enabled: Boolean) {
        editableSettings.update { settings -> settings.copy(scanlinesEnabled = enabled) }
        dispatchRuntimeSettingsIfRunning()
        persistChange {
            settingsRepository.updateScanlinesEnabled(enabled)
        }
    }

    fun onStereoPokeyChanged(enabled: Boolean) {
        editableSettings.update { settings -> settings.copy(stereoPokeyEnabled = enabled) }
        persistChange {
            settingsRepository.updateStereoPokeyEnabled(enabled)
        }
    }

    fun onKeyboardInputModeSelected(keyboardInputMode: KeyboardInputMode) {
        editableSettings.update { settings -> settings.copy(keyboardInputMode = keyboardInputMode) }
        persistChange {
            settingsRepository.updateKeyboardInputMode(keyboardInputMode)
        }
    }

    fun onKeyboardHapticsChanged(enabled: Boolean) {
        editableSettings.update { settings -> settings.copy(keyboardHapticsEnabled = enabled) }
        persistChange {
            settingsRepository.updateKeyboardHapticsEnabled(enabled)
        }
    }

    fun onStickyKeyboardShiftChanged(enabled: Boolean) {
        editableSettings.update { settings -> settings.copy(stickyKeyboardShiftEnabled = enabled) }
        persistChange {
            settingsRepository.updateStickyKeyboardShiftEnabled(enabled)
        }
    }

    fun onStickyKeyboardCtrlChanged(enabled: Boolean) {
        editableSettings.update { settings -> settings.copy(stickyKeyboardCtrlEnabled = enabled) }
        persistChange {
            settingsRepository.updateStickyKeyboardCtrlEnabled(enabled)
        }
    }

    fun onStickyKeyboardFnChanged(enabled: Boolean) {
        editableSettings.update { settings -> settings.copy(stickyKeyboardFnEnabled = enabled) }
        persistChange {
            settingsRepository.updateStickyKeyboardFnEnabled(enabled)
        }
    }

    fun onJoystickHapticsChanged(enabled: Boolean) {
        editableSettings.update { settings -> settings.copy(joystickHapticsEnabled = enabled) }
        persistChange {
            settingsRepository.updateJoystickHapticsEnabled(enabled)
        }
    }

    fun onJoystickInputStyleSelected(style: JoystickInputStyle) {
        editableSettings.update { settings -> settings.copy(joystickInputStyle = style) }
        persistChange {
            settingsRepository.updateJoystickInputStyle(style)
        }
    }

    fun onPauseOnAppSwitchChanged(enabled: Boolean) {
        editableSettings.update { settings -> settings.copy(pauseOnAppSwitch = enabled) }
        dispatchRuntimeSettingsIfRunning()
        persistChange {
            settingsRepository.updatePauseOnAppSwitch(enabled)
        }
    }

    fun onVideoStandardSelected(videoStandard: VideoStandard) {
        editableSettings.update { settings -> settings.copy(videoStandard = videoStandard) }
        dispatchRuntimeSettingsIfRunning()
        persistChange {
            settingsRepository.updateVideoStandard(videoStandard)
        }
    }

    fun onStartRequested() {
        val normalizedSettings = editableSettings.value.normalizedMachineMemory()
        sessionRepository.dispatch(
            SessionCommand.StartSession(
                SessionLaunchConfig(settings = normalizedSettings),
            ),
        )
        refreshFujiNetLog()
    }

    fun onSettingsOpened() {
        settingsOpenedSnapshot.value = editableSettings.value.normalizedMachineMemory()
        selectedTab.value = SettingsTab.FUJINET
        refreshFujiNetLog()
        refreshFujiNetSettings()
    }

    fun onSettingsClosed(currentState: SessionState) {
        val currentSettings = editableSettings.value.normalizedMachineMemory()
        val openedSettings = settingsOpenedSnapshot.value
        val restartRequired = openedSettings?.let { requiresRestart(it, currentSettings) } ?: false
        if (currentState is SessionState.Running && (currentState.launchMode != currentSettings.launchMode || restartRequired)) {
            sessionRepository.dispatch(SessionCommand.ReturnToLaunch)
            sessionRepository.dispatch(
                SessionCommand.StartSession(
                    SessionLaunchConfig(settings = currentSettings),
                ),
            )
        }
        settingsOpenedSnapshot.value = null
        selectedTab.value = SettingsTab.FUJINET
    }

    fun onSettingsTabSelected(tab: SettingsTab) {
        selectedTab.value = tab
    }

    fun onSystemRomImported(kind: SystemRomKind, selection: SystemRomSelection?) {
        val path = selection?.importedPath
        editableSettings.update { settings ->
            when (kind) {
                SystemRomKind.XL_XE -> settings.copy(xlxeRomPath = path)
                SystemRomKind.BASIC -> settings.copy(basicRomPath = path)
                SystemRomKind.ATARI_400_800 -> settings.copy(atari400800RomPath = path)
            }
        }
        dispatchRuntimeSettingsIfRunning()
        persistChange {
            if (selection == null) {
                systemRomDocumentStore?.clearSelection(kind)
            } else {
                systemRomDocumentStore?.saveSelection(kind, selection)
            }
            when (kind) {
                SystemRomKind.XL_XE -> settingsRepository.updateXlxeRomPath(path)
                SystemRomKind.BASIC -> settingsRepository.updateBasicRomPath(path)
                SystemRomKind.ATARI_400_800 -> settingsRepository.updateAtari400800RomPath(path)
            }
            refreshSystemRomSelections()
        }
    }

    private fun persistChange(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
        }
    }

    fun refreshFujiNetLog() {
        recentFujiNetLog.value = recentLogProvider()
    }

    fun refreshFujiNetSettings() {
        viewModelScope.launch {
            fujiNetSettings.value = fujiNetSettingsBridge.load()
        }
    }

    private fun refreshSystemRomSelections() {
        systemRomSelections.value = systemRomDocumentStore?.loadAllSelections().orEmpty()
    }

    fun onFujiNetPrinterEnabledChanged(enabled: Boolean) {
        updateFujiNetSetting("printer_enabled", if (enabled) "1" else "0")
    }

    fun onFujiNetPrinterPortSelected(port: Int) {
        updateFujiNetSetting("printerport1", port.toString())
    }

    fun onFujiNetPrinterModelSelected(modelValue: String) {
        updateFujiNetSetting("printermodel1", modelValue)
    }

    fun onFujiNetHsioIndexSelected(index: Int) {
        updateFujiNetSetting("hsioindex", index.toString())
    }

    fun onFujiNetConfigBootEnabledChanged(enabled: Boolean) {
        updateFujiNetSetting("config_enable", if (enabled) "1" else "0")
    }

    fun onFujiNetConfigNgChanged(enabled: Boolean) {
        updateFujiNetSetting("config_ng", if (enabled) "1" else "0")
    }

    fun onFujiNetStatusWaitChanged(enabled: Boolean) {
        updateFujiNetSetting("status_wait_enable", if (enabled) "1" else "0")
    }

    fun onFujiNetBootModeSelected(mode: FujiNetBootMode) {
        updateFujiNetSetting("boot_mode", mode.formValue)
    }

    fun onResetToDefaults() {
        editableSettings.value = EmulatorSettings()
        selectedTab.value = SettingsTab.APP
        persistChange {
            settingsRepository.resetToDefaults()
        }
    }

    private fun dispatchRuntimeSettingsIfRunning() {
        if (sessionRepository.state.value is SessionState.Running) {
            sessionRepository.dispatch(SessionCommand.ApplyRuntimeSettings(editableSettings.value))
        }
    }

    private fun updateFujiNetSetting(field: String, value: String) {
        viewModelScope.launch {
            fujiNetSettings.value = fujiNetSettingsBridge.update(field, value)
            refreshFujiNetLog()
        }
    }

    private fun updateNtscFilterCustom(
        transform: NtscFilterSettings.() -> NtscFilterSettings,
    ) {
        val updatedFilter = editableSettings.value.ntscFilter
            .transform()
            .copy(preset = NtscFilterPreset.CUSTOM)
        editableSettings.update { settings -> settings.copy(ntscFilter = updatedFilter) }
        dispatchRuntimeSettingsIfRunning()
        persistChange {
            settingsRepository.updateNtscFilterPreset(updatedFilter.preset)
            settingsRepository.updateNtscFilterSharpness(updatedFilter.sharpness)
            settingsRepository.updateNtscFilterResolution(updatedFilter.resolution)
            settingsRepository.updateNtscFilterArtifacts(updatedFilter.artifacts)
            settingsRepository.updateNtscFilterFringing(updatedFilter.fringing)
            settingsRepository.updateNtscFilterBleed(updatedFilter.bleed)
            settingsRepository.updateNtscFilterBurstPhase(updatedFilter.burstPhase)
        }
    }

    private fun LaunchMode.toLabel(): String = when (this) {
        LaunchMode.FUJINET_ENABLED -> "FujiNet enabled"
        LaunchMode.LOCAL_ONLY -> "Local only"
    }

    private fun ScaleMode.toLabel(): String = when (this) {
        ScaleMode.FIT -> "Fit"
        ScaleMode.FILL -> "Fill"
        ScaleMode.INTEGER -> "Integer"
    }

    private fun OrientationMode.toLabel(): String = when (this) {
        OrientationMode.FOLLOW_SYSTEM -> "Follow system"
        OrientationMode.PORTRAIT -> "Portrait"
        OrientationMode.LANDSCAPE -> "Landscape"
    }

    private fun AtariMachineType.toLabel(): String = when (this) {
        AtariMachineType.ATARI_400_800 -> "Atari 400/800"
        AtariMachineType.ATARI_1200XL -> "Atari 1200XL"
        AtariMachineType.ATARI_800XL -> "Atari 800XL"
        AtariMachineType.ATARI_130XE -> "Atari 130XE"
        AtariMachineType.ATARI_320XE_COMPY -> "Atari 320XE (Compy-Shop)"
        AtariMachineType.ATARI_320XE_RAMBO -> "Atari 320XE (Rambo)"
        AtariMachineType.ATARI_576XE -> "Atari 576XE"
        AtariMachineType.ATARI_1088XE -> "Atari 1088XE"
        AtariMachineType.ATARI_XEGS -> "Atari XEGS"
        AtariMachineType.ATARI_5200 -> "Atari 5200"
    }

    private fun MemoryProfile.toLabel(): String = when (this) {
        MemoryProfile.RAM_16 -> "16 KB"
        MemoryProfile.RAM_48 -> "48 KB"
        MemoryProfile.RAM_52 -> "52 KB"
        MemoryProfile.RAM_64 -> "64 KB"
        MemoryProfile.RAM_128 -> "128 KB"
        MemoryProfile.RAM_320 -> "320 KB"
        MemoryProfile.RAM_576 -> "576 KB"
        MemoryProfile.RAM_1088 -> "1088 KB"
    }

    private fun KeyboardInputMode.toLabel(): String = when (this) {
        KeyboardInputMode.ANDROID -> "Android keyboard"
        KeyboardInputMode.INTERNAL -> "Internal keyboard"
    }

    private fun JoystickInputStyle.toLabel(): String = when (this) {
        JoystickInputStyle.STICK_8_WAY -> "8-way stick"
        JoystickInputStyle.DPAD_4_WAY -> "4-way D-pad"
    }

    private fun VideoStandard.toLabel(): String = when (this) {
        VideoStandard.NTSC -> "NTSC"
        VideoStandard.PAL -> "PAL"
    }

    private fun SioPatchMode.toLabel(): String = when (this) {
        SioPatchMode.ENHANCED -> "Enhanced"
        SioPatchMode.NO_SIO_PATCH -> "No SIO patch"
        SioPatchMode.NO_PATCH_ALL -> "No patch all"
    }

    private fun ArtifactingMode.toLabel(): String = when (this) {
        ArtifactingMode.OFF -> "Off"
        ArtifactingMode.NTSC_OLD -> "NTSC old"
        ArtifactingMode.NTSC_NEW -> "NTSC new"
        ArtifactingMode.NTSC_FULL -> "NTSC full"
        ArtifactingMode.PAL_SIMPLE -> "PAL simple"
        ArtifactingMode.PAL_BLEND -> "PAL blend"
    }

    private fun NtscFilterPreset.toLabel(): String = when (this) {
        NtscFilterPreset.COMPOSITE -> "Composite"
        NtscFilterPreset.SVIDEO -> "S-Video"
        NtscFilterPreset.RGB -> "RGB"
        NtscFilterPreset.MONOCHROME -> "Monochrome"
        NtscFilterPreset.CUSTOM -> "Custom"
    }

    private fun NtscFilterPreset.toSettings(): NtscFilterSettings = when (this) {
        NtscFilterPreset.COMPOSITE -> NtscFilterSettings(
            preset = this,
            sharpness = -0.5f,
            resolution = -0.1f,
            artifacts = 0f,
            fringing = 0f,
            bleed = 0f,
            burstPhase = 0f,
        )
        NtscFilterPreset.SVIDEO -> NtscFilterSettings(
            preset = this,
            sharpness = -0.3f,
            resolution = 0.2f,
            artifacts = -1f,
            fringing = -1f,
            bleed = 0f,
            burstPhase = 0f,
        )
        NtscFilterPreset.RGB -> NtscFilterSettings(
            preset = this,
            sharpness = -0.3f,
            resolution = 0.7f,
            artifacts = -1f,
            fringing = -1f,
            bleed = -1f,
            burstPhase = 0f,
        )
        NtscFilterPreset.MONOCHROME -> NtscFilterSettings(
            preset = this,
            sharpness = -0.3f,
            resolution = 0.2f,
            artifacts = -0.2f,
            fringing = -0.2f,
            bleed = -1f,
            burstPhase = 0f,
        )
        NtscFilterPreset.CUSTOM -> editableSettings.value.ntscFilter.copy(preset = NtscFilterPreset.CUSTOM)
    }

    private fun Float.toSliderLabel(): String = String.format("%.2f", this)

    private fun Float.coerceToNtscFilterRange(): Float = coerceIn(-1f, 1f)

    private fun String?.toRomLabel(importedPath: String?, defaultLabel: String): String {
        return this
            ?.takeIf { it.isNotBlank() }
            ?: importedPath
            ?.substringAfterLast('/')
            ?.removePrefix("xlxe-")
            ?.removePrefix("basic-")
            ?.removePrefix("a800-")
            ?.takeIf { it.isNotBlank() }
            ?: defaultLabel
    }

    private fun String?.toFolderLabel(): String {
        return this
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: "Not set"
    }

    private fun Boolean.toWakeLabel(): String = if (this) "On" else "Off"

    private fun Int.toPercentLabel(): String = "${coerceIn(0, 100)}%"

    private fun Boolean.toWakeActionLabel(): String = if (this) "Turn off" else "Turn on"

    private fun Boolean.toEnabledLabel(): String = if (this) "Enabled" else "Disabled"

    private fun requiresRestart(previous: EmulatorSettings, current: EmulatorSettings): Boolean {
        return previous.launchMode != current.launchMode ||
            previous.machineType != current.machineType ||
            previous.memoryProfile != current.memoryProfile ||
            previous.basicEnabled != current.basicEnabled ||
            previous.sioPatchMode != current.sioPatchMode ||
            previous.stereoPokeyEnabled != current.stereoPokeyEnabled ||
            previous.hDevice1Path != current.hDevice1Path ||
            previous.hDevice2Path != current.hDevice2Path ||
            previous.hDevice3Path != current.hDevice3Path ||
            previous.hDevice4Path != current.hDevice4Path
    }

    companion object {
        fun provideFactory(
            settingsRepository: EmulatorSettingsRepository,
            sessionRepository: SessionRepository,
            runtimePaths: RuntimePaths,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LaunchSettingsViewModel(
                    settingsRepository = settingsRepository,
                    sessionRepository = sessionRepository,
                    runtimePaths = runtimePaths,
                    systemRomDocumentStore = SystemRomDocumentStore(runtimePaths),
                )
            }
        }
    }
}

fun FujiNetBootMode.toLabel(): String = when (this) {
    FujiNetBootMode.CONFIG -> "Config"
    FujiNetBootMode.MOUNT_ALL -> "Mount all"
}

fun FujiNetPrinterModel.toLabel(): String = label.ifBlank { value.ifBlank { "Default" } }

private fun RuntimePaths.toFujiNetStorageModeLabel(): String {
    return if (fujiNetUsesVisibleStorage) {
        "App-specific external storage"
    } else {
        "Private app storage fallback"
    }
}
