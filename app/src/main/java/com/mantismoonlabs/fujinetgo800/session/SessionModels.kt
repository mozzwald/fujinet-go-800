package com.mantismoonlabs.fujinetgo800.session

import android.view.Surface
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.storage.MediaRole

enum class RuntimeRecoveryReason {
    RuntimeLost,
    ProcessRestarted,
}

enum class FujiNetFailureReason(val defaultMessage: String) {
    AssetInitializationFailed("FujiNet assets failed to initialize"),
    ServiceStartFailed("FujiNet failed to start"),
    ReadinessTimeout("Timed out waiting for FujiNet readiness"),
}

sealed interface SessionState {
    data object Idle : SessionState
    data class ReadyToLaunch(val launchMode: LaunchMode) : SessionState
    data class StartingFujiNet(val launchMode: LaunchMode) : SessionState
    data class Starting(val launchMode: LaunchMode) : SessionState
    data class Recovering(
        val launchMode: LaunchMode,
        val reason: RuntimeRecoveryReason,
    ) : SessionState

    data class Running(
        val sessionToken: Long,
        val paused: Boolean,
        val surfaceAttached: Boolean,
        val launchMode: LaunchMode,
    ) : SessionState

    data class Failed(
        val launchMode: LaunchMode,
        val reason: FujiNetFailureReason,
        val canRecoverLocally: Boolean,
        val message: String = reason.defaultMessage,
    ) : SessionState
}

sealed interface SessionCommand {
    data class StartSession(val config: SessionLaunchConfig = SessionLaunchConfig(settings = EmulatorSettings())) : SessionCommand {
        val width: Int
            get() = config.width

        val height: Int
            get() = config.height

        val sampleRate: Int
            get() = config.sampleRate
    }

    data class ApplyRuntimeSettings(val settings: EmulatorSettings) : SessionCommand
    data class SetAudioVolume(val volumePercent: Int) : SessionCommand
    data object ReturnToLaunch : SessionCommand
    data object TogglePause : SessionCommand
    data object ResetSystem : SessionCommand
    data object WarmResetSystem : SessionCommand
    data class ApplyStoredMedia(val role: MediaRole) : SessionCommand
    data class ClearStoredMedia(val role: MediaRole) : SessionCommand
    data class MountDisk(val importedPath: String, val driveNumber: Int = 1) : SessionCommand
    data object EjectDisk : SessionCommand
    data class InsertCartridge(val importedPath: String) : SessionCommand
    data object RemoveCartridge : SessionCommand
    data class LoadExecutable(val importedPath: String) : SessionCommand
    data class ApplyCustomRom(val importedPath: String) : SessionCommand
    data object ClearCustomRom : SessionCommand
    data class ApplyBasicRom(val importedPath: String) : SessionCommand
    data object ClearBasicRom : SessionCommand
    data class ApplyAtari400800Rom(val importedPath: String) : SessionCommand
    data object ClearAtari400800Rom : SessionCommand
    data class SetKeyState(val aKeyCode: Int, val pressed: Boolean) : SessionCommand
    data class SetConsoleKeys(val start: Boolean, val select: Boolean, val option: Boolean) : SessionCommand
    data class SetJoystickState(val port: Int, val x: Float, val y: Float, val fire: Boolean) : SessionCommand
    data object HostStarted : SessionCommand
    data object HostStopped : SessionCommand
    data object RecoverLocalOnly : SessionCommand
    data class AttachSurface(
        val surface: Surface,
        val width: Int,
        val height: Int,
    ) : SessionCommand

    data object DetachSurface : SessionCommand
}

data class SessionLaunchConfig(
    val settings: EmulatorSettings,
    val width: Int = 320,
    val height: Int = 240,
    val sampleRate: Int = 44100,
)

data class ShellUiState(
    val sessionLabel: String = "No active session",
    val statusLabel: String = "Idle",
    val detailLabel: String = "",
    val currentSessionLabel: String = "",
    val fujiNetStatusLabel: String = "",
    val sessionStatusVisible: Boolean = false,
    val pauseButtonLabel: String = "Pause",
    val isPauseEnabled: Boolean = false,
    val settingsVisible: Boolean = false,
    val recoveryActionLabel: String = "",
    val recoveryActionVisible: Boolean = false,
)

data class MediaSlotUiState(
    val role: MediaRole,
    val title: String,
    val actionLabel: String,
    val emptyLabel: String,
    val clearLabel: String,
    val selectedLabel: String = "",
    val hasSelection: Boolean = false,
) {
    val displayLabel: String
        get() = if (hasSelection) selectedLabel else emptyLabel
}

data class LocalMediaUiState(
    val disk: MediaSlotUiState = MediaSlotUiState(
        role = MediaRole.DISK,
        title = "Disk Drive 1",
        actionLabel = "Mount Disk",
        emptyLabel = "No disk selected",
        clearLabel = "Eject Disk",
    ),
    val cartridge: MediaSlotUiState = MediaSlotUiState(
        role = MediaRole.CARTRIDGE,
        title = "Cartridge",
        actionLabel = "Select Cartridge",
        emptyLabel = "No cartridge selected",
        clearLabel = "Clear Cartridge",
    ),
    val executable: MediaSlotUiState = MediaSlotUiState(
        role = MediaRole.EXECUTABLE,
        title = "Executable",
        actionLabel = "Select Executable",
        emptyLabel = "No executable selected",
        clearLabel = "Clear Executable",
    ),
    val rom: MediaSlotUiState = MediaSlotUiState(
        role = MediaRole.ROM,
        title = "OS ROM",
        actionLabel = "Select OS ROM",
        emptyLabel = "Default built-in Altirra ROM",
        clearLabel = "Clear ROM",
    ),
)
