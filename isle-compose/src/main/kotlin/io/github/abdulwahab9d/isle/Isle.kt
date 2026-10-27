package io.github.abdulwahab9d.isle

/** Colors and meaning for a capsule. */
typealias Tone = io.github.abdulwahab9d.isle.core.Tone

/** Where the capsule should park. Notch is the default. */
typealias AnchorRequest = io.github.abdulwahab9d.isle.core.AnchorRequest

/** What a finished task shows before it leaves. */
typealias TaskResult = io.github.abdulwahab9d.isle.controller.TaskResult

/** A session that stays until the app dismisses it. */
typealias SessionHandle = io.github.abdulwahab9d.isle.controller.SessionHandle

/** Controller for the surrounding [IsleHost]. */
val LocalIsleController = io.github.abdulwahab9d.isle.controller.LocalIsleController

object Isle {
    const val VERSION: String = "0.1.0"
}
