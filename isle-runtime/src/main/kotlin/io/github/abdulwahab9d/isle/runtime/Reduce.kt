package io.github.abdulwahab9d.isle.runtime

import io.github.abdulwahab9d.isle.core.Content
import io.github.abdulwahab9d.isle.core.Foreground
import io.github.abdulwahab9d.isle.core.IsleEvent
import io.github.abdulwahab9d.isle.core.IsleState
import io.github.abdulwahab9d.isle.core.IsleTiming
import io.github.abdulwahab9d.isle.core.SessionRecord

fun reduce(state: IsleState, event: IsleEvent): IsleState = when (event) {
    is IsleEvent.ShowFlash -> state.copy(
        foreground = Foreground.Flash(
            content = event.content,
            ageMillis = 0L,
            durationMillis = event.durationMillis,
        ),
    )

    is IsleEvent.StartTask -> state.copy(
        foreground = Foreground.TaskRunning(content = event.content),
    )

    is IsleEvent.FinishTask -> finishTask(state, event)
    is IsleEvent.UpsertSession -> upsertSession(state, event)
    is IsleEvent.SetSessionExpanded -> setSessionExpanded(state, event)
    is IsleEvent.Dismiss -> dismiss(state, event.id)
    is IsleEvent.Tick -> tick(state, event.deltaMillis)
}

fun isExpanded(state: IsleState): Boolean = when (val foreground = state.foreground) {
    is Foreground.Flash -> timedExpanded(
        content = foreground.content,
        ageMillis = foreground.ageMillis,
        durationMillis = foreground.durationMillis,
        collapseBeforeEndMillis = IsleTiming.COLLAPSE_BEFORE_END_MILLIS,
    )

    is Foreground.TaskResult -> timedExpanded(
        content = foreground.content,
        ageMillis = foreground.ageMillis,
        durationMillis = foreground.durationMillis,
        collapseBeforeEndMillis = IsleTiming.TASK_RESULT_COLLAPSE_BEFORE_END_MILLIS,
    )

    is Foreground.TaskRunning -> false
    null -> state.session?.expanded == true
}

fun visibleContent(state: IsleState): Content? =
    state.foreground?.content ?: state.session?.content

private fun finishTask(state: IsleState, event: IsleEvent.FinishTask): IsleState {
    val running = state.foreground as? Foreground.TaskRunning ?: return state
    if (running.content.id != event.id) return state
    return state.copy(
        foreground = Foreground.TaskResult(
            content = event.result,
            ageMillis = 0L,
            durationMillis = event.durationMillis,
        ),
    )
}

private fun upsertSession(state: IsleState, event: IsleEvent.UpsertSession): IsleState {
    val expanded = state.session
        ?.takeIf { it.content.id == event.content.id }
        ?.expanded
        ?: false
    return state.copy(session = SessionRecord(content = event.content, expanded = expanded))
}

private fun setSessionExpanded(state: IsleState, event: IsleEvent.SetSessionExpanded): IsleState {
    val session = state.session ?: return state
    if (session.content.id != event.id) return state
    if (session.expanded == event.expanded) return state
    return state.copy(session = session.copy(expanded = event.expanded))
}

private fun dismiss(state: IsleState, id: String): IsleState {
    val session = state.session?.takeUnless { it.content.id == id }
    val foreground = state.foreground?.takeUnless { it.content.id == id }
    if (session === state.session && foreground === state.foreground) return state
    return state.copy(session = session, foreground = foreground)
}

private fun tick(state: IsleState, deltaMillis: Long): IsleState {
    if (deltaMillis <= 0L) return state
    return when (val foreground = state.foreground) {
        is Foreground.Flash -> ageTimed(
            state = state,
            ageMillis = foreground.ageMillis,
            durationMillis = foreground.durationMillis,
            deltaMillis = deltaMillis,
            aged = { foreground.copy(ageMillis = it) },
        )

        is Foreground.TaskResult -> ageTimed(
            state = state,
            ageMillis = foreground.ageMillis,
            durationMillis = foreground.durationMillis,
            deltaMillis = deltaMillis,
            aged = { foreground.copy(ageMillis = it) },
        )

        is Foreground.TaskRunning, null -> state
    }
}

private fun ageTimed(
    state: IsleState,
    ageMillis: Long,
    durationMillis: Long,
    deltaMillis: Long,
    aged: (Long) -> Foreground,
): IsleState {
    val age = ageMillis + deltaMillis
    if (age >= durationMillis) return state.copy(foreground = null)
    return state.copy(foreground = aged(age))
}

private fun timedExpanded(
    content: Content,
    ageMillis: Long,
    durationMillis: Long,
    collapseBeforeEndMillis: Long,
): Boolean {
    if (!content.hasBody()) return false
    val collapseAt = durationMillis - collapseBeforeEndMillis
    if (collapseAt <= IsleTiming.EXPAND_DELAY_MILLIS) return true
    return ageMillis >= IsleTiming.EXPAND_DELAY_MILLIS && ageMillis < collapseAt
}

private fun Content.hasBody(): Boolean =
    description != null || actionLabel != null || hasCustomBody
