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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var atariEnabled by remember { mutableStateOf(false) }
    val showAtariModifierButton = false
    LaunchedEffect(resetTrigger) {
        fnEnabled = false
        shiftEnabled = false
        ctrlEnabled = false
        atariEnabled = false
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
    val upButtonWidth = when {
        dense -> 26.dp
        compact -> 38.dp
        else -> 44.dp
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = outerVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(rowSpacing),
            ) {
                keyboardRows().forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(keyGap),
                    ) {
                        row.forEach { key ->
                            val resolved = resolveKeyDispatch(
                                key = key,
                                fnEnabled = fnEnabled,
                                shiftEnabled = shiftEnabled,
                                ctrlEnabled = ctrlEnabled,
                                atariEnabled = atariEnabled,
                            )
                            val displayLabel = key.displayLabel(
                                fnEnabled = fnEnabled,
                                shiftEnabled = shiftEnabled,
                            )
                            val displayFontScale = key.displayFontScale(
                                fnEnabled = fnEnabled,
                                shiftEnabled = shiftEnabled,
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
                                enabled = true,
                                onPressed = {
                                    when (key.kind) {
                                        KeyboardKeyKind.KEY -> resolved.pressMappings.forEach(onKeyPressed)
                                        KeyboardKeyKind.SHIFT -> shiftEnabled = !shiftEnabled
                                        KeyboardKeyKind.CTRL -> ctrlEnabled = !ctrlEnabled
                                        KeyboardKeyKind.FN -> fnEnabled = !fnEnabled
                                    }
                                },
                                onTapFeedback = emitHaptic,
                                onReleased = {
                                    if (key.kind == KeyboardKeyKind.KEY) {
                                        resolved.releaseMappings.forEach(onKeyReleased)
                                        if (resolved.clearsShift && !stickyShiftEnabled) {
                                            shiftEnabled = false
                                        }
                                        if (resolved.clearsCtrl && !stickyCtrlEnabled) {
                                            ctrlEnabled = false
                                        }
                                        if (resolved.clearsFn && !stickyFnEnabled) {
                                            fnEnabled = false
                                        }
                                        if (resolved.clearsAtari) {
                                            atariEnabled = false
                                        }
                                    }
                                },
                                height = dynamicKeyHeight,
                            )
                        }
                    }
                }

                KeyboardUtilityRow(
                    showAtariButton = showAtariModifierButton,
                    atariEnabled = atariEnabled,
                    hapticsEnabled = hapticsEnabled,
                    onTapFeedback = emitHaptic,
                    onToggleInputMode = onToggleInputMode,
                    onToggleInputLongPress = onToggleInputLongPress,
                    toggleIconResId = toggleIconResId,
                    toggleIconDescription = toggleIconDescription,
                    onAtariToggle = { atariEnabled = !atariEnabled },
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
                    upButtonWidth = upButtonWidth,
                    keyGap = keyGap,
                    singleRowArrows = dense,
                )
            }
        }
    }
}

@Composable
private fun KeyboardUtilityRow(
    showAtariButton: Boolean,
    atariEnabled: Boolean,
    hapticsEnabled: Boolean,
    onTapFeedback: () -> Unit,
    onToggleInputMode: () -> Unit,
    onToggleInputLongPress: () -> Unit,
    toggleIconResId: Int,
    toggleIconDescription: String,
    onAtariToggle: () -> Unit,
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
    upButtonWidth: androidx.compose.ui.unit.Dp,
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
            if (showAtariButton) {
                AtariKeyboardKey(
                    label = "△",
                    modifier = Modifier
                        .width(buttonSize),
                    fontScale = 1.15f,
                    fontWeight = FontWeight.Bold,
                    hapticsEnabled = hapticsEnabled,
                    active = atariEnabled,
                    enabled = true,
                    onPressed = onAtariToggle,
                    onTapFeedback = onTapFeedback,
                    onReleased = {},
                    height = buttonSize,
                )
            }
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
            upButtonWidth = upButtonWidth,
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
    upButtonWidth: androidx.compose.ui.unit.Dp,
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AtariKeyboardKey(
                label = "↑",
                modifier = Modifier.width(upButtonWidth),
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
    enabled: Boolean,
    onPressed: () -> Unit,
    onTapFeedback: () -> Unit,
    onReleased: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 46.dp,
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
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

internal fun resolveKeyDispatch(
    key: KeyboardKeySpec,
    fnEnabled: Boolean,
    shiftEnabled: Boolean,
    ctrlEnabled: Boolean,
    atariEnabled: Boolean,
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
    val pressMappings = buildList {
        if (atariEnabled && key.supportsAtariModifier) {
            add(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI))
        }
        effectiveMapping?.let(::add)
    }
    val releaseMappings = buildList {
        effectiveMapping?.let(::add)
        if (atariEnabled && key.supportsAtariModifier) {
            add(AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ATARI))
        }
    }
    return ResolvedKeyboardDispatch(
        pressMappings = pressMappings,
        releaseMappings = releaseMappings,
        clearsShift = shiftEnabled,
        clearsCtrl = ctrlEnabled,
        clearsFn = fnEnabled,
        clearsAtari = atariEnabled,
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
    val fontScale: Float = 1f,
    val fontWeight: FontWeight = FontWeight.SemiBold,
    val supportsModifiers: Boolean = false,
    val supportsAtariModifier: Boolean = true,
    val shiftedLabel: String? = null,
    val shiftedMapping: AtariKeyMapping? = null,
    val shiftedFontScale: Float? = null,
    val fnLabel: String? = null,
    val fnMapping: AtariKeyMapping? = null,
    val fnFontScale: Float? = null,
)

internal data class ResolvedKeyboardDispatch(
    val pressMappings: List<AtariKeyMapping> = emptyList(),
    val releaseMappings: List<AtariKeyMapping> = emptyList(),
    val clearsShift: Boolean = false,
    val clearsCtrl: Boolean = false,
    val clearsFn: Boolean = false,
    val clearsAtari: Boolean = false,
)

internal fun KeyboardKeySpec.displayLabel(
    fnEnabled: Boolean,
    shiftEnabled: Boolean,
): String {
    return when {
        fnEnabled && fnLabel != null -> fnLabel
        !fnEnabled && shiftEnabled && shiftedLabel != null -> shiftedLabel
        else -> label
    }
}

internal fun KeyboardKeySpec.displayFontScale(
    fnEnabled: Boolean,
    shiftEnabled: Boolean,
): Float {
    return when {
        fnEnabled && fnLabel != null -> fnFontScale ?: fontScale
        !fnEnabled && shiftEnabled && shiftedLabel != null -> shiftedFontScale ?: fontScale
        else -> fontScale
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
    KeyboardKeySpec("1", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_1), supportsModifiers = true, shiftedLabel = "!", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_EXCLAMATION), fnLabel = "ESC", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ESCAPE)),
    KeyboardKeySpec("2", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_2), supportsModifiers = true, shiftedLabel = "@", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_AT), fnLabel = ";", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_SEMICOLON)),
    KeyboardKeySpec("3", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_3), supportsModifiers = true, shiftedLabel = "#", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_HASH), fnLabel = ":", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_COLON)),
    KeyboardKeySpec("4", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_4), supportsModifiers = true, shiftedLabel = "$", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_DOLLAR), fnLabel = "'", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_QUOTE)),
    KeyboardKeySpec("5", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_5), supportsModifiers = true, shiftedLabel = "%", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_PERCENT), fnLabel = "\"", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_DBLQUOTE)),
    KeyboardKeySpec("6", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_6), supportsModifiers = true, shiftedLabel = "^", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_CARET), fnLabel = "[", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BRACKETLEFT)),
    KeyboardKeySpec("7", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_7), supportsModifiers = true, shiftedLabel = "&", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_AMPERSAND), fnLabel = "]", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BRACKETRIGHT)),
    KeyboardKeySpec("8", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_8), supportsModifiers = true, shiftedLabel = "*", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_ASTERISK), fnLabel = "\\", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BACKSLASH)),
    KeyboardKeySpec("9", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_9), supportsModifiers = true, shiftedLabel = "(", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_PARENLEFT), fnLabel = "-", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_MINUS)),
    KeyboardKeySpec("0", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_0), supportsModifiers = true, shiftedLabel = ")", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_PARENRIGHT), fnLabel = "=", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_EQUAL)),
)

private val qwertyRow = listOf(
    KeyboardKeySpec("Q", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_q), supportsModifiers = true),
    KeyboardKeySpec("W", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_w), supportsModifiers = true),
    KeyboardKeySpec("E", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_e), supportsModifiers = true),
    KeyboardKeySpec("R", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_r), supportsModifiers = true),
    KeyboardKeySpec("T", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_t), supportsModifiers = true),
    KeyboardKeySpec("Y", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_y), supportsModifiers = true),
    KeyboardKeySpec("U", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_u), supportsModifiers = true),
    KeyboardKeySpec("I", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_i), supportsModifiers = true),
    KeyboardKeySpec("O", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_o), supportsModifiers = true),
    KeyboardKeySpec("P", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_p), supportsModifiers = true),
)

private val homeRow = listOf(
    KeyboardKeySpec("TAB", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_TAB), weight = 1.32f, fontScale = 0.82f, supportsAtariModifier = false, fnLabel = "CAPS", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_CAPSTOGGLE)),
    KeyboardKeySpec("A", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_a), supportsModifiers = true),
    KeyboardKeySpec("S", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_s), supportsModifiers = true),
    KeyboardKeySpec("D", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_d), supportsModifiers = true),
    KeyboardKeySpec("F", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_f), supportsModifiers = true),
    KeyboardKeySpec("G", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_g), supportsModifiers = true),
    KeyboardKeySpec("H", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_h), supportsModifiers = true),
    KeyboardKeySpec("J", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_j), supportsModifiers = true),
    KeyboardKeySpec("K", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_k), supportsModifiers = true),
    KeyboardKeySpec("L", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_l), supportsModifiers = true),
)

private val controlRow = listOf(
    KeyboardKeySpec("CTRL", kind = KeyboardKeyKind.CTRL, weight = 1.34f, fontScale = 0.82f),
    KeyboardKeySpec("Z", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_z), supportsModifiers = true, fnLabel = "_", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_UNDERSCORE)),
    KeyboardKeySpec("X", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_x), supportsModifiers = true, fnLabel = "+", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_PLUS)),
    KeyboardKeySpec("C", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_c), supportsModifiers = true, fnLabel = "?", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_QUESTION)),
    KeyboardKeySpec("V", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_v), supportsModifiers = true, fnLabel = "<", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_LESS)),
    KeyboardKeySpec("B", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_b), supportsModifiers = true, fnLabel = ">", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_GREATER)),
    KeyboardKeySpec("N", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_n), supportsModifiers = true, fnLabel = "(", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_PARENLEFT)),
    KeyboardKeySpec("M", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_m), supportsModifiers = true, fnLabel = ")", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_PARENRIGHT)),
    KeyboardKeySpec("⌫", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BACKSPACE), weight = 1.28f, fontScale = 1.2f, fontWeight = FontWeight.Bold, supportsAtariModifier = false, fnLabel = "BRK", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BREAK), fnFontScale = 0.82f),
)

private val bottomRow = listOf(
    KeyboardKeySpec("SHIFT", kind = KeyboardKeyKind.SHIFT, weight = 1.52f, fontScale = 0.82f),
    KeyboardKeySpec("Fn", kind = KeyboardKeyKind.FN, weight = 0.92f),
    KeyboardKeySpec(",", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_COMMA), weight = 0.92f, supportsModifiers = true, shiftedLabel = "<", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_LESS), fnLabel = "#", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_HASH)),
    KeyboardKeySpec("▁", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_SPACE), weight = 3.26f, fontScale = 1.2f, fontWeight = FontWeight.Bold, supportsAtariModifier = false, fnLabel = "▁"),
    KeyboardKeySpec(".", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_FULLSTOP), weight = 0.92f, supportsModifiers = true, shiftedLabel = ">", shiftedMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_GREATER), fnLabel = "/", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_SLASH)),
    KeyboardKeySpec("↵", AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_RETURN), weight = 1.24f, fontScale = 1.52f, fontWeight = FontWeight.Bold, supportsAtariModifier = false, fnLabel = "CLEAR", fnMapping = AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_CLEAR), fnFontScale = 0.72f),
)
