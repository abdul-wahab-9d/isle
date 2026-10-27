package io.github.abdulwahab9d.isle.anchor

import io.github.abdulwahab9d.isle.core.AnchorFrame
import io.github.abdulwahab9d.isle.core.AnchorInput
import io.github.abdulwahab9d.isle.core.AnchorMode
import io.github.abdulwahab9d.isle.core.AnchorRequest
import io.github.abdulwahab9d.isle.core.CutoutRect
import io.github.abdulwahab9d.isle.core.IsleMetrics
import kotlin.math.abs

/**
 * Places the capsule on a qualifying notch so the hardware hole sits inside the pill.
 * Anything else — no cutout, a corner hole, landscape, or [AnchorRequest.TopCenter] —
 * uses a virtual top-center anchor.
 */
fun resolveAnchor(input: AnchorInput): AnchorFrame {
    val density = input.density
    val compactHeight = IsleMetrics.COMPACT_HEIGHT_DP * density
    val cardWidth = minOf(
        IsleMetrics.CARD_WIDTH_DP * density,
        input.screenWidthPx - 2 * IsleMetrics.CARD_HORIZONTAL_MARGIN_DP * density,
    ).coerceAtLeast(compactHeight)
    val cardLeft = (input.screenWidthPx - cardWidth) / 2f
    val safeWidth = input.screenWidthPx - 2 * IsleMetrics.SIDE_SAFE_DP * density
    val edgeWidth = input.screenWidthPx - 2 * IsleMetrics.SCREEN_EDGE_DP * density
    val widthCap = minOf(safeWidth, edgeWidth)

    val cutout = qualifyingCutout(input)
    val mode: AnchorMode
    val centerX: Float
    val pillTop: Float
    val compactMaxWidth: Float
    if (cutout != null) {
        mode = AnchorMode.Notch
        centerX = cutout.centerX
        pillTop = minOf(
            cutout.centerY - compactHeight / 2f,
            cutout.top.toFloat(),
        ).coerceAtLeast(0f)
        val minPill = cutout.width + IsleMetrics.MIN_PILL_GROWTH_DP * density
        compactMaxWidth = maxOf(minPill, widthCap)
    } else {
        mode = AnchorMode.VirtualTopCenter
        centerX = input.screenWidthPx / 2f
        pillTop = maxOf(0f, input.statusBarBottomPx.toFloat() - compactHeight)
        compactMaxWidth = maxOf(IsleMetrics.MIN_VIRTUAL_PILL_DP * density, widthCap)
    }
    val cardTop = maxOf(input.statusBarBottomPx.toFloat(), pillTop + compactHeight)
    return AnchorFrame(
        mode = mode,
        centerXPx = centerX,
        pillTopPx = pillTop,
        compactMaxWidthPx = compactMaxWidth,
        cardLeftPx = cardLeft,
        cardWidthPx = cardWidth,
        cardTopPx = cardTop,
    )
}

private fun qualifyingCutout(input: AnchorInput): CutoutRect? {
    if (!input.portrait || input.request != AnchorRequest.Notch) return null
    val topLimit = input.screenHeightPx * IsleMetrics.TOP_BAND
    val bandStart = input.screenWidthPx * IsleMetrics.MIDDLE_BAND_START
    val bandEnd = input.screenWidthPx * IsleMetrics.MIDDLE_BAND_END
    val screenCenter = input.screenWidthPx / 2f
    return input.cutouts
        .filter { cutout ->
            cutout.top < topLimit && cutout.centerX in bandStart..bandEnd
        }
        .minWithOrNull(
            compareBy<CutoutRect> { it.top }
                .thenBy { abs(it.centerX - screenCenter) },
        )
}
