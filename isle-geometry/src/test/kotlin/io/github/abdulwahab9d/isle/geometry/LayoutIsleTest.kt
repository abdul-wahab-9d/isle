package io.github.abdulwahab9d.isle.geometry

import io.github.abdulwahab9d.isle.core.IsleMetrics
import io.github.abdulwahab9d.isle.core.LayoutInput
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LayoutIsleTest {

    @Test
    fun fractionZero_pillAt140_bodyNull() {
        val layout = layoutIsle(vector(0f))

        assertEquals(140f, layout.pill.x, 0.001f)
        assertEquals(200f, layout.pill.x + layout.pill.width / 2f, 0.001f)
        assertNull(layout.body)
        assertNull(layout.neck)
    }

    @Test
    fun fractionOne_keepsPillCenter_bodyAndNeck() {
        val layout = layoutIsle(vector(1f))
        val body = layout.body

        assertEquals(200f, layout.pill.x + layout.pill.width / 2f, 0.001f)
        assertNotNull(body)
        assertEquals(25f, body!!.x, 0.001f)
        assertEquals(40f, body.y, 0.001f)
        assertEquals(350f, body.width, 0.001f)
        assertEquals(80f, body.height, 0.001f)
        assertNotNull(layout.neck)
    }

    @Test
    fun fractionHalf_bodyHeightIsHalfOfFullBody() {
        val input = vector(0.5f)
        val fullBody = max(
            input.contentHeight,
            input.compactHeight * IsleMetrics.EXPANDED_MIN_RATIO - input.compactHeight,
        )
        val body = layoutIsle(input).body

        assertNotNull(body)
        assertEquals(fullBody * 0.5f, body!!.height, 0.001f)
    }

    private fun vector(fraction: Float) = LayoutInput(
        anchorCenterX = 200f,
        pillTop = 0f,
        cardTop = 40f,
        cardLeft = 25f,
        cardWidth = 350f,
        pillWidth = 120f,
        compactHeight = 40f,
        contentHeight = 80f,
        expandedFraction = fraction,
        roundness = 16f,
    )
}
