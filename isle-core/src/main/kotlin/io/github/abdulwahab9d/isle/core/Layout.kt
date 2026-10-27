package io.github.abdulwahab9d.isle.core

data class RoundBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val radius: Float,
)

data class Cubic(
    val c1x: Float,
    val c1y: Float,
    val c2x: Float,
    val c2y: Float,
    val x: Float,
    val y: Float,
)

/** Left and right edges of the soft neck between the pill and the card. */
data class NeckBridge(
    val left: Cubic,
    val right: Cubic,
)

data class IsleLayout(
    val pill: RoundBox,
    val body: RoundBox?,
    val neck: NeckBridge?,
)

data class LayoutInput(
    val anchorCenterX: Float,
    val pillTop: Float,
    val cardTop: Float,
    val cardLeft: Float,
    val cardWidth: Float,
    val pillWidth: Float,
    val compactHeight: Float,
    val contentHeight: Float,
    val expandedFraction: Float,
    val roundness: Float,
)
