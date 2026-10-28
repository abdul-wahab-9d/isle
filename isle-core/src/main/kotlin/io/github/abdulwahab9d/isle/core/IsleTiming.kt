package io.github.abdulwahab9d.isle.core

/**
 * Same clock as Sileo: a toast lives 6s, opens at 150ms, and folds back to the
 * pill 2s before it leaves. A session does not use this clock.
 */
object IsleTiming {
    const val FLASH_DURATION_MILLIS = 6_000L
    const val EXPAND_DELAY_MILLIS = 150L
    const val COLLAPSE_BEFORE_END_MILLIS = 2_000L
    const val TASK_RESULT_DURATION_MILLIS = 6_000L
    const val TASK_RESULT_COLLAPSE_BEFORE_END_MILLIS = 2_000L
    const val SWIPE_DISMISS_DP = 30f
}
