package com.mantismoonlabs.fujinetgo800.ui.input

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

internal enum class FujiHapticPattern(
    val durationMillis: Long,
    val amplitude: Int,
) {
    KeyPress(durationMillis = 24L, amplitude = 220),
    JoystickTick(durationMillis = 18L, amplitude = 200),
}

@Composable
internal fun rememberFujiHaptic(pattern: FujiHapticPattern): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current
    val vibrator = remember(context) { context.resolveVibrator() }
    val currentVibrator = rememberUpdatedState(vibrator)
    val currentView = rememberUpdatedState(view)

    return remember(pattern) {
        {
            val resolvedVibrator = currentVibrator.value
            if (resolvedVibrator?.hasVibrator() == true) {
                resolvedVibrator.vibrate(
                    VibrationEffect.createOneShot(pattern.durationMillis, pattern.amplitude),
                )
            } else {
                currentView.value.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }
}

private fun Context.resolveVibrator(): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }
}
