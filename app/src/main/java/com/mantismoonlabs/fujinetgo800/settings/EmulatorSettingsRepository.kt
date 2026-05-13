package com.mantismoonlabs.fujinetgo800.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.emulatorSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "emulator_settings",
)

internal object EmulatorSettingsPreferenceKeys {
    val launchMode = stringPreferencesKey("launch_mode")
    val scaleMode = stringPreferencesKey("scale_mode")
    val emulatorVolumePercent = intPreferencesKey("emulator_volume_percent")
    val keepScreenOn = booleanPreferencesKey("keep_screen_on")
    val backgroundAudioEnabled = booleanPreferencesKey("background_audio_enabled")
    val pauseOnAppSwitch = booleanPreferencesKey("pause_on_app_switch")
    val orientationMode = stringPreferencesKey("orientation_mode")
    val turboEnabled = booleanPreferencesKey("turbo_enabled")
    val machineType = stringPreferencesKey("machine_type")
    val memoryProfile = stringPreferencesKey("memory_profile")
    val basicEnabled = booleanPreferencesKey("basic_enabled")
    val sioPatchMode = stringPreferencesKey("sio_patch_mode")
    val artifactingMode = stringPreferencesKey("artifacting_mode")
    val scanlinesEnabled = booleanPreferencesKey("scanlines_enabled")
    val stereoPokeyEnabled = booleanPreferencesKey("stereo_pokey_enabled")
    val hDevice1Path = stringPreferencesKey("h_device_1_path")
    val hDevice2Path = stringPreferencesKey("h_device_2_path")
    val hDevice3Path = stringPreferencesKey("h_device_3_path")
    val hDevice4Path = stringPreferencesKey("h_device_4_path")
    val controlMode = stringPreferencesKey("control_mode")
    val inputPanelVisible = booleanPreferencesKey("input_panel_visible")
    val landscapeControlsFullscreenHidden = booleanPreferencesKey("landscape_controls_fullscreen_hidden")
    val inputHideHintSeen = booleanPreferencesKey("input_hide_hint_seen_v2")
    val portraitInputPanelSizeFraction = floatPreferencesKey("portrait_input_panel_size_fraction")
    val keyboardInputMode = stringPreferencesKey("keyboard_input_mode")
    val keyboardHapticsEnabled = booleanPreferencesKey("keyboard_haptics_enabled")
    val stickyKeyboardShiftEnabled = booleanPreferencesKey("sticky_keyboard_shift_enabled")
    val stickyKeyboardCtrlEnabled = booleanPreferencesKey("sticky_keyboard_ctrl_enabled")
    val stickyKeyboardFnEnabled = booleanPreferencesKey("sticky_keyboard_fn_enabled")
    val joystickHapticsEnabled = booleanPreferencesKey("joystick_haptics_enabled")
    val joystickInputStyle = stringPreferencesKey("joystick_input_style")
    val port1InputDevice = stringPreferencesKey("port_1_input_device")
    val port2InputDevice = stringPreferencesKey("port_2_input_device")
    val port3InputDevice = stringPreferencesKey("port_3_input_device")
    val port4InputDevice = stringPreferencesKey("port_4_input_device")
    val port1HardwareControllerId = stringPreferencesKey("port_1_hardware_controller_id")
    val port2HardwareControllerId = stringPreferencesKey("port_2_hardware_controller_id")
    val port3HardwareControllerId = stringPreferencesKey("port_3_hardware_controller_id")
    val port4HardwareControllerId = stringPreferencesKey("port_4_hardware_controller_id")
    val port1HardwareControllerName = stringPreferencesKey("port_1_hardware_controller_name")
    val port2HardwareControllerName = stringPreferencesKey("port_2_hardware_controller_name")
    val port3HardwareControllerName = stringPreferencesKey("port_3_hardware_controller_name")
    val port4HardwareControllerName = stringPreferencesKey("port_4_hardware_controller_name")
    val mouseSpeed = intPreferencesKey("mouse_speed")
    val touchscreenMouseSensitivity = floatPreferencesKey("touchscreen_mouse_sensitivity")
    val paddlePotMinimum = intPreferencesKey("paddle_pot_minimum")
    val koalaPadShortcutKey = stringPreferencesKey("koala_pad_shortcut_key")
    val videoStandard = stringPreferencesKey("video_standard")
    val ntscFilterPreset = stringPreferencesKey("ntsc_filter_preset")
    val ntscFilterSharpness = floatPreferencesKey("ntsc_filter_sharpness")
    val ntscFilterResolution = floatPreferencesKey("ntsc_filter_resolution")
    val ntscFilterArtifacts = floatPreferencesKey("ntsc_filter_artifacts")
    val ntscFilterFringing = floatPreferencesKey("ntsc_filter_fringing")
    val ntscFilterBleed = floatPreferencesKey("ntsc_filter_bleed")
    val ntscFilterBurstPhase = floatPreferencesKey("ntsc_filter_burst_phase")
    val xlxeRomPath = stringPreferencesKey("xlxe_rom_path")
    val basicRomPath = stringPreferencesKey("basic_rom_path")
    val atari400800RomPath = stringPreferencesKey("atari_400_800_rom_path")
    val notificationPermissionEducationSeen = booleanPreferencesKey("notification_permission_education_seen")
}

class EmulatorSettingsRepository private constructor(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.emulatorSettingsDataStore)

    val settings: Flow<EmulatorSettings> = dataStore.data.map { preferences ->
        preferences.toEmulatorSettings()
    }

    suspend fun updateLaunchMode(launchMode: LaunchMode) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.launchMode] = LaunchMode.FUJINET_ENABLED.name
        }
    }

    suspend fun updateScaleMode(scaleMode: ScaleMode) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.scaleMode] = scaleMode.name
        }
    }

    suspend fun updateEmulatorVolumePercent(emulatorVolumePercent: Int) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.emulatorVolumePercent] = emulatorVolumePercent.coerceIn(0, 100)
        }
    }

    suspend fun updateKeepScreenOn(keepScreenOn: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.keepScreenOn] = keepScreenOn
        }
    }

    suspend fun updateBackgroundAudioEnabled(backgroundAudioEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.backgroundAudioEnabled] = backgroundAudioEnabled
        }
    }

    suspend fun updatePauseOnAppSwitch(pauseOnAppSwitch: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.pauseOnAppSwitch] = pauseOnAppSwitch
        }
    }

    suspend fun updateOrientationMode(orientationMode: OrientationMode) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.orientationMode] = orientationMode.name
        }
    }

    suspend fun updateTurboEnabled(turboEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.turboEnabled] = turboEnabled
        }
    }

    suspend fun updateMachineType(machineType: AtariMachineType) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.machineType] = machineType.name
        }
    }

    suspend fun updateMemoryProfile(memoryProfile: MemoryProfile) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.memoryProfile] = memoryProfile.name
        }
    }

    suspend fun updateBasicEnabled(basicEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.basicEnabled] = basicEnabled
        }
    }

    suspend fun updateSioPatchMode(sioPatchMode: SioPatchMode) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.sioPatchMode] = sioPatchMode.name
        }
    }

    suspend fun updateArtifactingMode(artifactingMode: ArtifactingMode) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.artifactingMode] = artifactingMode.name
        }
    }

    suspend fun updateScanlinesEnabled(scanlinesEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.scanlinesEnabled] = scanlinesEnabled
        }
    }

    suspend fun updateStereoPokeyEnabled(stereoPokeyEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.stereoPokeyEnabled] = stereoPokeyEnabled
        }
    }

    suspend fun updateHDevicePath(slot: Int, path: String?) {
        val key = when (slot) {
            1 -> EmulatorSettingsPreferenceKeys.hDevice1Path
            2 -> EmulatorSettingsPreferenceKeys.hDevice2Path
            3 -> EmulatorSettingsPreferenceKeys.hDevice3Path
            4 -> EmulatorSettingsPreferenceKeys.hDevice4Path
            else -> error("Unsupported H: device slot $slot")
        }
        dataStore.edit { preferences ->
            if (path.isNullOrBlank()) {
                preferences.remove(key)
            } else {
                preferences[key] = path
            }
        }
    }

    suspend fun updateControlMode(controlMode: ControlMode) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.controlMode] = controlMode.name
        }
    }

    suspend fun updateInputPanelVisible(visible: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.inputPanelVisible] = visible
        }
    }

    suspend fun updateLandscapeControlsFullscreenHidden(hidden: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.landscapeControlsFullscreenHidden] = hidden
        }
    }

    suspend fun updateInputHideHintSeen(seen: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.inputHideHintSeen] = seen
        }
    }

    suspend fun updatePortraitInputPanelSizeFraction(fraction: Float) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.portraitInputPanelSizeFraction] = fraction.coerceIn(0f, 1f)
        }
    }

    suspend fun updateKeyboardInputMode(keyboardInputMode: KeyboardInputMode) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.keyboardInputMode] = keyboardInputMode.name
        }
    }

    suspend fun updateKeyboardHapticsEnabled(keyboardHapticsEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.keyboardHapticsEnabled] = keyboardHapticsEnabled
        }
    }

    suspend fun updateStickyKeyboardShiftEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.stickyKeyboardShiftEnabled] = enabled
        }
    }

    suspend fun updateStickyKeyboardCtrlEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.stickyKeyboardCtrlEnabled] = enabled
        }
    }

    suspend fun updateStickyKeyboardFnEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.stickyKeyboardFnEnabled] = enabled
        }
    }

    suspend fun updateJoystickHapticsEnabled(joystickHapticsEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.joystickHapticsEnabled] = joystickHapticsEnabled
        }
    }

    suspend fun updateJoystickInputStyle(joystickInputStyle: JoystickInputStyle) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.joystickInputStyle] = joystickInputStyle.name
        }
    }

    suspend fun updatePortInputDevice(port: JoystickPort, device: PortInputDevice) {
        dataStore.edit { preferences ->
            val settings = preferences.toEmulatorSettings()
                .withInputDeviceFor(port, device)
            preferences.writeInputPortSettings(settings)
        }
    }

    suspend fun updatePortHardwareController(
        port: JoystickPort,
        device: PortInputDevice,
        controllerId: String,
        controllerName: String,
    ) {
        dataStore.edit { preferences ->
            val settings = preferences.toEmulatorSettings()
                .withHardwareControllerFor(port, device, controllerId, controllerName)
            preferences.writeInputPortSettings(settings)
        }
    }

    suspend fun updateMouseSpeed(speed: Int) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.mouseSpeed] = speed.coerceIn(1, 9)
        }
    }

    suspend fun updateTouchscreenMouseSensitivity(sensitivity: Float) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.touchscreenMouseSensitivity] = sensitivity.coerceIn(0.25f, 4f)
        }
    }

    suspend fun updatePaddlePotMinimum(value: Int) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.paddlePotMinimum] = value.coerceIn(0, 228)
        }
    }

    suspend fun updateKoalaPadShortcutKey(key: KoalaPadShortcutKey) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.koalaPadShortcutKey] = key.name
        }
    }

    suspend fun updateVideoStandard(videoStandard: VideoStandard) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.videoStandard] = videoStandard.name
        }
    }

    suspend fun updateNtscFilterPreset(preset: NtscFilterPreset) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.ntscFilterPreset] = preset.name
        }
    }

    suspend fun updateNtscFilterSharpness(value: Float) {
        updateClampedFloatPreference(EmulatorSettingsPreferenceKeys.ntscFilterSharpness, value)
    }

    suspend fun updateNtscFilterResolution(value: Float) {
        updateClampedFloatPreference(EmulatorSettingsPreferenceKeys.ntscFilterResolution, value)
    }

    suspend fun updateNtscFilterArtifacts(value: Float) {
        updateClampedFloatPreference(EmulatorSettingsPreferenceKeys.ntscFilterArtifacts, value)
    }

    suspend fun updateNtscFilterFringing(value: Float) {
        updateClampedFloatPreference(EmulatorSettingsPreferenceKeys.ntscFilterFringing, value)
    }

    suspend fun updateNtscFilterBleed(value: Float) {
        updateClampedFloatPreference(EmulatorSettingsPreferenceKeys.ntscFilterBleed, value)
    }

    suspend fun updateNtscFilterBurstPhase(value: Float) {
        updateClampedFloatPreference(EmulatorSettingsPreferenceKeys.ntscFilterBurstPhase, value)
    }

    suspend fun updateXlxeRomPath(path: String?) {
        dataStore.edit { preferences ->
            if (path.isNullOrBlank()) {
                preferences.remove(EmulatorSettingsPreferenceKeys.xlxeRomPath)
            } else {
                preferences[EmulatorSettingsPreferenceKeys.xlxeRomPath] = path
            }
        }
    }

    suspend fun updateBasicRomPath(path: String?) {
        dataStore.edit { preferences ->
            if (path.isNullOrBlank()) {
                preferences.remove(EmulatorSettingsPreferenceKeys.basicRomPath)
            } else {
                preferences[EmulatorSettingsPreferenceKeys.basicRomPath] = path
            }
        }
    }

    suspend fun updateAtari400800RomPath(path: String?) {
        dataStore.edit { preferences ->
            if (path.isNullOrBlank()) {
                preferences.remove(EmulatorSettingsPreferenceKeys.atari400800RomPath)
            } else {
                preferences[EmulatorSettingsPreferenceKeys.atari400800RomPath] = path
            }
        }
    }

    suspend fun updateNotificationPermissionEducationSeen(seen: Boolean) {
        dataStore.edit { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.notificationPermissionEducationSeen] = seen
        }
    }

    suspend fun notificationPermissionEducationSeen(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[EmulatorSettingsPreferenceKeys.notificationPermissionEducationSeen] ?: false
        }.first()
    }

    suspend fun currentSettings(): EmulatorSettings = settings.first()

    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private suspend fun updateClampedFloatPreference(
        key: Preferences.Key<Float>,
        value: Float,
    ) {
        dataStore.edit { preferences ->
            preferences[key] = value.coerceIn(NTSC_FILTER_MIN, NTSC_FILTER_MAX)
        }
    }

    companion object {
        private const val NTSC_FILTER_MIN = -1f
        private const val NTSC_FILTER_MAX = 1f

        internal fun createForTest(
            produceFile: () -> File,
            scope: CoroutineScope,
        ): EmulatorSettingsRepository {
            return EmulatorSettingsRepository(
                dataStore = PreferenceDataStoreFactory.create(
                    scope = scope,
                    produceFile = produceFile,
                ),
            )
        }
    }
}

private fun Preferences.toEmulatorSettings(): EmulatorSettings {
    return EmulatorSettings(
        launchMode = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.launchMode,
            defaultValue = LaunchMode.FUJINET_ENABLED,
        ).let { LaunchMode.FUJINET_ENABLED },
        scaleMode = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.scaleMode,
            defaultValue = ScaleMode.FIT,
        ),
        emulatorVolumePercent = (this[EmulatorSettingsPreferenceKeys.emulatorVolumePercent] ?: 35).coerceIn(0, 100),
        keepScreenOn = this[EmulatorSettingsPreferenceKeys.keepScreenOn] ?: true,
        backgroundAudioEnabled = this[EmulatorSettingsPreferenceKeys.backgroundAudioEnabled] ?: false,
        pauseOnAppSwitch = this[EmulatorSettingsPreferenceKeys.pauseOnAppSwitch] ?: false,
        orientationMode = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.orientationMode,
            defaultValue = OrientationMode.FOLLOW_SYSTEM,
        ),
        turboEnabled = this[EmulatorSettingsPreferenceKeys.turboEnabled] ?: false,
        machineType = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.machineType,
            defaultValue = AtariMachineType.ATARI_130XE,
            legacyAliases = mapOf(
                "ATARI_800" to AtariMachineType.ATARI_400_800,
            ),
        ),
        memoryProfile = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.memoryProfile,
            defaultValue = MemoryProfile.RAM_128,
        ),
        basicEnabled = this[EmulatorSettingsPreferenceKeys.basicEnabled] ?: true,
        sioPatchMode = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.sioPatchMode,
            defaultValue = SioPatchMode.ENHANCED,
        ),
        artifactingMode = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.artifactingMode,
            defaultValue = ArtifactingMode.OFF,
        ),
        scanlinesEnabled = this[EmulatorSettingsPreferenceKeys.scanlinesEnabled] ?: false,
        stereoPokeyEnabled = this[EmulatorSettingsPreferenceKeys.stereoPokeyEnabled] ?: false,
        hDevice1Path = this[EmulatorSettingsPreferenceKeys.hDevice1Path],
        hDevice2Path = this[EmulatorSettingsPreferenceKeys.hDevice2Path],
        hDevice3Path = this[EmulatorSettingsPreferenceKeys.hDevice3Path],
        hDevice4Path = this[EmulatorSettingsPreferenceKeys.hDevice4Path],
        controlMode = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.controlMode,
            defaultValue = ControlMode.KEYBOARD,
        ),
        inputPanelVisible = this[EmulatorSettingsPreferenceKeys.inputPanelVisible] ?: true,
        landscapeControlsFullscreenHidden =
            this[EmulatorSettingsPreferenceKeys.landscapeControlsFullscreenHidden] ?: false,
        inputHideHintSeen = this[EmulatorSettingsPreferenceKeys.inputHideHintSeen] ?: false,
        portraitInputPanelSizeFraction =
            (this[EmulatorSettingsPreferenceKeys.portraitInputPanelSizeFraction] ?: 1f).coerceIn(0f, 1f),
        keyboardInputMode = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.keyboardInputMode,
            defaultValue = KeyboardInputMode.INTERNAL,
        ),
        keyboardHapticsEnabled = this[EmulatorSettingsPreferenceKeys.keyboardHapticsEnabled] ?: true,
        stickyKeyboardShiftEnabled = this[EmulatorSettingsPreferenceKeys.stickyKeyboardShiftEnabled] ?: false,
        stickyKeyboardCtrlEnabled = this[EmulatorSettingsPreferenceKeys.stickyKeyboardCtrlEnabled] ?: false,
        stickyKeyboardFnEnabled = this[EmulatorSettingsPreferenceKeys.stickyKeyboardFnEnabled] ?: false,
        joystickHapticsEnabled = this[EmulatorSettingsPreferenceKeys.joystickHapticsEnabled] ?: true,
        joystickInputStyle = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.joystickInputStyle,
            defaultValue = JoystickInputStyle.STICK_8_WAY,
        ),
        port1InputDevice = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.port1InputDevice,
            defaultValue = PortInputDevice.TOUCHSCREEN_JOYSTICK,
        ),
        port2InputDevice = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.port2InputDevice,
            defaultValue = PortInputDevice.NONE,
        ),
        port3InputDevice = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.port3InputDevice,
            defaultValue = PortInputDevice.NONE,
        ),
        port4InputDevice = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.port4InputDevice,
            defaultValue = PortInputDevice.NONE,
        ),
        port1HardwareControllerId = this[EmulatorSettingsPreferenceKeys.port1HardwareControllerId],
        port2HardwareControllerId = this[EmulatorSettingsPreferenceKeys.port2HardwareControllerId],
        port3HardwareControllerId = this[EmulatorSettingsPreferenceKeys.port3HardwareControllerId],
        port4HardwareControllerId = this[EmulatorSettingsPreferenceKeys.port4HardwareControllerId],
        port1HardwareControllerName = this[EmulatorSettingsPreferenceKeys.port1HardwareControllerName],
        port2HardwareControllerName = this[EmulatorSettingsPreferenceKeys.port2HardwareControllerName],
        port3HardwareControllerName = this[EmulatorSettingsPreferenceKeys.port3HardwareControllerName],
        port4HardwareControllerName = this[EmulatorSettingsPreferenceKeys.port4HardwareControllerName],
        mouseSpeed = (this[EmulatorSettingsPreferenceKeys.mouseSpeed] ?: 3).coerceIn(1, 9),
        touchscreenMouseSensitivity = (
            this[EmulatorSettingsPreferenceKeys.touchscreenMouseSensitivity] ?: 1.5f
            ).coerceIn(0.25f, 4f),
        paddlePotMinimum = (this[EmulatorSettingsPreferenceKeys.paddlePotMinimum] ?: 95).coerceIn(0, 228),
        koalaPadShortcutKey = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.koalaPadShortcutKey,
            defaultValue = KoalaPadShortcutKey.SPACE,
        ),
        videoStandard = getEnumOrDefault(
            key = EmulatorSettingsPreferenceKeys.videoStandard,
            defaultValue = VideoStandard.NTSC,
        ),
        ntscFilter = NtscFilterSettings(
            preset = getEnumOrDefault(
                key = EmulatorSettingsPreferenceKeys.ntscFilterPreset,
                defaultValue = NtscFilterPreset.COMPOSITE,
            ),
            sharpness = (this[EmulatorSettingsPreferenceKeys.ntscFilterSharpness] ?: -0.5f).coerceIn(-1f, 1f),
            resolution = (this[EmulatorSettingsPreferenceKeys.ntscFilterResolution] ?: -0.1f).coerceIn(-1f, 1f),
            artifacts = (this[EmulatorSettingsPreferenceKeys.ntscFilterArtifacts] ?: 0f).coerceIn(-1f, 1f),
            fringing = (this[EmulatorSettingsPreferenceKeys.ntscFilterFringing] ?: 0f).coerceIn(-1f, 1f),
            bleed = (this[EmulatorSettingsPreferenceKeys.ntscFilterBleed] ?: 0f).coerceIn(-1f, 1f),
            burstPhase = (this[EmulatorSettingsPreferenceKeys.ntscFilterBurstPhase] ?: 0f).coerceIn(-1f, 1f),
        ),
        xlxeRomPath = this[EmulatorSettingsPreferenceKeys.xlxeRomPath],
        basicRomPath = this[EmulatorSettingsPreferenceKeys.basicRomPath],
        atari400800RomPath = this[EmulatorSettingsPreferenceKeys.atari400800RomPath],
    ).normalizedInputPorts()
}

private fun MutablePreferences.writeInputPortSettings(settings: EmulatorSettings) {
    this[EmulatorSettingsPreferenceKeys.port1InputDevice] = settings.port1InputDevice.name
    this[EmulatorSettingsPreferenceKeys.port2InputDevice] = settings.port2InputDevice.name
    this[EmulatorSettingsPreferenceKeys.port3InputDevice] = settings.port3InputDevice.name
    this[EmulatorSettingsPreferenceKeys.port4InputDevice] = settings.port4InputDevice.name
    writeNullableString(EmulatorSettingsPreferenceKeys.port1HardwareControllerId, settings.port1HardwareControllerId)
    writeNullableString(EmulatorSettingsPreferenceKeys.port2HardwareControllerId, settings.port2HardwareControllerId)
    writeNullableString(EmulatorSettingsPreferenceKeys.port3HardwareControllerId, settings.port3HardwareControllerId)
    writeNullableString(EmulatorSettingsPreferenceKeys.port4HardwareControllerId, settings.port4HardwareControllerId)
    writeNullableString(EmulatorSettingsPreferenceKeys.port1HardwareControllerName, settings.port1HardwareControllerName)
    writeNullableString(EmulatorSettingsPreferenceKeys.port2HardwareControllerName, settings.port2HardwareControllerName)
    writeNullableString(EmulatorSettingsPreferenceKeys.port3HardwareControllerName, settings.port3HardwareControllerName)
    writeNullableString(EmulatorSettingsPreferenceKeys.port4HardwareControllerName, settings.port4HardwareControllerName)
}

private fun MutablePreferences.writeNullableString(
    key: Preferences.Key<String>,
    value: String?,
) {
    if (value.isNullOrBlank()) {
        remove(key)
    } else {
        this[key] = value
    }
}

private inline fun <reified T : Enum<T>> Preferences.getEnumOrDefault(
    key: Preferences.Key<String>,
    defaultValue: T,
    legacyAliases: Map<String, T> = emptyMap(),
): T {
    val storedValue = this[key] ?: return defaultValue
    legacyAliases[storedValue]?.let { return it }
    return enumValues<T>().firstOrNull { entry ->
        entry.name == storedValue
    } ?: defaultValue
}
