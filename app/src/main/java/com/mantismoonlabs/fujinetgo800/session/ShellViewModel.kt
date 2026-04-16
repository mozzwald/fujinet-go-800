package com.mantismoonlabs.fujinetgo800.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ShellViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val settingsVisible = MutableStateFlow(false)

    val uiState: StateFlow<ShellUiState> = combine(
        sessionRepository.state,
        settingsVisible,
    ) { state, visible ->
        toUiState(state).copy(settingsVisible = visible)
    }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            toUiState(SessionState.ReadyToLaunch(LaunchMode.FUJINET_ENABLED)),
        )

    fun onPauseTogglePressed() {
        sessionRepository.dispatch(SessionCommand.TogglePause)
    }

    fun onResetPressed() {
        sessionRepository.dispatch(SessionCommand.ResetSystem)
    }

    fun onWarmResetPressed() {
        sessionRepository.dispatch(SessionCommand.WarmResetSystem)
    }

    fun onEndSessionPressed() {
        sessionRepository.dispatch(SessionCommand.ReturnToLaunch)
        settingsVisible.value = false
    }

    fun onRecoverLocalOnlyPressed() {
        sessionRepository.dispatch(SessionCommand.RecoverLocalOnly)
    }

    fun onSettingsPressed() {
        settingsVisible.value = true
    }

    fun onSettingsDismissed() {
        settingsVisible.value = false
    }

    private fun toUiState(state: SessionState): ShellUiState {
        return when (state) {
            SessionState.Idle -> ShellUiState(
                sessionLabel = "No active session",
                statusLabel = "Idle",
                detailLabel = "",
                pauseButtonLabel = "Pause",
                isPauseEnabled = false,
            )

            is SessionState.ReadyToLaunch -> ShellUiState(
                sessionLabel = "Ready to launch",
                statusLabel = state.launchMode.toStatusLabel(),
                detailLabel = "",
                pauseButtonLabel = "Pause",
                isPauseEnabled = false,
            )

            is SessionState.StartingFujiNet -> ShellUiState(
                sessionLabel = "Starting FujiNet",
                statusLabel = "Waiting for FujiNet readiness",
                detailLabel = "",
                currentSessionLabel = state.launchMode.toStatusLabel(),
                fujiNetStatusLabel = "Starting",
                sessionStatusVisible = true,
                pauseButtonLabel = "Pause",
                isPauseEnabled = false,
            )

            is SessionState.Starting -> ShellUiState(
                sessionLabel = "Starting session",
                statusLabel = "Preparing emulator",
                detailLabel = "",
                currentSessionLabel = state.launchMode.toStatusLabel(),
                fujiNetStatusLabel = if (state.launchMode == LaunchMode.FUJINET_ENABLED) "Ready" else "Disabled",
                sessionStatusVisible = true,
                pauseButtonLabel = "Pause",
                isPauseEnabled = false,
            )

            is SessionState.Running -> ShellUiState(
                sessionLabel = "Session #${state.sessionToken}",
                statusLabel = if (state.paused) "Paused" else "Running",
                detailLabel = "",
                currentSessionLabel = state.launchMode.toStatusLabel(),
                fujiNetStatusLabel = if (state.launchMode == LaunchMode.FUJINET_ENABLED) "Ready" else "Disabled",
                sessionStatusVisible = true,
                pauseButtonLabel = if (state.paused) "Resume" else "Pause",
                isPauseEnabled = true,
            )

            is SessionState.Recovering -> ShellUiState(
                sessionLabel = "Recovering session",
                statusLabel = "Recovering session",
                detailLabel = when (state.reason) {
                    RuntimeRecoveryReason.RuntimeLost ->
                        "The previous runtime was lost while the app was in the background."

                    RuntimeRecoveryReason.ProcessRestarted ->
                        "The app is restoring the previous session after a process restart."
                },
                currentSessionLabel = state.launchMode.toStatusLabel(),
                fujiNetStatusLabel = if (state.launchMode == LaunchMode.FUJINET_ENABLED) "Recovery needed" else "Disabled",
                sessionStatusVisible = true,
                pauseButtonLabel = "Pause",
                isPauseEnabled = false,
                recoveryActionLabel = "Launch Session",
                recoveryActionVisible = true,
            )

            is SessionState.Failed -> ShellUiState(
                sessionLabel = if (state.canRecoverLocally) "FujiNet Recovery" else "Session unavailable",
                statusLabel = when (state.reason) {
                    FujiNetFailureReason.AssetInitializationFailed -> "FujiNet assets could not be prepared."
                    FujiNetFailureReason.ServiceStartFailed -> "FujiNet could not start in the app."
                    FujiNetFailureReason.ReadinessTimeout -> "FujiNet did not become ready in time."
                },
                detailLabel = if (state.canRecoverLocally) {
                    "FujiNet failed before the default boot could finish."
                } else {
                    state.message
                },
                currentSessionLabel = state.launchMode.toStatusLabel(),
                fujiNetStatusLabel = "Recovery needed",
                sessionStatusVisible = true,
                pauseButtonLabel = "Pause",
                isPauseEnabled = false,
                recoveryActionLabel = if (state.canRecoverLocally) "Start Local Only" else "",
                recoveryActionVisible = state.canRecoverLocally,
            )
        }
    }

    private fun LaunchMode.toStatusLabel(): String = when (this) {
        LaunchMode.FUJINET_ENABLED -> "FujiNet enabled"
        LaunchMode.LOCAL_ONLY -> "Local only"
    }

    companion object {
        fun provideFactory(sessionRepository: SessionRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ShellViewModel(sessionRepository)
                }
            }
    }
}
