package com.mantismoonlabs.fujinetgo800.ui

internal enum class EmulatorRuntimePlacement {
    Stacked,
    Wide,
}

internal enum class EmulatorWideKeyboardRails {
    FunctionKeys,
    ConcurrentTouchControls,
}

internal data class EmulatorRuntimeLayout(
    val placement: EmulatorRuntimePlacement,
    val keyboardRails: EmulatorWideKeyboardRails,
) {
    val isWide: Boolean
        get() = placement == EmulatorRuntimePlacement.Wide

    val showsConcurrentTouchControls: Boolean
        get() = keyboardRails == EmulatorWideKeyboardRails.ConcurrentTouchControls
}

internal data class EmulatorRuntimeLayoutMetrics(
    val widthDp: Float,
    val heightDp: Float,
    val keyboardPanelHeightDp: Float,
    val keyboardSelected: Boolean,
)

internal fun calculateEmulatorRuntimeLayout(
    metrics: EmulatorRuntimeLayoutMetrics,
): EmulatorRuntimeLayout {
    val width = metrics.widthDp.coerceAtLeast(0f)
    val height = metrics.heightDp.coerceAtLeast(1f)
    val isGeometricallyLandscape = width > height
    val isExpandedNearSquare =
        width >= MinimumWideRuntimeWidthDp && width / height >= MinimumExpandedWideAspectRatio
    val isWide = isGeometricallyLandscape || isExpandedNearSquare
    val topRowHeight = (
        height - metrics.keyboardPanelHeightDp.coerceAtLeast(0f) - WideKeyboardVerticalSpacingDp
        ).coerceAtLeast(0f)
    val supportsConcurrentControls =
        isWide &&
            metrics.keyboardSelected &&
            width >= MinimumCombinedControlsWidthDp &&
            topRowHeight >= MinimumCombinedControlsTopRowHeightDp

    return EmulatorRuntimeLayout(
        placement = if (isWide) EmulatorRuntimePlacement.Wide else EmulatorRuntimePlacement.Stacked,
        keyboardRails = if (supportsConcurrentControls) {
            EmulatorWideKeyboardRails.ConcurrentTouchControls
        } else {
            EmulatorWideKeyboardRails.FunctionKeys
        },
    )
}

internal data class EmulatorPortraitHeightBudget(
    val maxInputPanelHeightDp: Float,
    val minimumViewportHeightDp: Float,
)

internal fun calculateEmulatorPortraitHeightBudget(
    totalHeightDp: Float,
    fixedChromeHeightDp: Float,
    desiredViewportHeightDp: Float,
    usefulInputPanelHeightDp: Float,
    allowInputPanelExpansion: Boolean = false,
): EmulatorPortraitHeightBudget {
    val flexibleBodyHeight = (totalHeightDp - fixedChromeHeightDp).coerceAtLeast(0f)
    val desiredViewportHeight = desiredViewportHeightDp.coerceIn(0f, flexibleBodyHeight)
    val minimumViewportHeight = minOf(
        desiredViewportHeight,
        maxOf(MinimumStackedViewportHeightDp, flexibleBodyHeight * MinimumStackedViewportFraction),
    )
    val availableInputPanelHeight = (flexibleBodyHeight - minimumViewportHeight).coerceAtLeast(0f)
    val maxInputPanelHeight = if (allowInputPanelExpansion) {
        availableInputPanelHeight
    } else {
        minOf(
            usefulInputPanelHeightDp.coerceAtLeast(0f),
            availableInputPanelHeight,
        )
    }

    return EmulatorPortraitHeightBudget(
        maxInputPanelHeightDp = maxInputPanelHeight,
        minimumViewportHeightDp = minimumViewportHeight,
    )
}

internal const val MinimumWideRuntimeWidthDp = 688f
internal const val MinimumExpandedWideAspectRatio = 0.78f
internal const val MinimumCombinedControlsWidthDp = 688f
internal const val MinimumCombinedControlsTopRowHeightDp = 260f
internal const val MinimumStackedViewportHeightDp = 180f
internal const val MinimumStackedViewportFraction = 0.45f
private const val WideKeyboardVerticalSpacingDp = 8f
