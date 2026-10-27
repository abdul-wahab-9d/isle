package io.github.abdulwahab9d.isle.runtime

import io.github.abdulwahab9d.isle.core.Content
import io.github.abdulwahab9d.isle.core.IsleEvent
import io.github.abdulwahab9d.isle.core.IsleState

/** Stub. The runtime agent replaces these with the SPEC.md two-slot rules. */
fun reduce(state: IsleState, event: IsleEvent): IsleState = state

fun isExpanded(state: IsleState): Boolean = false

fun visibleContent(state: IsleState): Content? =
    state.foreground?.content ?: state.session?.content
