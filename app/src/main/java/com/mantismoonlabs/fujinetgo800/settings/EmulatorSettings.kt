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

enum class KoalaPadShortcutKey {
    SPACE,
    RETURN,
    ESCAPE,
    TAB,
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H,
    I,
    J,
    K,
    L,
    M,
    N,
    O,
    P,
    Q,
    R,
    S,
    T,
    U,
    V,
    W,
    X,
    Y,
    Z,
    NUM_0,
    NUM_1,
    NUM_2,
    NUM_3,
    NUM_4,
    NUM_5,
    NUM_6,
    NUM_7,
    NUM_8,
    NUM_9,
}

enum class JoystickPort(val index: Int) {
    PORT_1(0),
    PORT_2(1),
    PORT_3(2),
    PORT_4(3),
}

enum class PortInputDevice {
    NONE,
    TOUCHSCREEN_JOYSTICK,
    BLUETOOTH_JOYSTICK,
    USB_JOYSTICK,
    ATARI_ST_MOUSE,
    AMIGA_MOUSE,
    PADDLE,
    KOALA_PAD,
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
    val machineType: AtariMachineType = AtariMachineType.ATARI_130XE,
    val memoryProfile: MemoryProfile = MemoryProfile.RAM_128,
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
    val landscapeControlsFullscreenHidden: Boolean = false,
    val inputHideHintSeen: Boolean = false,
    val portraitInputPanelSizeFraction: Float = 1f,
    val keyboardInputMode: KeyboardInputMode = KeyboardInputMode.INTERNAL,
    val keyboardHapticsEnabled: Boolean = true,
    val stickyKeyboardShiftEnabled: Boolean = false,
    val stickyKeyboardCtrlEnabled: Boolean = false,
    val stickyKeyboardFnEnabled: Boolean = false,
    val joystickHapticsEnabled: Boolean = true,
    val joystickInputStyle: JoystickInputStyle = JoystickInputStyle.STICK_8_WAY,
    val port1InputDevice: PortInputDevice = PortInputDevice.TOUCHSCREEN_JOYSTICK,
    val port2InputDevice: PortInputDevice = PortInputDevice.NONE,
    val port3InputDevice: PortInputDevice = PortInputDevice.NONE,
    val port4InputDevice: PortInputDevice = PortInputDevice.NONE,
    val port1HardwareControllerId: String? = null,
    val port2HardwareControllerId: String? = null,
    val port3HardwareControllerId: String? = null,
    val port4HardwareControllerId: String? = null,
    val port1HardwareControllerName: String? = null,
    val port2HardwareControllerName: String? = null,
    val port3HardwareControllerName: String? = null,
    val port4HardwareControllerName: String? = null,
    val mouseSpeed: Int = 3,
    val touchscreenMouseSensitivity: Float = 1.5f,
    val paddlePotMinimum: Int = 95,
    val koalaPadShortcutKey: KoalaPadShortcutKey = KoalaPadShortcutKey.SPACE,
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

fun EmulatorSettings.inputDeviceFor(port: JoystickPort): PortInputDevice = when (port) {
    JoystickPort.PORT_1 -> port1InputDevice
    JoystickPort.PORT_2 -> port2InputDevice
    JoystickPort.PORT_3 -> port3InputDevice
    JoystickPort.PORT_4 -> port4InputDevice
}

fun EmulatorSettings.hardwareControllerIdFor(port: JoystickPort): String? = when (port) {
    JoystickPort.PORT_1 -> port1HardwareControllerId
    JoystickPort.PORT_2 -> port2HardwareControllerId
    JoystickPort.PORT_3 -> port3HardwareControllerId
    JoystickPort.PORT_4 -> port4HardwareControllerId
}

fun EmulatorSettings.hardwareControllerNameFor(port: JoystickPort): String? = when (port) {
    JoystickPort.PORT_1 -> port1HardwareControllerName
    JoystickPort.PORT_2 -> port2HardwareControllerName
    JoystickPort.PORT_3 -> port3HardwareControllerName
    JoystickPort.PORT_4 -> port4HardwareControllerName
}

fun EmulatorSettings.withInputDeviceFor(port: JoystickPort, device: PortInputDevice): EmulatorSettings {
    var updated = this
    if (
        device == PortInputDevice.TOUCHSCREEN_JOYSTICK ||
        device == PortInputDevice.PADDLE ||
        device == PortInputDevice.KOALA_PAD
    ) {
        JoystickPort.entries.forEach { existingPort ->
            if (
                updated.inputDeviceFor(existingPort) == PortInputDevice.TOUCHSCREEN_JOYSTICK ||
                updated.inputDeviceFor(existingPort) == PortInputDevice.PADDLE ||
                updated.inputDeviceFor(existingPort) == PortInputDevice.KOALA_PAD ||
                (device == PortInputDevice.KOALA_PAD && updated.inputDeviceFor(existingPort).isMouse)
            ) {
                updated = updated.setInputDeviceForPort(existingPort, PortInputDevice.NONE)
            }
        }
    } else if (device.isMouse) {
        JoystickPort.entries.forEach { existingPort ->
            val existingDevice = updated.inputDeviceFor(existingPort)
            if (existingDevice.isMouse || existingDevice == PortInputDevice.KOALA_PAD) {
                updated = updated.setInputDeviceForPort(existingPort, PortInputDevice.NONE)
            }
        }
    }
    return when (port) {
        JoystickPort.PORT_1 -> updated.copy(port1InputDevice = device)
        JoystickPort.PORT_2 -> updated.copy(port2InputDevice = device)
        JoystickPort.PORT_3 -> updated.copy(port3InputDevice = device)
        JoystickPort.PORT_4 -> updated.copy(port4InputDevice = device)
    }.clearHardwareControllerForPortIfNeeded(port, device).normalizedInputPorts()
}

fun EmulatorSettings.withHardwareControllerFor(
    port: JoystickPort,
    device: PortInputDevice,
    controllerId: String,
    controllerName: String,
): EmulatorSettings {
    var updated = withInputDeviceFor(port, device)
    JoystickPort.entries.forEach { existingPort ->
        if (existingPort != port && updated.hardwareControllerIdFor(existingPort) == controllerId) {
            updated = updated.setHardwareControllerForPort(existingPort, null, null)
            if (updated.inputDeviceFor(existingPort) == PortInputDevice.BLUETOOTH_JOYSTICK ||
                updated.inputDeviceFor(existingPort) == PortInputDevice.USB_JOYSTICK
            ) {
                updated = updated.setInputDeviceForPort(existingPort, PortInputDevice.NONE)
            }
        }
    }
    return updated
        .setInputDeviceForPort(port, device)
        .setHardwareControllerForPort(port, controllerId, controllerName)
        .normalizedInputPorts()
}

fun EmulatorSettings.normalizedInputPorts(): EmulatorSettings {
    var normalized = this.copy(
        mouseSpeed = mouseSpeed.coerceIn(1, 9),
        touchscreenMouseSensitivity = touchscreenMouseSensitivity.coerceIn(0.25f, 4f),
        paddlePotMinimum = paddlePotMinimum.coerceIn(0, 228),
    )
    var touchscreenSeen = false
    var mouseSeen = false
    var koalaSeen = false
    JoystickPort.entries.forEach { port ->
        val device = normalized.inputDeviceFor(port)
        val replacement = when {
            (
                device == PortInputDevice.TOUCHSCREEN_JOYSTICK ||
                device == PortInputDevice.PADDLE ||
                device == PortInputDevice.KOALA_PAD
                ) && touchscreenSeen ->
                PortInputDevice.NONE
            device == PortInputDevice.KOALA_PAD && mouseSeen -> PortInputDevice.NONE
            device == PortInputDevice.TOUCHSCREEN_JOYSTICK ||
                device == PortInputDevice.PADDLE ||
                device == PortInputDevice.KOALA_PAD -> {
                touchscreenSeen = true
                if (device == PortInputDevice.KOALA_PAD) {
                    koalaSeen = true
                }
                device
            }
            device.isMouse && koalaSeen -> PortInputDevice.NONE
            (device.isMouse || device == PortInputDevice.KOALA_PAD) && mouseSeen -> PortInputDevice.NONE
            device.isMouse || device == PortInputDevice.KOALA_PAD -> {
                mouseSeen = true
                device
            }
            else -> device
        }
        if (replacement != device) {
            normalized = normalized.setInputDeviceForPort(port, replacement)
        }
        if (replacement != PortInputDevice.BLUETOOTH_JOYSTICK && replacement != PortInputDevice.USB_JOYSTICK) {
            normalized = normalized.setHardwareControllerForPort(port, null, null)
        }
    }
    return normalized
}

fun EmulatorSettings.touchscreenJoystickPort(): JoystickPort? {
    return JoystickPort.entries.firstOrNull { port ->
        inputDeviceFor(port) == PortInputDevice.TOUCHSCREEN_JOYSTICK
    }
}

fun EmulatorSettings.paddlePort(): JoystickPort? {
    return JoystickPort.entries.firstOrNull { port ->
        inputDeviceFor(port) == PortInputDevice.PADDLE
    }
}

fun EmulatorSettings.hardwareJoystickPort(): JoystickPort? {
    return JoystickPort.entries.firstOrNull { port ->
        val device = inputDeviceFor(port)
        device == PortInputDevice.BLUETOOTH_JOYSTICK || device == PortInputDevice.USB_JOYSTICK
    }
}

fun EmulatorSettings.mousePort(): JoystickPort? {
    return JoystickPort.entries.firstOrNull { port -> inputDeviceFor(port).isMouse }
}

fun EmulatorSettings.mouseDevice(): PortInputDevice? {
    return mousePort()?.let(::inputDeviceFor)
}

fun EmulatorSettings.koalaPadPort(): JoystickPort? {
    return JoystickPort.entries.firstOrNull { port ->
        inputDeviceFor(port) == PortInputDevice.KOALA_PAD
    }
}

val PortInputDevice.isMouse: Boolean
    get() = this == PortInputDevice.ATARI_ST_MOUSE || this == PortInputDevice.AMIGA_MOUSE

private fun EmulatorSettings.setInputDeviceForPort(
    port: JoystickPort,
    device: PortInputDevice,
): EmulatorSettings = when (port) {
    JoystickPort.PORT_1 -> copy(port1InputDevice = device)
    JoystickPort.PORT_2 -> copy(port2InputDevice = device)
    JoystickPort.PORT_3 -> copy(port3InputDevice = device)
    JoystickPort.PORT_4 -> copy(port4InputDevice = device)
}

private fun EmulatorSettings.clearHardwareControllerForPortIfNeeded(
    port: JoystickPort,
    device: PortInputDevice,
): EmulatorSettings {
    return if (device == PortInputDevice.BLUETOOTH_JOYSTICK || device == PortInputDevice.USB_JOYSTICK) {
        this
    } else {
        setHardwareControllerForPort(port, null, null)
    }
}

private fun EmulatorSettings.setHardwareControllerForPort(
    port: JoystickPort,
    controllerId: String?,
    controllerName: String?,
): EmulatorSettings = when (port) {
    JoystickPort.PORT_1 -> copy(port1HardwareControllerId = controllerId, port1HardwareControllerName = controllerName)
    JoystickPort.PORT_2 -> copy(port2HardwareControllerId = controllerId, port2HardwareControllerName = controllerName)
    JoystickPort.PORT_3 -> copy(port3HardwareControllerId = controllerId, port3HardwareControllerName = controllerName)
    JoystickPort.PORT_4 -> copy(port4HardwareControllerId = controllerId, port4HardwareControllerName = controllerName)
}
