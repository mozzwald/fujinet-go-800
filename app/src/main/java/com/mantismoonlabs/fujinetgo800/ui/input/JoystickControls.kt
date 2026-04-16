package com.mantismoonlabs.fujinetgo800.ui.input

import android.graphics.PointF
import android.view.MotionEvent
import com.mantismoonlabs.fujinetgo800.settings.JoystickInputStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

@Composable
fun JoystickControls(
    onJoystickMoved: (x: Float, y: Float) -> Unit,
    onJoystickReleased: () -> Unit,
    onFirePressed: () -> Unit,
    onFireReleased: () -> Unit,
    hapticsEnabled: Boolean,
    joystickInputStyle: JoystickInputStyle = JoystickInputStyle.STICK_8_WAY,
    compact: Boolean = false,
    topContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val topSectionHeight = if (topContent != null) {
            if (compact) 36.dp else 40.dp
        } else {
            0.dp
        }
        val footerSectionHeight = if (footerContent != null) {
            if (compact) 48.dp else 60.dp
        } else {
            0.dp
        }
        val sectionGap = if (compact) 4.dp else 8.dp
        val horizontalPadding = if (compact) 12.dp else 18.dp
        val verticalPadding = if (compact) 8.dp else 12.dp
        val spacerSize = if (compact) 8.dp else 12.dp
        val rowHeight = maxHeight -
            topSectionHeight -
            footerSectionHeight -
            if (topContent != null) sectionGap else 0.dp -
            if (footerContent != null) sectionGap else 0.dp
        val padControlSize = minOf(rowHeight * 0.76f, if (compact) 152.dp else 180.dp)
        val fireControlSize = minOf(rowHeight * 0.42f, if (compact) 78.dp else 96.dp)

        Surface(
            modifier = Modifier.fillMaxSize(),
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalArrangement = Arrangement.spacedBy(sectionGap),
            ) {
                if (topContent != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topSectionHeight),
                    ) {
                        topContent()
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        when (joystickInputStyle) {
                            JoystickInputStyle.STICK_8_WAY -> {
                                JoystickPadControl(
                                    modifier = Modifier.size(padControlSize),
                                    onJoystickMoved = onJoystickMoved,
                                    onJoystickReleased = onJoystickReleased,
                                    hapticsEnabled = hapticsEnabled,
                                )
                            }

                            JoystickInputStyle.DPAD_4_WAY -> {
                                DpadControl(
                                    modifier = Modifier.size(padControlSize),
                                    onJoystickMoved = onJoystickMoved,
                                    onJoystickReleased = onJoystickReleased,
                                    hapticsEnabled = hapticsEnabled,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(spacerSize))
                    FireButtonControl(
                        modifier = Modifier
                            .padding(end = spacerSize)
                            .size(fireControlSize),
                        onFirePressed = onFirePressed,
                        onFireReleased = onFireReleased,
                    )
                }
                if (footerContent != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(footerSectionHeight),
                    ) {
                        footerContent()
                    }
                }
            }
        }
    }
}

@Composable
fun DpadControl(
    onJoystickMoved: (x: Float, y: Float) -> Unit,
    onJoystickReleased: () -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    var highlightDirection by remember { mutableStateOf(DpadDirection.CENTER) }
    var activePointerId by remember { mutableStateOf<Int?>(null) }
    val emitJoystickHaptic = rememberFujiHaptic(FujiHapticPattern.JoystickTick)
    val resetDpad = {
        activePointerId = null
        highlightDirection = DpadDirection.CENTER
        onJoystickReleased()
    }
    Box(
        modifier = modifier
            .testTag("joystick-pad")
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp),
            )
            .onSizeChanged { padSize = it }
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        if (activePointerId != null) {
                            return@pointerInteropFilter true
                        }
                        activePointerId = event.getPointerId(event.actionIndex)
                        val direction = dpadDirectionFor(
                            x = event.getX(event.actionIndex),
                            y = event.getY(event.actionIndex),
                            size = padSize,
                        )
                        updateDpadState(
                            direction = direction,
                            lastDirection = highlightDirection,
                            hapticsEnabled = hapticsEnabled,
                            onHaptic = emitJoystickHaptic,
                            onDirectionChanged = { highlightDirection = it },
                            onMoved = onJoystickMoved,
                            onReleased = onJoystickReleased,
                        )
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val pointerId = activePointerId ?: return@pointerInteropFilter false
                        val pointerIndex = event.findPointerIndex(pointerId)
                        if (pointerIndex < 0) {
                            resetDpad()
                            return@pointerInteropFilter true
                        }
                        val direction = dpadDirectionFor(
                            x = event.getX(pointerIndex),
                            y = event.getY(pointerIndex),
                            size = padSize,
                        )
                        updateDpadState(
                            direction = direction,
                            lastDirection = highlightDirection,
                            hapticsEnabled = hapticsEnabled,
                            onHaptic = emitJoystickHaptic,
                            onDirectionChanged = { highlightDirection = it },
                            onMoved = onJoystickMoved,
                            onReleased = onJoystickReleased,
                        )
                        true
                    }

                    MotionEvent.ACTION_POINTER_UP -> {
                        val releasedPointerId = event.getPointerId(event.actionIndex)
                        if (releasedPointerId == activePointerId) {
                            resetDpad()
                        }
                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        resetDpad()
                        true
                    }

                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        DpadVisual(direction = highlightDirection)
    }
}

@Composable
private fun DpadVisual(direction: DpadDirection) {
    Box(modifier = Modifier.fillMaxSize(0.84f), contentAlignment = Alignment.Center) {
        DpadArm(
            active = direction == DpadDirection.UP,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize(0.34f),
        )
        DpadArm(
            active = direction == DpadDirection.DOWN,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize(0.34f),
        )
        DpadArm(
            active = direction == DpadDirection.LEFT,
            horizontal = true,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxSize(0.34f),
        )
        DpadArm(
            active = direction == DpadDirection.RIGHT,
            horizontal = true,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxSize(0.34f),
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.32f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
        )
    }
}

@Composable
private fun DpadArm(
    active: Boolean,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (horizontal) 12.dp else 10.dp))
            .background(
                if (active) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                },
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(if (horizontal) 12.dp else 10.dp),
            ),
    )
}

@Composable
fun JoystickPadControl(
    onJoystickMoved: (x: Float, y: Float) -> Unit,
    onJoystickReleased: () -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    var nubOffset by remember { mutableStateOf(PointF(0f, 0f)) }
    var lastDirection by remember { mutableStateOf(JoystickDirection.CENTER) }
    var activePointerId by remember { mutableStateOf<Int?>(null) }
    val emitJoystickHaptic = rememberFujiHaptic(FujiHapticPattern.JoystickTick)
    val resetJoystick = {
        activePointerId = null
        nubOffset = PointF(0f, 0f)
        lastDirection = JoystickDirection.CENTER
        onJoystickReleased()
    }
    Box(
        modifier = modifier
            .testTag("joystick-pad")
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = CircleShape,
            )
            .onSizeChanged { padSize = it }
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        if (activePointerId != null) {
                            return@pointerInteropFilter true
                        }
                        activePointerId = event.getPointerId(event.actionIndex)
                        val axes = joystickAxesFor(
                            x = event.getX(event.actionIndex),
                            y = event.getY(event.actionIndex),
                            size = padSize,
                        )
                        updateJoystickState(
                            axes = axes,
                            lastDirection = lastDirection,
                            hapticsEnabled = hapticsEnabled,
                            onHaptic = emitJoystickHaptic,
                            onDirectionChanged = { lastDirection = it },
                            onMoved = onJoystickMoved,
                            onNubChanged = { nubOffset = it },
                        )
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val pointerId = activePointerId ?: return@pointerInteropFilter false
                        val pointerIndex = event.findPointerIndex(pointerId)
                        if (pointerIndex < 0) {
                            resetJoystick()
                            return@pointerInteropFilter true
                        }
                        val axes = joystickAxesFor(
                            x = event.getX(pointerIndex),
                            y = event.getY(pointerIndex),
                            size = padSize,
                        )
                        updateJoystickState(
                            axes = axes,
                            lastDirection = lastDirection,
                            hapticsEnabled = hapticsEnabled,
                            onHaptic = emitJoystickHaptic,
                            onDirectionChanged = { lastDirection = it },
                            onMoved = onJoystickMoved,
                            onNubChanged = { nubOffset = it },
                        )
                        true
                    }

                    MotionEvent.ACTION_POINTER_UP -> {
                        val releasedPointerId = event.getPointerId(event.actionIndex)
                        if (releasedPointerId != activePointerId) {
                            return@pointerInteropFilter true
                        }
                        // Never transfer joystick ownership to another pointer.
                        // In normal gameplay the remaining pointer is often the fire finger,
                        // which belongs to a different control and can leave a bogus direction latched.
                        resetJoystick()
                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        resetJoystick()
                        true
                    }

                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.3f)
                .clip(CircleShape)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = CircleShape,
                ),
        )
        val nubTravelPx = min(padSize.width, padSize.height) * 0.2f
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (nubOffset.x * nubTravelPx).roundToInt(),
                        y = (nubOffset.y * nubTravelPx).roundToInt(),
                    )
                }
                .fillMaxSize(0.24f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
        )
    }
}

@Composable
fun FireButtonControl(
    onFirePressed: () -> Unit,
    onFireReleased: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var controlSize by remember { mutableStateOf(IntSize.Zero) }
    var activePointerId by remember { mutableStateOf<Int?>(null) }
    val releaseFire = {
        activePointerId = null
        onFireReleased()
    }
    Box(
        modifier = modifier
            .testTag("fire-button")
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), CircleShape)
            .onSizeChanged { controlSize = it }
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        if (activePointerId == null) {
                            activePointerId = event.getPointerId(event.actionIndex)
                            onFirePressed()
                        }
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val pointerId = activePointerId ?: return@pointerInteropFilter false
                        val pointerIndex = event.findPointerIndex(pointerId)
                        if (pointerIndex < 0) {
                            releaseFire()
                            return@pointerInteropFilter true
                        }
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)
                        if (!isWithinBounds(x, y, controlSize)) {
                            releaseFire()
                        }
                        true
                    }

                    MotionEvent.ACTION_POINTER_UP -> {
                        val releasedPointerId = event.getPointerId(event.actionIndex)
                        if (releasedPointerId == activePointerId) {
                            releaseFire()
                        }
                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        releaseFire()
                        true
                    }

                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {}
}

private fun updateJoystickState(
    axes: PointF,
    lastDirection: JoystickDirection,
    hapticsEnabled: Boolean,
    onHaptic: () -> Unit,
    onDirectionChanged: (JoystickDirection) -> Unit,
    onMoved: (Float, Float) -> Unit,
    onNubChanged: (PointF) -> Unit,
) {
    onNubChanged(axes)
    val direction = JoystickDirection.fromAxes(axes.x, axes.y)
    if (hapticsEnabled && direction != JoystickDirection.CENTER && direction != lastDirection) {
        onHaptic()
    }
    onDirectionChanged(direction)
    onMoved(axes.x, axes.y)
}

private fun updateDpadState(
    direction: DpadDirection,
    lastDirection: DpadDirection,
    hapticsEnabled: Boolean,
    onHaptic: () -> Unit,
    onDirectionChanged: (DpadDirection) -> Unit,
    onMoved: (Float, Float) -> Unit,
    onReleased: () -> Unit,
) {
    if (hapticsEnabled && direction != DpadDirection.CENTER && direction != lastDirection) {
        onHaptic()
    }
    onDirectionChanged(direction)
    if (direction == DpadDirection.CENTER) {
        onReleased()
    } else {
        val axes = direction.axes()
        onMoved(axes.x, axes.y)
    }
}

private fun joystickAxesFor(x: Float, y: Float, size: IntSize): PointF {
    if (size.width == 0 || size.height == 0) {
        return PointF(0f, 0f)
    }
    val halfWidth = size.width / 2f
    val halfHeight = size.height / 2f
    val normalizedX = ((x - halfWidth) / max(halfWidth, 1f)).coerceIn(-1f, 1f)
    val normalizedY = ((y - halfHeight) / max(halfHeight, 1f)).coerceIn(-1f, 1f)
    return PointF(applyDeadzone(normalizedX), applyDeadzone(normalizedY))
}

private fun isWithinBounds(x: Float, y: Float, size: IntSize): Boolean {
    return x >= 0f && y >= 0f && x < size.width && y < size.height
}

private fun dpadDirectionFor(x: Float, y: Float, size: IntSize): DpadDirection {
    if (size.width == 0 || size.height == 0) {
        return DpadDirection.CENTER
    }
    val halfWidth = size.width / 2f
    val halfHeight = size.height / 2f
    val normalizedX = ((x - halfWidth) / max(halfWidth, 1f)).coerceIn(-1f, 1f)
    val normalizedY = ((y - halfHeight) / max(halfHeight, 1f)).coerceIn(-1f, 1f)
    if (abs(normalizedX) < 0.18f && abs(normalizedY) < 0.18f) {
        return DpadDirection.CENTER
    }
    return if (abs(normalizedX) > abs(normalizedY)) {
        if (normalizedX < 0f) DpadDirection.LEFT else DpadDirection.RIGHT
    } else {
        if (normalizedY < 0f) DpadDirection.UP else DpadDirection.DOWN
    }
}

private fun applyDeadzone(value: Float): Float {
    return if (abs(value) < 0.15f) 0f else value
}

private enum class JoystickDirection {
    CENTER,
    UP,
    UP_RIGHT,
    RIGHT,
    DOWN_RIGHT,
    DOWN,
    DOWN_LEFT,
    LEFT,
    UP_LEFT;

    companion object {
        fun fromAxes(x: Float, y: Float): JoystickDirection {
            if (abs(x) < 0.2f && abs(y) < 0.2f) {
                return CENTER
            }
            val horizontal = when {
                x <= -0.35f -> -1
                x >= 0.35f -> 1
                else -> 0
            }
            val vertical = when {
                y <= -0.35f -> -1
                y >= 0.35f -> 1
                else -> 0
            }
            return when {
                horizontal == 0 && vertical < 0 -> UP
                horizontal > 0 && vertical < 0 -> UP_RIGHT
                horizontal > 0 && vertical == 0 -> RIGHT
                horizontal > 0 && vertical > 0 -> DOWN_RIGHT
                horizontal == 0 && vertical > 0 -> DOWN
                horizontal < 0 && vertical > 0 -> DOWN_LEFT
                horizontal < 0 && vertical == 0 -> LEFT
                horizontal < 0 && vertical < 0 -> UP_LEFT
                else -> CENTER
            }
        }
    }
}

private enum class DpadDirection {
    CENTER,
    UP,
    RIGHT,
    DOWN,
    LEFT;

    fun axes(): PointF = when (this) {
        CENTER -> PointF(0f, 0f)
        UP -> PointF(0f, -1f)
        RIGHT -> PointF(1f, 0f)
        DOWN -> PointF(0f, 1f)
        LEFT -> PointF(-1f, 0f)
    }
}
