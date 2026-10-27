package io.github.abdulwahab9d.isle.core

enum class Tone {
    Loading,
    Success,
    Error,
    Warning,
    Info,
}

enum class AnchorRequest {
    Notch,
    TopCenter,
}

data class Content(
    val id: String,
    val title: String,
    val description: String?,
    val tone: Tone,
    val actionLabel: String?,
    val progress: Float?,
    val hasCustomBody: Boolean,
    val anchor: AnchorRequest,
)

sealed interface Foreground {
    val content: Content

    data class Flash(
        override val content: Content,
        val ageMillis: Long,
        val durationMillis: Long,
    ) : Foreground

    data class TaskRunning(
        override val content: Content,
    ) : Foreground

    data class TaskResult(
        override val content: Content,
        val ageMillis: Long,
        val durationMillis: Long,
    ) : Foreground
}

data class SessionRecord(
    val content: Content,
    val expanded: Boolean,
)

data class IsleState(
    val session: SessionRecord? = null,
    val foreground: Foreground? = null,
)

sealed interface IsleEvent {
    data class ShowFlash(
        val content: Content,
        val durationMillis: Long,
    ) : IsleEvent

    data class StartTask(
        val content: Content,
    ) : IsleEvent

    data class FinishTask(
        val id: String,
        val result: Content,
        val durationMillis: Long,
    ) : IsleEvent

    data class UpsertSession(
        val content: Content,
    ) : IsleEvent

    data class SetSessionExpanded(
        val id: String,
        val expanded: Boolean,
    ) : IsleEvent

    data class Dismiss(
        val id: String,
    ) : IsleEvent

    data class Tick(
        val deltaMillis: Long,
    ) : IsleEvent
}
