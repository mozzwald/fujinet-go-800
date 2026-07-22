package com.mantismoonlabs.fujinetgo800.ui

internal data class EmulatorWindowInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
)

internal data class EmulatorWindowRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val area: Long get() = width.toLong() * height.toLong()
}

internal enum class EmulatorFoldOrientation {
    Vertical,
    Horizontal,
}

internal data class EmulatorFold(
    val bounds: EmulatorWindowRect,
    val orientation: EmulatorFoldOrientation,
    val isSeparating: Boolean,
    val isOccluding: Boolean,
)

internal data class EmulatorWindowMetrics(
    val widthPx: Int,
    val heightPx: Int,
    val safeInsets: EmulatorWindowInsets = EmulatorWindowInsets(),
    val fold: EmulatorFold? = null,
)

internal enum class EmulatorLayoutClass {
    CompactPortrait,
    CompactLandscape,
    ExpandedPortrait,
    ExpandedLandscape,
}

internal enum class EmulatorFoldPosture {
    None,
    Book,
    Tabletop,
}

internal data class EmulatorWindowLayout(
    val contentBounds: EmulatorWindowRect,
    val layoutClass: EmulatorLayoutClass,
    val foldPosture: EmulatorFoldPosture,
) {
    val isLandscape: Boolean
        get() = layoutClass == EmulatorLayoutClass.CompactLandscape ||
            layoutClass == EmulatorLayoutClass.ExpandedLandscape
}

internal fun calculateEmulatorWindowLayout(
    metrics: EmulatorWindowMetrics,
    expandedWidthThresholdPx: Int,
): EmulatorWindowLayout {
    val windowWidth = metrics.widthPx.coerceAtLeast(1)
    val windowHeight = metrics.heightPx.coerceAtLeast(1)
    val safeBounds = EmulatorWindowRect(
        left = metrics.safeInsets.left.coerceIn(0, windowWidth),
        top = metrics.safeInsets.top.coerceIn(0, windowHeight),
        right = (windowWidth - metrics.safeInsets.right).coerceIn(0, windowWidth),
        bottom = (windowHeight - metrics.safeInsets.bottom).coerceIn(0, windowHeight),
    ).ensureNonEmpty(windowWidth, windowHeight)

    val activeFold = metrics.fold?.takeIf { fold ->
        (fold.isSeparating || fold.isOccluding) && fold.bounds.intersects(safeBounds)
    }
    val panes = activeFold?.let { fold -> safeBounds.splitAround(fold) }.orEmpty()
    val contentBounds = panes
        .filter { it.width > 0 && it.height > 0 }
        .maxWithOrNull(compareBy<EmulatorWindowRect> { it.area }.thenBy { -it.top }.thenBy { -it.left })
        ?: safeBounds
    val expanded = contentBounds.width >= expandedWidthThresholdPx
    val landscape = contentBounds.width > contentBounds.height
    val layoutClass = when {
        expanded && landscape -> EmulatorLayoutClass.ExpandedLandscape
        expanded -> EmulatorLayoutClass.ExpandedPortrait
        landscape -> EmulatorLayoutClass.CompactLandscape
        else -> EmulatorLayoutClass.CompactPortrait
    }
    val foldPosture = when (activeFold?.orientation) {
        EmulatorFoldOrientation.Vertical -> EmulatorFoldPosture.Book
        EmulatorFoldOrientation.Horizontal -> EmulatorFoldPosture.Tabletop
        null -> EmulatorFoldPosture.None
    }

    return EmulatorWindowLayout(
        contentBounds = contentBounds,
        layoutClass = layoutClass,
        foldPosture = foldPosture,
    )
}

private fun EmulatorWindowRect.ensureNonEmpty(
    windowWidth: Int,
    windowHeight: Int,
): EmulatorWindowRect {
    if (width > 0 && height > 0) {
        return this
    }
    return EmulatorWindowRect(0, 0, windowWidth, windowHeight)
}

private fun EmulatorWindowRect.intersects(other: EmulatorWindowRect): Boolean =
    left <= other.right && right >= other.left && top <= other.bottom && bottom >= other.top

private fun EmulatorWindowRect.splitAround(fold: EmulatorFold): List<EmulatorWindowRect> =
    when (fold.orientation) {
        EmulatorFoldOrientation.Vertical -> listOf(
            copy(right = fold.bounds.left.coerceIn(left, right)),
            copy(left = fold.bounds.right.coerceIn(left, right)),
        )

        EmulatorFoldOrientation.Horizontal -> listOf(
            copy(bottom = fold.bounds.top.coerceIn(top, bottom)),
            copy(top = fold.bounds.bottom.coerceIn(top, bottom)),
        )
    }
