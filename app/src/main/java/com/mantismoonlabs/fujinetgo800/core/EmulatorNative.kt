package com.mantismoonlabs.fujinetgo800.core

import android.view.Surface
import com.mantismoonlabs.fujinetgo800.settings.AtariMachineType
import com.mantismoonlabs.fujinetgo800.settings.ArtifactingMode
import com.mantismoonlabs.fujinetgo800.settings.MemoryProfile
import com.mantismoonlabs.fujinetgo800.settings.NtscFilterSettings
import com.mantismoonlabs.fujinetgo800.settings.SioPatchMode

private object NativeLibraryLoader {
    val loaded: Unit by lazy {
        System.loadLibrary("ataricore")
    }
}

object EmulatorNative {

    init {
        NativeLibraryLoader.loaded
    }

    private external fun nativeStartSession(
        width: Int,
        height: Int,
        sampleRate: Int,
        enableFujiNet: Boolean,
        runtimeRootPath: String,
        machineType: Int,
        memorySizeKb: Int,
        basicEnabled: Boolean,
    )
    private external fun nativePauseSession(paused: Boolean)
    private external fun nativeAttachSurface(surface: Surface, width: Int, height: Int)
    private external fun nativeDetachSurface()
    private external fun nativeGetSessionToken(): Long
    private external fun nativeIsSessionAlive(sessionToken: Long): Boolean
    private external fun nativeMountDisk(path: String, driveNumber: Int): Boolean
    private external fun nativeEjectDisk(driveNumber: Int): Boolean
    private external fun nativeInsertCartridge(path: String): Boolean
    private external fun nativeRemoveCartridge(): Boolean
    private external fun nativeLoadExecutable(path: String): Boolean
    private external fun nativeSetCustomRomPath(path: String): Boolean
    private external fun nativeClearCustomRomPath(): Boolean
    private external fun nativeSetBasicRomPath(path: String): Boolean
    private external fun nativeClearBasicRomPath(): Boolean
    private external fun nativeSetAtari400800RomPath(path: String): Boolean
    private external fun nativeClearAtari400800RomPath(): Boolean
    private external fun nativeSetTurboEnabled(enabled: Boolean)
    private external fun nativeSetVideoStandard(isPal: Boolean)
    private external fun nativeSetSioPatchMode(mode: Int)
    private external fun nativeSetArtifactingMode(mode: Int)
    private external fun nativeSetNtscFilterConfig(
        preset: Int,
        sharpness: Float,
        resolution: Float,
        artifacts: Float,
        fringing: Float,
        bleed: Float,
        burstPhase: Float,
    )
    private external fun nativeSetStereoPokeyEnabled(enabled: Boolean)
    private external fun nativeSetHDevicePath(slot: Int, path: String?)
    external fun loadRom(romData: ByteArray): Boolean
    external fun loadCartridge(data: ByteArray): Boolean
    external fun loadXex(data: ByteArray): Boolean
    external fun loadAtr(data: ByteArray): Boolean
    external fun loadFile(path: String): Boolean

    /**
     * Renders one 60fps frame into the given direct ByteBuffer (RGBA8888).
     * The buffer size must be width * height * 4.
     */
    external fun renderFrame(frameBuffer: java.nio.ByteBuffer)

    // Audio – will be used with AudioTrack later
    external fun fillAudioBuffer(audioBuffer: ShortArray)

    // Gamepad stub – placeholder for future use
    external fun setGamepadState(
        playerIndex: Int,
        buttonsMask: Int,
        axisX: Float,
        axisY: Float
    )

    // System controls
    private external fun nativeResetSystem(notifyFujiNet: Boolean)
    private external fun nativeWarmResetSystem()

    // Input
    external fun setKeyState(aKeyCode: Int, pressed: Boolean)
    external fun setConsoleKeys(start: Boolean, select: Boolean, option: Boolean)
    external fun setJoystickState(port: Int, x: Float, y: Float, fire: Boolean)

    fun startSession(
        width: Int,
        height: Int,
        sampleRate: Int,
        enableFujiNet: Boolean = false,
        runtimeRootPath: String,
        machineType: AtariMachineType = AtariMachineType.ATARI_800XL,
        memoryProfile: MemoryProfile = MemoryProfile.RAM_64,
        basicEnabled: Boolean = false,
    ) {
        nativeStartSession(
            width = width,
            height = height,
            sampleRate = sampleRate,
            enableFujiNet = enableFujiNet,
            runtimeRootPath = runtimeRootPath,
            machineType = machineType.toNativeValue(),
            memorySizeKb = memoryProfile.toRamSizeKb(),
            basicEnabled = basicEnabled,
        )
        nativePauseSession(false)
    }

    fun pauseSession(paused: Boolean) {
        nativePauseSession(paused)
    }

    fun attachSurface(surface: Surface, width: Int, height: Int) {
        nativeAttachSurface(surface, width, height)
    }

    fun detachSurface() {
        nativeDetachSurface()
    }

    fun getSessionToken(): Long {
        return nativeGetSessionToken()
    }

    fun isSessionAlive(sessionToken: Long): Boolean {
        return nativeIsSessionAlive(sessionToken)
    }

    fun mountDisk(path: String, driveNumber: Int): Boolean {
        return nativeMountDisk(path, driveNumber)
    }

    fun ejectDisk(driveNumber: Int): Boolean {
        return nativeEjectDisk(driveNumber)
    }

    fun insertCartridge(path: String): Boolean {
        return nativeInsertCartridge(path)
    }

    fun removeCartridge(): Boolean {
        return nativeRemoveCartridge()
    }

    fun loadExecutable(path: String): Boolean {
        return nativeLoadExecutable(path)
    }

    fun setCustomRomPath(path: String): Boolean {
        return nativeSetCustomRomPath(path)
    }

    fun clearCustomRomPath(): Boolean {
        return nativeClearCustomRomPath()
    }

    fun setBasicRomPath(path: String): Boolean {
        return nativeSetBasicRomPath(path)
    }

    fun clearBasicRomPath(): Boolean {
        return nativeClearBasicRomPath()
    }

    fun setAtari400800RomPath(path: String): Boolean {
        return nativeSetAtari400800RomPath(path)
    }

    fun clearAtari400800RomPath(): Boolean {
        return nativeClearAtari400800RomPath()
    }

    fun setTurboEnabled(enabled: Boolean) {
        nativeSetTurboEnabled(enabled)
    }

    fun setVideoStandard(isPal: Boolean) {
        nativeSetVideoStandard(isPal)
    }

    fun setSioPatchMode(mode: SioPatchMode) {
        nativeSetSioPatchMode(mode.toNativeValue())
    }

    fun setArtifactingMode(mode: ArtifactingMode) {
        nativeSetArtifactingMode(mode.toNativeValue())
    }

    fun setNtscFilterConfig(settings: NtscFilterSettings) {
        nativeSetNtscFilterConfig(
            preset = settings.preset.toNativeValue(),
            sharpness = settings.sharpness,
            resolution = settings.resolution,
            artifacts = settings.artifacts,
            fringing = settings.fringing,
            bleed = settings.bleed,
            burstPhase = settings.burstPhase,
        )
    }

    fun setStereoPokeyEnabled(enabled: Boolean) {
        nativeSetStereoPokeyEnabled(enabled)
    }

    fun setHDevicePath(slot: Int, path: String?) {
        nativeSetHDevicePath(slot, path)
    }

    fun resetSystem(notifyFujiNet: Boolean = true) {
        nativeResetSystem(notifyFujiNet)
    }

    fun warmResetSystem() {
        nativeWarmResetSystem()
    }

    private fun AtariMachineType.toNativeValue(): Int = when (this) {
        AtariMachineType.ATARI_400_800 -> 0
        AtariMachineType.ATARI_1200XL -> 1
        AtariMachineType.ATARI_800XL -> 2
        AtariMachineType.ATARI_130XE -> 3
        AtariMachineType.ATARI_320XE_COMPY -> 4
        AtariMachineType.ATARI_320XE_RAMBO -> 5
        AtariMachineType.ATARI_576XE -> 6
        AtariMachineType.ATARI_1088XE -> 7
        AtariMachineType.ATARI_XEGS -> 8
        AtariMachineType.ATARI_5200 -> 9
    }

    private fun MemoryProfile.toRamSizeKb(): Int = when (this) {
        MemoryProfile.RAM_16 -> 16
        MemoryProfile.RAM_48 -> 48
        MemoryProfile.RAM_52 -> 52
        MemoryProfile.RAM_64 -> 64
        MemoryProfile.RAM_128 -> 128
        MemoryProfile.RAM_320 -> 320
        MemoryProfile.RAM_576 -> 576
        MemoryProfile.RAM_1088 -> 1088
    }

    private fun SioPatchMode.toNativeValue(): Int = when (this) {
        SioPatchMode.ENHANCED -> 0
        SioPatchMode.NO_SIO_PATCH -> 1
        SioPatchMode.NO_PATCH_ALL -> 2
    }

    private fun ArtifactingMode.toNativeValue(): Int = when (this) {
        ArtifactingMode.OFF -> 0
        ArtifactingMode.NTSC_OLD -> 1
        ArtifactingMode.NTSC_NEW -> 2
        ArtifactingMode.NTSC_FULL -> 3
        ArtifactingMode.PAL_SIMPLE -> 4
        ArtifactingMode.PAL_BLEND -> 5
    }

    private fun com.mantismoonlabs.fujinetgo800.settings.NtscFilterPreset.toNativeValue(): Int = when (this) {
        com.mantismoonlabs.fujinetgo800.settings.NtscFilterPreset.COMPOSITE -> 0
        com.mantismoonlabs.fujinetgo800.settings.NtscFilterPreset.SVIDEO -> 1
        com.mantismoonlabs.fujinetgo800.settings.NtscFilterPreset.RGB -> 2
        com.mantismoonlabs.fujinetgo800.settings.NtscFilterPreset.MONOCHROME -> 3
        com.mantismoonlabs.fujinetgo800.settings.NtscFilterPreset.CUSTOM -> 4
    }
}

object FujiNetNative {
    init {
        NativeLibraryLoader.loaded
    }

    private external fun nativeStartRuntime(
        runtimeRootPath: String,
        configPath: String,
        sdPath: String,
        dataPath: String,
        listenPort: Int,
    ): Boolean

    private external fun nativeStopRuntime()
    private external fun nativeLastErrorMessage(): String?
    private external fun nativeIsRuntimeRunning(): Boolean
    private external fun nativeRecentLog(maxBytes: Int): String?

    fun startRuntime(
        runtimeRootPath: String,
        configPath: String,
        sdPath: String,
        dataPath: String,
        listenPort: Int,
    ): Boolean = nativeStartRuntime(
        runtimeRootPath = runtimeRootPath,
        configPath = configPath,
        sdPath = sdPath,
        dataPath = dataPath,
        listenPort = listenPort,
    )

    fun stopRuntime() {
        nativeStopRuntime()
    }

    fun lastErrorMessage(): String? = nativeLastErrorMessage()

    fun isRuntimeRunning(): Boolean = nativeIsRuntimeRunning()

    fun recentLog(maxBytes: Int = 16 * 1024): String = nativeRecentLog(maxBytes).orEmpty()
}
