package io.github.abdulwahab9d.isle.runtime

import io.github.abdulwahab9d.isle.core.IsleTiming
import org.junit.Assert.assertEquals
import org.junit.Test

class IsleTimingTest {
    @Test
    fun secondsBecomeMillis() {
        assertEquals(3_000L, IsleTiming.secondsToMillis(3.0))
        assertEquals(1_500L, IsleTiming.secondsToMillis(1.5))
        assertEquals(0L, IsleTiming.secondsToMillis(0.0))
    }

    @Test
    fun badSecondsUseTheDefaultLifetime() {
        assertEquals(IsleTiming.FLASH_DURATION_MILLIS, IsleTiming.secondsToMillis(-1.0))
        assertEquals(IsleTiming.FLASH_DURATION_MILLIS, IsleTiming.secondsToMillis(Double.NaN))
    }
}
