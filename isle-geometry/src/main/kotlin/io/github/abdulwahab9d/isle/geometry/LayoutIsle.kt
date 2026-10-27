package io.github.abdulwahab9d.isle.geometry

import io.github.abdulwahab9d.isle.core.IsleLayout
import io.github.abdulwahab9d.isle.core.LayoutInput
import io.github.abdulwahab9d.isle.core.RoundBox
import kotlin.math.max

/** Stub. The shape agent replaces this with the SPEC.md geometry. */
fun layoutIsle(input: LayoutInput): IsleLayout {
    val width = max(input.pillWidth, input.compactHeight)
    return IsleLayout(
        pill = RoundBox(
            x = input.anchorCenterX - width / 2f,
            y = input.pillTop,
            width = width,
            height = input.compactHeight,
            radius = input.roundness,
        ),
        body = null,
        neck = null,
    )
}
