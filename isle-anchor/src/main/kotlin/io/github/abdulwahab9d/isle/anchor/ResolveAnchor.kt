package io.github.abdulwahab9d.isle.anchor

import io.github.abdulwahab9d.isle.core.AnchorFrame
import io.github.abdulwahab9d.isle.core.AnchorInput
import io.github.abdulwahab9d.isle.core.AnchorMode
import io.github.abdulwahab9d.isle.core.IsleMetrics

/** Stub. The anchor agent replaces this with the SPEC.md placement rules. */
fun resolveAnchor(input: AnchorInput): AnchorFrame {
    val compact = IsleMetrics.COMPACT_HEIGHT_DP * input.density
    val cardWidth = minOf(
        IsleMetrics.CARD_WIDTH_DP * input.density,
        input.screenWidthPx - 2 * IsleMetrics.CARD_HORIZONTAL_MARGIN_DP * input.density,
    ).coerceAtLeast(compact)
    return AnchorFrame(
        mode = AnchorMode.VirtualTopCenter,
        centerXPx = input.screenWidthPx / 2f,
        pillTopPx = 0f,
        compactMaxWidthPx = cardWidth,
        cardLeftPx = (input.screenWidthPx - cardWidth) / 2f,
        cardWidthPx = cardWidth,
        cardTopPx = input.statusBarBottomPx.toFloat().coerceAtLeast(compact),
    )
}
