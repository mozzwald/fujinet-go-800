package com.mantismoonlabs.fujinetgo800.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.flow.map

@Composable
internal fun rememberEmulatorFold(): EmulatorFold? {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() } ?: return null
    val tracker = remember(activity) { WindowInfoTracker.getOrCreate(activity) }
    val layoutInfoFlow = remember(tracker, activity) {
        tracker.windowLayoutInfo(activity).map<WindowLayoutInfo, WindowLayoutInfo?> { it }
    }
    val layoutInfo by layoutInfoFlow.collectAsStateWithLifecycle(initialValue = null)
    val feature = layoutInfo?.displayFeatures
        .orEmpty()
        .filterIsInstance<FoldingFeature>()
        .firstOrNull()
        ?: return null
    val bounds = feature.bounds

    return EmulatorFold(
        bounds = EmulatorWindowRect(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom,
        ),
        orientation = when (feature.orientation) {
            FoldingFeature.Orientation.VERTICAL -> EmulatorFoldOrientation.Vertical
            FoldingFeature.Orientation.HORIZONTAL -> EmulatorFoldOrientation.Horizontal
            else -> if (bounds.width() > bounds.height()) {
                EmulatorFoldOrientation.Horizontal
            } else {
                EmulatorFoldOrientation.Vertical
            }
        },
        isSeparating = feature.isSeparating,
        isOccluding = feature.occlusionType == FoldingFeature.OcclusionType.FULL,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
