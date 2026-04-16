package com.mantismoonlabs.fujinetgo800.ui

import android.content.res.Configuration
import com.mantismoonlabs.fujinetgo800.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mantismoonlabs.fujinetgo800.input.AtariConsoleKey
import com.mantismoonlabs.fujinetgo800.input.AtariKeyCode
import com.mantismoonlabs.fujinetgo800.input.AtariKeyMapping
import com.mantismoonlabs.fujinetgo800.fujinet.FujiNetBootMode
import com.mantismoonlabs.fujinetgo800.settings.ControlMode
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.AtariMachineType
import com.mantismoonlabs.fujinetgo800.settings.ArtifactingMode
import com.mantismoonlabs.fujinetgo800.settings.KeyboardInputMode
import com.mantismoonlabs.fujinetgo800.settings.JoystickInputStyle
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.settings.MemoryProfile
import com.mantismoonlabs.fujinetgo800.settings.NtscFilterPreset
import com.mantismoonlabs.fujinetgo800.settings.OrientationMode
import com.mantismoonlabs.fujinetgo800.settings.ScaleMode
import com.mantismoonlabs.fujinetgo800.settings.SioPatchMode
import com.mantismoonlabs.fujinetgo800.settings.SystemRomKind
import com.mantismoonlabs.fujinetgo800.settings.VideoStandard
import com.mantismoonlabs.fujinetgo800.settings.normalizedMachineMemory
import com.mantismoonlabs.fujinetgo800.settings.validMemoryProfiles
import com.mantismoonlabs.fujinetgo800.session.LaunchSettingsViewModel
import com.mantismoonlabs.fujinetgo800.session.LaunchSettingsUiState
import com.mantismoonlabs.fujinetgo800.session.LocalMediaUiState
import com.mantismoonlabs.fujinetgo800.session.MediaSlotUiState
import com.mantismoonlabs.fujinetgo800.session.SettingsTab
import com.mantismoonlabs.fujinetgo800.session.SessionCommand
import com.mantismoonlabs.fujinetgo800.session.SessionLaunchConfig
import com.mantismoonlabs.fujinetgo800.session.SessionState
import com.mantismoonlabs.fujinetgo800.session.SessionRepository
import com.mantismoonlabs.fujinetgo800.session.ShellViewModel
import com.mantismoonlabs.fujinetgo800.session.toLabel
import com.mantismoonlabs.fujinetgo800.storage.LocalMediaViewModel
import com.mantismoonlabs.fujinetgo800.storage.MediaRole
import com.mantismoonlabs.fujinetgo800.storage.RuntimePaths
import com.mantismoonlabs.fujinetgo800.ui.input.AtariFunctionBar
import com.mantismoonlabs.fujinetgo800.ui.input.AtariFunctionKeySpec
import com.mantismoonlabs.fujinetgo800.ui.input.AtariKeyboard
import com.mantismoonlabs.fujinetgo800.ui.input.DpadControl
import com.mantismoonlabs.fujinetgo800.ui.input.FireButtonControl
import com.mantismoonlabs.fujinetgo800.ui.input.InputControlsUiState
import com.mantismoonlabs.fujinetgo800.ui.input.InputControlsViewModel
import com.mantismoonlabs.fujinetgo800.ui.input.JoystickPadControl
import com.mantismoonlabs.fujinetgo800.ui.input.JoystickControls
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun EmulatorScreen(
    settingsRepository: EmulatorSettingsRepository,
    sessionRepository: SessionRepository,
    localMediaViewModel: LocalMediaViewModel,
    keyboardResetTrigger: Int,
    onClearMediaSelection: (MediaRole) -> Unit,
    onPickSystemRom: (SystemRomKind) -> Unit,
    onClearSystemRom: (SystemRomKind) -> Unit,
    onOpenFujiNetWebUi: () -> Unit = {},
    onSwapFujiNetDisks: () -> Unit = {},
    onOpenFujiNetLogFile: (String) -> Unit = {},
    onPickHDevice: (Int) -> Unit = {},
    onClearHDevice: (Int) -> Unit = {},
    onShutdownRequested: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shellViewModel: ShellViewModel = viewModel(
        key = "shell",
        factory = ShellViewModel.provideFactory(sessionRepository)
    )
    val launchSettingsViewModel: LaunchSettingsViewModel = viewModel(
        key = "launch",
        factory = LaunchSettingsViewModel.provideFactory(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
            runtimePaths = RuntimePaths.fromFilesDirectory(
                filesDirectory = context.filesDir,
                externalMediaDirectory = context.getExternalMediaDirs().firstOrNull(),
            ),
        ),
    )
    val inputControlsViewModel: InputControlsViewModel = viewModel(
        key = "input",
        factory = InputControlsViewModel.provideFactory(
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
        ),
    )
    val sessionState by sessionRepository.state.collectAsStateWithLifecycle()
    val uiState by shellViewModel.uiState.collectAsStateWithLifecycle()
    val launchSettingsState by launchSettingsViewModel.uiState.collectAsStateWithLifecycle()
    val inputControlsState by inputControlsViewModel.uiState.collectAsStateWithLifecycle()
    val localMediaState by localMediaViewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val view = LocalView.current
    val bottomNavigationInset = with(density) {
        (
            ViewCompat.getRootWindowInsets(view)
                ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
                ?.bottom ?: 0
            ).toDp()
    }
    val screenHeight = configuration.screenHeightDp.dp + bottomNavigationInset
    val screenWidth = configuration.screenWidthDp.dp
    val contentWidth = (screenWidth - 16.dp).coerceAtLeast(0.dp)
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compactPortraitControls = !isLandscape && configuration.screenHeightDp <= 760
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val inputPanelVisible = if (isLandscape) true else inputControlsState.isInputPanelVisible
    val baseInputPanelHeight = if (compactPortraitControls) {
        (screenHeight * 0.43f).coerceIn(280.dp, 340.dp)
    } else if (isLandscape) {
        (screenHeight * 0.58f - 24.dp).coerceIn(180.dp, 240.dp)
    } else {
        (screenHeight * 0.43f).coerceIn(300.dp, 390.dp)
    }
    val useLandscapeJoystickLayout = sessionState is SessionState.Running &&
        isLandscape &&
        inputPanelVisible &&
        inputControlsState.controlMode == ControlMode.JOYSTICK
    val useInternalKeyboard = launchSettingsState.settings.keyboardInputMode == KeyboardInputMode.INTERNAL
    val landscapeKeyboardLayout = sessionState is SessionState.Running &&
        isLandscape &&
        inputControlsState.isKeyboardVisible
    val useLandscapeRuntimeChrome = useLandscapeJoystickLayout || landscapeKeyboardLayout
    val shouldReclaimViewportSpace = sessionState is SessionState.Running &&
        !isLandscape &&
        !uiState.settingsVisible
    val compactPortraitKeyboardLayout = sessionState is SessionState.Running &&
        !isLandscape &&
        inputControlsState.isKeyboardVisible &&
        !useInternalKeyboard &&
        imeVisible &&
        !uiState.settingsVisible
    val emulatorViewportHeight = (contentWidth / EmulatorDisplayAspectRatio)
        .coerceAtMost(screenHeight)
    val topControlsHeight = if (!useLandscapeRuntimeChrome) {
        CompactButtonHeight + StandardSectionSpacing
    } else {
        0.dp
    }
    val portraitFunctionBarHeight = if (sessionState is SessionState.Running && !useLandscapeJoystickLayout && isLandscape) {
        StandardSectionSpacing + portraitFunctionBarContainerHeight(compactPortraitControls || isLandscape) +
            StandardSectionSpacing
    } else {
        0.dp
    }
    val portraitMaxInputPanelHeight = if (
        sessionState is SessionState.Running &&
        !isLandscape &&
        !uiState.settingsVisible
    ) {
        (
            screenHeight -
                ScreenVerticalPadding -
                topControlsHeight -
                portraitFunctionBarHeight -
                PortraitControlsVerticalSpacing -
                emulatorViewportHeight
            ).coerceAtLeast(0.dp)
    } else {
        0.dp
    }
    var portraitResizeFractionOverride by rememberSaveable { mutableStateOf<Float?>(null) }
    var portraitResizeGestureStartFraction by rememberSaveable { mutableStateOf<Float?>(null) }
    var portraitResizeAccumulatedDragPx by rememberSaveable { mutableStateOf(0f) }
    val activePortraitInputPanelFraction = portraitResizeFractionOverride ?: if (
        inputPanelVisible
    ) {
        inputControlsState.portraitInputPanelSizeFraction
    } else {
        0f
    }
    val portraitInputPanelMetrics = if (
        sessionState is SessionState.Running &&
        !isLandscape &&
        !uiState.settingsVisible
    ) {
        calculatePortraitInputPanelMetrics(
            maxAvailableHeight = portraitMaxInputPanelHeight,
            sizeFraction = activePortraitInputPanelFraction,
        )
    } else {
        null
    }
    val landscapeKeyboardPanelHeight = maxOf(
        baseInputPanelHeight,
        internalKeyboardContainerHeight(
            compact = true,
            dense = true,
        ),
    )
    val reservedInputPanelHeight = if (
        sessionState is SessionState.Running &&
        inputPanelVisible &&
        !useLandscapeRuntimeChrome
    ) {
        portraitInputPanelMetrics?.totalHeight ?: baseInputPanelHeight
    } else {
        0.dp
    }
    val portraitAvailableEmulatorContainerHeight = if (shouldReclaimViewportSpace) {
        (
            screenHeight -
                ScreenVerticalPadding -
                topControlsHeight -
                portraitFunctionBarHeight -
                PortraitControlsVerticalSpacing -
                reservedInputPanelHeight
            ).coerceAtLeast(0.dp)
    } else {
        emulatorViewportHeight
    }
    val toggleInputIconResId = if (inputControlsState.isKeyboardVisible) {
        R.drawable.ic_joystick
    } else {
        R.drawable.ic_keyboard
    }
    val toggleInputDescription = if (inputControlsState.isKeyboardVisible) {
        "Switch to joystick input"
    } else {
        "Switch to keyboard input"
    }
    var entryAutoStartPending by rememberSaveable { mutableStateOf(true) }
    var resetDialogVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(entryAutoStartPending, sessionState) {
        if (
            entryAutoStartPending &&
            (sessionState is SessionState.ReadyToLaunch || sessionState is SessionState.Recovering)
        ) {
            sessionRepository.dispatch(
                SessionCommand.StartSession(
                    SessionLaunchConfig(
                        settings = settingsRepository.currentSettings().normalizedMachineMemory(),
                    ),
                ),
            )
        }
    }

    LaunchedEffect(entryAutoStartPending, sessionState) {
        if (
            entryAutoStartPending &&
            sessionState !is SessionState.ReadyToLaunch &&
            sessionState !is SessionState.Recovering
        ) {
            entryAutoStartPending = false
        }
    }

    val onCloseSettings = {
        launchSettingsViewModel.onSettingsClosed(sessionState)
        shellViewModel.onSettingsDismissed()
    }
    val toggleInputMode = inputControlsViewModel::toggleControlMode
    val toggleInputPanelVisibility = inputControlsViewModel::toggleInputPanelVisibility
    val portraitDrawerFunctionBarHeight = portraitFunctionBarContainerHeight(compactPortraitControls)
    val portraitDrawerFunctionBarBlockHeight = portraitDrawerFunctionBarHeight + StandardSectionSpacing
    val beginPortraitResize = {
        portraitResizeGestureStartFraction =
            portraitResizeFractionOverride ?: inputControlsState.portraitInputPanelSizeFraction
        portraitResizeAccumulatedDragPx = 0f
    }
    val onPortraitResizeDelta: (Float) -> Unit = { dragAmountPx ->
        val metrics = portraitInputPanelMetrics
        if (metrics != null) {
            val minHeightPx = with(density) { metrics.minHeight.toPx() }
            val maxHeightPx = with(density) { metrics.maxHeight.toPx() }
            if (maxHeightPx <= minHeightPx) {
                portraitResizeFractionOverride = 1f
            } else {
                portraitResizeAccumulatedDragPx += dragAmountPx
                val startFraction = portraitResizeGestureStartFraction ?: metrics.fraction
                val startHeightPx = minHeightPx + ((maxHeightPx - minHeightPx) * startFraction)
                val nextHeightPx = (startHeightPx - portraitResizeAccumulatedDragPx)
                    .coerceIn(minHeightPx, maxHeightPx)
                portraitResizeFractionOverride = ((nextHeightPx - minHeightPx) / (maxHeightPx - minHeightPx))
                    .coerceIn(0f, 1f)
            }
        }
    }
    val commitPortraitResize = {
        portraitResizeFractionOverride?.let { fraction ->
            if (fraction <= PortraitInputDrawerCollapseFraction) {
                portraitResizeFractionOverride = 0f
                inputControlsViewModel.hideInputPanel()
            } else {
                val committedFraction = if (fraction >= PortraitInputDrawerExpandSnapFraction) {
                    1f
                } else {
                    fraction
                }
                portraitResizeFractionOverride = committedFraction
                inputControlsViewModel.showInputPanel()
                inputControlsViewModel.setPortraitInputPanelSizeFraction(committedFraction)
            }
        }
        portraitResizeGestureStartFraction = null
        portraitResizeAccumulatedDragPx = 0f
    }
    val resetPortraitResize = {
        portraitResizeFractionOverride = null
        portraitResizeGestureStartFraction = null
        portraitResizeAccumulatedDragPx = 0f
        inputControlsViewModel.showInputPanel()
        inputControlsViewModel.resetPortraitInputPanelSize()
    }
    val openResetDialog = { resetDialogVisible = true }

    LaunchedEffect(uiState.settingsVisible) {
        if (uiState.settingsVisible) {
            launchSettingsViewModel.onSettingsOpened()
        }
    }

    LaunchedEffect(
        inputControlsState.portraitInputPanelSizeFraction,
        inputControlsState.isInputPanelVisible,
        isLandscape,
    ) {
        val override = portraitResizeFractionOverride ?: return@LaunchedEffect
        val persistedFraction = if (inputControlsState.isInputPanelVisible) {
            inputControlsState.portraitInputPanelSizeFraction
        } else {
            0f
        }
        if (isLandscape || kotlin.math.abs(override - persistedFraction) < 0.001f) {
            portraitResizeFractionOverride = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (resetDialogVisible) {
            ResetChooserDialog(
                onDismiss = { resetDialogVisible = false },
                onColdReset = {
                    resetDialogVisible = false
                    shellViewModel.onResetPressed()
                },
                onWarmReset = {
                    resetDialogVisible = false
                    shellViewModel.onWarmResetPressed()
                },
                onShutdown = {
                    resetDialogVisible = false
                    onShutdownRequested()
                },
            )
        }
        if (uiState.settingsVisible) {
            FullScreenSettings(
                launchSettingsState = launchSettingsState,
                localMediaState = localMediaState,
                currentLaunchModeLabel = sessionState.toLaunchModeLabel(),
                showFujiNetSection = sessionState !is SessionState.ReadyToLaunch,
                onSettingsTabSelected = launchSettingsViewModel::onSettingsTabSelected,
                onScaleModeSelected = launchSettingsViewModel::onScaleModeSelected,
                onEmulatorVolumeChanged = launchSettingsViewModel::onEmulatorVolumePreviewChanged,
                onEmulatorVolumeChangeFinished = launchSettingsViewModel::onEmulatorVolumeChangeFinished,
                onKeepScreenOnChanged = launchSettingsViewModel::onKeepScreenOnChanged,
                onBackgroundAudioChanged = launchSettingsViewModel::onBackgroundAudioChanged,
                onOrientationModeSelected = launchSettingsViewModel::onOrientationModeSelected,
                onTurboModeChanged = launchSettingsViewModel::onTurboModeChanged,
                onMachineTypeSelected = launchSettingsViewModel::onMachineTypeSelected,
                onMemoryProfileSelected = launchSettingsViewModel::onMemoryProfileSelected,
                onBasicEnabledChanged = launchSettingsViewModel::onBasicEnabledChanged,
                onSioPatchModeSelected = launchSettingsViewModel::onSioPatchModeSelected,
                onArtifactingModeSelected = launchSettingsViewModel::onArtifactingModeSelected,
                onNtscFilterPresetSelected = launchSettingsViewModel::onNtscFilterPresetSelected,
                onNtscFilterSharpnessChanged = launchSettingsViewModel::onNtscFilterSharpnessChanged,
                onNtscFilterResolutionChanged = launchSettingsViewModel::onNtscFilterResolutionChanged,
                onNtscFilterArtifactsChanged = launchSettingsViewModel::onNtscFilterArtifactsChanged,
                onNtscFilterFringingChanged = launchSettingsViewModel::onNtscFilterFringingChanged,
                onNtscFilterBleedChanged = launchSettingsViewModel::onNtscFilterBleedChanged,
                onNtscFilterBurstPhaseChanged = launchSettingsViewModel::onNtscFilterBurstPhaseChanged,
                onScanlinesChanged = launchSettingsViewModel::onScanlinesChanged,
                onStereoPokeyChanged = launchSettingsViewModel::onStereoPokeyChanged,
                onKeyboardInputModeSelected = launchSettingsViewModel::onKeyboardInputModeSelected,
                onKeyboardHapticsChanged = launchSettingsViewModel::onKeyboardHapticsChanged,
                onStickyKeyboardShiftChanged = launchSettingsViewModel::onStickyKeyboardShiftChanged,
                onStickyKeyboardCtrlChanged = launchSettingsViewModel::onStickyKeyboardCtrlChanged,
                onStickyKeyboardFnChanged = launchSettingsViewModel::onStickyKeyboardFnChanged,
                onJoystickInputStyleSelected = launchSettingsViewModel::onJoystickInputStyleSelected,
                onJoystickHapticsChanged = launchSettingsViewModel::onJoystickHapticsChanged,
                onPauseOnAppSwitchChanged = launchSettingsViewModel::onPauseOnAppSwitchChanged,
                onVideoStandardSelected = launchSettingsViewModel::onVideoStandardSelected,
                onUseFujiNet = {
                    launchSettingsViewModel.onLaunchModeSelected(LaunchMode.FUJINET_ENABLED)
                },
                onOpenFujiNetWebUi = onOpenFujiNetWebUi,
                onOpenFujiNetLogFile = onOpenFujiNetLogFile,
                onRefreshFujiNetLog = launchSettingsViewModel::refreshFujiNetLog,
                onFujiNetPrinterEnabledChanged = launchSettingsViewModel::onFujiNetPrinterEnabledChanged,
                onFujiNetPrinterPortSelected = launchSettingsViewModel::onFujiNetPrinterPortSelected,
                onFujiNetPrinterModelSelected = launchSettingsViewModel::onFujiNetPrinterModelSelected,
                onFujiNetHsioIndexSelected = launchSettingsViewModel::onFujiNetHsioIndexSelected,
                onFujiNetConfigBootEnabledChanged = launchSettingsViewModel::onFujiNetConfigBootEnabledChanged,
                onFujiNetConfigNgChanged = launchSettingsViewModel::onFujiNetConfigNgChanged,
                onFujiNetStatusWaitChanged = launchSettingsViewModel::onFujiNetStatusWaitChanged,
                onFujiNetBootModeSelected = launchSettingsViewModel::onFujiNetBootModeSelected,
                onResetToDefaults = launchSettingsViewModel::onResetToDefaults,
                onPickSystemRom = onPickSystemRom,
                onClearSystemRom = onClearSystemRom,
                onPickHDevice = onPickHDevice,
                onClearHDevice = onClearHDevice,
                onPickLocalMedia = { role ->
                    when (role) {
                        MediaRole.DISK -> localMediaViewModel.onDiskPickRequested()
                        MediaRole.CARTRIDGE -> localMediaViewModel.onCartridgePickRequested()
                        MediaRole.EXECUTABLE -> localMediaViewModel.onExecutablePickRequested()
                        MediaRole.ROM -> localMediaViewModel.onRomPickRequested()
                    }
                },
                onClearLocalMedia = onClearMediaSelection,
                onCloseSettings = onCloseSettings,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            if (!useLandscapeRuntimeChrome) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CompactIconButton(
                                iconResId = R.drawable.ic_disk_swap,
                                contentDescription = "Swap FujiNet disks",
                                modifier = Modifier.width(52.dp),
                                onClick = onSwapFujiNetDisks,
                                enabled = sessionState is SessionState.Running,
                            )
                            CompactIconButton(
                                iconResId = toggleInputIconResId,
                                contentDescription = toggleInputDescription,
                                modifier = Modifier.width(52.dp),
                                onClick = toggleInputMode,
                                enabled = sessionState is SessionState.Running,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            CompactControlButton(
                                label = "",
                                modifier = Modifier.width(52.dp),
                                onClick = shellViewModel::onPauseTogglePressed,
                                enabled = uiState.isPauseEnabled,
                                iconResId = if (uiState.pauseButtonLabel == "Resume") {
                                    R.drawable.ic_play
                                } else {
                                    R.drawable.ic_pause
                                },
                                contentDescription = if (uiState.pauseButtonLabel == "Resume") {
                                    "Resume emulation"
                                } else {
                                    "Pause emulation"
                                },
                            )
                            CompactIconButton(
                                iconResId = R.drawable.ic_reset,
                                contentDescription = "Reset emulator",
                                modifier = Modifier.width(52.dp),
                                onClick = openResetDialog,
                                enabled = sessionState is SessionState.Running,
                            )
                            CompactIconButton(
                                iconResId = R.drawable.ic_settings_gear,
                                contentDescription = "Settings",
                                modifier = Modifier.width(52.dp),
                                onClick = shellViewModel::onSettingsPressed,
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                if (sessionState is SessionState.Running) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CompactIconButton(
                                            iconResId = R.drawable.ic_disk_swap,
                                            contentDescription = "Swap FujiNet disks",
                                            modifier = Modifier.width(52.dp),
                                            onClick = onSwapFujiNetDisks,
                                        )
                                        CompactIconButton(
                                            iconResId = toggleInputIconResId,
                                            contentDescription = toggleInputDescription,
                                            modifier = Modifier
                                                .width(52.dp)
                                                .testTag("top-toggle-input-button"),
                                            onClick = toggleInputMode,
                                            onLongClick = toggleInputPanelVisibility,
                                            longPressTimeoutMillis = InputPanelToggleLongPressTimeoutMillis,
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CompactControlButton(
                                    label = "",
                                    modifier = Modifier.width(52.dp),
                                    onClick = shellViewModel::onPauseTogglePressed,
                                    enabled = uiState.isPauseEnabled,
                                    iconResId = if (uiState.pauseButtonLabel == "Resume") {
                                        R.drawable.ic_play
                                    } else {
                                        R.drawable.ic_pause
                                    },
                                    contentDescription = if (uiState.pauseButtonLabel == "Resume") {
                                        "Resume emulation"
                                    } else {
                                        "Pause emulation"
                                    },
                                )
                                CompactIconButton(
                                    iconResId = R.drawable.ic_reset,
                                    contentDescription = "Reset emulator",
                                    modifier = Modifier.width(52.dp),
                                    onClick = openResetDialog,
                                    enabled = sessionState is SessionState.Running,
                                )
                                CompactIconButton(
                                    iconResId = R.drawable.ic_settings_gear,
                                    contentDescription = "Settings",
                                    modifier = Modifier.width(52.dp),
                                    onClick = shellViewModel::onSettingsPressed,
                                )
                            }
                        }
                    }

                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(
                modifier = if (compactPortraitKeyboardLayout || shouldReclaimViewportSpace) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                },
            ) {
                if (useLandscapeJoystickLayout) {
                    LandscapeJoystickSessionLayout(
                        sessionRepository = sessionRepository,
                        scaleMode = launchSettingsState.settings.scaleMode,
                        scanlinesEnabled = launchSettingsState.settings.scanlinesEnabled,
                        keepScreenOn = launchSettingsState.settings.keepScreenOn,
                        screenWidth = screenWidth,
                        onSwapFujiNetDisks = onSwapFujiNetDisks,
                        onToggleInputMode = toggleInputMode,
                        toggleInputIconResId = toggleInputIconResId,
                        toggleInputDescription = toggleInputDescription,
                        onPauseTogglePressed = shellViewModel::onPauseTogglePressed,
                        pauseEnabled = uiState.isPauseEnabled,
                        pauseIconResId = if (uiState.pauseButtonLabel == "Resume") {
                            R.drawable.ic_play
                        } else {
                            R.drawable.ic_pause
                        },
                        pauseDescription = if (uiState.pauseButtonLabel == "Resume") {
                            "Resume emulation"
                        } else {
                            "Pause emulation"
                        },
                        onResetPressed = openResetDialog,
                        onSettingsPressed = shellViewModel::onSettingsPressed,
                        onJoystickMoved = inputControlsViewModel::onJoystickMoved,
                        onJoystickReleased = inputControlsViewModel::onJoystickReleased,
                        onFirePressed = inputControlsViewModel::onFirePressed,
                        onFireReleased = inputControlsViewModel::onFireReleased,
                        onFunctionKeyPressed = inputControlsViewModel::onFunctionKeyPressed,
                        onFunctionKeyReleased = inputControlsViewModel::onFunctionKeyReleased,
                        joystickInputStyle = inputControlsState.joystickInputStyle,
                        joystickHapticsEnabled = inputControlsState.joystickHapticsEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                } else if (landscapeKeyboardLayout) {
                    LandscapeKeyboardSessionLayout(
                        sessionRepository = sessionRepository,
                        scaleMode = launchSettingsState.settings.scaleMode,
                        scanlinesEnabled = launchSettingsState.settings.scanlinesEnabled,
                        keepScreenOn = launchSettingsState.settings.keepScreenOn,
                        screenWidth = screenWidth,
                        onSwapFujiNetDisks = onSwapFujiNetDisks,
                        onToggleInputMode = toggleInputMode,
                        toggleInputIconResId = toggleInputIconResId,
                        toggleInputDescription = toggleInputDescription,
                        onPauseTogglePressed = shellViewModel::onPauseTogglePressed,
                        pauseEnabled = uiState.isPauseEnabled,
                        pauseIconResId = if (uiState.pauseButtonLabel == "Resume") {
                            R.drawable.ic_play
                        } else {
                            R.drawable.ic_pause
                        },
                        pauseDescription = if (uiState.pauseButtonLabel == "Resume") {
                            "Resume emulation"
                        } else {
                            "Pause emulation"
                        },
                        onResetPressed = openResetDialog,
                        onSettingsPressed = shellViewModel::onSettingsPressed,
                        onFunctionKeyPressed = inputControlsViewModel::onFunctionKeyPressed,
                        onFunctionKeyReleased = inputControlsViewModel::onFunctionKeyReleased,
                        useInternalKeyboard = useInternalKeyboard,
                        onKeyPressed = inputControlsViewModel::onKeyPressed,
                        onKeyReleased = inputControlsViewModel::onKeyReleased,
                        onToggleInputLongPress = toggleInputPanelVisibility,
                        keyboardResetTrigger = keyboardResetTrigger,
                        keyboardHapticsEnabled = inputControlsState.keyboardHapticsEnabled,
                        stickyShiftEnabled = launchSettingsState.settings.stickyKeyboardShiftEnabled,
                        stickyCtrlEnabled = launchSettingsState.settings.stickyKeyboardCtrlEnabled,
                        stickyFnEnabled = launchSettingsState.settings.stickyKeyboardFnEnabled,
                        onAtariPressed = {
                            inputControlsViewModel.onKeyPressed(
                                AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI),
                            )
                        },
                        onAtariReleased = {
                            inputControlsViewModel.onKeyReleased(
                                AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI),
                            )
                        },
                        onImeTextChanged = inputControlsViewModel::onImeTextChanged,
                        onImeEnterPressed = inputControlsViewModel::onImeEnterPressed,
                        keyboardPanelHeight = landscapeKeyboardPanelHeight,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                } else {
                    val emulatorStageModifier = if (compactPortraitKeyboardLayout || shouldReclaimViewportSpace) {
                        Modifier
                            .fillMaxWidth()
                            .height(emulatorViewportHeight)
                    } else {
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    }
                    val emulatorContainerModifier = if (shouldReclaimViewportSpace && !compactPortraitKeyboardLayout) {
                        Modifier
                            .fillMaxWidth()
                            .height(
                                portraitAvailableEmulatorContainerHeight
                                    .coerceAtLeast(emulatorViewportHeight),
                            )
                    } else {
                        Modifier
                    }
                    Box(
                        modifier = emulatorContainerModifier,
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(modifier = emulatorStageModifier) {
                            when (sessionState) {
                                is SessionState.ReadyToLaunch -> {
                                    BootStatusCard(
                                        sessionLabel = sessionState.toAutoStartSessionLabel(),
                                        statusLabel = sessionState.toAutoStartStatusLabel(),
                                        detailLabel = "",
                                    )
                                }

                                is SessionState.Starting,
                                is SessionState.StartingFujiNet -> {
                                    BootStatusCard(
                                        sessionLabel = uiState.sessionLabel,
                                        statusLabel = uiState.statusLabel,
                                        detailLabel = uiState.detailLabel,
                                    )
                                }

                                is SessionState.Recovering -> {
                                    BootStatusCard(
                                        sessionLabel = sessionState.toAutoStartSessionLabel(),
                                        statusLabel = sessionState.toAutoStartStatusLabel(),
                                        detailLabel = "",
                                    )
                                }

                                is SessionState.Failed -> {
                                    RecoveryStatusCard(
                                        sessionLabel = uiState.sessionLabel,
                                        statusLabel = uiState.statusLabel,
                                        detailLabel = uiState.detailLabel,
                                        actionLabel = uiState.recoveryActionLabel,
                                        actionVisible = uiState.recoveryActionVisible,
                                        onAction = shellViewModel::onRecoverLocalOnlyPressed,
                                    )
                                }

                                else -> {
                                    EmulatorRenderHost(
                                        sessionRepository = sessionRepository,
                                        scaleMode = launchSettingsState.settings.scaleMode,
                                        scanlinesEnabled = launchSettingsState.settings.scanlinesEnabled,
                                        keepScreenOn = launchSettingsState.settings.keepScreenOn,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (sessionState is SessionState.Running && !useLandscapeRuntimeChrome) {
                    Spacer(modifier = Modifier.height(StandardSectionSpacing))
                    if (inputControlsState.isKeyboardVisible) {
                        if (isLandscape) {
                            if (useInternalKeyboard) {
                                AtariKeyboard(
                                    onKeyPressed = inputControlsViewModel::onKeyPressed,
                                    onKeyReleased = inputControlsViewModel::onKeyReleased,
                                    onToggleInputMode = toggleInputMode,
                                    onToggleInputLongPress = toggleInputPanelVisibility,
                                    resetTrigger = keyboardResetTrigger,
                                    hapticsEnabled = inputControlsState.keyboardHapticsEnabled,
                                    stickyShiftEnabled = launchSettingsState.settings.stickyKeyboardShiftEnabled,
                                    stickyCtrlEnabled = launchSettingsState.settings.stickyKeyboardCtrlEnabled,
                                    stickyFnEnabled = launchSettingsState.settings.stickyKeyboardFnEnabled,
                                    toggleIconResId = toggleInputIconResId,
                                    toggleIconDescription = toggleInputDescription,
                                    compact = true,
                                    dense = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(landscapeKeyboardPanelHeight),
                                )
                            } else {
                                AndroidKeyboardPanel(
                                    onToggleInputMode = toggleInputMode,
                                    onToggleInputLongPress = toggleInputPanelVisibility,
                                    toggleIconResId = toggleInputIconResId,
                                    toggleIconDescription = toggleInputDescription,
                                    onAtariPressed = {
                                        inputControlsViewModel.onKeyPressed(
                                            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI),
                                        )
                                    },
                                    onAtariReleased = {
                                        inputControlsViewModel.onKeyReleased(
                                            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI),
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(baseInputPanelHeight),
                                    imeProxy = {
                                        AndroidImeProxy(
                                            active = true,
                                            onTextChanged = inputControlsViewModel::onImeTextChanged,
                                            onEnterPressed = inputControlsViewModel::onImeEnterPressed,
                                        )
                                    },
                                )
                            }
                        } else if (useInternalKeyboard) {
                            portraitInputPanelMetrics?.let { metrics ->
                                PortraitResizableInputPanel(
                                    totalHeight = metrics.totalHeight,
                                    onResizeStarted = beginPortraitResize,
                                    onResizeDelta = onPortraitResizeDelta,
                                    onResizeFinished = commitPortraitResize,
                                    onReset = resetPortraitResize,
                                ) { contentHeight ->
                                    val functionBarVisible = contentHeight >= portraitDrawerFunctionBarHeight
                                    val panelHeight = (contentHeight - if (functionBarVisible) {
                                        portraitDrawerFunctionBarBlockHeight
                                    } else {
                                        0.dp
                                    }).coerceAtLeast(0.dp)
                                    val panelVisible = panelHeight > 0.dp
                                    if (functionBarVisible || panelVisible) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            if (functionBarVisible) {
                                                AtariFunctionBar(
                                                    keys = portraitFunctionBarKeys,
                                                    onKeyPressed = inputControlsViewModel::onFunctionKeyPressed,
                                                    onKeyReleased = inputControlsViewModel::onFunctionKeyReleased,
                                                    hapticsEnabled = inputControlsState.keyboardHapticsEnabled,
                                                    compact = compactPortraitControls,
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                                Spacer(modifier = Modifier.height(StandardSectionSpacing))
                                            }
                                            if (panelVisible) {
                                                AtariKeyboard(
                                                    onKeyPressed = inputControlsViewModel::onKeyPressed,
                                                    onKeyReleased = inputControlsViewModel::onKeyReleased,
                                                    onToggleInputMode = toggleInputMode,
                                                    onToggleInputLongPress = toggleInputPanelVisibility,
                                                    resetTrigger = keyboardResetTrigger,
                                                    hapticsEnabled = inputControlsState.keyboardHapticsEnabled,
                                                    stickyShiftEnabled = launchSettingsState.settings.stickyKeyboardShiftEnabled,
                                                    stickyCtrlEnabled = launchSettingsState.settings.stickyKeyboardCtrlEnabled,
                                                    stickyFnEnabled = launchSettingsState.settings.stickyKeyboardFnEnabled,
                                                    toggleIconResId = toggleInputIconResId,
                                                    toggleIconDescription = toggleInputDescription,
                                                    compact = compactPortraitControls,
                                                    dense = landscapeKeyboardLayout ||
                                                        shouldUsePortraitDenseKeyboard(
                                                            contentHeight = panelHeight,
                                                            compact = compactPortraitControls,
                                                        ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(panelHeight),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            portraitInputPanelMetrics?.let { metrics ->
                                PortraitResizableInputPanel(
                                    totalHeight = metrics.totalHeight,
                                    onResizeStarted = beginPortraitResize,
                                    onResizeDelta = onPortraitResizeDelta,
                                    onResizeFinished = commitPortraitResize,
                                    onReset = resetPortraitResize,
                                ) { contentHeight ->
                                    val functionBarVisible = contentHeight >= portraitDrawerFunctionBarHeight
                                    val panelHeight = (contentHeight - if (functionBarVisible) {
                                        portraitDrawerFunctionBarBlockHeight
                                    } else {
                                        0.dp
                                    }).coerceAtLeast(0.dp)
                                    val panelVisible = panelHeight > 0.dp
                                    if (functionBarVisible || panelVisible) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            if (functionBarVisible) {
                                                AtariFunctionBar(
                                                    keys = portraitFunctionBarKeys,
                                                    onKeyPressed = inputControlsViewModel::onFunctionKeyPressed,
                                                    onKeyReleased = inputControlsViewModel::onFunctionKeyReleased,
                                                    hapticsEnabled = inputControlsState.keyboardHapticsEnabled,
                                                    compact = compactPortraitControls,
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                                Spacer(modifier = Modifier.height(StandardSectionSpacing))
                                            }
                                            if (panelVisible) {
                                                AndroidKeyboardPanel(
                                                    onToggleInputMode = toggleInputMode,
                                                    onToggleInputLongPress = toggleInputPanelVisibility,
                                                    toggleIconResId = toggleInputIconResId,
                                                    toggleIconDescription = toggleInputDescription,
                                                    onAtariPressed = {
                                                        inputControlsViewModel.onKeyPressed(
                                                            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI),
                                                        )
                                                    },
                                                    onAtariReleased = {
                                                        inputControlsViewModel.onKeyReleased(
                                                            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI),
                                                        )
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(panelHeight),
                                                    imeProxy = {
                                                        AndroidImeProxy(
                                                            active = panelVisible,
                                                            onTextChanged = inputControlsViewModel::onImeTextChanged,
                                                            onEnterPressed = inputControlsViewModel::onImeEnterPressed,
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        portraitInputPanelMetrics?.let { metrics ->
                            PortraitResizableInputPanel(
                                totalHeight = metrics.totalHeight,
                                onResizeStarted = beginPortraitResize,
                                onResizeDelta = onPortraitResizeDelta,
                                onResizeFinished = commitPortraitResize,
                                onReset = resetPortraitResize,
                            ) { contentHeight ->
                                val functionBarVisible = contentHeight >= portraitDrawerFunctionBarHeight
                                val panelHeight = (contentHeight - if (functionBarVisible) {
                                    portraitDrawerFunctionBarBlockHeight
                                } else {
                                    0.dp
                                }).coerceAtLeast(0.dp)
                                val panelVisible = panelHeight >= PortraitInputDrawerContentThreshold
                                if (functionBarVisible || panelVisible) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        if (functionBarVisible) {
                                            AtariFunctionBar(
                                                keys = portraitFunctionBarKeys,
                                                onKeyPressed = inputControlsViewModel::onFunctionKeyPressed,
                                                onKeyReleased = inputControlsViewModel::onFunctionKeyReleased,
                                                hapticsEnabled = inputControlsState.joystickHapticsEnabled,
                                                compact = compactPortraitControls,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            Spacer(modifier = Modifier.height(StandardSectionSpacing))
                                        }
                                        if (panelVisible) {
                                            JoystickControls(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(panelHeight),
                                                onJoystickMoved = inputControlsViewModel::onJoystickMoved,
                                                onJoystickReleased = inputControlsViewModel::onJoystickReleased,
                                                onFirePressed = inputControlsViewModel::onFirePressed,
                                                onFireReleased = inputControlsViewModel::onFireReleased,
                                                hapticsEnabled = inputControlsState.joystickHapticsEnabled,
                                                joystickInputStyle = inputControlsState.joystickInputStyle,
                                                compact = compactPortraitControls,
                                                footerContent = {
                                                    JoystickFooterControls(
                                                        onToggleInputMode = toggleInputMode,
                                                        onToggleInputLongPress = toggleInputPanelVisibility,
                                                        toggleIconResId = toggleInputIconResId,
                                                        toggleIconDescription = toggleInputDescription,
                                                        compact = compactPortraitControls,
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(BottomChromeSpacing))
            }
        }
    }
}

@Composable
private fun PortraitResizableInputPanel(
    totalHeight: androidx.compose.ui.unit.Dp,
    onResizeStarted: () -> Unit,
    onResizeDelta: (Float) -> Unit,
    onResizeFinished: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.ui.unit.Dp) -> Unit,
) {
    val contentHeight = (totalHeight - PortraitInputResizeChromeHeight).coerceAtLeast(0.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
    ) {
        PortraitInputResizeHandle(
            onResizeStarted = onResizeStarted,
            onResizeDelta = onResizeDelta,
            onResizeFinished = onResizeFinished,
            onReset = onReset,
        )
        Spacer(modifier = Modifier.height(PortraitInputResizeHandleSpacing))
        if (contentHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight),
            ) {
                content(contentHeight)
            }
        }
    }
}

@Composable
private fun PortraitInputResizeHandle(
    onResizeStarted: () -> Unit,
    onResizeDelta: (Float) -> Unit,
    onResizeFinished: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragState = rememberDraggableState { delta ->
        onResizeDelta(delta)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PortraitInputResizeHandleHeight)
            .testTag("portrait-input-resize-handle")
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = { onResizeStarted() },
                onDragStopped = { onResizeFinished() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(6.dp)
                .pointerInput(onReset) {
                    detectTapGestures(onLongPress = { onReset() })
                }
                .background(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(999.dp),
                ),
        )
    }
}

@Composable
private fun InputHideHintBubble(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .testTag("input-hide-hint")
            .clickable(onClick = onDismiss),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.inverseSurface,
    ) {
        Text(
            text = "Long press to hide controls",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}

@Composable
private fun ResetChooserDialog(
    onDismiss: () -> Unit,
    onColdReset: () -> Unit,
    onWarmReset: () -> Unit,
    onShutdown: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Reset Emulator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "Cold reset restarts Atari and FujiNet. Warm reset restarts only Atari. Shutdown turns both off and exits the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onColdReset, modifier = Modifier.fillMaxWidth()) {
                        Text("Cold Reset")
                    }
                    FilledTonalButton(onClick = onWarmReset, modifier = Modifier.fillMaxWidth()) {
                        Text("Warm Reset")
                    }
                    OutlinedButton(onClick = onShutdown, modifier = Modifier.fillMaxWidth()) {
                        Text("Shutdown")
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeKeyboardSessionLayout(
    sessionRepository: SessionRepository,
    scaleMode: ScaleMode,
    scanlinesEnabled: Boolean,
    keepScreenOn: Boolean,
    screenWidth: androidx.compose.ui.unit.Dp,
    onSwapFujiNetDisks: () -> Unit,
    onToggleInputMode: () -> Unit,
    toggleInputIconResId: Int,
    toggleInputDescription: String,
    onPauseTogglePressed: () -> Unit,
    pauseEnabled: Boolean,
    pauseIconResId: Int,
    pauseDescription: String,
    onResetPressed: () -> Unit,
    onSettingsPressed: () -> Unit,
    onFunctionKeyPressed: (AtariKeyMapping) -> Unit,
    onFunctionKeyReleased: (AtariKeyMapping) -> Unit,
    useInternalKeyboard: Boolean,
    onKeyPressed: (AtariKeyMapping) -> Unit,
    onKeyReleased: (AtariKeyMapping) -> Unit,
    onToggleInputLongPress: () -> Unit,
    keyboardResetTrigger: Int,
    keyboardHapticsEnabled: Boolean,
    stickyShiftEnabled: Boolean,
    stickyCtrlEnabled: Boolean,
    stickyFnEnabled: Boolean,
    onAtariPressed: () -> Unit,
    onAtariReleased: () -> Unit,
    onImeTextChanged: (String, String) -> Unit,
    onImeEnterPressed: () -> Unit,
    keyboardPanelHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val functionRailWidth = minOf(screenWidth * 0.18f, 160.dp)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(functionRailWidth)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactIconButton(
                            iconResId = R.drawable.ic_disk_swap,
                            contentDescription = "Swap FujiNet disks",
                            modifier = Modifier.width(52.dp),
                            onClick = onSwapFujiNetDisks,
                        )
                        CompactIconButton(
                            iconResId = toggleInputIconResId,
                            contentDescription = toggleInputDescription,
                            modifier = Modifier.width(52.dp),
                            onClick = onToggleInputMode,
                        )
                        CompactControlButton(
                            label = "",
                            modifier = Modifier.width(52.dp),
                            onClick = onPauseTogglePressed,
                            enabled = pauseEnabled,
                            iconResId = pauseIconResId,
                            contentDescription = pauseDescription,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    AtariFunctionBar(
                        keys = landscapeLeftFunctionKeys,
                        onKeyPressed = onFunctionKeyPressed,
                        onKeyReleased = onFunctionKeyReleased,
                        hapticsEnabled = keyboardHapticsEnabled,
                        compact = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            EmulatorRenderHost(
                sessionRepository = sessionRepository,
                scaleMode = scaleMode,
                scanlinesEnabled = scanlinesEnabled,
                keepScreenOn = keepScreenOn,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Box(
                modifier = Modifier
                    .width(functionRailWidth)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactIconButton(
                            iconResId = R.drawable.ic_reset,
                            contentDescription = "Reset emulator",
                            modifier = Modifier.width(52.dp),
                            onClick = onResetPressed,
                        )
                        CompactIconButton(
                            iconResId = R.drawable.ic_settings_gear,
                            contentDescription = "Settings",
                            modifier = Modifier.width(52.dp),
                            onClick = onSettingsPressed,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    AtariFunctionBar(
                        keys = landscapeRightFunctionKeys,
                        onKeyPressed = onFunctionKeyPressed,
                        onKeyReleased = onFunctionKeyReleased,
                        hapticsEnabled = keyboardHapticsEnabled,
                        compact = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (useInternalKeyboard) {
            AtariKeyboard(
                onKeyPressed = onKeyPressed,
                onKeyReleased = onKeyReleased,
                onToggleInputMode = onToggleInputMode,
                onToggleInputLongPress = onToggleInputLongPress,
                resetTrigger = keyboardResetTrigger,
                hapticsEnabled = keyboardHapticsEnabled,
                stickyShiftEnabled = stickyShiftEnabled,
                stickyCtrlEnabled = stickyCtrlEnabled,
                stickyFnEnabled = stickyFnEnabled,
                toggleIconResId = toggleInputIconResId,
                toggleIconDescription = toggleInputDescription,
                compact = true,
                dense = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(keyboardPanelHeight),
            )
        } else {
            AndroidKeyboardPanel(
                onToggleInputMode = onToggleInputMode,
                onToggleInputLongPress = onToggleInputLongPress,
                toggleIconResId = toggleInputIconResId,
                toggleIconDescription = toggleInputDescription,
                onAtariPressed = onAtariPressed,
                onAtariReleased = onAtariReleased,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(keyboardPanelHeight),
                imeProxy = {
                    AndroidImeProxy(
                        active = true,
                        onTextChanged = onImeTextChanged,
                        onEnterPressed = onImeEnterPressed,
                    )
                },
            )
        }
    }
}

@Composable
private fun LandscapeJoystickSessionLayout(
    sessionRepository: SessionRepository,
    scaleMode: ScaleMode,
    scanlinesEnabled: Boolean,
    keepScreenOn: Boolean,
    screenWidth: androidx.compose.ui.unit.Dp,
    onSwapFujiNetDisks: () -> Unit,
    onToggleInputMode: () -> Unit,
    toggleInputIconResId: Int,
    toggleInputDescription: String,
    onPauseTogglePressed: () -> Unit,
    pauseEnabled: Boolean,
    pauseIconResId: Int,
    pauseDescription: String,
    onResetPressed: () -> Unit,
    onSettingsPressed: () -> Unit,
    onJoystickMoved: (Float, Float) -> Unit,
    onJoystickReleased: () -> Unit,
    onFirePressed: () -> Unit,
    onFireReleased: () -> Unit,
    onFunctionKeyPressed: (AtariKeyMapping) -> Unit,
    onFunctionKeyReleased: (AtariKeyMapping) -> Unit,
    joystickInputStyle: JoystickInputStyle,
    joystickHapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val padWidth = minOf(screenWidth * 0.22f, 220.dp)
    val fireWidth = minOf(screenWidth * 0.18f, 180.dp)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(padWidth)
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactIconButton(
                        iconResId = R.drawable.ic_disk_swap,
                        contentDescription = "Swap FujiNet disks",
                        modifier = Modifier.width(52.dp),
                        onClick = onSwapFujiNetDisks,
                    )
                    CompactIconButton(
                        iconResId = toggleInputIconResId,
                        contentDescription = toggleInputDescription,
                        modifier = Modifier.width(52.dp),
                        onClick = onToggleInputMode,
                    )
                    CompactControlButton(
                        label = "",
                        modifier = Modifier.width(52.dp),
                        onClick = onPauseTogglePressed,
                        enabled = pauseEnabled,
                        iconResId = pauseIconResId,
                        contentDescription = pauseDescription,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    when (joystickInputStyle) {
                        JoystickInputStyle.STICK_8_WAY -> {
                            JoystickPadControl(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .padding(12.dp),
                                onJoystickMoved = onJoystickMoved,
                                onJoystickReleased = onJoystickReleased,
                                hapticsEnabled = joystickHapticsEnabled,
                            )
                        }

                        JoystickInputStyle.DPAD_4_WAY -> {
                            DpadControl(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .padding(12.dp),
                                onJoystickMoved = onJoystickMoved,
                                onJoystickReleased = onJoystickReleased,
                                hapticsEnabled = joystickHapticsEnabled,
                            )
                        }
                    }
                }
                AtariFunctionBar(
                    keys = landscapeLeftFunctionKeys,
                    onKeyPressed = onFunctionKeyPressed,
                    onKeyReleased = onFunctionKeyReleased,
                    hapticsEnabled = joystickHapticsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        EmulatorRenderHost(
            sessionRepository = sessionRepository,
            scaleMode = scaleMode,
            scanlinesEnabled = scanlinesEnabled,
            keepScreenOn = keepScreenOn,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        Box(
            modifier = Modifier
                .width(fireWidth)
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactIconButton(
                        iconResId = R.drawable.ic_reset,
                        contentDescription = "Reset emulator",
                        modifier = Modifier.width(52.dp),
                        onClick = onResetPressed,
                    )
                    CompactIconButton(
                        iconResId = R.drawable.ic_settings_gear,
                        contentDescription = "Settings",
                        modifier = Modifier.width(52.dp),
                        onClick = onSettingsPressed,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    FireButtonControl(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .aspectRatio(1f),
                        onFirePressed = onFirePressed,
                        onFireReleased = onFireReleased,
                    )
                }
                AtariFunctionBar(
                    keys = landscapeRightFunctionKeys,
                    onKeyPressed = onFunctionKeyPressed,
                    onKeyReleased = onFunctionKeyReleased,
                    hapticsEnabled = joystickHapticsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CompactControlButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconResId: Int? = null,
    contentDescription: String? = null,
) {
    if (iconResId != null && contentDescription != null) {
        ShellIconButton(
            iconResId = iconResId,
            contentDescription = contentDescription,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
    } else {
        ShellButton(label = label, onClick = onClick, modifier = modifier, enabled = enabled)
    }
}

@Composable
private fun CompactIconButton(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    longPressTimeoutMillis: Long? = null,
) {
    ShellIconButton(
        iconResId = iconResId,
        contentDescription = contentDescription,
        onClick = onClick,
        onLongClick = onLongClick,
        longPressTimeoutMillis = longPressTimeoutMillis,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
private fun AndroidKeyboardPanel(
    onToggleInputMode: () -> Unit,
    onToggleInputLongPress: () -> Unit,
    toggleIconResId: Int,
    toggleIconDescription: String,
    onAtariPressed: () -> Unit,
    onAtariReleased: () -> Unit,
    imeProxy: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showAtariModifierButton = false
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Android keyboard active",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "Use the system IME for typing. Toggle input to return to joystick mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                ShellIconButton(
                    iconResId = toggleIconResId,
                    contentDescription = toggleIconDescription,
                    onClick = onToggleInputMode,
                    onLongClick = onToggleInputLongPress,
                    longPressTimeoutMillis = InputPanelToggleLongPressTimeoutMillis,
                    modifier = Modifier
                        .testTag("toggle-input-button")
                        .size(56.dp),
                    height = 56.dp,
                )
                if (showAtariModifierButton) {
                    HoldableTriangleButton(
                        active = false,
                        onPressed = onAtariPressed,
                        onReleased = onAtariReleased,
                    )
                }
            }
            imeProxy()
        }
    }
}

@Composable
private fun JoystickFooterControls(
    onToggleInputMode: () -> Unit,
    onToggleInputLongPress: () -> Unit,
    toggleIconResId: Int,
    toggleIconDescription: String,
    compact: Boolean,
) {
    val buttonSize = if (compact) 44.dp else 52.dp
    ShellIconButton(
        iconResId = toggleIconResId,
        contentDescription = toggleIconDescription,
        onClick = onToggleInputMode,
        onLongClick = onToggleInputLongPress,
        longPressTimeoutMillis = InputPanelToggleLongPressTimeoutMillis,
        modifier = Modifier
            .testTag("toggle-input-button")
            .size(buttonSize),
        height = buttonSize,
    )
}

@Composable
private fun HoldableTriangleButton(
    active: Boolean,
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 52.dp,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(size)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
            .background(
                if (active) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape,
            )
            .pointerInput(active) {
                detectTapGestures(
                    onPress = {
                        onPressed()
                        try {
                            tryAwaitRelease()
                        } finally {
                            onReleased()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "△",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun FullScreenSettings(
    launchSettingsState: LaunchSettingsUiState,
    localMediaState: LocalMediaUiState,
    currentLaunchModeLabel: String,
    showFujiNetSection: Boolean,
    onSettingsTabSelected: (SettingsTab) -> Unit,
    onScaleModeSelected: (ScaleMode) -> Unit,
    onEmulatorVolumeChanged: (Int) -> Unit,
    onEmulatorVolumeChangeFinished: () -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onBackgroundAudioChanged: (Boolean) -> Unit,
    onOrientationModeSelected: (OrientationMode) -> Unit,
    onTurboModeChanged: (Boolean) -> Unit,
    onMachineTypeSelected: (AtariMachineType) -> Unit,
    onMemoryProfileSelected: (MemoryProfile) -> Unit,
    onBasicEnabledChanged: (Boolean) -> Unit,
    onSioPatchModeSelected: (SioPatchMode) -> Unit,
    onArtifactingModeSelected: (ArtifactingMode) -> Unit,
    onNtscFilterPresetSelected: (NtscFilterPreset) -> Unit,
    onNtscFilterSharpnessChanged: (Float) -> Unit,
    onNtscFilterResolutionChanged: (Float) -> Unit,
    onNtscFilterArtifactsChanged: (Float) -> Unit,
    onNtscFilterFringingChanged: (Float) -> Unit,
    onNtscFilterBleedChanged: (Float) -> Unit,
    onNtscFilterBurstPhaseChanged: (Float) -> Unit,
    onScanlinesChanged: (Boolean) -> Unit,
    onStereoPokeyChanged: (Boolean) -> Unit,
    onKeyboardInputModeSelected: (KeyboardInputMode) -> Unit,
    onKeyboardHapticsChanged: (Boolean) -> Unit,
    onStickyKeyboardShiftChanged: (Boolean) -> Unit,
    onStickyKeyboardCtrlChanged: (Boolean) -> Unit,
    onStickyKeyboardFnChanged: (Boolean) -> Unit,
    onJoystickInputStyleSelected: (JoystickInputStyle) -> Unit,
    onJoystickHapticsChanged: (Boolean) -> Unit,
    onPauseOnAppSwitchChanged: (Boolean) -> Unit,
    onVideoStandardSelected: (VideoStandard) -> Unit,
    onUseFujiNet: () -> Unit,
    onOpenFujiNetWebUi: () -> Unit,
    onOpenFujiNetLogFile: (String) -> Unit,
    onRefreshFujiNetLog: () -> Unit,
    onFujiNetPrinterEnabledChanged: (Boolean) -> Unit,
    onFujiNetPrinterPortSelected: (Int) -> Unit,
    onFujiNetPrinterModelSelected: (String) -> Unit,
    onFujiNetHsioIndexSelected: (Int) -> Unit,
    onFujiNetConfigBootEnabledChanged: (Boolean) -> Unit,
    onFujiNetConfigNgChanged: (Boolean) -> Unit,
    onFujiNetStatusWaitChanged: (Boolean) -> Unit,
    onFujiNetBootModeSelected: (FujiNetBootMode) -> Unit,
    onResetToDefaults: () -> Unit,
    onPickSystemRom: (SystemRomKind) -> Unit,
    onClearSystemRom: (SystemRomKind) -> Unit,
    onPickHDevice: (Int) -> Unit,
    onClearHDevice: (Int) -> Unit,
    onPickLocalMedia: (MediaRole) -> Unit,
    onClearLocalMedia: (MediaRole) -> Unit,
    onCloseSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var aboutVisible by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { aboutVisible = true },
                        modifier = Modifier.widthIn(min = 84.dp),
                    ) {
                        Text("About")
                    }
                    FilledTonalButton(
                        onClick = onCloseSettings,
                        modifier = Modifier.widthIn(min = 84.dp),
                    ) {
                        Text("Close")
                    }
                }
            }

            SettingsTabStrip(
                selectedTab = launchSettingsState.selectedTab,
                onTabSelected = onSettingsTabSelected,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                when (launchSettingsState.selectedTab) {
                    SettingsTab.MACHINE -> MachineSettingsTab(
                        state = launchSettingsState,
                        onMachineTypeSelected = onMachineTypeSelected,
                        onMemoryProfileSelected = onMemoryProfileSelected,
                        onBasicEnabledChanged = onBasicEnabledChanged,
                        onVideoStandardSelected = onVideoStandardSelected,
                        onArtifactingModeSelected = onArtifactingModeSelected,
                        onNtscFilterPresetSelected = onNtscFilterPresetSelected,
                        onNtscFilterSharpnessChanged = onNtscFilterSharpnessChanged,
                        onNtscFilterResolutionChanged = onNtscFilterResolutionChanged,
                        onNtscFilterArtifactsChanged = onNtscFilterArtifactsChanged,
                        onNtscFilterFringingChanged = onNtscFilterFringingChanged,
                        onNtscFilterBleedChanged = onNtscFilterBleedChanged,
                        onNtscFilterBurstPhaseChanged = onNtscFilterBurstPhaseChanged,
                        onScanlinesChanged = onScanlinesChanged,
                        onPickSystemRom = onPickSystemRom,
                        onClearSystemRom = onClearSystemRom,
                    )

                    SettingsTab.FUJINET -> FujiNetSettingsTab(
                        state = launchSettingsState,
                        onOpenWebUi = onOpenFujiNetWebUi,
                        onOpenLogFile = { onOpenFujiNetLogFile(launchSettingsState.fujiNetConsoleLogPathLabel) },
                        onRefreshLog = onRefreshFujiNetLog,
                        onPrinterEnabledChanged = onFujiNetPrinterEnabledChanged,
                        onPrinterPortSelected = onFujiNetPrinterPortSelected,
                        onPrinterModelSelected = onFujiNetPrinterModelSelected,
                        onHsioIndexSelected = onFujiNetHsioIndexSelected,
                        onConfigBootEnabledChanged = onFujiNetConfigBootEnabledChanged,
                        onConfigNgChanged = onFujiNetConfigNgChanged,
                        onStatusWaitChanged = onFujiNetStatusWaitChanged,
                        onBootModeSelected = onFujiNetBootModeSelected,
                    )

                    SettingsTab.APP -> AppSettingsTab(
                        state = launchSettingsState,
                        onScaleModeSelected = onScaleModeSelected,
                        onEmulatorVolumeChanged = onEmulatorVolumeChanged,
                        onEmulatorVolumeChangeFinished = onEmulatorVolumeChangeFinished,
                        onKeepScreenOnChanged = onKeepScreenOnChanged,
                        onBackgroundAudioChanged = onBackgroundAudioChanged,
                        onOrientationModeSelected = onOrientationModeSelected,
                        onKeyboardInputModeSelected = onKeyboardInputModeSelected,
                        onKeyboardHapticsChanged = onKeyboardHapticsChanged,
                        onStickyKeyboardShiftChanged = onStickyKeyboardShiftChanged,
                        onStickyKeyboardCtrlChanged = onStickyKeyboardCtrlChanged,
                        onStickyKeyboardFnChanged = onStickyKeyboardFnChanged,
                        onJoystickInputStyleSelected = onJoystickInputStyleSelected,
                        onJoystickHapticsChanged = onJoystickHapticsChanged,
                        onPauseOnAppSwitchChanged = onPauseOnAppSwitchChanged,
                        onResetToDefaults = onResetToDefaults,
                    )
                }
            }

            if (launchSettingsState.restartRequiredVisible) {
                SettingsNotice(text = launchSettingsState.restartRequiredLabel)
            }

            Button(
                onClick = onCloseSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (launchSettingsState.restartRequiredVisible) "Close & Restart" else "Close Settings")
            }
        }
    }

    if (aboutVisible) {
        Dialog(onDismissRequest = { aboutVisible = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = stringResource(R.string.about_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = { uriHandler.openUri("https://fujinet.online") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.about_site_button_label))
                    }
                    OutlinedButton(
                        onClick = { aboutVisible = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabStrip(
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
    ) {
        SettingsTab.entries.forEachIndexed { index, tab ->
            val selected = tab == selectedTab
            SegmentedButton(
                selected = selected,
                onClick = { onTabSelected(tab) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = SettingsTab.entries.size,
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings-tab-${tab.name.lowercase()}"),
                label = {
                    Text(
                        text = tab.toLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun MachineSettingsTab(
    state: LaunchSettingsUiState,
    onMachineTypeSelected: (AtariMachineType) -> Unit,
    onMemoryProfileSelected: (MemoryProfile) -> Unit,
    onBasicEnabledChanged: (Boolean) -> Unit,
    onVideoStandardSelected: (VideoStandard) -> Unit,
    onArtifactingModeSelected: (ArtifactingMode) -> Unit,
    onNtscFilterPresetSelected: (NtscFilterPreset) -> Unit,
    onNtscFilterSharpnessChanged: (Float) -> Unit,
    onNtscFilterResolutionChanged: (Float) -> Unit,
    onNtscFilterArtifactsChanged: (Float) -> Unit,
    onNtscFilterFringingChanged: (Float) -> Unit,
    onNtscFilterBleedChanged: (Float) -> Unit,
    onNtscFilterBurstPhaseChanged: (Float) -> Unit,
    onScanlinesChanged: (Boolean) -> Unit,
    onPickSystemRom: (SystemRomKind) -> Unit,
    onClearSystemRom: (SystemRomKind) -> Unit,
) {
    SettingsSection(
        title = "Machine",
        subtitle = "Static machine identity and firmware. These changes restart the emulator when you close settings.",
    ) {
        SettingsGroup {
            SettingsPickerRow(
                title = "Machine type",
                value = state.machineTypeLabel,
                subtitle = "800, XL/XE, and XEGS presets.",
                options = AtariMachineType.entries.map { machineType ->
                    PickerOption(
                        value = machineType,
                        label = machineType.toShortLabel(),
                    )
                },
                selectedValue = state.settings.machineType,
                onSelected = onMachineTypeSelected,
                testTagPrefix = "machine-type",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsPickerRow(
                title = "RAM size",
                value = state.memoryProfileLabel,
                subtitle = state.settings.machineType.toMemoryHint(),
                options = state.settings.machineType.validMemoryProfiles().map { memoryProfile ->
                    PickerOption(
                        value = memoryProfile,
                        label = memoryProfile.toShortLabel(),
                    )
                },
                selectedValue = state.settings.memoryProfile,
                onSelected = onMemoryProfileSelected,
                testTagPrefix = "memory-profile",
            )

        }
    }

    SettingsSection(
        title = "Audio / Video",
        subtitle = "Display-mode options that remain available in the FujiNet-only MVP.",
    ) {
        SettingsGroup {
            SettingsPickerRow(
                title = "Video standard",
                value = state.videoStandardLabel,
                subtitle = "Applies immediately.",
                options = VideoStandard.entries.map { standard ->
                    PickerOption(value = standard, label = standard.toLabel())
                },
                selectedValue = state.settings.videoStandard,
                onSelected = onVideoStandardSelected,
                testTagPrefix = "video-standard",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsPickerRow(
                title = "Artifacting",
                value = state.artifactingModeLabel,
                subtitle = "NTSC and PAL artifacting modes for supported render paths.",
                options = ArtifactingMode.entries.map { artifactingMode ->
                    PickerOption(value = artifactingMode, label = artifactingMode.toShortLabel())
                },
                selectedValue = state.settings.artifactingMode,
                onSelected = onArtifactingModeSelected,
                testTagPrefix = "artifacting-mode",
            )

            if (state.ntscFilterControlsVisible) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsPickerRow(
                    title = "NTSC filter preset",
                    value = state.ntscFilterPresetLabel,
                    subtitle = "Composite, S-Video, RGB and Monochrome presets.",
                    options = NtscFilterPreset.entries
                        .filter { it != NtscFilterPreset.CUSTOM }
                        .map { preset ->
                            PickerOption(value = preset, label = preset.toLabel())
                        },
                    selectedValue = if (state.settings.ntscFilter.preset == NtscFilterPreset.CUSTOM) {
                        NtscFilterPreset.COMPOSITE
                    } else {
                        state.settings.ntscFilter.preset
                    },
                    onSelected = onNtscFilterPresetSelected,
                    testTagPrefix = "ntsc-filter-preset",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsSliderRow(
                    title = "Sharpness",
                    value = state.ntscFilterSharpnessLabel,
                    subtitle = "Range -1.0 to 1.0.",
                    sliderValue = state.settings.ntscFilter.sharpness,
                    onValueChange = onNtscFilterSharpnessChanged,
                    valueRange = -1f..1f,
                    steps = 99,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsSliderRow(
                    title = "Resolution",
                    value = state.ntscFilterResolutionLabel,
                    subtitle = "Range -1.0 to 1.0.",
                    sliderValue = state.settings.ntscFilter.resolution,
                    onValueChange = onNtscFilterResolutionChanged,
                    valueRange = -1f..1f,
                    steps = 99,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsSliderRow(
                    title = "Artifacts",
                    value = state.ntscFilterArtifactsLabel,
                    subtitle = "Luma artifacts caused by color changes.",
                    sliderValue = state.settings.ntscFilter.artifacts,
                    onValueChange = onNtscFilterArtifactsChanged,
                    valueRange = -1f..1f,
                    steps = 99,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsSliderRow(
                    title = "Fringing",
                    value = state.ntscFilterFringingLabel,
                    subtitle = "Chroma fringing caused by brightness changes.",
                    sliderValue = state.settings.ntscFilter.fringing,
                    onValueChange = onNtscFilterFringingChanged,
                    valueRange = -1f..1f,
                    steps = 99,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsSliderRow(
                    title = "Bleed",
                    value = state.ntscFilterBleedLabel,
                    subtitle = "Color bleed and chroma softening.",
                    sliderValue = state.settings.ntscFilter.bleed,
                    onValueChange = onNtscFilterBleedChanged,
                    valueRange = -1f..1f,
                    steps = 99,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsSliderRow(
                    title = "Burst phase",
                    value = state.ntscFilterBurstPhaseLabel,
                    subtitle = "Artifact color phase. Range -1.0 to 1.0.",
                    sliderValue = state.settings.ntscFilter.burstPhase,
                    onValueChange = onNtscFilterBurstPhaseChanged,
                    valueRange = -1f..1f,
                    steps = 99,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Scanlines",
                subtitle = "Overlay scanlines on the Android render output.",
                checked = state.settings.scanlinesEnabled,
                checkedLabel = state.scanlinesLabel,
                onToggle = { onScanlinesChanged(!state.settings.scanlinesEnabled) },
                testTagPrefix = "scanlines",
            )
        }
    }

    SettingsSection(
        title = "ROM & Firmware",
        subtitle = "Custom ROM files override the built-in Altirra defaults.",
    ) {
        SettingsGroup {
            SystemRomSettingsRow(
                title = "XL/XE ROM",
                currentLabel = state.xlxeRomLabel,
                onPick = { onPickSystemRom(SystemRomKind.XL_XE) },
                onClear = { onClearSystemRom(SystemRomKind.XL_XE) },
                hasSelection = state.settings.xlxeRomPath != null,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SystemRomSettingsRow(
                title = "BASIC ROM",
                currentLabel = state.basicRomLabel,
                onPick = { onPickSystemRom(SystemRomKind.BASIC) },
                onClear = { onClearSystemRom(SystemRomKind.BASIC) },
                hasSelection = state.settings.basicRomPath != null,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SystemRomSettingsRow(
                title = "400/800 ROM",
                currentLabel = state.atari400800RomLabel,
                onPick = { onPickSystemRom(SystemRomKind.ATARI_400_800) },
                onClear = { onClearSystemRom(SystemRomKind.ATARI_400_800) },
                hasSelection = state.settings.atari400800RomPath != null,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Boot with BASIC",
                subtitle = "If disabled, the emulator boots with BASIC off.",
                checked = state.settings.basicEnabled,
                checkedLabel = state.basicBootLabel,
                onToggle = { onBasicEnabledChanged(!state.settings.basicEnabled) },
                testTagPrefix = "basic-boot",
            )
        }
    }
}

private val FujiNetHsioOptions = listOf(
    -1 to "Disabled",
    0 to "0: 124Kb",
    1 to "1: 109Kb",
    2 to "2: 97Kb",
    3 to "3: 87Kb",
    4 to "4: 79Kb",
    5 to "5: 73Kb",
    6 to "6: 67Kb",
    7 to "7: 63Kb",
    8 to "8: 58Kb",
    9 to "9: 55Kb",
    10 to "10: 52Kb",
    16 to "16: 38Kb",
)

@Composable
private fun FujiNetSettingsTab(
    state: LaunchSettingsUiState,
    onOpenWebUi: () -> Unit,
    onOpenLogFile: () -> Unit,
    onRefreshLog: () -> Unit,
    onPrinterEnabledChanged: (Boolean) -> Unit,
    onPrinterPortSelected: (Int) -> Unit,
    onPrinterModelSelected: (String) -> Unit,
    onHsioIndexSelected: (Int) -> Unit,
    onConfigBootEnabledChanged: (Boolean) -> Unit,
    onConfigNgChanged: (Boolean) -> Unit,
    onStatusWaitChanged: (Boolean) -> Unit,
    onBootModeSelected: (FujiNetBootMode) -> Unit,
) {
    val fujiNet = state.fujiNetSettingsState
    SettingsSection(
        title = "FujiNet",
        subtitle = "Native controls for the common FujiNet options. Use the webUI for everything else.",
    ) {
        SettingsGroup {
            SettingsValueRow(
                title = "Runtime storage",
                subtitle = fujiNet.runtimeStoragePath,
            )
            FilledTonalButton(onClick = onOpenWebUi, modifier = Modifier.fillMaxWidth()) {
                Text("Open FujiNet webUI")
            }
            fujiNet.loadError?.takeIf { it.isNotBlank() }?.let { message ->
                SettingsNotice(text = "FujiNet webUI is not reachable. Showing file-backed values where possible. $message")
            }
        }
    }

    SettingsSection(title = "Printer", subtitle = "These controls map to FujiNet's printer form fields.") {
        SettingsGroup {
            SettingsToggleRow(
                title = "Use as virtual printer",
                subtitle = "Keep FujiNet printer emulation enabled.",
                checked = fujiNet.printerEnabled,
                checkedLabel = if (fujiNet.printerEnabled) "Enabled" else "Disabled",
                onToggle = { onPrinterEnabledChanged(!fujiNet.printerEnabled) },
                enabled = fujiNet.webUiReachable,
                testTagPrefix = "fujinet-printer",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsPickerRow(
                title = "Select port",
                value = "P${fujiNet.printerPort}",
                subtitle = "Choose the active FujiNet printer device slot.",
                options = listOf(1, 2, 3).map { port ->
                    PickerOption(value = port, label = "P$port")
                },
                selectedValue = fujiNet.printerPort,
                onSelected = onPrinterPortSelected,
                enabled = fujiNet.webUiReachable,
                testTagPrefix = "printer-port",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsPickerRow(
                title = "Emulated printer",
                value = fujiNet.printerModelLabel,
                subtitle = if (fujiNet.printerModels.isEmpty()) "Open FujiNet webUI for the complete printer list." else "Current printer model.",
                options = fujiNet.printerModels.map { model ->
                    PickerOption(value = model.value, label = model.toLabel())
                },
                selectedValue = fujiNet.printerModelValue,
                onSelected = onPrinterModelSelected,
                enabled = fujiNet.webUiReachable && fujiNet.printerModels.isNotEmpty(),
                testTagPrefix = "printer-model",
            )
        }
    }

    SettingsSection(title = "HSIO Settings", subtitle = "Native control for FujiNet high-speed SIO index.") {
        SettingsGroup {
            SettingsPickerRow(
                title = "HSIO Index",
                value = FujiNetHsioOptions.firstOrNull { it.first == fujiNet.hsioIndex }?.second ?: fujiNet.hsioIndex.toString(),
                subtitle = "Matches the same speed index exposed by the FujiNet webUI.",
                options = FujiNetHsioOptions.map { (index, label) ->
                    PickerOption(value = index, label = label)
                },
                selectedValue = fujiNet.hsioIndex,
                onSelected = onHsioIndexSelected,
                enabled = fujiNet.webUiReachable,
                testTagPrefix = "hsio-index",
            )
        }
    }

    SettingsSection(title = "Boot Settings", subtitle = "These controls map directly to FujiNet's CONFIG boot settings.") {
        SettingsGroup {
            SettingsToggleRow(
                title = "Enable CONFIG boot disk",
                subtitle = "Controls whether FujiNet inserts the CONFIG disk at boot.",
                checked = fujiNet.configBootEnabled,
                checkedLabel = if (fujiNet.configBootEnabled) "Enabled" else "Disabled",
                onToggle = { onConfigBootEnabledChanged(!fujiNet.configBootEnabled) },
                enabled = fujiNet.webUiReachable,
                testTagPrefix = "fujinet-config-boot",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Use CONFIG-NG boot disk",
                subtitle = "Switch between the classic CONFIG disk and CONFIG-NG.",
                checked = fujiNet.configNgEnabled,
                checkedLabel = if (fujiNet.configNgEnabled) "Enabled" else "Disabled",
                onToggle = { onConfigNgChanged(!fujiNet.configNgEnabled) },
                enabled = fujiNet.webUiReachable,
                testTagPrefix = "fujinet-config-ng",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Enable SIO STATUS wait routine",
                subtitle = "Matches the STATUS wait option in FujiNet's boot settings.",
                checked = fujiNet.statusWaitEnabled,
                checkedLabel = if (fujiNet.statusWaitEnabled) "Enabled" else "Disabled",
                onToggle = { onStatusWaitChanged(!fujiNet.statusWaitEnabled) },
                enabled = fujiNet.webUiReachable,
                testTagPrefix = "fujinet-status-wait",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsPickerRow(
                title = "CONFIG boot mode",
                value = fujiNet.bootMode.toLabel(),
                subtitle = "Choose between CONFIG and Mount All boot behavior.",
                options = FujiNetBootMode.entries.map { mode ->
                    PickerOption(value = mode, label = mode.toLabel())
                },
                selectedValue = fujiNet.bootMode,
                onSelected = onBootModeSelected,
                enabled = fujiNet.webUiReachable,
                testTagPrefix = "boot-mode",
            )
        }
    }

    SettingsSection(title = "Debug") {
        SettingsGroup {
            FilledTonalButton(onClick = onOpenLogFile, modifier = Modifier.fillMaxWidth()) {
                Text("Open log file")
            }
            OutlinedButton(onClick = onRefreshLog, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh log")
            }
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
            ) {
                SelectionContainer {
                    Text(
                        text = state.fujiNetRecentLogLabel.ifBlank { "No FujiNet console output captured yet." },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSettingsTab(
    state: LaunchSettingsUiState,
    onScaleModeSelected: (ScaleMode) -> Unit,
    onEmulatorVolumeChanged: (Int) -> Unit,
    onEmulatorVolumeChangeFinished: () -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onBackgroundAudioChanged: (Boolean) -> Unit,
    onOrientationModeSelected: (OrientationMode) -> Unit,
    onKeyboardInputModeSelected: (KeyboardInputMode) -> Unit,
    onKeyboardHapticsChanged: (Boolean) -> Unit,
    onStickyKeyboardShiftChanged: (Boolean) -> Unit,
    onStickyKeyboardCtrlChanged: (Boolean) -> Unit,
    onStickyKeyboardFnChanged: (Boolean) -> Unit,
    onJoystickInputStyleSelected: (JoystickInputStyle) -> Unit,
    onJoystickHapticsChanged: (Boolean) -> Unit,
    onPauseOnAppSwitchChanged: (Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
) {
    SettingsSection(title = "Display settings") {
        SettingsGroup {
            SettingsPickerRow(
                title = "Scale mode",
                value = state.scaleModeLabel,
                subtitle = "Choose between fit and fill for the display output.",
                options = ScaleMode.entries.map { mode ->
                    PickerOption(value = mode, label = mode.toLabel())
                },
                selectedValue = state.settings.scaleMode,
                onSelected = onScaleModeSelected,
                testTagPrefix = "scale-mode",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsPickerRow(
                title = "Rotate screen",
                value = state.orientationModeLabel,
                subtitle = "Follow system, portrait, or landscape.",
                options = OrientationMode.entries.map { mode ->
                    PickerOption(value = mode, label = mode.toLabel())
                },
                selectedValue = state.settings.orientationMode,
                onSelected = onOrientationModeSelected,
                testTagPrefix = "orientation-mode",
            )
        }
    }

    SettingsSection(title = "Input") {
        SettingsGroup {
            SettingsPickerRow(
                title = "Keyboard input",
                value = state.keyboardInputModeLabel,
                subtitle = "Switch between the built-in Atari keyboard and Android IME passthrough.",
                options = KeyboardInputMode.entries.map { mode ->
                    PickerOption(value = mode, label = mode.toLabel())
                },
                selectedValue = state.settings.keyboardInputMode,
                onSelected = onKeyboardInputModeSelected,
                testTagPrefix = "keyboard-input-mode",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Keyboard haptics",
                subtitle = "Pulse when keys are tapped on the internal Atari keyboard.",
                checked = state.settings.keyboardHapticsEnabled,
                checkedLabel = state.keyboardHapticsLabel,
                onToggle = { onKeyboardHapticsChanged(!state.settings.keyboardHapticsEnabled) },
                testTagPrefix = "keyboard-haptics",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Sticky Shift",
                subtitle = "Keep Shift enabled until tapped again. When off, it clears after the next character key.",
                checked = state.settings.stickyKeyboardShiftEnabled,
                checkedLabel = state.stickyKeyboardShiftLabel,
                onToggle = { onStickyKeyboardShiftChanged(!state.settings.stickyKeyboardShiftEnabled) },
                testTagPrefix = "sticky-keyboard-shift",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Sticky Ctrl",
                subtitle = "Keep Ctrl enabled until tapped again. When off, it clears after the next character key.",
                checked = state.settings.stickyKeyboardCtrlEnabled,
                checkedLabel = state.stickyKeyboardCtrlLabel,
                onToggle = { onStickyKeyboardCtrlChanged(!state.settings.stickyKeyboardCtrlEnabled) },
                testTagPrefix = "sticky-keyboard-ctrl",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Sticky Fn",
                subtitle = "Keep Fn enabled until tapped again. When off, it clears after the next character key.",
                checked = state.settings.stickyKeyboardFnEnabled,
                checkedLabel = state.stickyKeyboardFnLabel,
                onToggle = { onStickyKeyboardFnChanged(!state.settings.stickyKeyboardFnEnabled) },
                testTagPrefix = "sticky-keyboard-fn",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsPickerRow(
                title = "Joystick style",
                value = state.joystickInputStyleLabel,
                subtitle = "Choose between an 8-way stick and a 4-way D-pad.",
                options = JoystickInputStyle.entries.map { style ->
                    PickerOption(value = style, label = style.toLabel())
                },
                selectedValue = state.settings.joystickInputStyle,
                onSelected = onJoystickInputStyleSelected,
                testTagPrefix = "joystick-input-style",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Joystick haptics",
                subtitle = "Pulse when a new joystick direction is registered.",
                checked = state.settings.joystickHapticsEnabled,
                checkedLabel = state.joystickHapticsLabel,
                onToggle = { onJoystickHapticsChanged(!state.settings.joystickHapticsEnabled) },
                modifier = Modifier.testTag("joystick-haptics-toggle"),
                testTagPrefix = "joystick-haptics",
            )
        }
    }

    SettingsSection(title = "Power") {
        SettingsGroup {
            SettingsValueRow(
                title = "Emulator volume",
                value = state.emulatorVolumeLabel,
                subtitle = stringResource(R.string.power_audio_subtitle),
            )
            Slider(
                value = state.settings.emulatorVolumePercent.toFloat(),
                onValueChange = { onEmulatorVolumeChanged(it.roundToInt()) },
                onValueChangeFinished = onEmulatorVolumeChangeFinished,
                valueRange = 0f..100f,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Background audio",
                subtitle = "Keep emulation audio audible when the Activity is not focused.",
                checked = state.settings.backgroundAudioEnabled,
                checkedLabel = state.backgroundAudioLabel,
                onToggle = { onBackgroundAudioChanged(!state.settings.backgroundAudioEnabled) },
                modifier = Modifier.testTag("background-audio-toggle"),
                testTagPrefix = "background-audio",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Keep screen awake",
                subtitle = "Prevent the display from sleeping while the app is in front.",
                checked = state.settings.keepScreenOn,
                checkedLabel = state.keepScreenOnLabel,
                onToggle = { onKeepScreenOnChanged(!state.settings.keepScreenOn) },
                modifier = Modifier.testTag("keep-screen-awake-toggle"),
                testTagPrefix = "keep-screen-awake",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsToggleRow(
                title = "Pause on app switch",
                subtitle = stringResource(R.string.power_pause_switch_subtitle),
                checked = state.settings.pauseOnAppSwitch,
                checkedLabel = state.pauseOnAppSwitchLabel,
                onToggle = { onPauseOnAppSwitchChanged(!state.settings.pauseOnAppSwitch) },
                testTagPrefix = "pause-on-app-switch",
            )
        }
    }

    SettingsSection(title = "Reset") {
        OutlinedButton(onClick = onResetToDefaults, modifier = Modifier.fillMaxWidth()) {
            Text("Reset to defaults")
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String = "",
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsValueRow(
    title: String,
    value: String = "",
    subtitle: String = "",
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (value.isNotBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .widthIn(max = 180.dp)
                    .wrapContentWidth(Alignment.End),
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    checkedLabel: String,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    testTagPrefix: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsValueRow(title = title, value = checkedLabel, subtitle = subtitle)
        }
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) {
                { onToggle() }
            } else {
                null
            },
            modifier = (testTagPrefix?.let { Modifier.testTag(it) } ?: Modifier)
                .padding(start = 12.dp),
        )
    }
}

private data class PickerOption<T>(
    val value: T,
    val label: String,
)

@Composable
private fun <T> SettingsPickerRow(
    title: String,
    value: String,
    subtitle: String,
    options: List<PickerOption<T>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    testTagPrefix: String? = null,
) {
    var pickerVisible by rememberSaveable(title) { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { pickerVisible = true },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsValueRow(title = title, value = value, subtitle = subtitle)
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            },
            modifier = (testTagPrefix?.let { Modifier.testTag(it) } ?: Modifier)
                .padding(start = 12.dp),
        )
    }

    if (pickerVisible) {
        Dialog(onDismissRequest = { pickerVisible = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        options.forEachIndexed { index, option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelected(option.value)
                                        pickerVisible = false
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                RadioButton(
                                    selected = option.value == selectedValue,
                                    onClick = {
                                        onSelected(option.value)
                                        pickerVisible = false
                                    },
                                )
                            }
                            if (index != options.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { pickerVisible = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSliderRow(
    title: String,
    value: String,
    subtitle: String,
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsValueRow(
            title = title,
            value = value,
            subtitle = subtitle,
        )
        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

@Composable
private fun SettingsNotice(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun SystemRomSettingsRow(
    title: String,
    currentLabel: String,
    onPick: () -> Unit,
    onClear: () -> Unit,
    hasSelection: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsValueRow(
            title = title,
            value = if (hasSelection) "Custom" else "Built-in",
            subtitle = currentLabel,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onPick,
                modifier = Modifier.weight(1f),
            ) {
                Text("Choose file")
            }
            if (hasSelection) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Use built-in")
                }
            }
        }
    }
}

@Composable
private fun HDeviceSettingsRow(
    title: String,
    currentLabel: String,
    onPick: () -> Unit,
    onClear: () -> Unit,
    hasSelection: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsValueRow(
            title = title,
            value = if (hasSelection) "Mapped" else "Not set",
            subtitle = currentLabel,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onPick,
                modifier = Modifier.weight(1f),
            ) {
                Text("Choose folder")
            }
            if (hasSelection) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Clear")
                }
            }
        }
    }
}

private fun AtariMachineType.toShortLabel(): String = when (this) {
    AtariMachineType.ATARI_400_800 -> "400/800"
    AtariMachineType.ATARI_1200XL -> "1200XL"
    AtariMachineType.ATARI_800XL -> "800XL"
    AtariMachineType.ATARI_130XE -> "130XE"
    AtariMachineType.ATARI_320XE_COMPY -> "320XE C"
    AtariMachineType.ATARI_320XE_RAMBO -> "320XE R"
    AtariMachineType.ATARI_576XE -> "576XE"
    AtariMachineType.ATARI_1088XE -> "1088XE"
    AtariMachineType.ATARI_XEGS -> "XEGS"
    AtariMachineType.ATARI_5200 -> "5200"
}

private fun AtariMachineType.toMemoryHint(): String = when (this) {
    AtariMachineType.ATARI_400_800 -> "48 KB or 52 KB (+C RAM)"
    AtariMachineType.ATARI_1200XL -> "64 KB"
    AtariMachineType.ATARI_800XL -> "64 KB"
    AtariMachineType.ATARI_130XE -> "128 KB"
    AtariMachineType.ATARI_320XE_COMPY -> "320 KB (Compy-Shop)"
    AtariMachineType.ATARI_320XE_RAMBO -> "320 KB (Rambo)"
    AtariMachineType.ATARI_576XE -> "576 KB"
    AtariMachineType.ATARI_1088XE -> "1088 KB"
    AtariMachineType.ATARI_XEGS -> "64 KB"
    AtariMachineType.ATARI_5200 -> "16 KB"
}

private fun MemoryProfile.toShortLabel(): String = when (this) {
    MemoryProfile.RAM_16 -> "16K"
    MemoryProfile.RAM_48 -> "48K"
    MemoryProfile.RAM_52 -> "52K"
    MemoryProfile.RAM_64 -> "64K"
    MemoryProfile.RAM_128 -> "128K"
    MemoryProfile.RAM_320 -> "320K"
    MemoryProfile.RAM_576 -> "576K"
    MemoryProfile.RAM_1088 -> "1088K"
}

private fun SioPatchMode.toShortLabel(): String = when (this) {
    SioPatchMode.ENHANCED -> "Enhanced"
    SioPatchMode.NO_SIO_PATCH -> "No SIO"
    SioPatchMode.NO_PATCH_ALL -> "No patches"
}

private fun ArtifactingMode.toShortLabel(): String = when (this) {
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

private fun ScaleMode.toLabel(): String = when (this) {
    ScaleMode.FIT -> "Fit"
    ScaleMode.FILL -> "Fill"
    ScaleMode.INTEGER -> "Integer"
}

private fun OrientationMode.toLabel(): String = when (this) {
    OrientationMode.FOLLOW_SYSTEM -> "System"
    OrientationMode.PORTRAIT -> "Portrait"
    OrientationMode.LANDSCAPE -> "Landscape"
}

private fun KeyboardInputMode.toLabel(): String = when (this) {
    KeyboardInputMode.INTERNAL -> "Internal"
    KeyboardInputMode.ANDROID -> "Android"
}

private fun JoystickInputStyle.toLabel(): String = when (this) {
    JoystickInputStyle.STICK_8_WAY -> "8-way stick"
    JoystickInputStyle.DPAD_4_WAY -> "4-way D-pad"
}

private fun VideoStandard.toLabel(): String = when (this) {
    VideoStandard.NTSC -> "NTSC"
    VideoStandard.PAL -> "PAL"
}

private fun SettingsTab.toLabel(): String = when (this) {
    SettingsTab.MACHINE -> "Machine"
    SettingsTab.FUJINET -> "FujiNet"
    SettingsTab.APP -> "App"
}

@Composable
private fun BootStatusCard(
    sessionLabel: String,
    statusLabel: String,
    detailLabel: String,
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Boot status",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = sessionLabel,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (detailLabel.isNotBlank()) {
                Text(
                    text = detailLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun RecoveryStatusCard(
    sessionLabel: String,
    statusLabel: String,
    detailLabel: String,
    actionLabel: String,
    actionVisible: Boolean,
    onAction: () -> Unit,
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = sessionLabel,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = detailLabel,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (actionVisible) {
                ShellButton(label = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
private fun AndroidImeProxy(
    active: Boolean,
    onTextChanged: (String, String) -> Unit,
    onEnterPressed: () -> Unit,
) {
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(active) {
        if (active) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            value = ""
        }
    }

    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            onTextChanged(value, newValue)
            value = newValue.takeLast(32)
        },
        modifier = Modifier
            .testTag("android-ime-proxy")
            .width(1.dp)
            .height(1.dp)
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.None),
        keyboardActions = KeyboardActions(
            onDone = { onEnterPressed() },
            onGo = { onEnterPressed() },
            onNext = { onEnterPressed() },
            onPrevious = { onEnterPressed() },
            onSearch = { onEnterPressed() },
            onSend = { onEnterPressed() },
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Transparent),
    )
}

private fun SessionState.toLaunchModeLabel(): String = when (this) {
    is SessionState.ReadyToLaunch -> launchMode.toLabel()
    is SessionState.StartingFujiNet -> launchMode.toLabel()
    is SessionState.Starting -> launchMode.toLabel()
    is SessionState.Recovering -> launchMode.toLabel()
    is SessionState.Running -> launchMode.toLabel()
    is SessionState.Failed -> launchMode.toLabel()
    SessionState.Idle -> "No active session"
}

private fun SessionState.toAutoStartSessionLabel(): String = when (this) {
    is SessionState.ReadyToLaunch -> launchMode.toAutoStartSessionLabel()
    is SessionState.Recovering -> launchMode.toAutoStartSessionLabel()
    is SessionState.StartingFujiNet -> launchMode.toAutoStartSessionLabel()
    is SessionState.Starting -> launchMode.toAutoStartSessionLabel()
    is SessionState.Running -> launchMode.toAutoStartSessionLabel()
    is SessionState.Failed -> launchMode.toAutoStartSessionLabel()
    SessionState.Idle -> "Starting emulator"
}

private fun SessionState.toAutoStartStatusLabel(): String = when (this) {
    is SessionState.ReadyToLaunch -> launchMode.toAutoStartStatusLabel()
    is SessionState.Recovering -> launchMode.toAutoStartStatusLabel()
    is SessionState.StartingFujiNet -> launchMode.toAutoStartStatusLabel()
    is SessionState.Starting -> launchMode.toAutoStartStatusLabel()
    is SessionState.Running -> launchMode.toAutoStartStatusLabel()
    is SessionState.Failed -> launchMode.toAutoStartStatusLabel()
    SessionState.Idle -> "Preparing emulator"
}

private fun LaunchMode.toAutoStartSessionLabel(): String = when (this) {
    LaunchMode.FUJINET_ENABLED -> "Starting FujiNet"
    LaunchMode.LOCAL_ONLY -> "Starting emulator"
}

private fun LaunchMode.toAutoStartStatusLabel(): String = when (this) {
    LaunchMode.FUJINET_ENABLED -> "Waiting for FujiNet readiness"
    LaunchMode.LOCAL_ONLY -> "Preparing emulator"
}

private fun LaunchMode.toLabel(): String = when (this) {
    LaunchMode.FUJINET_ENABLED -> "FujiNet enabled"
    LaunchMode.LOCAL_ONLY -> "Local only"
}

private val portraitFunctionBarKeys = listOf(
    AtariFunctionKeySpec("ESC", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ESCAPE)),
    AtariFunctionKeySpec("HELP", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_HELP)),
    AtariFunctionKeySpec("START", AtariKeyMapping(consoleKey = AtariConsoleKey.START)),
    AtariFunctionKeySpec("SELECT", AtariKeyMapping(consoleKey = AtariConsoleKey.SELECT)),
    AtariFunctionKeySpec("OPTION", AtariKeyMapping(consoleKey = AtariConsoleKey.OPTION)),
)

private val landscapeLeftFunctionKeys = listOf(
    AtariFunctionKeySpec("HELP", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_HELP)),
    AtariFunctionKeySpec("START", AtariKeyMapping(consoleKey = AtariConsoleKey.START)),
)

private val landscapeRightFunctionKeys = listOf(
    AtariFunctionKeySpec("SELECT", AtariKeyMapping(consoleKey = AtariConsoleKey.SELECT)),
    AtariFunctionKeySpec("OPTION", AtariKeyMapping(consoleKey = AtariConsoleKey.OPTION)),
)

private fun internalKeyboardContainerHeight(compact: Boolean, dense: Boolean): androidx.compose.ui.unit.Dp {
    val outerVerticalPadding = when {
        dense -> 2.dp
        compact -> 6.dp
        else -> 10.dp
    }
    val rowSpacing = when {
        dense -> 2.dp
        compact -> 4.dp
        else -> 6.dp
    }
    val keyHeight = when {
        dense -> 26.dp
        compact -> 38.dp
        else -> 46.dp
    }
    val utilityButtonSize = when {
        dense -> 30.dp
        compact -> 44.dp
        else -> 56.dp
    }
    val arrowButtonHeight = when {
        dense -> 28.dp
        compact -> 40.dp
        else -> 48.dp
    }
    val utilityRowHeight = if (dense) {
        utilityButtonSize
    } else {
        maxOf(utilityButtonSize, (arrowButtonHeight * 2) + rowSpacing)
    }
    return (outerVerticalPadding * 2) + (rowSpacing * 5) + (keyHeight * 5) + utilityRowHeight
}

private fun shouldUsePortraitDenseKeyboard(
    contentHeight: androidx.compose.ui.unit.Dp,
    compact: Boolean,
): Boolean = contentHeight < if (compact) 220.dp else 280.dp

private fun joystickContainerHeight(compact: Boolean): androidx.compose.ui.unit.Dp {
    val verticalPadding = if (compact) 8.dp else 12.dp
    val footerSectionHeight = if (compact) 48.dp else 60.dp
    val sectionGap = if (compact) 4.dp else 8.dp
    val minimumControlSectionHeight = if (compact) 152.dp else 180.dp
    return (verticalPadding * 2) + footerSectionHeight + sectionGap + minimumControlSectionHeight
}

private fun portraitFunctionBarContainerHeight(compact: Boolean) = if (compact) 48.dp else 58.dp

private fun calculatePortraitInputPanelMetrics(
    maxAvailableHeight: androidx.compose.ui.unit.Dp,
    sizeFraction: Float,
): PortraitInputPanelMetrics {
    val minHeight = PortraitInputResizeChromeHeight.coerceAtMost(maxAvailableHeight)
    val maxHeight = maxAvailableHeight.coerceAtLeast(minHeight)
    if (maxHeight <= minHeight) {
        return PortraitInputPanelMetrics(
            totalHeight = maxHeight,
            minHeight = minHeight,
            maxHeight = maxHeight,
            fraction = 1f,
            contentHeight = 0.dp,
        )
    }

    val clampedFraction = sizeFraction.coerceIn(0f, 1f)
    val totalHeight = minHeight + ((maxHeight - minHeight) * clampedFraction)
    return PortraitInputPanelMetrics(
        totalHeight = totalHeight,
        minHeight = minHeight,
        maxHeight = maxHeight,
        fraction = clampedFraction,
        contentHeight = (totalHeight - PortraitInputResizeChromeHeight).coerceAtLeast(0.dp),
    )
}

private val CompactButtonHeight = 34.dp
private val StandardSectionSpacing = 8.dp
private val ScreenVerticalPadding = 12.dp
private val BottomChromeSpacing = 8.dp
private val PortraitControlsVerticalSpacing = StandardSectionSpacing + BottomChromeSpacing
private val PortraitInputResizeHandleHeight = 28.dp
private val PortraitInputResizeHandleSpacing = 6.dp
private val PortraitInputResizeChromeHeight = PortraitInputResizeHandleHeight + PortraitInputResizeHandleSpacing
private val PortraitInputDrawerContentThreshold = 56.dp
private const val PortraitInputDrawerCollapseFraction = 0.02f
private const val PortraitInputDrawerExpandSnapFraction = 0.98f
private const val EmulatorDisplayAspectRatio = 4f / 3f

private data class PortraitInputPanelMetrics(
    val totalHeight: androidx.compose.ui.unit.Dp,
    val minHeight: androidx.compose.ui.unit.Dp,
    val maxHeight: androidx.compose.ui.unit.Dp,
    val fraction: Float,
    val contentHeight: androidx.compose.ui.unit.Dp,
)

@Composable
private fun LocalMediaSection(
    state: LocalMediaUiState,
    onPick: (MediaRole) -> Unit,
    onClear: (MediaRole) -> Unit,
    includeRom: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MediaSlotCard(slot = state.disk, onPick = onPick, onClear = onClear)
        MediaSlotCard(slot = state.cartridge, onPick = onPick, onClear = onClear)
        MediaSlotCard(slot = state.executable, onPick = onPick, onClear = onClear)
        if (includeRom) {
            MediaSlotCard(slot = state.rom, onPick = onPick, onClear = onClear)
        }
    }
}

@Composable
private fun MediaSlotCard(
    slot: MediaSlotUiState,
    onPick: (MediaRole) -> Unit,
    onClear: (MediaRole) -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = slot.title, style = MaterialTheme.typography.titleSmall)
            Text(text = slot.displayLabel, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = { onPick(slot.role) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(slot.actionLabel)
                }
                if (slot.hasSelection) {
                    OutlinedButton(
                        onClick = { onClear(slot.role) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(slot.clearLabel)
                    }
                }
            }
        }
    }
}
