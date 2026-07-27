package com.mantismoonlabs.fujinetgo800.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mantismoonlabs.fujinetgo800.R
import com.mantismoonlabs.fujinetgo800.input.AtariKeyCode
import com.mantismoonlabs.fujinetgo800.input.AtariKeyMapping
import com.mantismoonlabs.fujinetgo800.ui.InputPanelToggleLongPressTimeoutMillis
import com.mantismoonlabs.fujinetgo800.ui.ShellIconButton

@Composable
fun AtariKeyboard(
    onKeyPressed: (AtariKeyMapping) -> Unit,
    onKeyReleased: (AtariKeyMapping) -> Unit,
    onToggleInputMode: () -> Unit,
    onToggleInputLongPress: () -> Unit,
    resetTrigger: Int,
    hapticsEnabled: Boolean,
    stickyShiftEnabled: Boolean,
    stickyCtrlEnabled: Boolean,
    stickyFnEnabled: Boolean,
    toggleIconResId: Int,
    toggleIconDescription: String,
    compact: Boolean = false,
    dense: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var fnEnabled by remember { mutableStateOf(false) }
    var shiftEnabled by remember { mutableStateOf(false) }
    var ctrlEnabled by remember { mutableStateOf(false) }
    var inverseEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(resetTrigger) {
        fnEnabled = false
        shiftEnabled = false
        ctrlEnabled = false
        inverseEnabled = false
    }
    val emitStrongHaptic = rememberFujiHaptic(FujiHapticPattern.KeyPress)
    val emitHaptic = {
        if (hapticsEnabled) {
            emitStrongHaptic()
        }
    }

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
    val keyGap = if (dense) 2.dp else if (compact) 3.dp else 4.dp
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
    val arrowClusterWidth = when {
        dense -> 90.dp
        compact -> 124.dp
        else -> 144.dp
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val dynamicKeyHeight = if (dense) {
                val availableHeight = maxHeight - (outerVerticalPadding * 2) - (rowSpacing * 5)
                (availableHeight / 6).coerceIn(26.dp, 38.dp)
            } else {
                keyHeight
            }
            val dynamicUtilityButtonSize = if (dense) {
                (dynamicKeyHeight + 4.dp).coerceIn(30.dp, 42.dp)
            } else {
                utilityButtonSize
            }
            val dynamicArrowButtonHeight = if (dense) {
                dynamicUtilityButtonSize
            } else {
                arrowButtonHeight
            }
            val dynamicArrowClusterWidth = if (dense) {
                (maxWidth * 0.34f).coerceIn(128.dp, 184.dp)
            } else {
                arrowClusterWidth
            }
            val stackedArrowClusterHeight = (dynamicArrowButtonHeight * 2) + keyGap
            val utilityRowHeightBudget = (
                maxHeight -
                    (outerVerticalPadding * 2) -
                    (rowSpacing * 5) -
                    (dynamicKeyHeight * 5)
                ).coerceAtLeast(0.dp)
            val useSingleRowArrows = stackedArrowClusterHeight > utilityRowHeightBudget

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = outerVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(rowSpacing),
            ) {
                keyboardRows().forEach { row ->
                    AtariKeyboardKeyRow(
                        row = row,
                        keyGap = keyGap,
                        keyHeight = dynamicKeyHeight,
                        fnEnabled = fnEnabled,
                        shiftEnabled = shiftEnabled,
                        ctrlEnabled = ctrlEnabled,
                        inverseEnabled = inverseEnabled,
                        hapticsEnabled = hapticsEnabled,
                        onTapFeedback = emitHaptic,
                        onKeyPressed = onKeyPressed,
                        onKeyReleased = onKeyReleased,
                        onShiftToggled = { shiftEnabled = !shiftEnabled },
                        onCtrlToggled = { ctrlEnabled = !ctrlEnabled },
                        onFnToggled = { fnEnabled = !fnEnabled },
                        onKeyReleaseSideEffects = { resolved ->
                            if (resolved.clearsShift && !stickyShiftEnabled) {
                                shiftEnabled = false
                            }
                            if (resolved.clearsCtrl && !stickyCtrlEnabled) {
                                ctrlEnabled = false
                            }
                            if (resolved.clearsFn && !stickyFnEnabled) {
                                fnEnabled = false
                            }
                        },
                    )
                }

                KeyboardUtilityRow(
                    inverseEnabled = inverseEnabled,
                    hapticsEnabled = hapticsEnabled,
                    onTapFeedback = emitHaptic,
                    onToggleInputMode = onToggleInputMode,
                    onToggleInputLongPress = onToggleInputLongPress,
                    toggleIconResId = toggleIconResId,
                    toggleIconDescription = toggleIconDescription,
                    onInversePressed = {
                        inverseEnabled = !inverseEnabled
                        onKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI))
                    },
                    onInverseReleased = {
                        onKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI))
                    },
                    onLeftPressed = { onKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_LEFT)) },
                    onLeftReleased = { onKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_LEFT)) },
                    onDownPressed = { onKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_DOWN)) },
                    onDownReleased = { onKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_DOWN)) },
                    onRightPressed = { onKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_RIGHT)) },
                    onRightReleased = { onKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_RIGHT)) },
                    onUpPressed = { onKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_UP)) },
                    onUpReleased = { onKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_UP)) },
                    buttonSize = dynamicUtilityButtonSize,
                    arrowButtonHeight = dynamicArrowButtonHeight,
                    arrowClusterWidth = dynamicArrowClusterWidth,
                    keyGap = keyGap,
                    singleRowArrows = useSingleRowArrows,
                )
            }
        }
    }
}

@Composable
private fun AtariKeyboardKeyRow(
    row: List<KeyboardKeySpec>,
    keyGap: androidx.compose.ui.unit.Dp,
    keyHeight: androidx.compose.ui.unit.Dp,
    fnEnabled: Boolean,
    shiftEnabled: Boolean,
    ctrlEnabled: Boolean,
    inverseEnabled: Boolean,
    hapticsEnabled: Boolean,
    onTapFeedback: () -> Unit,
    onKeyPressed: (AtariKeyMapping) -> Unit,
    onKeyReleased: (AtariKeyMapping) -> Unit,
    onShiftToggled: () -> Unit,
    onCtrlToggled: () -> Unit,
    onFnToggled: () -> Unit,
    onKeyReleaseSideEffects: (ResolvedKeyboardDispatch) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(keyGap),
    ) {
        row.forEach { key ->
            val resolved = resolveKeyDispatch(
                key = key,
                fnEnabled = fnEnabled,
                shiftEnabled = shiftEnabled,
                ctrlEnabled = ctrlEnabled,
            )
            val displayLabel = key.displayLabel(
                fnEnabled = fnEnabled,
                shiftEnabled = shiftEnabled,
                ctrlEnabled = ctrlEnabled,
            )
            val displayFontScale = key.displayFontScale(
                fnEnabled = fnEnabled,
                shiftEnabled = shiftEnabled,
                ctrlEnabled = ctrlEnabled,
            )
            AtariKeyboardKey(
                label = displayLabel,
                modifier = Modifier.weight(key.weight),
                fontScale = displayFontScale,
                fontWeight = key.fontWeight,
                hapticsEnabled = hapticsEnabled,
                active = when (key.kind) {
                    KeyboardKeyKind.SHIFT -> shiftEnabled
                    KeyboardKeyKind.CTRL -> ctrlEnabled
                    KeyboardKeyKind.FN -> fnEnabled
                    else -> false
                },
                inverse = inverseEnabled && key.isInverseEligible(fnEnabled = fnEnabled),
                enabled = true,
                onPressed = {
                    when (key.kind) {
                        KeyboardKeyKind.KEY -> resolved.pressMappings.forEach(onKeyPressed)
                        KeyboardKeyKind.SHIFT -> onShiftToggled()
                        KeyboardKeyKind.CTRL -> onCtrlToggled()
                        KeyboardKeyKind.FN -> onFnToggled()
                    }
                },
                onTapFeedback = onTapFeedback,
                onReleased = {
                    if (key.kind == KeyboardKeyKind.KEY) {
                        resolved.releaseMappings.forEach(onKeyReleased)
                        onKeyReleaseSideEffects(resolved)
                    }
                },
                height = keyHeight,
            )
        }
    }
}

/**
 * Holds the SHIFT/CTRL/SYM/inverse-video toggle state shared between a
 * [SplitAtariKeyboardLeft]/[SplitAtariKeyboardRight] pair, so toggling a modifier on one
 * half updates the key labels shown on the other. Create with
 * [rememberAtariKeyboardModifierState].
 */
class AtariKeyboardModifierState internal constructor() {
    var fnEnabled by mutableStateOf(false)
    var shiftEnabled by mutableStateOf(false)
    var ctrlEnabled by mutableStateOf(false)
    var inverseEnabled by mutableStateOf(false)
}

@Composable
fun rememberAtariKeyboardModifierState(resetTrigger: Int): AtariKeyboardModifierState {
    val state = remember { AtariKeyboardModifierState() }
    LaunchedEffect(resetTrigger) {
        state.fnEnabled = false
        state.shiftEnabled = false
        state.ctrlEnabled = false
        state.inverseEnabled = false
    }
    return state
}

/**
 * Left half of a split on-screen keyboard: number/letter columns (see [splitLeftRows]) plus
 * a bottom row with TAB, CTRL, and the toggle-input/inverse-video buttons (moved here from
 * the right half so the right rail can dedicate its full width to a centered arrow cluster).
 * Meant to sit in a rail to the left of the emulator viewport in landscape. Pair with
 * [SplitAtariKeyboardRight], both sharing the same [modifierState], so this is a drop-in
 * alternative to [AtariKeyboard] when the two halves need to live in separate parent
 * layouts (e.g. two different rail `Box`es flanking the emulator) rather than side by side
 * in one composable call.
 */
@Composable
fun SplitAtariKeyboardLeft(
    modifierState: AtariKeyboardModifierState,
    onKeyPressed: (AtariKeyMapping) -> Unit,
    onKeyReleased: (AtariKeyMapping) -> Unit,
    onToggleInputMode: () -> Unit,
    onToggleInputLongPress: () -> Unit,
    toggleIconResId: Int,
    toggleIconDescription: String,
    hapticsEnabled: Boolean,
    stickyShiftEnabled: Boolean,
    stickyCtrlEnabled: Boolean,
    stickyFnEnabled: Boolean,
    compact: Boolean = false,
    dense: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val emitStrongHaptic = rememberFujiHaptic(FujiHapticPattern.KeyPress)
    val emitHaptic = {
        if (hapticsEnabled) {
            emitStrongHaptic()
        }
    }
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
    val keyGap = if (dense) 2.dp else if (compact) 3.dp else 4.dp
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

    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(6.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 5 letter/number rows + a TAB/CTRL row + a toggle-input/inverse-video row = 7
            // rows total (6 gaps between them), one more than the right half's 6 rows.
            val dynamicKeyHeight = if (dense) {
                val availableHeight = maxHeight - (outerVerticalPadding * 2) - (rowSpacing * 6)
                (availableHeight / 7).coerceIn(26.dp, 38.dp)
            } else {
                keyHeight
            }
            val dynamicUtilityButtonSize = if (dense) {
                (dynamicKeyHeight + 4.dp).coerceIn(30.dp, 42.dp)
            } else {
                utilityButtonSize
            }
            val onShiftToggled = { modifierState.shiftEnabled = !modifierState.shiftEnabled }
            val onCtrlToggled = { modifierState.ctrlEnabled = !modifierState.ctrlEnabled }
            val onFnToggled = { modifierState.fnEnabled = !modifierState.fnEnabled }
            val onKeyReleaseSideEffects: (ResolvedKeyboardDispatch) -> Unit = { resolved ->
                if (resolved.clearsShift && !stickyShiftEnabled) {
                    modifierState.shiftEnabled = false
                }
                if (resolved.clearsCtrl && !stickyCtrlEnabled) {
                    modifierState.ctrlEnabled = false
                }
                if (resolved.clearsFn && !stickyFnEnabled) {
                    modifierState.fnEnabled = false
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = outerVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(rowSpacing),
            ) {
                splitLeftRows.forEach { row ->
                    AtariKeyboardKeyRow(
                        row = row,
                        keyGap = keyGap,
                        keyHeight = dynamicKeyHeight,
                        fnEnabled = modifierState.fnEnabled,
                        shiftEnabled = modifierState.shiftEnabled,
                        ctrlEnabled = modifierState.ctrlEnabled,
                        inverseEnabled = modifierState.inverseEnabled,
                        hapticsEnabled = hapticsEnabled,
                        onTapFeedback = emitHaptic,
                        onKeyPressed = onKeyPressed,
                        onKeyReleased = onKeyReleased,
                        onShiftToggled = onShiftToggled,
                        onCtrlToggled = onCtrlToggled,
                        onFnToggled = onFnToggled,
                        onKeyReleaseSideEffects = onKeyReleaseSideEffects,
                    )
                }

                AtariKeyboardKeyRow(
                    row = splitLeftUtilityKeys,
                    keyGap = keyGap,
                    keyHeight = dynamicKeyHeight,
                    fnEnabled = modifierState.fnEnabled,
                    shiftEnabled = modifierState.shiftEnabled,
                    ctrlEnabled = modifierState.ctrlEnabled,
                    inverseEnabled = modifierState.inverseEnabled,
                    hapticsEnabled = hapticsEnabled,
                    onTapFeedback = emitHaptic,
                    onKeyPressed = onKeyPressed,
                    onKeyReleased = onKeyReleased,
                    onShiftToggled = onShiftToggled,
                    onCtrlToggled = onCtrlToggled,
                    onFnToggled = onFnToggled,
                    onKeyReleaseSideEffects = onKeyReleaseSideEffects,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(keyGap),
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
                            .width(dynamicUtilityButtonSize),
                        height = dynamicUtilityButtonSize,
                    )
                    AtariKeyboardIconKey(
                        iconResId = R.drawable.ic_inverse,
                        contentDescription = if (modifierState.inverseEnabled) {
                            "Inverse video on"
                        } else {
                            "Inverse video off"
                        },
                        modifier = Modifier.width(dynamicUtilityButtonSize),
                        hapticsEnabled = hapticsEnabled,
                        active = modifierState.inverseEnabled,
                        enabled = true,
                        onPressed = {
                            modifierState.inverseEnabled = !modifierState.inverseEnabled
                            onKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI))
                        },
                        onTapFeedback = emitHaptic,
                        onReleased = {
                            onKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI))
                        },
                        height = dynamicUtilityButtonSize,
                    )
                }
            }
        }
    }
}

/**
 * Right half of a split on-screen keyboard: remaining number/letter columns (see
 * [splitRightRows]) plus a bottom row holding just the arrow cluster, centered -- the
 * toggle-input/inverse-video buttons live on [SplitAtariKeyboardLeft] instead. Meant to sit
 * in a rail to the right of the emulator viewport in landscape. Pair with
 * [SplitAtariKeyboardLeft], both sharing the same [modifierState].
 */
@Composable
fun SplitAtariKeyboardRight(
    modifierState: AtariKeyboardModifierState,
    onKeyPressed: (AtariKeyMapping) -> Unit,
    onKeyReleased: (AtariKeyMapping) -> Unit,
    hapticsEnabled: Boolean,
    stickyShiftEnabled: Boolean,
    stickyCtrlEnabled: Boolean,
    stickyFnEnabled: Boolean,
    compact: Boolean = false,
    dense: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val emitStrongHaptic = rememberFujiHaptic(FujiHapticPattern.KeyPress)
    val emitHaptic = {
        if (hapticsEnabled) {
            emitStrongHaptic()
        }
    }
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
    val keyGap = if (dense) 2.dp else if (compact) 3.dp else 4.dp
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
    val arrowClusterWidth = when {
        dense -> 90.dp
        compact -> 124.dp
        else -> 144.dp
    }

    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(6.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val dynamicKeyHeight = if (dense) {
                val availableHeight = maxHeight - (outerVerticalPadding * 2) - (rowSpacing * 5)
                (availableHeight / 6).coerceIn(26.dp, 38.dp)
            } else {
                keyHeight
            }
            val dynamicUtilityButtonSize = if (dense) {
                (dynamicKeyHeight + 4.dp).coerceIn(30.dp, 42.dp)
            } else {
                utilityButtonSize
            }
            val dynamicArrowButtonHeight = if (dense) {
                dynamicUtilityButtonSize
            } else {
                arrowButtonHeight
            }
            val dynamicArrowClusterWidth = if (dense) {
                (maxWidth * 0.34f).coerceIn(128.dp, 184.dp)
            } else {
                arrowClusterWidth
            }
            val stackedArrowClusterHeight = (dynamicArrowButtonHeight * 2) + keyGap
            val utilityRowHeightBudget = (
                maxHeight -
                    (outerVerticalPadding * 2) -
                    (rowSpacing * 5) -
                    (dynamicKeyHeight * 5)
                ).coerceAtLeast(0.dp)
            val useSingleRowArrows = stackedArrowClusterHeight > utilityRowHeightBudget

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = outerVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(rowSpacing),
            ) {
                splitRightRows.forEach { row ->
                    AtariKeyboardKeyRow(
                        row = row,
                        keyGap = keyGap,
                        keyHeight = dynamicKeyHeight,
                        fnEnabled = modifierState.fnEnabled,
                        shiftEnabled = modifierState.shiftEnabled,
                        ctrlEnabled = modifierState.ctrlEnabled,
                        inverseEnabled = modifierState.inverseEnabled,
                        hapticsEnabled = hapticsEnabled,
                        onTapFeedback = emitHaptic,
                        onKeyPressed = onKeyPressed,
                        onKeyReleased = onKeyReleased,
                        onShiftToggled = { modifierState.shiftEnabled = !modifierState.shiftEnabled },
                        onCtrlToggled = { modifierState.ctrlEnabled = !modifierState.ctrlEnabled },
                        onFnToggled = { modifierState.fnEnabled = !modifierState.fnEnabled },
                        onKeyReleaseSideEffects = { resolved ->
                            if (resolved.clearsShift && !stickyShiftEnabled) {
                                modifierState.shiftEnabled = false
                            }
                            if (resolved.clearsCtrl && !stickyCtrlEnabled) {
                                modifierState.ctrlEnabled = false
                            }
                            if (resolved.clearsFn && !stickyFnEnabled) {
                                modifierState.fnEnabled = false
                            }
                        },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    ArrowCluster(
                        onLeftPressed = { onKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_LEFT)) },
                        onLeftReleased = { onKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_LEFT)) },
                        onDownPressed = { onKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_DOWN)) },
                        onDownReleased = { onKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_DOWN)) },
                        onRightPressed = { onKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_RIGHT)) },
                        onRightReleased = { onKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_RIGHT)) },
                        onUpPressed = { onKeyPressed(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_UP)) },
                        onUpReleased = { onKeyReleased(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_UP)) },
                        hapticsEnabled = hapticsEnabled,
                        onTapFeedback = emitHaptic,
                        buttonHeight = dynamicArrowButtonHeight,
                        keyGap = keyGap,
                        singleRowLayout = useSingleRowArrows,
                        modifier = Modifier.width(dynamicArrowClusterWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardUtilityRow(
    inverseEnabled: Boolean,
    hapticsEnabled: Boolean,
    onTapFeedback: () -> Unit,
    onToggleInputMode: () -> Unit,
    onToggleInputLongPress: () -> Unit,
    toggleIconResId: Int,
    toggleIconDescription: String,
    onInversePressed: () -> Unit,
    onInverseReleased: () -> Unit,
    onLeftPressed: () -> Unit,
    onLeftReleased: () -> Unit,
    onDownPressed: () -> Unit,
    onDownReleased: () -> Unit,
    onRightPressed: () -> Unit,
    onRightReleased: () -> Unit,
    onUpPressed: () -> Unit,
    onUpReleased: () -> Unit,
    buttonSize: androidx.compose.ui.unit.Dp,
    arrowButtonHeight: androidx.compose.ui.unit.Dp,
    arrowClusterWidth: androidx.compose.ui.unit.Dp,
    keyGap: androidx.compose.ui.unit.Dp,
    singleRowArrows: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(keyGap),
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
                    .size(buttonSize),
                height = buttonSize,
            )
            AtariKeyboardIconKey(
                iconResId = R.drawable.ic_inverse,
                contentDescription = if (inverseEnabled) {
                    "Inverse video on"
                } else {
                    "Inverse video off"
                },
                modifier = Modifier.width(buttonSize),
                hapticsEnabled = hapticsEnabled,
                active = inverseEnabled,
                enabled = true,
                onPressed = onInversePressed,
                onTapFeedback = onTapFeedback,
                onReleased = onInverseReleased,
                height = buttonSize,
            )
        }
        ArrowCluster(
            onLeftPressed = onLeftPressed,
            onLeftReleased = onLeftReleased,
            onDownPressed = onDownPressed,
            onDownReleased = onDownReleased,
            onRightPressed = onRightPressed,
            onRightReleased = onRightReleased,
            onUpPressed = onUpPressed,
            onUpReleased = onUpReleased,
            hapticsEnabled = hapticsEnabled,
            onTapFeedback = onTapFeedback,
            buttonHeight = arrowButtonHeight,
            keyGap = keyGap,
            singleRowLayout = singleRowArrows,
            modifier = Modifier.width(arrowClusterWidth),
        )
    }
}

@Composable
private fun ArrowCluster(
    onLeftPressed: () -> Unit,
    onLeftReleased: () -> Unit,
    onDownPressed: () -> Unit,
    onDownReleased: () -> Unit,
    onRightPressed: () -> Unit,
    onRightReleased: () -> Unit,
    onUpPressed: () -> Unit,
    onUpReleased: () -> Unit,
    hapticsEnabled: Boolean,
    onTapFeedback: () -> Unit,
    buttonHeight: androidx.compose.ui.unit.Dp,
    keyGap: androidx.compose.ui.unit.Dp,
    singleRowLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    if (singleRowLayout) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(keyGap),
            verticalAlignment = Alignment.Bottom,
        ) {
            AtariKeyboardKey(
                label = "↑",
                modifier = Modifier.weight(1f),
                fontScale = 1.5f,
                fontWeight = FontWeight.Bold,
                hapticsEnabled = hapticsEnabled,
                active = false,
                enabled = true,
                onPressed = onUpPressed,
                onTapFeedback = onTapFeedback,
                onReleased = onUpReleased,
                height = buttonHeight,
            )
            AtariKeyboardKey(
                label = "↓",
                modifier = Modifier.weight(1f),
                fontScale = 1.5f,
                fontWeight = FontWeight.Bold,
                hapticsEnabled = hapticsEnabled,
                active = false,
                enabled = true,
                onPressed = onDownPressed,
                onTapFeedback = onTapFeedback,
                onReleased = onDownReleased,
                height = buttonHeight,
            )
            AtariKeyboardKey(
                label = "←",
                modifier = Modifier.weight(1f),
                fontScale = 1.5f,
                fontWeight = FontWeight.Bold,
                hapticsEnabled = hapticsEnabled,
                active = false,
                enabled = true,
                onPressed = onLeftPressed,
                onTapFeedback = onTapFeedback,
                onReleased = onLeftReleased,
                height = buttonHeight,
            )
            AtariKeyboardKey(
                label = "→",
                modifier = Modifier.weight(1f),
                fontScale = 1.5f,
                fontWeight = FontWeight.Bold,
                hapticsEnabled = hapticsEnabled,
                active = false,
                enabled = true,
                onPressed = onRightPressed,
                onTapFeedback = onTapFeedback,
                onReleased = onRightReleased,
                height = buttonHeight,
            )
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(keyGap),
        ) {
            // Match the up-arrow's width to the left/down/right row below (each of those
            // gets one third of the full width via weight(1f)) by giving it the same
            // weight(1f) treatment flanked by equal-weight spacers, instead of a separate
            // fixed upButtonWidth that didn't line up with the row beneath it.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(keyGap),
            ) {
                Spacer(modifier = Modifier.weight(1f))
                AtariKeyboardKey(
                    label = "↑",
                    modifier = Modifier.weight(1f),
                    fontScale = 1.5f,
                    fontWeight = FontWeight.Bold,
                    hapticsEnabled = hapticsEnabled,
                    active = false,
                    enabled = true,
                    onPressed = onUpPressed,
                    onTapFeedback = onTapFeedback,
                    onReleased = onUpReleased,
                    height = buttonHeight,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(keyGap),
            ) {
                AtariKeyboardKey(
                    label = "←",
                    modifier = Modifier.weight(1f),
                    fontScale = 1.5f,
                    fontWeight = FontWeight.Bold,
                    hapticsEnabled = hapticsEnabled,
                    active = false,
                    enabled = true,
                    onPressed = onLeftPressed,
                    onTapFeedback = onTapFeedback,
                    onReleased = onLeftReleased,
                    height = buttonHeight,
                )
                AtariKeyboardKey(
                    label = "↓",
                    modifier = Modifier.weight(1f),
                    fontScale = 1.5f,
                    fontWeight = FontWeight.Bold,
                    hapticsEnabled = hapticsEnabled,
                    active = false,
                    enabled = true,
                    onPressed = onDownPressed,
                    onTapFeedback = onTapFeedback,
                    onReleased = onDownReleased,
                    height = buttonHeight,
                )
                AtariKeyboardKey(
                    label = "→",
                    modifier = Modifier.weight(1f),
                    fontScale = 1.5f,
                    fontWeight = FontWeight.Bold,
                    hapticsEnabled = hapticsEnabled,
                    active = false,
                    enabled = true,
                    onPressed = onRightPressed,
                    onTapFeedback = onTapFeedback,
                    onReleased = onRightReleased,
                    height = buttonHeight,
                )
            }
        }
    }
}

@Composable
private fun AtariKeyboardKey(
    label: String,
    modifier: Modifier,
    fontScale: Float,
    fontWeight: FontWeight,
    hapticsEnabled: Boolean,
    active: Boolean,
    inverse: Boolean = false,
    enabled: Boolean,
    onPressed: () -> Unit,
    onTapFeedback: () -> Unit,
    onReleased: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 46.dp,
) {
    val shape = RoundedCornerShape(6.dp)
    val backgroundColor = when {
        inverse -> MaterialTheme.colorScheme.onSurface
        active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = if (inverse) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val currentOnPressed by rememberUpdatedState(onPressed)
    val currentOnTapFeedback by rememberUpdatedState(onTapFeedback)
    val currentOnReleased by rememberUpdatedState(onReleased)
    Box(
        modifier = modifier
            .height(height)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
            .background(backgroundColor, shape)
            .pointerInput(label, active) {
                detectTapGestures(
                    onPress = {
                        if (hapticsEnabled) {
                            currentOnTapFeedback()
                        }
                        currentOnPressed()
                        try {
                            tryAwaitRelease()
                        } finally {
                            currentOnReleased()
                        }
                    },
                )
            }
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            fontWeight = fontWeight,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = MaterialTheme.typography.labelMedium.fontSize * fontScale,
            ),
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun AtariKeyboardIconKey(
    iconResId: Int,
    contentDescription: String,
    modifier: Modifier,
    hapticsEnabled: Boolean,
    active: Boolean,
    enabled: Boolean,
    onPressed: () -> Unit,
    onTapFeedback: () -> Unit,
    onReleased: () -> Unit,
    height: androidx.compose.ui.unit.Dp,
) {
    val shape = RoundedCornerShape(6.dp)
    val backgroundColor = if (active) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val currentOnPressed by rememberUpdatedState(onPressed)
    val currentOnTapFeedback by rememberUpdatedState(onTapFeedback)
    val currentOnReleased by rememberUpdatedState(onReleased)
    Box(
        modifier = modifier
            .height(height)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
            .background(backgroundColor, shape)
            .pointerInput(contentDescription, active) {
                detectTapGestures(
                    onPress = {
                        if (hapticsEnabled) {
                            currentOnTapFeedback()
                        }
                        currentOnPressed()
                        try {
                            tryAwaitRelease()
                        } finally {
                            currentOnReleased()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

internal fun resolveKeyDispatch(
    key: KeyboardKeySpec,
    fnEnabled: Boolean,
    shiftEnabled: Boolean,
    ctrlEnabled: Boolean,
): ResolvedKeyboardDispatch {
    if (key.kind != KeyboardKeyKind.KEY) {
        return ResolvedKeyboardDispatch()
    }
    val layerMapping = when {
        fnEnabled && key.fnMapping != null -> key.fnMapping
        else -> key.mapping
    }
    val effectiveMapping = layerMapping?.let {
        when {
            !fnEnabled && shiftEnabled && key.shiftedMapping != null -> key.shiftedMapping
            else -> applyModifiers(
                mapping = it,
                shiftEnabled = !fnEnabled && shiftEnabled,
                ctrlEnabled = ctrlEnabled,
                supportsModifiers = key.supportsModifiers,
            )
        }
    }
    val pressMappings = effectiveMapping?.let(::listOf).orEmpty()
    val releaseMappings = effectiveMapping?.let(::listOf).orEmpty()
    return ResolvedKeyboardDispatch(
        pressMappings = pressMappings,
        releaseMappings = releaseMappings,
        clearsShift = shiftEnabled,
        clearsCtrl = ctrlEnabled,
        clearsFn = fnEnabled,
    )
}

private fun applyModifiers(
    mapping: AtariKeyMapping,
    shiftEnabled: Boolean,
    ctrlEnabled: Boolean,
    supportsModifiers: Boolean,
): AtariKeyMapping {
    val aKeyCode = mapping.aKeyCode ?: return mapping
    if (!supportsModifiers || aKeyCode < 0) {
        return mapping
    }
    var effectiveCode = aKeyCode
    if (shiftEnabled) {
        effectiveCode = effectiveCode or AtariKeyCode.AKEY_SHFT
    }
    if (ctrlEnabled) {
        effectiveCode = effectiveCode or AtariKeyCode.AKEY_CTRL
    }
    return mapping.copy(aKeyCode = effectiveCode)
}

internal enum class KeyboardKeyKind {
    KEY,
    SHIFT,
    CTRL,
    FN,
}

internal data class KeyboardKeySpec(
    val label: String,
    val mapping: AtariKeyMapping? = null,
    val kind: KeyboardKeyKind = KeyboardKeyKind.KEY,
    val weight: Float = 1f,
    val fontScale: Float = 1.3f,
    val fontWeight: FontWeight = FontWeight.SemiBold,
    val supportsModifiers: Boolean = false,
    val inverseEligible: Boolean = true,
    val shiftedLabel: String? = null,
    val shiftedMapping: AtariKeyMapping? = null,
    val shiftedFontScale: Float? = null,
    val ctrlLabel: String? = null,
    val ctrlFontScale: Float? = null,
    val fnLabel: String? = null,
    val fnMapping: AtariKeyMapping? = null,
    val fnFontScale: Float? = null,
    val fnInverseEligible: Boolean = true,
)

internal data class ResolvedKeyboardDispatch(
    val pressMappings: List<AtariKeyMapping> = emptyList(),
    val releaseMappings: List<AtariKeyMapping> = emptyList(),
    val clearsShift: Boolean = false,
    val clearsCtrl: Boolean = false,
    val clearsFn: Boolean = false,
)

internal fun KeyboardKeySpec.displayLabel(
    fnEnabled: Boolean,
    shiftEnabled: Boolean,
    ctrlEnabled: Boolean = false,
): String {
    return when {
        fnEnabled && fnLabel != null -> fnLabel
        !fnEnabled && shiftEnabled && shiftedLabel != null -> shiftedLabel
        !fnEnabled && ctrlEnabled && ctrlLabel != null -> ctrlLabel
        else -> label
    }
}

internal fun KeyboardKeySpec.displayFontScale(
    fnEnabled: Boolean,
    shiftEnabled: Boolean,
    ctrlEnabled: Boolean = false,
): Float {
    return when {
        fnEnabled && fnLabel != null -> fnFontScale ?: fontScale
        !fnEnabled && shiftEnabled && shiftedLabel != null -> shiftedFontScale ?: fontScale
        !fnEnabled && ctrlEnabled && ctrlLabel != null -> ctrlFontScale ?: fontScale
        else -> fontScale
    }
}

internal fun KeyboardKeySpec.isInverseEligible(fnEnabled: Boolean): Boolean {
    if (kind != KeyboardKeyKind.KEY) {
        return false
    }
    return if (fnEnabled && fnMapping != null) {
        fnInverseEligible
    } else {
        inverseEligible
    }
}

internal fun keyboardRows(): List<List<KeyboardKeySpec>> = listOf(
    numberRow,
    qwertyRow,
    homeRow,
    controlRow,
    bottomRow,
)

private val numberRow = listOf(
    KeyboardKeySpec("1", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_1), supportsModifiers = true, shiftedLabel = "!", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_EXCLAMATION)),
    KeyboardKeySpec("2", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_2), supportsModifiers = true, shiftedLabel = "\"", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_DBLQUOTE)),
    KeyboardKeySpec("3", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_3), supportsModifiers = true, shiftedLabel = "#", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_HASH)),
    KeyboardKeySpec("4", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_4), supportsModifiers = true, shiftedLabel = "$", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_DOLLAR)),
    KeyboardKeySpec("5", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_5), supportsModifiers = true, shiftedLabel = "%", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_PERCENT)),
    KeyboardKeySpec("6", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_6), supportsModifiers = true, shiftedLabel = "&", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_AMPERSAND)),
    KeyboardKeySpec("7", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_7), supportsModifiers = true, shiftedLabel = "'", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_QUOTE)),
    KeyboardKeySpec("8", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_8), supportsModifiers = true, shiftedLabel = "@", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_AT), fnLabel = "CLR", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_CLEAR), fnFontScale = 0.98f, fnInverseEligible = false),
    KeyboardKeySpec("9", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_9), supportsModifiers = true, shiftedLabel = "(", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_PARENLEFT), fnLabel = "INS", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_INSERT_CHAR), fnFontScale = 0.98f, fnInverseEligible = false),
    KeyboardKeySpec("0", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_0), supportsModifiers = true, shiftedLabel = ")", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_PARENRIGHT), fnLabel = "BRK", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BREAK), fnFontScale = 0.98f, fnInverseEligible = false),
)

private val qwertyRow = listOf(
    KeyboardKeySpec("q", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_q), supportsModifiers = true, shiftedLabel = "Q"),
    KeyboardKeySpec("w", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_w), supportsModifiers = true, shiftedLabel = "W"),
    KeyboardKeySpec("e", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_e), supportsModifiers = true, shiftedLabel = "E"),
    KeyboardKeySpec("r", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_r), supportsModifiers = true, shiftedLabel = "R"),
    KeyboardKeySpec("t", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_t), supportsModifiers = true, shiftedLabel = "T"),
    KeyboardKeySpec("y", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_y), supportsModifiers = true, shiftedLabel = "Y"),
    KeyboardKeySpec("u", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_u), supportsModifiers = true, shiftedLabel = "U"),
    KeyboardKeySpec("i", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_i), supportsModifiers = true, shiftedLabel = "I", fnLabel = "[", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BRACKETLEFT)),
    KeyboardKeySpec("o", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_o), supportsModifiers = true, shiftedLabel = "O", fnLabel = "]", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BRACKETRIGHT)),
    KeyboardKeySpec("p", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_p), supportsModifiers = true, shiftedLabel = "P", fnLabel = "CAPS", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_CAPSTOGGLE), fnFontScale = 0.88f, fnInverseEligible = false),
)

private val homeRow = listOf(
    KeyboardKeySpec("TAB", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_TAB), weight = 1.32f, fontScale = 1f, inverseEligible = false),
    KeyboardKeySpec("a", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_a), supportsModifiers = true, shiftedLabel = "A"),
    KeyboardKeySpec("s", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_s), supportsModifiers = true, shiftedLabel = "S"),
    KeyboardKeySpec("d", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_d), supportsModifiers = true, shiftedLabel = "D"),
    KeyboardKeySpec("f", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_f), supportsModifiers = true, shiftedLabel = "F"),
    KeyboardKeySpec("g", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_g), supportsModifiers = true, shiftedLabel = "G", fnLabel = ";", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_SEMICOLON)),
    KeyboardKeySpec("h", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_h), supportsModifiers = true, shiftedLabel = "H", fnLabel = "_", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_UNDERSCORE)),
    KeyboardKeySpec("j", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_j), supportsModifiers = true, shiftedLabel = "J", fnLabel = "-", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_MINUS)),
    KeyboardKeySpec("k", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_k), supportsModifiers = true, shiftedLabel = "K", fnLabel = "|", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BAR)),
    KeyboardKeySpec("l", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_l), supportsModifiers = true, shiftedLabel = "L", fnLabel = "=", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_EQUAL)),
)

private val controlRow = listOf(
    KeyboardKeySpec("CTRL", kind = KeyboardKeyKind.CTRL, weight = 1.34f, fontScale = 1f),
    KeyboardKeySpec("z", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_z), supportsModifiers = true, shiftedLabel = "Z"),
    KeyboardKeySpec("x", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_x), supportsModifiers = true, shiftedLabel = "X"),
    KeyboardKeySpec("c", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_c), supportsModifiers = true, shiftedLabel = "C", fnLabel = ":", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_COLON)),
    KeyboardKeySpec("v", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_v), supportsModifiers = true, shiftedLabel = "V", fnLabel = "\\", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BACKSLASH)),
    KeyboardKeySpec("b", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_b), supportsModifiers = true, shiftedLabel = "B", fnLabel = "+", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_PLUS)),
    KeyboardKeySpec("n", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_n), supportsModifiers = true, shiftedLabel = "N", fnLabel = "^", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_CIRCUMFLEX)),
    KeyboardKeySpec("m", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_m), supportsModifiers = true, shiftedLabel = "M", fnLabel = "*", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ASTERISK)),
    KeyboardKeySpec("⌫", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BACKSPACE), supportsModifiers = true, shiftedLabel = "DEL-L", ctrlLabel = "DEL", weight = 1.28f, fontScale = 1.2f, fontWeight = FontWeight.Bold, inverseEligible = false),
)

private val bottomRow = listOf(
    KeyboardKeySpec("SHIFT", kind = KeyboardKeyKind.SHIFT, weight = 1.52f, fontScale = 1f),
    KeyboardKeySpec("SYM", kind = KeyboardKeyKind.FN, weight = 0.92f, fontScale = 1f),
    KeyboardKeySpec(".", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_FULLSTOP), weight = 0.92f, supportsModifiers = true, shiftedLabel = ",", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_COMMA), fnLabel = "<", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_LESS)),
    KeyboardKeySpec("▁", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_SPACE), weight = 3.26f, fontScale = 1.2f, fontWeight = FontWeight.Bold, fnLabel = "▁"),
    KeyboardKeySpec("/", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_SLASH), weight = 0.92f, supportsModifiers = true, shiftedLabel = "?", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_QUESTION), fnLabel = ">", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_GREATER)),
    KeyboardKeySpec("↵", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_RETURN), weight = 1.24f, fontScale = 1.52f, fontWeight = FontWeight.Bold, inverseEligible = false),
)

// Split-keyboard row slicing for SplitAtariKeyboardLeft/SplitAtariKeyboardRight: each row is
// cut by hand/column position (physical split keyboard style), reusing the same key specs
// above rather than redefining them. The spacebar is split into two half-weight keys that
// both send AKEY_SPACE. TAB and CTRL are pulled out of the home/control rows entirely (so
// the remaining letter keys on the left get more width) and rendered together as their own
// row -- see splitLeftUtilityKeys -- alongside the toggle-input/inverse-video buttons.
internal val halfSpaceKey = bottomRow[3].copy(weight = bottomRow[3].weight / 2f)

internal val splitLeftUtilityKeys: List<KeyboardKeySpec> = listOf(homeRow[0], controlRow[0])

internal val splitLeftRows: List<List<KeyboardKeySpec>> = listOf(
    numberRow.take(5),
    qwertyRow.take(5),
    homeRow.subList(1, 6),
    controlRow.subList(1, 6),
    bottomRow.take(3) + halfSpaceKey,
)

internal val splitRightRows: List<List<KeyboardKeySpec>> = listOf(
    numberRow.drop(5),
    qwertyRow.drop(5),
    homeRow.drop(6),
    controlRow.drop(6),
    listOf(halfSpaceKey) + bottomRow.drop(4),
)
