package io.github.abdulwahab9d.isle.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.abdulwahab9d.isle.core.AnchorRequest
import io.github.abdulwahab9d.isle.core.IsleFrame
import io.github.abdulwahab9d.isle.core.IsleTiming
import io.github.abdulwahab9d.isle.core.Tone

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

/** Stub. The runtime agent wires this to [io.github.abdulwahab9d.isle.runtime.reduce]. */
class IsleController {
    var frame: IsleFrame by mutableStateOf(IsleFrame())
        private set

    fun flash(
        title: String,
        description: String? = null,
        tone: Tone = Tone.Success,
        durationMillis: Long = IsleTiming.FLASH_DURATION_MILLIS,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        anchor: AnchorRequest = AnchorRequest.Notch,
        id: String = newIsleId(),
    ) = Unit

    fun task(
        title: String,
        tone: Tone = Tone.Loading,
        anchor: AnchorRequest = AnchorRequest.Notch,
        id: String = newIsleId(),
        work: suspend () -> TaskResult,
    ) = Unit

    fun session(
        id: String,
        title: String,
        description: String? = null,
        tone: Tone = Tone.Info,
        progress: Float? = null,
        anchor: AnchorRequest = AnchorRequest.Notch,
        expanded: (@Composable () -> Unit)? = null,
    ): SessionHandle = NoopSession(id)

    fun dismiss(id: String) = Unit

    private class NoopSession(private val id: String) : SessionHandle {
        override fun update(
            title: String?,
            description: String?,
            tone: Tone?,
            progress: Float?,
        ) = Unit

        override fun dismiss() = Unit
    }
}

val LocalIsleController = staticCompositionLocalOf<IsleController> {
    error("IsleHost is not in the composition")
}

@Composable
fun rememberIsleController(): IsleController = IsleController()

internal fun newIsleId(): String = "isle"
