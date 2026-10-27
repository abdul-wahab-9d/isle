package io.github.abdulwahab9d.isle.geometry

import io.github.abdulwahab9d.isle.core.Cubic
import io.github.abdulwahab9d.isle.core.IsleLayout
import io.github.abdulwahab9d.isle.core.IsleMetrics
import io.github.abdulwahab9d.isle.core.LayoutInput
import io.github.abdulwahab9d.isle.core.NeckBridge
import io.github.abdulwahab9d.isle.core.RoundBox
import kotlin.math.max

/**
 * Places the pill, the opening card, and the neck between them.
 * Each cubic ends on the body top corner and starts at the pill bottom corner on that side.
 */
fun layoutIsle(input: LayoutInput): IsleLayout {
    val fraction = coercedFraction(input.expandedFraction)
    val pillWidth = max(input.pillWidth, input.compactHeight)
    val pill = RoundBox(
        x = input.anchorCenterX - pillWidth / 2f,
        y = input.pillTop,
        width = pillWidth,
        height = input.compactHeight,
        radius = input.roundness,
    )
    val fullBody = max(
        input.contentHeight,
        input.compactHeight * IsleMetrics.EXPANDED_MIN_RATIO - input.compactHeight,
    )
    val bodyHeight = fullBody * fraction
    val body = if (fraction == 0f || !bodyHeight.isFinite() || bodyHeight < 1f) {
        null
    } else {
        RoundBox(
            x = input.cardLeft,
            y = input.cardTop,
            width = input.cardWidth,
            height = bodyHeight,
            radius = input.roundness,
        )
    }
    val neck = if (
        body != null &&
        body.height >= IsleMetrics.NECK_MIN_BODY_PX &&
        fraction >= IsleMetrics.NECK_FRACTION
    ) {
        neckBridge(pill, body)
    } else {
        null
    }
    return IsleLayout(pill = pill, body = body, neck = neck)
}

private fun coercedFraction(fraction: Float): Float {
    if (!fraction.isFinite()) return 0f
    return fraction.coerceIn(0f, 1f)
}

private fun neckBridge(pill: RoundBox, body: RoundBox): NeckBridge {
    val bottom = pill.y + pill.height
    return NeckBridge(
        left = connector(pill.x, bottom, body.x, body.y),
        right = connector(pill.x + pill.width, bottom, body.x + body.width, body.y),
    )
}

/** Vertical tangents at both edges so a gap between the pill and the card reads as one neck. */
private fun connector(startX: Float, startY: Float, endX: Float, endY: Float): Cubic {
    val midY = (startY + endY) * 0.5f
    return Cubic(
        c1x = startX,
        c1y = midY,
        c2x = endX,
        c2y = midY,
        x = endX,
        y = endY,
    )
}
