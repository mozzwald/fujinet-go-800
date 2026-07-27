package com.mantismoonlabs.fujinetgo800.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * Single-line [Text] that shrinks its font size to fit [modifier]'s width instead of clipping.
 *
 * Some OEM font stacks (e.g. Samsung One UI Sans, or a device-wide bold-text/larger-font-size
 * accessibility setting) render noticeably wider/heavier glyphs than stock Roboto at the same sp
 * size, which clips short labels like "OPTION" or "P1" inside their fixed-size badges/buttons.
 * Shrinking down to fit is more robust than special-casing any one OEM.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    minScale: Float = 0.6f,
) {
    var resolvedStyle by remember(text, style) { mutableStateOf(style) }
    var readyToDraw by remember(text, style) { mutableStateOf(false) }
    val minFontSizeSp = style.fontSize.value * minScale
    val resolvedColor = if (color != Color.Unspecified) color else LocalContentColor.current

    Text(
        text = text,
        style = resolvedStyle,
        color = resolvedColor,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (
                (result.didOverflowWidth || result.didOverflowHeight) &&
                resolvedStyle.fontSize.value > minFontSizeSp
            ) {
                val nextSp = (resolvedStyle.fontSize.value * 0.9f).coerceAtLeast(minFontSizeSp)
                resolvedStyle = resolvedStyle.copy(fontSize = nextSp.sp)
            } else {
                readyToDraw = true
            }
        },
    )
}
