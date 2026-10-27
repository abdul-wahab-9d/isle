package io.github.abdulwahab9d.isle.runtime

import io.github.abdulwahab9d.isle.core.AnchorRequest
import io.github.abdulwahab9d.isle.core.Content
import io.github.abdulwahab9d.isle.core.Foreground
import io.github.abdulwahab9d.isle.core.IsleEvent
import io.github.abdulwahab9d.isle.core.IsleState
import io.github.abdulwahab9d.isle.core.IsleTiming
import io.github.abdulwahab9d.isle.core.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReduceTest {

    @Test
    fun case1_flashWithDescriptionExpandsThenLeaves() {
        val duration = IsleTiming.FLASH_DURATION_MILLIS
        val collapseAt = duration - IsleTiming.COLLAPSE_BEFORE_END_MILLIS
        var state = reduce(IsleState(), showFlash("f", description = "Hello", durationMillis = duration))

        assertEquals(0L, flashAge(state))
        assertFalse(isExpanded(state))
        assertEquals("f", visibleContent(state)?.id)

        state = tick(state, IsleTiming.EXPAND_DELAY_MILLIS)
        assertEquals(IsleTiming.EXPAND_DELAY_MILLIS, flashAge(state))
        assertTrue(isExpanded(state))

        state = tick(state, collapseAt - IsleTiming.EXPAND_DELAY_MILLIS - 1L)
        assertEquals(collapseAt - 1L, flashAge(state))
        assertTrue(isExpanded(state))

        state = tick(state, 1L)
        assertEquals(collapseAt, flashAge(state))
        assertFalse(isExpanded(state))
        assertEquals("f", visibleContent(state)?.id)

        state = tick(state, duration - collapseAt)
        assertNull(state.foreground)
    }

    @Test
    fun case2_sessionReturnsCompactAfterFlashLeaves() {
        var state = reduce(IsleState(), session("s"))
        assertFalse(state.session!!.expanded)

        state = reduce(state, showFlash("f"))
        assertEquals("f", visibleContent(state)?.id)
        assertEquals("s", state.session?.content?.id)

        state = tick(state, IsleTiming.FLASH_DURATION_MILLIS)
        assertNull(state.foreground)
        assertEquals("s", visibleContent(state)?.id)
        assertFalse(state.session!!.expanded)
        assertFalse(isExpanded(state))
    }

    @Test
    fun case3_newerFlashReplacesForeground() {
        var state = reduce(IsleState(), showFlash("f1"))
        state = tick(state, 1_000L)
        state = reduce(state, showFlash("f2"))

        assertEquals("f2", visibleContent(state)?.id)
        assertEquals(0L, flashAge(state))
        assertTrue(state.foreground is Foreground.Flash)

        state = tick(state, 500L)
        state = reduce(state, showFlash("f2", description = "Replaced"))
        assertEquals(0L, flashAge(state))
        assertEquals("Replaced", visibleContent(state)?.description)
    }

    @Test
    fun case4_newerSessionDoesNotClearFlash() {
        var state = reduce(IsleState(), showFlash("f"))
        state = tick(state, 40L)
        state = reduce(state, session("s1", title = "First"))
        state = reduce(state, session("s2", title = "Second"))

        assertEquals("f", state.foreground?.content?.id)
        assertEquals(40L, flashAge(state))
        assertEquals("s2", state.session?.content?.id)
        assertEquals("Second", state.session?.content?.title)
        assertEquals("f", visibleContent(state)?.id)
        assertFalse(state.session!!.expanded)
    }

    @Test
    fun case5_finishTaskBecomesResultThenLeaves() {
        val duration = IsleTiming.TASK_RESULT_DURATION_MILLIS
        val collapseAt = duration - IsleTiming.TASK_RESULT_COLLAPSE_BEFORE_END_MILLIS
        var state = reduce(
            IsleState(),
            IsleEvent.StartTask(content("task", title = "Saving", description = "Please wait", tone = Tone.Loading)),
        )
        assertTrue(state.foreground is Foreground.TaskRunning)
        assertFalse(isExpanded(state))
        assertEquals(state, tick(state, duration))

        state = reduce(
            state,
            IsleEvent.FinishTask(
                id = "task",
                result = content("task", title = "Saved", description = "All set", tone = Tone.Success),
                durationMillis = duration,
            ),
        )
        val result = state.foreground as Foreground.TaskResult
        assertEquals("Saved", result.content.title)
        assertEquals(Tone.Success, result.content.tone)
        assertEquals(0L, result.ageMillis)
        assertEquals(duration, result.durationMillis)
        assertFalse(isExpanded(state))

        state = tick(state, IsleTiming.EXPAND_DELAY_MILLIS)
        assertTrue(isExpanded(state))

        state = tick(state, 600L - IsleTiming.EXPAND_DELAY_MILLIS)
        assertEquals(600L, (state.foreground as Foreground.TaskResult).ageMillis)
        assertTrue(isExpanded(state))

        state = tick(state, collapseAt - 600L)
        assertEquals(collapseAt, (state.foreground as Foreground.TaskResult).ageMillis)
        assertFalse(isExpanded(state))

        state = tick(state, duration - collapseAt)
        assertNull(state.foreground)
    }

    @Test
    fun case6_sessionSurvivesTaskResult() {
        var state = reduce(IsleState(), session("s", title = "Transfer"))
        state = reduce(state, IsleEvent.SetSessionExpanded(id = "s", expanded = true))
        state = reduce(state, IsleEvent.StartTask(content("task", title = "Uploading", tone = Tone.Loading)))
        assertEquals("task", visibleContent(state)?.id)
        assertEquals("s", state.session?.content?.id)

        state = reduce(
            state,
            IsleEvent.FinishTask(
                id = "task",
                result = content("task", title = "Uploaded", tone = Tone.Success),
                durationMillis = IsleTiming.TASK_RESULT_DURATION_MILLIS,
            ),
        )
        state = tick(state, IsleTiming.TASK_RESULT_DURATION_MILLIS)

        assertNull(state.foreground)
        assertEquals("s", visibleContent(state)?.id)
        assertEquals("Transfer", state.session?.content?.title)
        assertTrue(state.session!!.expanded)
        assertTrue(isExpanded(state))
    }

    @Test
    fun case7_sameSessionIdKeepsExpanded() {
        var state = reduce(IsleState(), session("s", title = "Before"))
        state = reduce(state, IsleEvent.SetSessionExpanded(id = "s", expanded = true))
        assertEquals(state, reduce(state, IsleEvent.SetSessionExpanded(id = "other", expanded = false)))

        state = reduce(state, session("s", title = "After"))
        assertEquals("After", state.session?.content?.title)
        assertTrue(state.session!!.expanded)
        assertTrue(isExpanded(state))

        state = reduce(state, showFlash("f"))
        state = reduce(state, session("s", title = "Under flash"))
        assertEquals("Under flash", state.session?.content?.title)
        assertTrue(state.session!!.expanded)
        assertEquals("f", visibleContent(state)?.id)

        state = reduce(state, session("s2", title = "New"))
        assertEquals("s2", state.session?.content?.id)
        assertFalse(state.session!!.expanded)
        assertEquals("f", state.foreground?.content?.id)
    }

    @Test
    fun case8_dismissSessionDuringFlashKeepsFlash() {
        var state = reduce(IsleState(), showFlash("f", description = "Notice"))
        state = reduce(state, session("s", title = "Parked"))
        state = reduce(state, IsleEvent.Dismiss(id = "s"))

        assertNull(state.session)
        assertEquals("f", state.foreground?.content?.id)
        assertEquals("f", visibleContent(state)?.id)
    }

    @Test
    fun case9_finishTaskForDifferentIdDoesNotChangeState() {
        val running = reduce(IsleState(), IsleEvent.StartTask(content("task", tone = Tone.Loading)))
        val ignored = reduce(
            running,
            IsleEvent.FinishTask(
                id = "other",
                result = content("other", title = "Nope", tone = Tone.Success),
                durationMillis = IsleTiming.TASK_RESULT_DURATION_MILLIS,
            ),
        )
        assertEquals(running, ignored)
        assertTrue(ignored.foreground is Foreground.TaskRunning)

        val flashed = reduce(IsleState(), showFlash("f"))
        assertEquals(
            flashed,
            reduce(
                flashed,
                IsleEvent.FinishTask(
                    id = "f",
                    result = content("f", title = "Nope", tone = Tone.Success),
                    durationMillis = IsleTiming.TASK_RESULT_DURATION_MILLIS,
                ),
            ),
        )

        val finished = reduce(
            running,
            IsleEvent.FinishTask(
                id = "task",
                result = content("task", title = "Done", tone = Tone.Success),
                durationMillis = IsleTiming.TASK_RESULT_DURATION_MILLIS,
            ),
        )
        assertEquals(
            finished,
            reduce(
                finished,
                IsleEvent.FinishTask(
                    id = "task",
                    result = content("task", title = "Again", tone = Tone.Error),
                    durationMillis = 50,
                ),
            ),
        )
    }

    @Test
    fun dismissClearsEverySlotWithThatId() {
        val shared = content("same", title = "Shared")
        var state = reduce(IsleState(), IsleEvent.UpsertSession(shared))
        state = reduce(
            state,
            IsleEvent.ShowFlash(content = shared, durationMillis = IsleTiming.FLASH_DURATION_MILLIS),
        )
        state = reduce(state, IsleEvent.Dismiss(id = "same"))
        assertNull(state.session)
        assertNull(state.foreground)

        var parked = reduce(IsleState(), session("s"))
        parked = reduce(parked, showFlash("f"))
        parked = reduce(parked, IsleEvent.Dismiss(id = "f"))
        assertNull(parked.foreground)
        assertEquals("s", visibleContent(parked)?.id)

        assertEquals(parked, reduce(parked, IsleEvent.Dismiss(id = "missing")))
    }

    @Test
    fun flashWithoutBodyStaysCompact() {
        val openedAt = IsleTiming.EXPAND_DELAY_MILLIS
        val duration = IsleTiming.FLASH_DURATION_MILLIS

        var plain = reduce(IsleState(), showFlash("plain", description = null, durationMillis = duration))
        plain = tick(plain, openedAt)
        assertFalse(isExpanded(plain))

        var action = reduce(
            IsleState(),
            showFlash("action", description = null, actionLabel = "Undo", durationMillis = duration),
        )
        action = tick(action, openedAt)
        assertTrue(isExpanded(action))

        var custom = reduce(
            IsleState(),
            showFlash("custom", description = null, hasCustomBody = true, durationMillis = duration),
        )
        custom = tick(custom, openedAt)
        assertTrue(isExpanded(custom))
    }

    @Test
    fun shortDurationStaysExpandedUntilItLeaves() {
        val flashDuration = IsleTiming.EXPAND_DELAY_MILLIS + IsleTiming.COLLAPSE_BEFORE_END_MILLIS
        assertFalse(flashDuration - IsleTiming.COLLAPSE_BEFORE_END_MILLIS > IsleTiming.EXPAND_DELAY_MILLIS)

        var flash = reduce(
            IsleState(),
            showFlash("short", description = "Body", durationMillis = flashDuration),
        )
        assertTrue(isExpanded(flash))
        flash = tick(flash, flashDuration - 1L)
        assertTrue(flash.foreground is Foreground.Flash)
        assertTrue(isExpanded(flash))
        flash = tick(flash, 1L)
        assertNull(flash.foreground)

        val resultDuration = IsleTiming.EXPAND_DELAY_MILLIS + IsleTiming.TASK_RESULT_COLLAPSE_BEFORE_END_MILLIS
        var task = reduce(IsleState(), IsleEvent.StartTask(content("task", tone = Tone.Loading)))
        task = reduce(
            task,
            IsleEvent.FinishTask(
                id = "task",
                result = content("task", description = "Body", tone = Tone.Success),
                durationMillis = resultDuration,
            ),
        )
        assertTrue(isExpanded(task))
        task = tick(task, resultDuration - 1L)
        assertTrue(task.foreground is Foreground.TaskResult)
        assertTrue(isExpanded(task))
        task = tick(task, 1L)
        assertNull(task.foreground)

        val justLongEnough = flashDuration + 1L
        val collapseAt = justLongEnough - IsleTiming.COLLAPSE_BEFORE_END_MILLIS
        assertTrue(collapseAt > IsleTiming.EXPAND_DELAY_MILLIS)
        var edge = reduce(
            IsleState(),
            showFlash("edge", description = "Body", durationMillis = justLongEnough),
        )
        assertFalse(isExpanded(edge))
        edge = tick(edge, IsleTiming.EXPAND_DELAY_MILLIS)
        assertTrue(isExpanded(edge))
        edge = tick(edge, collapseAt - IsleTiming.EXPAND_DELAY_MILLIS)
        assertEquals(collapseAt, flashAge(edge))
        assertFalse(isExpanded(edge))
    }

    @Test
    fun tickIgnoresNonPositiveDelta() {
        val state = reduce(IsleState(), showFlash("f", description = "Hello"))
        val aged = tick(state, IsleTiming.EXPAND_DELAY_MILLIS)
        assertEquals(aged, tick(aged, 0L))
        assertEquals(aged, tick(aged, -10L))
    }

    private fun showFlash(
        id: String,
        description: String? = null,
        actionLabel: String? = null,
        hasCustomBody: Boolean = false,
        durationMillis: Long = IsleTiming.FLASH_DURATION_MILLIS,
    ) = IsleEvent.ShowFlash(
        content = content(
            id = id,
            description = description,
            actionLabel = actionLabel,
            hasCustomBody = hasCustomBody,
        ),
        durationMillis = durationMillis,
    )

    private fun session(id: String, title: String = id) =
        IsleEvent.UpsertSession(content(id, title = title))

    private fun tick(state: IsleState, deltaMillis: Long): IsleState =
        reduce(state, IsleEvent.Tick(deltaMillis))

    private fun flashAge(state: IsleState): Long =
        (state.foreground as Foreground.Flash).ageMillis

    private fun content(
        id: String,
        title: String = id,
        description: String? = null,
        tone: Tone = Tone.Info,
        actionLabel: String? = null,
        progress: Float? = null,
        hasCustomBody: Boolean = false,
        anchor: AnchorRequest = AnchorRequest.Notch,
    ) = Content(
        id = id,
        title = title,
        description = description,
        tone = tone,
        actionLabel = actionLabel,
        progress = progress,
        hasCustomBody = hasCustomBody,
        anchor = anchor,
    )
}
