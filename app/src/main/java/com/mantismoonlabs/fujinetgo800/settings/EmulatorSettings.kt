package com.mantismoonlabs.fujinetgo800.settings

enum class LaunchMode {
    FUJINET_ENABLED,
    LOCAL_ONLY,
}

enum class ScaleMode {
    FIT,
    FILL,
    INTEGER,
}

enum class OrientationMode {
    FOLLOW_SYSTEM,
    PORTRAIT,
    LANDSCAPE,
}

enum class ControlMode {
    KEYBOARD,
    JOYSTICK,
}

enum class KeyboardInputMode {
    ANDROID,
    INTERNAL,
}

enum class JoystickInputStyle {
    STICK_8_WAY,
    DPAD_4_WAY,
}

enum class VideoStandard {
    NTSC,
    PAL,
}

enum class SioPatchMode {
    ENHANCED,
    NO_SIO_PATCH,
    NO_PATCH_ALL,
}

enum class ArtifactingMode {
    OFF,
    NTSC_OLD,
    NTSC_NEW,
    NTSC_FULL,
    PAL_SIMPLE,
    PAL_BLEND,
}

enum class NtscFilterPreset {
    COMPOSITE,
    SVIDEO,
    RGB,
    MONOCHROME,
    CUSTOM,
}

enum class AtariMachineType {
    ATARI_400_800,
    ATARI_1200XL,
    ATARI_800XL,
    ATARI_130XE,
    ATARI_320XE_COMPY,
    ATARI_320XE_RAMBO,
    ATARI_576XE,
    ATARI_1088XE,
    ATARI_XEGS,
    ATARI_5200,
}

enum class MemoryProfile {
    RAM_16,
    RAM_48,
    RAM_52,
    RAM_64,
    RAM_128,
    RAM_320,
    RAM_576,
    RAM_1088,
}

enum class SystemRomKind {
    XL_XE,
    BASIC,
    ATARI_400_800,
}

data class NtscFilterSettings(
    val preset: NtscFilterPreset = NtscFilterPreset.COMPOSITE,
    val sharpness: Float = -0.5f,
    val resolution: Float = -0.1f,
    val artifacts: Float = 0f,
    val fringing: Float = 0f,
    val bleed: Float = 0f,
    val burstPhase: Float = 0f,
)

data class EmulatorSettings(
    val launchMode: LaunchMode = LaunchMode.FUJINET_ENABLED,
    val scaleMode: ScaleMode = ScaleMode.FIT,
    val emulatorVolumePercent: Int = 35,
    val keepScreenOn: Boolean = true,
    val backgroundAudioEnabled: Boolean = false,
    val pauseOnAppSwitch: Boolean = false,
    val orientationMode: OrientationMode = OrientationMode.FOLLOW_SYSTEM,
    val turboEnabled: Boolean = false,
    val machineType: AtariMachineType = AtariMachineType.ATARI_800XL,
    val memoryProfile: MemoryProfile = MemoryProfile.RAM_64,
    val basicEnabled: Boolean = true,
    val sioPatchMode: SioPatchMode = SioPatchMode.ENHANCED,
    val artifactingMode: ArtifactingMode = ArtifactingMode.OFF,
    val scanlinesEnabled: Boolean = false,
    val stereoPokeyEnabled: Boolean = false,
    val hDevice1Path: String? = null,
    val hDevice2Path: String? = null,
    val hDevice3Path: String? = null,
    val hDevice4Path: String? = null,
    val controlMode: ControlMode = ControlMode.KEYBOARD,
    val inputPanelVisible: Boolean = true,
    val inputHideHintSeen: Boolean = false,
    val portraitInputPanelSizeFraction: Float = 1f,
    val keyboardInputMode: KeyboardInputMode = KeyboardInputMode.INTERNAL,
    val keyboardHapticsEnabled: Boolean = true,
    val stickyKeyboardShiftEnabled: Boolean = false,
    val stickyKeyboardCtrlEnabled: Boolean = false,
    val stickyKeyboardFnEnabled: Boolean = false,
    val joystickHapticsEnabled: Boolean = true,
    val joystickInputStyle: JoystickInputStyle = JoystickInputStyle.STICK_8_WAY,
    val videoStandard: VideoStandard = VideoStandard.NTSC,
    val ntscFilter: NtscFilterSettings = NtscFilterSettings(),
    val xlxeRomPath: String? = null,
    val basicRomPath: String? = null,
    val atari400800RomPath: String? = null,
)

fun MemoryProfile.isValidFor(machineType: AtariMachineType): Boolean = when (machineType) {
    AtariMachineType.ATARI_400_800 -> this == MemoryProfile.RAM_48 || this == MemoryProfile.RAM_52
    AtariMachineType.ATARI_1200XL -> this == MemoryProfile.RAM_64
    AtariMachineType.ATARI_800XL -> this == MemoryProfile.RAM_64
    AtariMachineType.ATARI_130XE -> this == MemoryProfile.RAM_128
    AtariMachineType.ATARI_320XE_COMPY,
    AtariMachineType.ATARI_320XE_RAMBO -> this == MemoryProfile.RAM_320
    AtariMachineType.ATARI_576XE -> this == MemoryProfile.RAM_576
    AtariMachineType.ATARI_1088XE -> this == MemoryProfile.RAM_1088
    AtariMachineType.ATARI_XEGS -> this == MemoryProfile.RAM_64
    AtariMachineType.ATARI_5200 -> this == MemoryProfile.RAM_16
}

fun AtariMachineType.defaultMemoryProfile(): MemoryProfile = when (this) {
    AtariMachineType.ATARI_400_800 -> MemoryProfile.RAM_48
    AtariMachineType.ATARI_1200XL -> MemoryProfile.RAM_64
    AtariMachineType.ATARI_800XL -> MemoryProfile.RAM_64
    AtariMachineType.ATARI_130XE -> MemoryProfile.RAM_128
    AtariMachineType.ATARI_320XE_COMPY,
    AtariMachineType.ATARI_320XE_RAMBO -> MemoryProfile.RAM_320
    AtariMachineType.ATARI_576XE -> MemoryProfile.RAM_576
    AtariMachineType.ATARI_1088XE -> MemoryProfile.RAM_1088
    AtariMachineType.ATARI_XEGS -> MemoryProfile.RAM_64
    AtariMachineType.ATARI_5200 -> MemoryProfile.RAM_16
}

fun AtariMachineType.validMemoryProfiles(): List<MemoryProfile> = when (this) {
    AtariMachineType.ATARI_400_800 -> listOf(
        MemoryProfile.RAM_48,
        MemoryProfile.RAM_52,
    )
    AtariMachineType.ATARI_1200XL -> listOf(MemoryProfile.RAM_64)
    AtariMachineType.ATARI_800XL -> listOf(MemoryProfile.RAM_64)
    AtariMachineType.ATARI_130XE -> listOf(MemoryProfile.RAM_128)
    AtariMachineType.ATARI_320XE_COMPY,
    AtariMachineType.ATARI_320XE_RAMBO -> listOf(MemoryProfile.RAM_320)
    AtariMachineType.ATARI_576XE -> listOf(MemoryProfile.RAM_576)
    AtariMachineType.ATARI_1088XE -> listOf(MemoryProfile.RAM_1088)
    AtariMachineType.ATARI_XEGS -> listOf(MemoryProfile.RAM_64)
    AtariMachineType.ATARI_5200 -> listOf(MemoryProfile.RAM_16)
}

fun EmulatorSettings.normalizedMachineMemory(): EmulatorSettings {
    return if (memoryProfile.isValidFor(machineType)) {
        this
    } else {
        copy(memoryProfile = machineType.defaultMemoryProfile())
    }
}
