package io.github.abdulwahab9d.isle.anchor

import io.github.abdulwahab9d.isle.core.AnchorFrame
import io.github.abdulwahab9d.isle.core.AnchorInput
import io.github.abdulwahab9d.isle.core.AnchorMode
import io.github.abdulwahab9d.isle.core.AnchorRequest
import io.github.abdulwahab9d.isle.core.CutoutRect
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveAnchorTest {

    @Test
    fun centerCutoutInPortrait_anchorsOnTheNotch() {
        val frame = resolveAnchor(
            input(
                cutouts = listOf(CutoutRect(180, 0, 220, 30)),
                portrait = true,
                request = AnchorRequest.Notch,
            ),
        )
        assertEquals(AnchorMode.Notch, frame.mode)
        assertClose("centerX", 200f, frame.centerXPx)
        assertClose("pillTop", 0f, frame.pillTopPx)
        assertClose("compactMaxWidth", 256f, frame.compactMaxWidthPx)
        assertClose("cardWidth", 350f, frame.cardWidthPx)
        assertClose("cardLeft", 25f, frame.cardLeftPx)
        assertClose("cardTop", 40f, frame.cardTopPx)
    }

    @Test
    fun cornerHole_usesVirtualTopCenter() {
        val frame = resolveAnchor(
            input(
                cutouts = listOf(CutoutRect(0, 0, 40, 40)),
                portrait = true,
                request = AnchorRequest.Notch,
            ),
        )
        assertVirtual(frame)
    }

    @Test
    fun noCutout_usesVirtualTopCenter() {
        val frame = resolveAnchor(
            input(
                cutouts = emptyList(),
                portrait = true,
                request = AnchorRequest.Notch,
            ),
        )
        assertVirtual(frame)
    }

    @Test
    fun landscape_usesVirtualTopCenter() {
        val frame = resolveAnchor(
            input(
                cutouts = listOf(CutoutRect(180, 0, 220, 30)),
                portrait = false,
                request = AnchorRequest.Notch,
            ),
        )
        assertVirtual(frame)
    }

    @Test
    fun topCenterRequest_usesVirtualTopCenter() {
        val frame = resolveAnchor(
            input(
                cutouts = listOf(CutoutRect(180, 0, 220, 30)),
                portrait = true,
                request = AnchorRequest.TopCenter,
            ),
        )
        assertVirtual(frame)
    }

    private fun assertVirtual(frame: AnchorFrame) {
        assertEquals(AnchorMode.VirtualTopCenter, frame.mode)
        assertClose("centerX", 200f, frame.centerXPx)
        assertClose("pillTop", 48f, frame.pillTopPx)
        assertClose("compactMaxWidth", 256f, frame.compactMaxWidthPx)
        assertClose("cardWidth", 350f, frame.cardWidthPx)
        assertClose("cardLeft", 25f, frame.cardLeftPx)
        assertClose("cardTop", 88f, frame.cardTopPx)
    }

    private fun assertClose(field: String, expected: Float, actual: Float) {
        assertEquals(field, expected, actual, 0f)
    }

    private fun input(
        cutouts: List<CutoutRect>,
        portrait: Boolean,
        request: AnchorRequest,
    ) = AnchorInput(
        screenWidthPx = 400,
        screenHeightPx = 800,
        statusBarBottomPx = 40,
        cutouts = cutouts,
        portrait = portrait,
        request = request,
        density = 1f,
    )
}
