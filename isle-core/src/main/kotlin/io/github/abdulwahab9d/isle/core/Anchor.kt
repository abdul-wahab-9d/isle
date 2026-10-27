package io.github.abdulwahab9d.isle.core

data class CutoutRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

enum class AnchorMode {
    Notch,
    VirtualTopCenter,
}

data class AnchorInput(
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val statusBarBottomPx: Int,
    val cutouts: List<CutoutRect>,
    val portrait: Boolean,
    val request: AnchorRequest,
    val density: Float,
)

data class AnchorFrame(
    val mode: AnchorMode,
    val centerXPx: Float,
    val pillTopPx: Float,
    val compactMaxWidthPx: Float,
    val cardLeftPx: Float,
    val cardWidthPx: Float,
    val cardTopPx: Float,
)
