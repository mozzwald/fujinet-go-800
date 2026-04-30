package com.mantismoonlabs.fujinetgo800.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.mantismoonlabs.fujinetgo800.input.AtariKeyMapping

data class AtariFunctionKeySpec(
    val label: String,
    val mapping: AtariKeyMapping,
)

@Composable
fun AtariFunctionBar(
    keys: List<AtariFunctionKeySpec>,
    onKeyPressed: (AtariKeyMapping) -> Unit,
    onKeyReleased: (AtariKeyMapping) -> Unit,
    hapticsEnabled: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = if (compact) 6.dp else 8.dp
    val verticalPadding = if (compact) 6.dp else 8.dp
    val buttonHeight = if (compact) 36.dp else 42.dp
    Surface(
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            keys.forEach { key ->
                HoldableFunctionButton(
                    label = key.label,
                    onPressed = { onKeyPressed(key.mapping) },
                    onReleased = { onKeyReleased(key.mapping) },
                    hapticsEnabled = hapticsEnabled,
                    height = buttonHeight,
                    modifier = Modifier
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HoldableFunctionButton(
    label: String,
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    hapticsEnabled: Boolean,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    val emitStrongHaptic = rememberFujiHaptic(FujiHapticPattern.KeyPress)
    Box(
        modifier = modifier
            .height(height)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        if (hapticsEnabled) {
                            emitStrongHaptic()
                        }
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
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
