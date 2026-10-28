package io.github.abdulwahab9d.isle.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import io.github.abdulwahab9d.isle.core.AnchorRequest
import io.github.abdulwahab9d.isle.core.Content
import io.github.abdulwahab9d.isle.core.Foreground
import io.github.abdulwahab9d.isle.core.IsleEvent
import io.github.abdulwahab9d.isle.core.IsleFrame
import io.github.abdulwahab9d.isle.core.IsleState
import io.github.abdulwahab9d.isle.core.IsleTiming
import io.github.abdulwahab9d.isle.core.Tone
import io.github.abdulwahab9d.isle.runtime.isExpanded
import io.github.abdulwahab9d.isle.runtime.reduce
import io.github.abdulwahab9d.isle.runtime.visibleContent
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TaskResult(
    val title: String,
    val description: String? = null,
    val tone: Tone,
    val durationMillis: Long = IsleTiming.TASK_RESULT_DURATION_MILLIS,
)

interface SessionHandle {
    fun update(
        title: String? = null,
        description: String? = null,
        tone: Tone? = null,
        progress: Float? = null,
    )

    fun dismiss()
}

/** Host-facing controller. [frame], [action], and [customBody] describe the visible slot. */
class IsleController internal constructor(
    private val scope: CoroutineScope,
) {
    constructor() : this(CoroutineScope(SupervisorJob()))

    var frame: IsleFrame by mutableStateOf(IsleFrame())
        private set

    val action: (() -> Unit)?
        get() = actionSlot

    val customBody: (@Composable () -> Unit)?
        get() = customBodySlot

    private var actionSlot: (() -> Unit)? by mutableStateOf<(() -> Unit)?>(null)
    private var customBodySlot: (@Composable () -> Unit)? by mutableStateOf<(@Composable () -> Unit)?>(null)
    private var state: IsleState = IsleState()
    private val actions = mutableMapOf<String, () -> Unit>()
    private val customBodies = mutableMapOf<String, @Composable () -> Unit>()
    private val jobs = mutableMapOf<String, Job>()
    private var ticker: Job? = null

    fun flash(
        title: String,
        description: String? = null,
        tone: Tone = Tone.Success,
        durationMillis: Long = IsleTiming.FLASH_DURATION_MILLIS,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        anchor: AnchorRequest = AnchorRequest.Notch,
        id: String = newIsleId(),
        expanded: (@Composable () -> Unit)? = null,
    ) {
        if (actionLabel != null && onAction != null) {
            actions[id] = onAction
        } else {
            actions.remove(id)
        }
        if (expanded != null) {
            customBodies[id] = expanded
        } else {
            customBodies.remove(id)
        }
        apply(
            IsleEvent.ShowFlash(
                content = slotContent(
                    id = id,
                    title = title,
                    description = description,
                    tone = tone,
                    actionLabel = actionLabel,
                    hasCustomBody = expanded != null,
                    anchor = anchor,
                ),
                durationMillis = durationMillis,
            ),
        )
    }

    fun task(
        title: String,
        tone: Tone = Tone.Loading,
        anchor: AnchorRequest = AnchorRequest.Notch,
        id: String = newIsleId(),
        work: suspend () -> TaskResult,
    ) {
        apply(
            IsleEvent.StartTask(
                content = slotContent(
                    id = id,
                    title = title,
                    tone = tone,
                    anchor = anchor,
                ),
            ),
        )
        val previous = jobs[id]
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val self = coroutineContext[Job] ?: return@launch
            try {
                val result = work()
                if (jobs[id] !== self) return@launch
                coroutineContext.ensureActive()
                apply(finishTask(id, anchor, result))
            } catch (cancelled: CancellationException) {
                if (jobs[id] === self) {
                    jobs.remove(id, self)
                    apply(IsleEvent.Dismiss(id))
                }
                throw cancelled
            } catch (failure: Exception) {
                if (jobs[id] !== self) return@launch
                apply(failTask(id, anchor, failure))
            } finally {
                if (jobs[id] === self) jobs.remove(id, self)
            }
        }
        jobs[id] = job
        job.start()
        if (job.isCancelled && jobs[id] === job) {
            jobs.remove(id, job)
            apply(IsleEvent.Dismiss(id))
        }
        previous?.cancel()
    }

    fun session(
        id: String,
        title: String,
        description: String? = null,
        tone: Tone = Tone.Info,
        progress: Float? = null,
        anchor: AnchorRequest = AnchorRequest.Notch,
        expanded: (@Composable () -> Unit)? = null,
    ): SessionHandle {
        if (expanded != null) {
            customBodies[id] = expanded
        } else {
            customBodies.remove(id)
        }
        apply(
            IsleEvent.UpsertSession(
                content = slotContent(
                    id = id,
                    title = title,
                    description = description,
                    tone = tone,
                    progress = progress,
                    hasCustomBody = expanded != null,
                    anchor = anchor,
                ),
            ),
        )
        return object : SessionHandle {
            override fun update(
                title: String?,
                description: String?,
                tone: Tone?,
                progress: Float?,
            ) {
                val current = state.session?.takeIf { it.content.id == id }?.content ?: return
                apply(
                    IsleEvent.UpsertSession(
                        content = current.copy(
                            title = title ?: current.title,
                            description = description ?: current.description,
                            tone = tone ?: current.tone,
                            progress = progress ?: current.progress,
                        ),
                    ),
                )
            }

            override fun dismiss() {
                this@IsleController.dismiss(id)
            }
        }
    }

    fun dismiss(id: String) {
        val job = jobs.remove(id)
        apply(IsleEvent.Dismiss(id))
        job?.cancel()
    }

    /** Toggles the stored session when that session is the visible slot. */
    fun toggleExpanded() {
        if (state.foreground != null) return
        val session = state.session ?: return
        apply(IsleEvent.SetSessionExpanded(id = session.content.id, expanded = !session.expanded))
    }

    private fun apply(event: IsleEvent) {
        val previousForegroundId = state.foreground?.content?.id
        val previousSessionId = state.session?.content?.id
        state = reduce(state, event)
        val foregroundId = state.foreground?.content?.id
        val sessionId = state.session?.content?.id
        if (previousForegroundId != null &&
            previousForegroundId != foregroundId &&
            previousForegroundId != sessionId
        ) {
            actions.remove(previousForegroundId)
            customBodies.remove(previousForegroundId)
        }
        if (previousSessionId != null &&
            previousSessionId != sessionId &&
            previousSessionId != foregroundId
        ) {
            actions.remove(previousSessionId)
            customBodies.remove(previousSessionId)
        }
        publish()
        syncTicker()
    }

    private fun publish() {
        val content = visibleContent(state)
        actionSlot = content?.let { slot -> slot.actionLabel?.let { actions[slot.id] } }
        customBodySlot = content?.takeIf { it.hasCustomBody }?.let { customBodies[it.id] }
        frame = IsleFrame(content = content, expanded = isExpanded(state))
    }

    private fun syncTicker() {
        if (!ticks(state.foreground)) {
            ticker?.cancel()
            ticker = null
            return
        }
        if (ticker?.isActive == true) return
        if (scope.coroutineContext[MonotonicFrameClock] == null) return
        ticker = scope.launch { runClock() }
    }

    private suspend fun runClock() {
        val origin = withFrameNanos { it }
        var agedMillis = 0L
        while (kotlin.coroutines.coroutineContext.isActive && ticks(state.foreground)) {
            val now = withFrameNanos { it }
            if (!kotlin.coroutines.coroutineContext.isActive || !ticks(state.foreground)) break
            val totalMillis = (now - origin) / 1_000_000L
            val delta = totalMillis - agedMillis
            if (delta > 0L) {
                agedMillis = totalMillis
                apply(IsleEvent.Tick(delta))
            }
        }
    }

    private fun finishTask(id: String, anchor: AnchorRequest, result: TaskResult): IsleEvent.FinishTask =
        IsleEvent.FinishTask(
            id = id,
            result = slotContent(
                id = id,
                title = result.title,
                description = result.description,
                tone = result.tone,
                anchor = anchor,
            ),
            durationMillis = result.durationMillis,
        )

    private fun failTask(id: String, anchor: AnchorRequest, failure: Exception): IsleEvent.FinishTask =
        IsleEvent.FinishTask(
            id = id,
            result = slotContent(
                id = id,
                title = failure.message?.takeIf { it.isNotBlank() } ?: "Failed",
                tone = Tone.Error,
                anchor = anchor,
            ),
            durationMillis = IsleTiming.TASK_RESULT_DURATION_MILLIS,
        )

    private fun slotContent(
        id: String,
        title: String,
        tone: Tone,
        anchor: AnchorRequest,
        description: String? = null,
        actionLabel: String? = null,
        progress: Float? = null,
        hasCustomBody: Boolean = false,
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

val LocalIsleController = staticCompositionLocalOf<IsleController> {
    error("IsleHost is not in the composition")
}

@Composable
fun rememberIsleController(): IsleController {
    val scope = rememberCoroutineScope()
    return remember { IsleController(scope) }
}

private val nextIsleId = AtomicLong(0)

internal fun newIsleId(): String = "isle-${nextIsleId.incrementAndGet()}"

private fun ticks(foreground: Foreground?): Boolean =
    foreground is Foreground.Flash || foreground is Foreground.TaskResult
