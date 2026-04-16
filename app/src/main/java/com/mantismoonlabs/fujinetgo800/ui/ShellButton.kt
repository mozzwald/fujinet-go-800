package com.mantismoonlabs.fujinetgo800.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val ShellButtonShape = RoundedCornerShape(6.dp)
private val ShellButtonHeight = 34.dp
internal const val InputPanelToggleLongPressTimeoutMillis = 750L

@Composable
fun ShellButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = ShellButtonHeight,
    contentPadding: PaddingValues = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ShellButtonShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        ),
        contentPadding = contentPadding,
        modifier = modifier.height(height),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShellIconButton(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    longPressTimeoutMillis: Long? = null,
    height: androidx.compose.ui.unit.Dp = ShellButtonHeight,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val buttonModifier = modifier.height(height)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val containerColor = if (active) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    if (onLongClick == null) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = ShellButtonShape,
            border = BorderStroke(1.dp, borderColor),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            ),
            contentPadding = contentPadding,
            modifier = buttonModifier,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
        return
    }

    val baseModifier = buttonModifier
        .border(1.dp, borderColor, ShellButtonShape)
        .background(
            color = if (enabled) {
                containerColor
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
            shape = ShellButtonShape,
        )

    if (longPressTimeoutMillis == null) {
        Box(
            modifier = baseModifier.combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                },
            )
        }
        return
    }

    val parentViewConfiguration = LocalViewConfiguration.current
    val adjustedViewConfiguration = remember(parentViewConfiguration, longPressTimeoutMillis) {
        object : ViewConfiguration by parentViewConfiguration {
            override val longPressTimeoutMillis: Long = longPressTimeoutMillis
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides adjustedViewConfiguration) {
        Box(
            modifier = baseModifier.combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                },
            )
        }
    }
}
